package at.photosniper.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import at.photosniper.R;

public class SeekArc extends View {

    private static final String TAG = SeekArc.class.getSimpleName();
    private static final int INVALID_PROGRESS_VALUE = -1;

    // Attributes
    private int mArcRadius = 0;
    private Drawable mThumb;
    private int mThumbOffSet = 0;
    private int mMax = 100;
    private int mProgress = 0;
    // Progress width in dp
    private int mProgressWidth = 4;
    private int mArcWidth = 2;
    private int mStartAngle = 0;
    // The angle to draw the arc through, cannot be greater than 360;
    private int mSweepAngle = 360;
    // The rotation of the View from 12, clockwise
    private int mRotation = 0;
    private boolean mRoundedEdges = false;
    private boolean mTouchInside = true;

    // Internal variables
    private float mProgressSweep = 0;
    private final RectF mArcRect = new RectF();
    private Paint mArcPaint;
    private Paint mProgressPaint;
    private int mTranslateX;
    private int mTranslateY;
    private int mThumbXPos;
    private int mThumbYPos;
    private float mTouchIgnoreRadius;
    private OnSeekArcChangeListener mOnSeekArcChangeListener;

    public SeekArc(Context context) {
        super(context);
        init(context, null, 0);
    }

    public SeekArc(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SeekArc(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {

        Log.d(TAG, "Initialising SeekArc");
        final Resources res = getResources();
        float density = context.getResources().getDisplayMetrics().density;

        // Defaults, may need to link this into theme settings
        int arcColor = res.getColor(android.R.color.darker_gray);
        int progressColor = res.getColor(android.R.color.holo_blue_light);
        int thumbHalfheight = 0;
        int thumbHalfWidth = 0;
        mThumb = res.getDrawable(R.drawable.seek_arc_control_selector);
        // Convert progress width to pixels for current density
        mProgressWidth = (int) (mProgressWidth * density);

        if (attrs != null) {
            // Attribute initialization
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekArc, defStyle, 0);

            Drawable thumb = a.getDrawable(R.styleable.SeekArc_thumb);
            if (thumb != null) {
                mThumb = thumb;
            }

            thumbHalfheight = mThumb.getIntrinsicHeight() / 2;
            thumbHalfWidth = mThumb.getIntrinsicWidth() / 2;
            mThumb.setBounds(-thumbHalfWidth, -thumbHalfheight, thumbHalfWidth, thumbHalfheight);

            mMax = a.getInteger(R.styleable.SeekArc_max, mMax);
            mProgress = a.getInteger(R.styleable.SeekArc_progress, mProgress);
            mProgressWidth = (int) a.getDimension(R.styleable.SeekArc_progressWidth, mProgressWidth);
            mArcWidth = (int) a.getDimension(R.styleable.SeekArc_arcWidth, mArcWidth);
            mStartAngle = a.getInt(R.styleable.SeekArc_startAngle, mStartAngle);
            mSweepAngle = a.getInt(R.styleable.SeekArc_sweepAngle, mSweepAngle);
            mRotation = a.getInt(R.styleable.SeekArc_rotation, mRotation);
            mRoundedEdges = a.getBoolean(R.styleable.SeekArc_roundEdges, mRoundedEdges);
            mTouchInside = a.getBoolean(R.styleable.SeekArc_touchInside, mTouchInside);
            mThumbOffSet = a.getInteger(R.styleable.SeekArc_thumbOffset, mThumbOffSet);
            mArcRadius = (int) a.getDimension(R.styleable.SeekArc_arcRadius, mArcRadius);

            arcColor = a.getColor(R.styleable.SeekArc_arcColor, arcColor);
            progressColor = a.getColor(R.styleable.SeekArc_progressColor, progressColor);

            a.recycle();
        }

        mProgress = (mProgress > mMax) ? mMax : mProgress;
        mProgress = (mProgress < 0) ? 0 : mProgress;

        mSweepAngle = (mSweepAngle > 360) ? 360 : mSweepAngle;
        mSweepAngle = (mSweepAngle < 0) ? 0 : mSweepAngle;

        // TODO Could make this modulo of 360.
        mStartAngle = (mStartAngle > 360) ? 0 : mStartAngle;
        mStartAngle = (mStartAngle < 0) ? 0 : mStartAngle;

        mArcPaint = new Paint();
        mArcPaint.setColor(arcColor);
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);

        mProgressPaint = new Paint();
        mProgressPaint.setColor(progressColor);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mProgressWidth);

        if (mRoundedEdges) {
            mArcPaint.setStrokeCap(Paint.Cap.ROUND);
            mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        }

