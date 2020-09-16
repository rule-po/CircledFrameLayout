package com.ivjukic.circledframelayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Created by rule on 12/04/16.
 */
public class CircledFrameLayout extends FrameLayout {

    private float previousAnimatedProgress = 0;

    public interface ProgressAnimationListener {
        void onAnimationStart();

        void onAnimationEnd(float progress, boolean hasProgressChanged);

        void onDisabledActionStart();

        void onDisabledActionEnd();

        void onDisabledActionCanceled();
    }

    public interface ProgressListener{
        void onProgressChanged(float progress);
    }

    private Path path = new Path();

    //constants
    public static int MAX_PROGRESS = 100;
    public static int MIN_PROGRESS = 0;
    private static int PROGRESS_ANIM_DURATION = 1000;
    private static float DEFAULT_STROKE_WIDTH = 2.0f;
    private static int DEFAULT_SCALE_DURATION = 150;
    private static float DEFAULT_SCALE_FACTOR = 1.3f;
    private static float DEFAULT_FAKE_SCALE_FACTOR = 1.4f;
    private static int DEFAULT_INNER_FILL_COLOR = Color.TRANSPARENT;
    private Paint backgroundStrokePaint;
    private Paint foregroundStrokePaint;
    private Paint innerFillPaint;
    private RectF rectF;
    private RectF fullRectF;
    private int startAngle = -90;

    //Private variables
    ObjectAnimator progressAnimator;
    AnimatorSet scaleAnimatorSet = new AnimatorSet();
    AnimatorSet scaleUpAnimatorSet = new AnimatorSet();
    AnimatorSet scaleDownAnimatorSet = new AnimatorSet();

    private int lastProgress = MIN_PROGRESS;
    private int previousProgress = MIN_PROGRESS;


    //Properties
    protected int backgroundStrokeColor = Color.WHITE;
    private float backgroundStrokeWidth = Util.pxFromDp(getContext(), DEFAULT_STROKE_WIDTH);

    protected int foregroundStrokeColor = Color.BLUE;
    private float foregroundStrokeWidth = Util.pxFromDp(getContext(), DEFAULT_STROKE_WIDTH);

    private float initialForegroundStrokeWidth = Util.pxFromDp(getContext(), DEFAULT_STROKE_WIDTH);
    protected int innerFillColor = DEFAULT_INNER_FILL_COLOR;
    private float progress = 0;
    private boolean isUserInteractionEnabled = true;
    private boolean justScale = false;
    private boolean isSelected = false;

    private int scaleAnimationDuration = DEFAULT_SCALE_DURATION;
    private float scaleFactor = DEFAULT_SCALE_FACTOR;
    private float fakeScaleFactor = DEFAULT_FAKE_SCALE_FACTOR;
    private int progressAnimationDuration = PROGRESS_ANIM_DURATION;

    private boolean shouldStealLongPressGesture = false;

    ProgressAnimationListener progressAnimationListener;
    ProgressListener progressListener;


