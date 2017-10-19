package com.tunjid.fingergestures.behaviors;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.List;

/**
 * Animates a {@link View} when a {@link Snackbar} appears.
 * <p>
 * Mostly identical to {@link android.support.design.widget.FloatingActionButton.Behavior}
 * <p>
 * Created by tj.dahunsi on 4/15/17.
 */
@SuppressWarnings("unused") // Constructed via xml
public class SnackBarBehavior extends CoordinatorLayout.Behavior<View> {

    private static final Interpolator fastOutSlowInInterpolator = new FastOutSlowInInterpolator();

    public SnackBarBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (child.getVisibility() == View.VISIBLE) {
            float translationY = this.getViewTranslationYForSnackbar(parent, child);
            child.setTranslationY(translationY);
        }
        return true;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout && child.getTranslationY() != 0.0F) {
            ViewCompat.animate(child).translationY(0.0F).scaleX(1.0F).scaleY(1.0F).alpha(1.0F)
                    .setInterpolator(fastOutSlowInInterpolator);
        }
    }

    private float getViewTranslationYForSnackbar(CoordinatorLayout parent, View child) {
        float minOffset = 0.0F;
        List dependencies = parent.getDependencies(child);
        int i = 0;

        for (int z = dependencies.size(); i < z; ++i) {
            View view = (View) dependencies.get(i);
            if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(child, view)) {
                minOffset = Math.min(minOffset, view.getTranslationY() - (float) view.getHeight());
            }
        }

        return minOffset;
    }
}
