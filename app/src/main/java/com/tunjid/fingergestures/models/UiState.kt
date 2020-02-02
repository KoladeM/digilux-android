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

package com.tunjid.fingergestures.models

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.dynamicanimation.animation.SpringAnimation

data class UiState(
        @MenuRes val toolBarMenu: Int,
        val toolbarShows: Boolean,
        val toolbarInvalidated: Boolean,
        val toolbarTitle: CharSequence,
        @DrawableRes val fabIcon: Int,
        val fabShows: Boolean,
        val fabExtended: Boolean,
        val fabText: CharSequence,
        val snackbarText: CharSequence,
        @ColorInt val navBarColor: Int,
        @ColorInt val statusBarColor: Int,
        val showsBottomNav: Boolean,
        val fabClickListener: View.OnClickListener?,
        val fabTransitionOptions: (SpringAnimation.() -> Unit)?
) : Parcelable {

    fun diff(newState: UiState,
             showsBottomNavConsumer: (Boolean) -> Unit,
             showsFabConsumer: (Boolean) -> Unit,
             showsToolbarConsumer: (Boolean) -> Unit,
             navBarColorConsumer: (Int) -> Unit,
             statusBarConsumer: (Int) -> Unit,
             fabStateConsumer: (Int, CharSequence) -> Unit,
             fabExtendedConsumer: (Boolean) -> Unit,
             snackbarTextConsumer: (CharSequence) -> Unit,
             toolbarStateConsumer: (Int, Boolean, CharSequence) -> Unit,
             fabClickListenerConsumer: (View.OnClickListener?) -> Unit,
             fabTransitionOptionConsumer: ((SpringAnimation.() -> Unit)?) -> Unit
    ): UiState {

        fabClickListenerConsumer.invoke(newState.fabClickListener)
        fabTransitionOptionConsumer.invoke(newState.fabTransitionOptions)

        onChanged(newState, UiState::toolBarMenu, UiState::toolbarInvalidated, UiState::toolbarTitle) {
            toolbarStateConsumer(toolBarMenu, toolbarInvalidated, toolbarTitle)
        }

        onChanged(newState, UiState::fabIcon, UiState::fabText) { fabStateConsumer(fabIcon, fabText) }
        onChanged(newState, UiState::showsBottomNav) { showsBottomNavConsumer(showsBottomNav) }
        onChanged(newState, UiState::fabShows) { showsFabConsumer(fabShows) }
        onChanged(newState, UiState::fabExtended) { fabExtendedConsumer(fabExtended) }
        onChanged(newState, UiState::snackbarText) { snackbarTextConsumer(snackbarText) }
        onChanged(newState, UiState::toolbarShows) { showsToolbarConsumer(toolbarShows) }
        onChanged(newState, UiState::navBarColor) { navBarColorConsumer(navBarColor) }
        onChanged(newState, UiState::statusBarColor) { statusBarConsumer(statusBarColor) }

        return newState
    }

    private inline fun onChanged(that: UiState, vararg selectors: (UiState) -> Any?, invocation: UiState.() -> Unit) {
        if (selectors.any { it(this) != it(that) }) invocation.invoke(that)
    }

    private constructor(`in`: Parcel) : this(
            toolBarMenu = `in`.readInt(),
            toolbarShows = `in`.readByte().toInt() != 0x00,
            toolbarInvalidated = `in`.readByte().toInt() != 0x00,
            toolbarTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`),
            fabIcon = `in`.readInt(),
            fabShows = `in`.readByte().toInt() != 0x00,
            fabExtended = `in`.readByte().toInt() != 0x00,
            fabText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`),
            snackbarText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`),
            navBarColor = `in`.readInt(),
            statusBarColor = `in`.readInt(),
            showsBottomNav = `in`.readByte().toInt() != 0x00,
            fabClickListener = null,
            fabTransitionOptions = null
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(toolBarMenu)
        dest.writeByte((if (toolbarShows) 0x01 else 0x00).toByte())
        dest.writeByte((if (toolbarInvalidated) 0x01 else 0x00).toByte())
        TextUtils.writeToParcel(toolbarTitle, dest, 0)
        dest.writeInt(fabIcon)
        dest.writeByte((if (fabShows) 0x01 else 0x00).toByte())
        dest.writeByte((if (fabExtended) 0x01 else 0x00).toByte())
        TextUtils.writeToParcel(fabText, dest, 0)
        TextUtils.writeToParcel(snackbarText, dest, 0)
        dest.writeInt(navBarColor)
        dest.writeInt(statusBarColor)
    }

    companion object {

        fun freshState(): UiState = UiState(
                fabIcon = 0,
                fabText = "",
                toolBarMenu = 0,
                navBarColor = Color.BLACK,
                statusBarColor = Color.TRANSPARENT,
                showsBottomNav = true,
                fabShows = true,
                fabExtended = true,
                toolbarShows = true,
                snackbarText = "",
                toolbarInvalidated = false,
                toolbarTitle = "",
                fabClickListener = null,
                fabTransitionOptions = null
        )

        @JvmField
        @Suppress("unused")
        val CREATOR: Parcelable.Creator<UiState> = object : Parcelable.Creator<UiState> {
            override fun createFromParcel(`in`: Parcel): UiState = UiState(`in`)

            override fun newArray(size: Int): Array<UiState?> = arrayOfNulls(size)
        }
    }
}