    //Constructor
    public CircledFrameLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CircledFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircledFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircledFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        rectF = new RectF();
        fullRectF = new RectF();
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CircledFrameLayout,
                0, 0);

        try {
            backgroundStrokeColor = a.getColor(R.styleable.CircledFrameLayout_cfl_background_border_color, Color.WHITE);
            foregroundStrokeColor = a.getColor(R.styleable.CircledFrameLayout_cfl_foreground_border_color, Color.BLUE);
            innerFillColor = a.getColor(R.styleable.CircledFrameLayout_cfl_inner_fill_color, Color.TRANSPARENT);
            backgroundStrokeWidth = a.getDimension(R.styleable.CircledFrameLayout_cfl_background_border_width, Util.pxFromDp(getContext(), DEFAULT_STROKE_WIDTH));
            foregroundStrokeWidth = a.getDimension(R.styleable.CircledFrameLayout_cfl_foreground_border_width, Util.pxFromDp(getContext(), DEFAULT_STROKE_WIDTH));
            progressAnimationDuration = a.getInt(R.styleable.CircledFrameLayout_cfl_progress_animation_duration, PROGRESS_ANIM_DURATION);
            scaleAnimationDuration = a.getInt(R.styleable.CircledFrameLayout_cfl_scale_animation_duration, DEFAULT_SCALE_DURATION);
            scaleFactor = a.getFloat(R.styleable.CircledFrameLayout_cfl_scale_factor, DEFAULT_SCALE_FACTOR);
            fakeScaleFactor = a.getFloat(R.styleable.CircledFrameLayout_cfl_fake_scale_factor, DEFAULT_FAKE_SCALE_FACTOR);
            isUserInteractionEnabled = a.getBoolean(R.styleable.CircledFrameLayout_cfl_user_interaction_enabled, true);
            initialForegroundStrokeWidth = foregroundStrokeWidth;
        } finally {
            a.recycle();
        }

        innerFillPaint = new Paint();
        innerFillPaint.setColor(innerFillColor);

        backgroundStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundStrokePaint.setColor(backgroundStrokeColor);
        backgroundStrokePaint.setStyle(Paint.Style.STROKE);
        backgroundStrokePaint.setStrokeWidth(backgroundStrokeWidth);

        foregroundStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foregroundStrokePaint.setColor(foregroundStrokeColor);
        foregroundStrokePaint.setStyle(Paint.Style.STROKE);
        foregroundStrokePaint.setStrokeWidth(foregroundStrokeWidth);

        scaleUpAnimatorSet.playTogether(ObjectAnimator.ofFloat(this, "scaleX", 1, scaleFactor),
                ObjectAnimator.ofFloat(this, "scaleY", 1, scaleFactor));
        scaleUpAnimatorSet.setDuration(scaleAnimationDuration);

        scaleDownAnimatorSet.playTogether(ObjectAnimator.ofFloat(this, "scaleX", scaleFactor, 1),
                ObjectAnimator.ofFloat(this, "scaleY", scaleFactor, 1));
        scaleDownAnimatorSet.setDuration(scaleAnimationDuration);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // compute the path
        float halfWidth = w / 2f;
        float halfHeight = h / 2f;
        path.reset();
        path.addCircle(halfWidth, halfHeight, Math.min(halfWidth, halfHeight), Path.Direction.CW);
        path.close();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int min = Math.min(width, height);
        setMeasuredDimension(min, min);

        float strokeWidth = Math.max(backgroundStrokeWidth, foregroundStrokeWidth);
        rectF.set(0 + strokeWidth / 2, 0 + strokeWidth / 2, min - strokeWidth / 2, min - strokeWidth / 2);
        fullRectF.set(0, 0, min, min);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(path);

        canvas.drawOval(fullRectF, innerFillPaint);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);

        float angle = 360 * progress / MAX_PROGRESS;
        canvas.drawOval(rectF, backgroundStrokePaint);
        canvas.drawArc(rectF, startAngle, angle, false, foregroundStrokePaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    PointF startPoint = new PointF();
    PointF currentPoint = new PointF();
    boolean interactionCanceled = false;
    boolean interactionStarted = false;
    Handler animHandler = new Handler(Looper.getMainLooper());

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isUserInteractionEnabled) {
            currentPoint.set(event.getX(), event.getY());
            if (justScale) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    interactionCanceled = false;
                    startPoint.set(event.getX(), event.getY());
                    scaleIfNeeded();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    interactionCanceled = true;
                    animHandler.removeCallbacksAndMessages(null);
                    if (progressAnimationListener != null && interactionStarted) {
                        progressAnimationListener.onDisabledActionEnd();
                        interactionStarted = false;
                    }
                    //
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    animHandler.removeCallbacksAndMessages(null);
                    interactionCanceled = true;
                    if (interactionStarted) {
                        if (progressAnimationListener != null)
                            progressAnimationListener.onDisabledActionCanceled();
                        scaleDownFake();
                        interactionStarted = false;
                    }
                }

            } else {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    interactionCanceled = false;
                    startPoint.set(event.getX(), event.getY());
                    scaleIfNeeded();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    interactionCanceled = true;
                    animHandler.removeCallbacksAndMessages(null);
                    if (interactionStarted) {
                        if (this.progress < MAX_PROGRESS && this.lastProgress == MAX_PROGRESS)
                            revertToLastFinalProgress();
                        else if (this.progress > MIN_PROGRESS && this.lastProgress == MIN_PROGRESS)
                            revertToLastFinalProgress();
                        interactionStarted = false;
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    interactionCanceled = true;
                    animHandler.removeCallbacksAndMessages(null);
                    if (interactionStarted) {
                        if (this.progress < MAX_PROGRESS && this.lastProgress == MAX_PROGRESS)
                            revertToLastFinalProgress();
                        else if (this.progress > MIN_PROGRESS && this.lastProgress == MIN_PROGRESS)
                            revertToLastFinalProgress();
                        interactionStarted = false;
                    }
                }
            }
            return !interactionCanceled;
        }
        return false;
    }

    private void scaleIfNeeded() {
        animHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (interactionCanceled)
                    return;
                if (Math.sqrt(Math.pow(currentPoint.x - startPoint.x, 2) + Math.pow(currentPoint.y - startPoint.y, 2)) > Util.pxFromDp(getContext(), 20)) {
                    return;
                }
                interactionStarted = true;
                if (justScale) {
                    scaleUpFake();
                } else {
                    animateProgressAutomatically();
                }
            }
        }, 500);
    }



    public void scaleUp() {
        if (scaleUpAnimatorSet.isRunning())
            scaleUpAnimatorSet.cancel();
        scaleUpAnimatorSet.start();
    }

    public void scaleDown() {
        if (scaleDownAnimatorSet.isRunning())
            scaleDownAnimatorSet.cancel();
        scaleDownAnimatorSet.start();
    }

    private void animateProgressAutomatically() {
        if ((progressAnimator != null && progressAnimator.isRunning()) || (scaleAnimatorSet != null && scaleAnimatorSet.isRunning()))
            return;

        float finalProgress;
        if (this.progress == MIN_PROGRESS) {
            finalProgress = MAX_PROGRESS;
        } else {
            finalProgress = MIN_PROGRESS;
        }

        progressAnimator = ObjectAnimator.ofFloat(this, "progress", progress, finalProgress);
        progressAnimator.setDuration(progressAnimationDuration);
        progressAnimator.addListener(animatorListenerAdapter);
        progressAnimator.start();
    }

    public void animateProgress(int from, int to, long duration) {
        if ((progressAnimator != null && progressAnimator.isRunning()) || (scaleAnimatorSet != null && scaleAnimatorSet.isRunning()))
            return;
        progressAnimator = ObjectAnimator.ofFloat(this, "progress", from, to);
        progressAnimator.setDuration(duration);
        progressAnimator.addListener(animatorListenerAdapter);
        progressAnimator.start();
    }

    private void revertToLastFinalProgress() {
        if (progressAnimator != null) {
            progressAnimator.removeAllListeners();
            progressAnimator.cancel();
        }
        progressAnimator = ObjectAnimator.ofFloat(this, "progress", progress, lastProgress);
        progressAnimator.addListener(cancelListenerAdapter);
        progressAnimator.start();
    }

    private void startScaling() {
        float deltaWidth = initialForegroundStrokeWidth / 3.f;
        scaleAnimatorSet.playTogether(ObjectAnimator.ofFloat(this, "scaleX", 1, 1.1f),
                ObjectAnimator.ofFloat(this, "scaleY", 1, 1.1f),
                ObjectAnimator.ofFloat(this, "foregroundStrokeWidth", initialForegroundStrokeWidth,
                        initialForegroundStrokeWidth + deltaWidth));
        scaleAnimatorSet.start();
    }

    private void stopScaling() {
        float deltaWidth = initialForegroundStrokeWidth / 3.f;
        scaleAnimatorSet.playTogether(ObjectAnimator.ofFloat(this, "scaleX", 1.1f, 1f),
                ObjectAnimator.ofFloat(this, "scaleY", 1.1f, 1f),
                ObjectAnimator.ofFloat(this, "foregroundStrokeWidth", initialForegroundStrokeWidth + deltaWidth,
                        initialForegroundStrokeWidth));

        scaleAnimatorSet.start();
    }

    public void scaleDownInstantly() {
        this.setScaleX(1);
        this.setScaleY(1);
    }

    private void scaleUpFake() {
        scaleAnimatorSet = new AnimatorSet();
        scaleAnimatorSet.playTogether(ObjectAnimator.ofFloat(this, "scaleX", 1, fakeScaleFactor),
                ObjectAnimator.ofFloat(this, "scaleY", 1, fakeScaleFactor));
        scaleAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (progressAnimationListener != null)
                    progressAnimationListener.onDisabledActionStart();
            }
        });
        scaleAnimatorSet.start();
    }

    private void scaleDownFake() {
        scaleAnimatorSet = new AnimatorSet();
        scaleAnimatorSet.playTogether(ObjectAnimator.ofFloat(this, "scaleX", fakeScaleFactor, 1),
                ObjectAnimator.ofFloat(this, "scaleY", fakeScaleFactor, 1));
        scaleAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }
        });
        scaleAnimatorSet.start();
    }

    private void scaleUpDown() {
        scaleAnimatorSet = new AnimatorSet();
        scaleAnimatorSet.playTogether(ObjectAnimator.ofFloat(this, "scaleX", 1, fakeScaleFactor, 1),
                ObjectAnimator.ofFloat(this, "scaleY", 1, fakeScaleFactor, 1));
        scaleAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (progressAnimationListener != null)
                    progressAnimationListener.onDisabledActionStart();
            }
        });
        scaleAnimatorSet.start();
    }

    AnimatorListenerAdapter animatorListenerAdapter = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            if (progressAnimationListener != null)
                progressAnimationListener.onAnimationStart();
            startScaling();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (progressAnimationListener != null)
                progressAnimationListener.onAnimationEnd(lastProgress, previousProgress != lastProgress);
            stopScaling();
        }
    };

    AnimatorListenerAdapter cancelListenerAdapter = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (progressAnimationListener != null)
                progressAnimationListener.onAnimationEnd(lastProgress, previousProgress != lastProgress);
            progressAnimator.removeAllListeners();
            stopScaling();
        }
    };

    private static class Util {
        static float pxFromDp(Context context, float dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        }
    }

    //SETTERS & GETTERS
    public void setJustScale(boolean justScale) {
        this.justScale = justScale;
    }

    public void setProgressAnimationListener(ProgressAnimationListener progressAnimationListener) {
        this.progressAnimationListener = progressAnimationListener;
    }

    public void setUserInteractionEnabled(boolean isUserInteractionEnabled) {
        this.isUserInteractionEnabled = isUserInteractionEnabled;
    }

    @Override
    public void setSelected(boolean selected) {
        isSelected = selected;
        if(isSelected){
            if (progress >= MAX_PROGRESS){
                this.innerFillColor = foregroundStrokeColor;
            }else {
                this.innerFillColor = backgroundStrokeColor;
            }
        }else {
            this.innerFillColor = DEFAULT_INNER_FILL_COLOR;
        }
        innerFillPaint.setColor(this.innerFillColor);
        invalidate();
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    public void setStartAngle(int startAngle) {
        this.startAngle = startAngle;
    }

    public void setProgressAnimationDuration(int progressAnimationDuration) {
        this.progressAnimationDuration = progressAnimationDuration;
    }

    public void setScaleAnimationDuration(int scaleAnimationDuration) {
        this.scaleAnimationDuration = scaleAnimationDuration;
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public void setFakeScaleFactor(float fakeScaleFactor) {
        this.fakeScaleFactor = fakeScaleFactor;
    }

    public void setBackgroundStrokeColor(int backgroundStrokeColor) {
        this.backgroundStrokeColor = backgroundStrokeColor;
        backgroundStrokePaint.setColor(backgroundStrokeColor);
        invalidate();
    }

    public void setInnerFillColor(int innerFillColor) {
        this.innerFillColor = innerFillColor;
        innerFillPaint.setColor(this.innerFillColor);
        invalidate();
    }

    public void setForegroundStrokeColor(int foregroundStrokeColor) {
        this.foregroundStrokeColor = foregroundStrokeColor;
        foregroundStrokePaint.setColor(foregroundStrokeColor);
        invalidate();
    }

    public void setBackgroundStrokeWidth(float backgroundStrokeWidth) {
        this.backgroundStrokeWidth = backgroundStrokeWidth;
        this.backgroundStrokePaint.setStrokeWidth(backgroundStrokeWidth);
        invalidate();
    }

    public void setForegroundStrokeWidth(float foregroundStrokeWidth) {
        final int min = Math.min(getWidth(), getHeight());
        this.foregroundStrokeWidth = foregroundStrokeWidth;
        rectF.set(0 + foregroundStrokeWidth / 2, 0 + foregroundStrokeWidth / 2, min - foregroundStrokeWidth / 2, min - foregroundStrokeWidth / 2);
        foregroundStrokePaint.setStrokeWidth(foregroundStrokeWidth);
        invalidate();
    }

    public void setInitialForegroundStrokeWidth(float initialForegroundStrokeWidth) {
        this.initialForegroundStrokeWidth = initialForegroundStrokeWidth;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        if (progress <= MIN_PROGRESS) {
            previousProgress = lastProgress;
            lastProgress = MIN_PROGRESS;
            this.progress = MIN_PROGRESS;
        } else if (progress >= MAX_PROGRESS) {
            previousProgress = lastProgress;
            lastProgress = MAX_PROGRESS;
            this.progress = MAX_PROGRESS;
        }else {
            this.progress = progress;
        }
        if (progressListener != null){
            progressListener.onProgressChanged(progress);
        }
        invalidate();
    }

    public void setProgressAnimate(float progress, boolean animate, long delay, long duration) {
        if (progress == this.progress) return;

        if (animate) {
            if (progressAnimator != null && progressAnimator.isRunning())
                return;
            progressAnimator = ObjectAnimator.ofFloat(this, "progress", this.previousAnimatedProgress, progress);
            progressAnimator.setDuration(duration);
            progressAnimator.setStartDelay(delay);
            progressAnimator.start();
            this.previousAnimatedProgress = progress;
        } else {
            setProgress(progress);
            this.previousAnimatedProgress = progress;
        }
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public boolean shouldStealLongPressGesture() {
        return shouldStealLongPressGesture;
    }

    public void setShouldStealLongPressGesture(boolean shouldStealLongPressGesture) {
        this.shouldStealLongPressGesture = shouldStealLongPressGesture;
    }

    // SAVE STATE

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.progressState = progress;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        progress = ss.progressState;
    }

    private static class SavedState extends BaseSavedState {
        float progressState;

        public SavedState(Parcel source) {
            super(source);
            progressState = source.readFloat();
        }

        private SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(progressState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
