/*
 * android-spinnerwheel
 * https://github.com/ai212983/android-spinnerwheel
 *
 * based on
 *
 * Android Wheel Control.
 * https://code.google.com/p/android-wheel/
 *
 * Copyright 2011 Yuri Kanivets
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package antistatic.spinnerwheel;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;


/**
 * Abstract spinner spinnerwheel view.
 * This class should be subclassed.
 *
 * @author Yuri Kanivets
 * @author Dimitri Fedorov
 */
public abstract class AbstractWheelView extends AbstractWheel {

    private static int itemID = -1;

    @SuppressWarnings("unused")
    private final String LOG_TAG = AbstractWheelView.class.getName() + " #" + (++itemID);

    //----------------------------------
    //  Default properties values
    //----------------------------------

    private static final int DEF_ITEMS_DIMMED_ALPHA = 50; // 60 in ICS

    private static final int DEF_SELECTION_DIVIDER_ACTIVE_ALPHA = 50;

    private static final int DEF_SELECTION_DIVIDER_DIMMED_ALPHA = 255;

    private static final int DEF_ITEM_OFFSET_PERCENT = 10;

    private static final int DEF_ITEM_PADDING = 10;

    static final int DEF_SELECTION_DIVIDER_SIZE = 2;

    //----------------------------------
    //  Class properties
    //----------------------------------

    // configurable properties

    /**
     * The alpha of the selector spinnerwheel when it is dimmed.
     */
    int mItemsDimmedAlpha;

    /**
     * The alpha of separators spinnerwheel when they are shown.
     */
    private int mSelectionDividerActiveAlpha;

    /**
     * The alpha of separators when they are is dimmed.
     */
    private int mSelectionDividerDimmedAlpha;

    /**
     * Top and bottom items offset
     */
    int mItemOffsetPercent;

    /**
     * Left and right padding value
     */
    int mItemsPadding;

    /**
     * Divider for showing item to be selected while scrolling
     */
    Drawable mSelectionDivider;

    // the rest

    /**
     * The {@link android.graphics.Paint} for drawing the selector.
     */
    Paint mSelectorWheelPaint;

    /**
     * The {@link android.graphics.Paint} for drawing the separators.
     */
    Paint mSeparatorsPaint;

    /**
     * {@link com.nineoldandroids.animation.Animator} for dimming the selector spinnerwheel.
     */
    private Animator mDimSelectorWheelAnimator;

    /**
     * {@link com.nineoldandroids.animation.Animator} for dimming the selector spinnerwheel.
     */
    private Animator mDimSeparatorsAnimator;

    /**
     * The property for setting the selector paint.
     */
    private static final String PROPERTY_SELECTOR_PAINT_COEFF = "selectorPaintCoeff";

    /**
     * The property for setting the separators paint.
     */
    private static final String PROPERTY_SEPARATORS_PAINT_ALPHA = "separatorsPaintAlpha";


    Bitmap mSpinBitmap;
    Bitmap mSeparatorsBitmap;


    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    public AbstractWheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //--------------------------------------------------------------------------
    //
    //  Initiating assets and setters for paints
    //
    //--------------------------------------------------------------------------