		/*
         * We want to ignore touches right in the center of the arc otherwise
		 * things become too jumpy.
		 */
        if (mTouchInside) {
            mTouchIgnoreRadius = (float) mArcRadius / 4;
        } else {
            // Don't use the exact radius makes interaction too tricky
            mTouchIgnoreRadius = mArcRadius - Math.min(thumbHalfWidth, thumbHalfheight);
        }

    }

    @Override
    protected void onFinishInflate() {
        //Log.d(TAG, "onFinishInflate startAngle: " + mStartAngle +  " Rotation:" + mRotation + " Radius:" + mArcRadius);
        super.onFinishInflate();
        int arcStart = mStartAngle + mRotation + 90;
        mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(arcStart)));
        mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(arcStart)));

    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Draw the arcs
        int mAngleOffset = -90;
        final int arcStart = mStartAngle + mAngleOffset + mRotation;
        final int arcSweep = mSweepAngle;
        canvas.drawArc(mArcRect, arcStart, arcSweep, false, mArcPaint);
        canvas.drawArc(mArcRect, arcStart, mProgressSweep, false, mProgressPaint);

        // Draw the thumb nail
        canvas.translate(mTranslateX, mTranslateY);
        canvas.translate(-mThumbXPos, -mThumbYPos);
        mThumb.draw(canvas);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int min = Math.min(width, height);
        float top = 0;
        float left = 0;
        int arcDiameter = 0;
        Log.d(TAG, "onMeasure width:" + width + " height:" + height);

        mTranslateX = (int) (width * 0.5f);
        mTranslateY = (int) (height * 0.5f);

        if (mArcRadius != 0) {
            arcDiameter = mArcRadius * 2;
            top = (height / 2 - arcDiameter / 2);
            left = (width / 2 - arcDiameter / 2);
            mArcRect.set(left, top, left + arcDiameter, top + arcDiameter);
        } else {
            arcDiameter = min - getPaddingLeft();
            mArcRadius = arcDiameter / 2;
            top = height / 2 - (arcDiameter / 2);
            left = width / 2 - (arcDiameter / 2);
            mArcRect.set(left, top, left + arcDiameter, top + arcDiameter);
        }

        //Log.d(TAG, "onMeasure startAngle: " + mStartAngle +  " Rotation:" + mRotation + " Radius:" + mArcRadius);
        int arcStart = (int) mProgressSweep + mStartAngle + mRotation + 90;
        mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(arcStart)));
        mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(arcStart)));

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onStartTrackingTouch();
                updateOnTouch(event);
                break;
            case MotionEvent.ACTION_MOVE:
                updateOnTouch(event);
                break;
            case MotionEvent.ACTION_UP:
                onStopTrackingTouch();
                setPressed(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                onStopTrackingTouch();
                setPressed(false);

                break;
        }

        return true;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mThumb != null && mThumb.isStateful()) {
            int[] state = getDrawableState();
            mThumb.setState(state);
        }
        invalidate();
    }

    private void onStartTrackingTouch() {
        if (mOnSeekArcChangeListener != null) {
            mOnSeekArcChangeListener.onStartTrackingTouch(this);
        }
    }

    private void onStopTrackingTouch() {
        if (mOnSeekArcChangeListener != null) {
            mOnSeekArcChangeListener.onStopTrackingTouch(this);
        }
    }

    private void updateOnTouch(MotionEvent event) {
        boolean ignoreTouch = ignoreTouch(event.getX(), event.getY());
        if (ignoreTouch) {
            return;
        }
        setPressed(true);
        double mTouchAngle = getTouchDegrees(event.getX(), event.getY());
        int progress = getProgressForAngle(mTouchAngle);
        onProgressRefresh(progress);
    }

    private boolean ignoreTouch(float xPos, float yPos) {
        boolean ignore = false;
        float x = xPos - mTranslateX;
        float y = yPos - mTranslateY;

        float touchRadius = (float) Math.sqrt(((x * x) + (y * y)));
        if (touchRadius < mTouchIgnoreRadius) {
            ignore = true;
        }
        return ignore;
    }

    private double getTouchDegrees(float xPos, float yPos) {
        float x = xPos - mTranslateX;
        float y = yPos - mTranslateY;
        // convert to arc Angle
        double angle = Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2) - Math.toRadians(mRotation));
        if (angle < 0) {
            angle = 360 + angle;
        }
        angle -= mStartAngle;

        return angle;
    }

    private int getProgressForAngle(double angle) {
        int touchProgress = (int) Math.round(valuePerDegree() * angle);

        touchProgress = (touchProgress < 0) ? INVALID_PROGRESS_VALUE : touchProgress;
        touchProgress = (touchProgress > mMax) ? INVALID_PROGRESS_VALUE : touchProgress;
        return touchProgress;
    }

    private float valuePerDegree() {
        return (float) mMax / mSweepAngle;
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's
     * progress level. Also provides notifications of when the user starts and
     * stops a touch gesture within the SeekBar.
     *
     * @param l The seek bar notification listener
     * @see SeekArc.OnSeekBarChangeListener
     */
    public void setOnSeekArcChangeListener(OnSeekArcChangeListener l) {
        mOnSeekArcChangeListener = l;
    }

    public void setProgress(int progress) {
        updateProgress(progress, false);
    }

    private void onProgressRefresh(int progress) {
        updateProgress(progress, true);
    }

    private void updateProgress(int progress, boolean fromUser) {

        if (progress == INVALID_PROGRESS_VALUE) {
            return;
        }

        if (mOnSeekArcChangeListener != null) {
            mOnSeekArcChangeListener.onProgressChanged(this, progress, fromUser);
        }

        progress = (progress > mMax) ? mMax : progress;
        progress = (mProgress < 0) ? 0 : progress;

        mProgress = progress;
        mProgressSweep = (float) progress / mMax * mSweepAngle;

        int thumbAngle = (int) (mStartAngle + mProgressSweep + mRotation + 90);

        mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(thumbAngle)));
        mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(thumbAngle)));

        invalidate();
    }

    public interface OnSeekArcChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the
         * fromUser parameter to distinguish user-initiated changes from those
         * that occurred programmatically.
         *
         * @param seekArc  The SeekArc whose progress has changed
         * @param progress The current progress level. This will be in the range
         *                 0..max where max was set by
         *                 {@link ProgressArc#setMax(int)}. (The default value for
         *                 max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may
         * want to use this to disable advancing the seekbar.
         *
         * @param seekArc The SeekArc in which the touch gesture began
         */
        void onStartTrackingTouch(SeekArc seekArc);

        /**
         * Notification that the user has finished a touch gesture. Clients may
         * want to use this to re-enable advancing the seekarc.
         *
         * @param seekArc The SeekArc in which the touch gesture began
         */
        void onStopTrackingTouch(SeekArc seekArc);
    }

}
