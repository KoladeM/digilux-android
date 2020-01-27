/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import com.tunjid.androidx.navigation.Navigator
import com.tunjid.androidx.view.util.InsetFlags
import com.tunjid.androidx.view.util.innermostFocusedChild
import com.tunjid.androidx.view.util.marginLayoutParams

class InsetLifecycleCallbacks(
        globalUiController: GlobalUiController,
        private val parentContainer: ViewGroup,
        private val fragmentContainer: FragmentContainerView,
        private val coordinatorLayout: CoordinatorLayout,
        private val toolbar: Toolbar,
        private val bottomNavView: View,
        private val stackNavigatorSource: () -> Navigator?
) : FragmentManager.FragmentLifecycleCallbacks(), GlobalUiController by globalUiController {

    private var leftInset: Int = 0
    private var rightInset: Int = 0
    private var insetsApplied: Boolean = false
    private var lastWindowInsets: WindowInsetsCompat? = null
    private var lastInsetDispatch: InsetDispatch? = InsetDispatch()

    private val bottomNavHeight get() = bottomNavView.height

    init {
        ViewCompat.setOnApplyWindowInsetsListener(parentContainer) { _, insets -> onInsetsApplied(insets) }
        bottomNavView.doOnLayout { lastWindowInsets?.let(this::consumeFragmentInsets) }
        fragmentContainer.bottomPaddingSpring {
            addEndListener { _, _, _, _ ->
                val input = fragmentContainer.innermostFocusedChild as? EditText
                        ?: return@addEndListener
                input.text = input.text // Scroll to text that has focus
            }
        }
    }

    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) =
            onFragmentViewCreated(v, f)

    private fun isNotInCurrentFragmentContainer(fragment: Fragment): Boolean =
            stackNavigatorSource()?.run { fragment.id != containerId } ?: true

    private fun onInsetsApplied(insets: WindowInsetsCompat): WindowInsetsCompat {
        if (this.insetsApplied) return insets

        topInset = insets.systemWindowInsetTop
        leftInset = insets.systemWindowInsetLeft
        rightInset = insets.systemWindowInsetRight
        bottomInset = insets.systemWindowInsetBottom

        toolbar.marginLayoutParams.topMargin = topInset
        bottomNavView.marginLayoutParams.bottomMargin = bottomInset

        adjustInsetForFragment(stackNavigatorSource()?.current)

        this.insetsApplied = true
        return insets
    }

    private fun onFragmentViewCreated(v: View, fragment: Fragment) {
        if (fragment !is InsetProvider || isNotInCurrentFragmentContainer(fragment)) return
        adjustInsetForFragment(fragment)

        ViewCompat.setOnApplyWindowInsetsListener(v) { _, insets -> consumeFragmentInsets(insets) }
    }

    private fun consumeFragmentInsets(insets: WindowInsetsCompat): WindowInsetsCompat = insets.apply {
        lastWindowInsets = this

        coordinatorLayout.ifBottomInsetChanged(coordinatorInsetReducer(systemWindowInsetBottom)) {
            bottomPaddingSpring().animateToFinalPosition(it.toFloat())
        }

        fragmentContainer.ifBottomInsetChanged(contentInsetReducer(systemWindowInsetBottom)) {
            bottomPaddingSpring().animateToFinalPosition(it.toFloat())
        }

        val current = stackNavigatorSource()?.current ?: return@apply
        if (isNotInCurrentFragmentContainer(current)) return@apply
        if (current !is InsetProvider) return@apply

        val large = systemWindowInsetBottom > bottomInset + bottomNavHeight.given(uiState.showsBottomNav)
        val bottom = if (large) bottomInset else fragmentInsetReducer(current.insetFlags)

        current.view?.apply { ifBottomInsetChanged(bottom) { updatePadding(bottom = it) } }

        return insets
    }

    @SuppressLint("InlinedApi")
    fun adjustInsetForFragment(fragment: Fragment?) {
        if (fragment !is InsetProvider || isNotInCurrentFragmentContainer(fragment)) return

        fragment.insetFlags.dispatch(fragment.tag) {
            if (insetFlags == null || lastInsetDispatch == this) return

            parentContainer.updatePadding(
                    left = this.leftInset given insetFlags.hasLeftInset,
                    right = this.rightInset given insetFlags.hasRightInset
            )

            fragment.view?.updatePadding(
                    top = topInset given insetFlags.hasTopInset,
                    bottom = fragmentInsetReducer(insetFlags)
            )

            lastInsetDispatch = this
        }
    }

    private inline fun InsetFlags.dispatch(tag: String?, receiver: InsetDispatch.() -> Unit) =
            receiver.invoke(InsetDispatch(tag, leftInset, topInset, rightInset, bottomInset, this))

    private fun contentInsetReducer(systemBottomInset: Int) =
            systemBottomInset - bottomInset

    private fun coordinatorInsetReducer(systemBottomInset: Int) =
            if (systemBottomInset > bottomInset) systemBottomInset
            else bottomInset + (bottomNavView.height given uiState.showsBottomNav)

    private fun fragmentInsetReducer(insetFlags: InsetFlags): Int {
        return bottomNavHeight.given(uiState.showsBottomNav) + bottomInset.given(insetFlags.hasBottomInset)
    }

    companion object {
        var topInset: Int = 0
        var bottomInset: Int = 0
    }

    private data class InsetDispatch(
            val tag: String? = null,
            val leftInset: Int = 0,
            val topInset: Int = 0,
            val rightInset: Int = 0,
            val bottomInset: Int = 0,
            val insetFlags: InsetFlags? = null
    )
}

private infix fun Int.given(flag: Boolean) = if (flag) this else 0

private fun View.ifBottomInsetChanged(newInset: Int, action: View.(Int) -> Unit) {
    if (paddingBottom != newInset) action(newInset)
}

private fun View.bottomPaddingSpring(modifier: SpringAnimation.() -> Unit = {}): SpringAnimation {
    return getTag(R.id.main_fragment_container) as? SpringAnimation ?: springAnimationOf(
            { updatePadding(bottom = it.toInt()); invalidate() },
            { paddingBottom.toFloat() },
            0F
    ).apply {
        setTag(R.id.main_fragment_container, this@apply)
        spring.stiffness = SpringForce.STIFFNESS_MEDIUM
        spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        modifier(this)
    }
}

interface InsetProvider {
    val insetFlags: InsetFlags
}