    @Override
    protected void initAttributes(AttributeSet attrs, int defStyle) {
        super.initAttributes(attrs, defStyle);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AbstractWheelView, defStyle, 0);
        mItemsDimmedAlpha = a.getInt(R.styleable.AbstractWheelView_itemsDimmedAlpha, DEF_ITEMS_DIMMED_ALPHA);
        mSelectionDividerActiveAlpha = a.getInt(R.styleable.AbstractWheelView_selectionDividerActiveAlpha, DEF_SELECTION_DIVIDER_ACTIVE_ALPHA);
        mSelectionDividerDimmedAlpha = a.getInt(R.styleable.AbstractWheelView_selectionDividerDimmedAlpha, DEF_SELECTION_DIVIDER_DIMMED_ALPHA);
        mItemOffsetPercent = a.getInt(R.styleable.AbstractWheelView_itemOffsetPercent, DEF_ITEM_OFFSET_PERCENT);
        mItemsPadding = a.getDimensionPixelSize(R.styleable.AbstractWheelView_itemsPadding, DEF_ITEM_PADDING);
        mSelectionDivider = a.getDrawable(R.styleable.AbstractWheelView_selectionDivider);
        a.recycle();
    }

    @Override
    protected void initData(Context context) {
        super.initData(context);

        // creating animators
        mDimSelectorWheelAnimator = ObjectAnimator.ofFloat(this, PROPERTY_SELECTOR_PAINT_COEFF, 1, 0);

        mDimSeparatorsAnimator = ObjectAnimator.ofInt(this, PROPERTY_SEPARATORS_PAINT_ALPHA,
                mSelectionDividerActiveAlpha, mSelectionDividerDimmedAlpha
        );

        // creating paints
        mSeparatorsPaint = new Paint();
        mSeparatorsPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mSeparatorsPaint.setAlpha(mSelectionDividerDimmedAlpha);

        mSelectorWheelPaint = new Paint();
        mSelectorWheelPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    /**
     * Recreates assets (like bitmaps) when layout size has been changed
     *
     * @param width  New spinnerwheel width
     * @param height New spinnerwheel height
     */
    @Override
    protected void recreateAssets(int width, int height) {
        mSpinBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mSeparatorsBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        setSelectorPaintCoeff(0);
    }

    /**
     * Sets the <code>alpha</code> of the {@link Paint} for drawing separators
     * spinnerwheel.
     *
     * @param alpha alpha value from 0 to 255
     */
    @SuppressWarnings("unused")  // Called via reflection
    public void setSeparatorsPaintAlpha(int alpha) {
        mSeparatorsPaint.setAlpha(alpha);
        invalidate();
    }

    /**
     * Sets the <code>coeff</code> of the {@link Paint} for drawing
     * the selector spinnerwheel.
     *
     * @param coeff Coefficient from 0 (selector is passive) to 1 (selector is active)
     */
    protected abstract void setSelectorPaintCoeff(float coeff);


    //--------------------------------------------------------------------------
    //
    //  Processing scroller events
    //
    //--------------------------------------------------------------------------

    @Override
    protected void onScrollTouched() {
        mDimSelectorWheelAnimator.cancel();
        mDimSeparatorsAnimator.cancel();
        setSelectorPaintCoeff(1);
        setSeparatorsPaintAlpha(mSelectionDividerActiveAlpha);
    }

    @Override
    protected void onScrollTouchedUp() {
        super.onScrollTouchedUp();
        fadeSelectorWheel(750);
        lightSeparators(750);
    }

    @Override
    protected void onScrollFinished() {
        fadeSelectorWheel(500);
        lightSeparators(500);
    }

    //----------------------------------
    //  Animating components
    //----------------------------------

    /**
     * Fade the selector spinnerwheel via an animation.
     *
     * @param animationDuration The duration of the animation.
     */
    private void fadeSelectorWheel(long animationDuration) {
        mDimSelectorWheelAnimator.setDuration(animationDuration);
        mDimSelectorWheelAnimator.start();
    }

    /**
     * Fade the selector spinnerwheel via an animation.
     *
     * @param animationDuration The duration of the animation.
     */
    private void lightSeparators(long animationDuration) {
        mDimSeparatorsAnimator.setDuration(animationDuration);
        mDimSeparatorsAnimator.start();
    }


    //--------------------------------------------------------------------------
    //
    //  Layout measuring
    //
    //--------------------------------------------------------------------------

    /**
     * Perform layout measurements
     */
    abstract protected void measureLayout();


    //--------------------------------------------------------------------------
    //
    //  Drawing stuff
    //
    //--------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mViewAdapter != null && mViewAdapter.getItemsCount() > 0) {
            if (rebuildItems()) {
                measureLayout();
            }
            doItemsLayout();
            drawItems(canvas);
        }
    }

    /**
     * Draws items on specified canvas
     *
     * @param canvas the canvas for drawing
     */
    abstract protected void drawItems(Canvas canvas);
}
