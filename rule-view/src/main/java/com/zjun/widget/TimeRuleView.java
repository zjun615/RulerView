package com.zjun.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.lang.reflect.Field;
import java.util.List;

/**
 * TimeRuleView
 *
 * 时间尺控件
 * （仿“萤石云视频”里的时间选择控件）
 *
 * 功能：
 *  - 可选择一天（00:00 ~ 24:00）内的任一时刻，精确到秒级
 *  - 可显示多个时间块
 *  - 支持滑动及惯性滑动
 *  - 支持缩放时间间隔
 *  - 支持滑动与缩放的连续切换
 *
 * 思路：
 *  - 时间绘制思路参考{@link RuleView}
 *  - 时间缩放，采用缩放手势检测器 ScaleGestureDetector
 *  - 缩放的等级估算方式：进入默认比例为1，根据每隔所占的秒数与宽度，可估算出每个等级的宽度范围，再与默认等级对应的宽度相除，即可算出缩放比例
 *  - 惯性滑动，使用速度追踪器 VelocityTracker
 *  - 缩放与滑动之间的连续操作，ScaleGestureDetector 开始与结束的条件是第二个手指按下与松开，
 *    所以onTouchEvent()中应该使用 getActionMasked()来监听第二个手指的 DOWN(ACTION_POINTER_DOWN) 与 UP(ACTION_POINTER_UP) 事件，
 *    MOVE 都是一样的
 *  - 时间块，由起始时间与终止时间组成，采用一个有序的集合来装入即可
 *
 * Author: Ralap
 * Description:
 * Date 2018/8/11
 */
public class TimeRuleView extends View {

    private static final boolean LOG_ENABLE = BuildConfig.DEBUG;
    public static final int MAX_TIME_VALUE = 24 * 3600;
    
    private int bgColor;
    /**
     * 刻度颜色
     */
    private int gradationColor;
    /**
     * 时间块的高度
     */
    private float partHeight;
    /**
     * 时间块的颜色
     */
    private int partColor;
    /**
     * 刻度宽度
     */
    private float gradationWidth;
    /**
     * 秒、分、时刻度的长度
     */
    private float secondLen;
    private float minuteLen;
    private float hourLen;
    /**
     * 刻度数值颜色、大小、与时刻度的距离
     */
    private int gradationTextColor;
    private float gradationTextSize;
    private float gradationTextGap;

    /**
     * 当前时间，单位：s
     */
    private @IntRange(from = 0, to = MAX_TIME_VALUE) int currentTime;
    /**
     * 指针颜色
     */
    private int indicatorColor;
    /**
     * 指针上三角形的边长
     */
    private float indicatorTriangleSideLen;
    /**
     * 指针的宽度
     */
    private float indicatorWidth;
    
    /**
     * 最小单位对应的单位秒数值，一共四级: 10s、1min、5min、15min
     * 与 {@link #mPerTextCounts} 和 {@link #mPerCountScaleThresholds} 对应的索引值
     *
     * 可以组合优化成数组
     */
    private static int[] mUnitSeconds = {
            10,     10,     10,     10,
            60,     60,
            5*60,   5*60,
            15 * 60, 15 * 60, 15 * 60, 15 * 60, 15 * 60, 15 * 60
    };

    /**
     * 数值显示间隔。一共13级，第一级最大值，不包括
     */
    @SuppressWarnings("all")
    private static int[] mPerTextCounts = {
            60,         60,         2 * 60,     4 * 60, // 10s/unit: 最大值, 1min, 2min, 4min
            5 * 60,     10 * 60, // 1min/unit: 5min, 10min
            20 * 60,    30 * 60, // 5min/unit: 20min, 30min
            3600,       2 * 3600,   3 * 3600,   4 * 3600,   5 * 3600,   6 * 3600 // 15min/unit
    };

    /**
     * 与 {@link #mPerTextCounts} 对应的阈值，在此阈值与前一个阈值之间，则使用此阈值对应的间隔数值
     * 如：1.5f 代表 4*60 对应的阈值，如果 mScale >= 1.5f && mScale < 1.8f，则使用 4*60
     *
     * 这些数值，都是估算出来的
     */
    @SuppressWarnings("all")
    private float[] mPerCountScaleThresholds = {
            6f,     3.6f,   1.8f,   1.5f, // 10s/unit: 最大值, 1min, 2min, 4min
            0.8f,     0.4f,   // 1min/unit: 5min, 10min
            0.25f,  0.125f, // 5min/unit: 20min, 30min
            0.07f,  0.04f,  0.03f,  0.025f, 0.02f,  0.015f // 15min/unit: 1h, 2h, 3h, 4h, 5h, 6h
    };
    /**
     * 默认mScale为1
     */
    private float mScale = 1;
    /**
     * 1s对应的间隔，比较好估算
     */
    private final float mOneSecondGap = dp2px(12) / 60f;
    /**
     * 当前最小单位秒数值对应的间隔
     */
    private float mUnitGap = mOneSecondGap * 60;
    /**
     * 默认索引值
     */
    private int mPerTextCountIndex = 4;
    /**
     * 一格代表的秒数。默认1min
     */
    private int mUnitSecond = mUnitSeconds[mPerTextCountIndex];
    
    /**
     * 数值文字宽度的一半：时间格式为“00:00”，所以长度固定
     */
    private final float mTextHalfWidth;

    private final int SCROLL_SLOP;
    private final int MIN_VELOCITY;
    private final int MAX_VELOCITY;
    
    /**
     * 当前时间与 00:00 的距离值
     */
    private float mCurrentDistance;


    private Paint mPaint;
    private TextPaint mTextPaint;
    private Path mTrianglePath;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    /**
     * 缩放手势检测器
     */
    private ScaleGestureDetector mScaleGestureDetector;

    private int mWidth, mHeight;
    private int mHalfWidth;

    private int mInitialX;
    private int mLastX, mLastY;
    private boolean isMoving;
    private boolean isScaling;

    private List<TimePart> mTimePartList;
    private OnTimeChangedListener mListener;

    public interface OnTimeChangedListener{
        void onTimeChanged(int newTimeValue);
    }

    /**
     * 时间片段
     */
    public static class TimePart{
        /**
         * 起始时间，单位：s，取值范围∈[0, 86399]
         *  0       —— 00:00:00
         *  86399   —— 23:59:59
         */
        public int startTime;

        /**
         * 结束时间，必须大于{@link #startTime}
         */
        public int endTime;
    }

    public TimeRuleView(Context context) {
        this(context, null);
    }

    public TimeRuleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeRuleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);

        init(context);
        initScaleGestureDetector(context);

        mTextHalfWidth = mTextPaint.measureText("00:00") * .5f;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        SCROLL_SLOP = viewConfiguration.getScaledTouchSlop();
        MIN_VELOCITY = viewConfiguration.getScaledMinimumFlingVelocity();
        MAX_VELOCITY = viewConfiguration.getScaledMaximumFlingVelocity();

        calculateValues();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TimeRuleView);
        bgColor = ta.getColor(R.styleable.TimeRuleView_zjun_bgColor, Color.parseColor("#EEEEEE"));
        gradationColor = ta.getColor(R.styleable.TimeRuleView_zjun_gradationColor, Color.GRAY);
        partHeight = ta.getDimension(R.styleable.TimeRuleView_trv_partHeight, dp2px(20));
        partColor = ta.getColor(R.styleable.TimeRuleView_trv_partColor, Color.parseColor("#F58D24"));
        gradationWidth = ta.getDimension(R.styleable.TimeRuleView_trv_gradationWidth, 1);
        secondLen = ta.getDimension(R.styleable.TimeRuleView_trv_secondLen, dp2px(3));
        minuteLen = ta.getDimension(R.styleable.TimeRuleView_trv_minuteLen, dp2px(5));
        hourLen = ta.getDimension(R.styleable.TimeRuleView_trv_hourLen, dp2px(10));
        gradationTextColor = ta.getColor(R.styleable.TimeRuleView_trv_gradationTextColor, Color.GRAY);
        gradationTextSize = ta.getDimension(R.styleable.TimeRuleView_trv_gradationTextSize, sp2px(12));
        gradationTextGap = ta.getDimension(R.styleable.TimeRuleView_trv_gradationTextGap, dp2px(2));
        currentTime = ta.getInt(R.styleable.TimeRuleView_trv_currentTime, 0);
        indicatorTriangleSideLen = ta.getDimension(R.styleable.TimeRuleView_trv_indicatorTriangleSideLen, dp2px(15));
        indicatorWidth = ta.getDimension(R.styleable.TimeRuleView_zjun_indicatorLineWidth, dp2px(1));
        indicatorColor = ta.getColor(R.styleable.TimeRuleView_zjun_indicatorLineColor, Color.RED);
        ta.recycle();
    }

    private void calculateValues() {
        mCurrentDistance = currentTime / mUnitSecond * mUnitGap;
    }

    private void init(Context context) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(gradationTextSize);
        mTextPaint.setColor(gradationTextColor);

        mTrianglePath = new Path();

        mScroller = new Scroller(context);
    }

    private void initScaleGestureDetector(Context context) {
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {

            /**
             * 缩放被触发(会调用0次或者多次)，
             * 如果返回 true 则表示当前缩放事件已经被处理，检测器会重新积累缩放因子
             * 返回 false 则会继续积累缩放因子。
             */
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                final float scaleFactor = detector.getScaleFactor();
                logD("onScale...focusX=%f, focusY=%f, scaleFactor=%f",
                        detector.getFocusX(), detector.getFocusY(), scaleFactor);

                final float maxScale = mPerCountScaleThresholds[0];
                final float minScale = mPerCountScaleThresholds[mPerCountScaleThresholds.length - 1];
                if (scaleFactor > 1 && mScale >= maxScale) {
                    // 已经放大到最大值
                    return true;
                } else if (scaleFactor < 1 && mScale <= minScale) {
                    // 已经缩小到最小值
                    return true;
                }

                mScale *= scaleFactor;
                mScale = Math.max(minScale, Math.min(maxScale, mScale));
                mPerTextCountIndex = findScaleIndex(mScale);

                mUnitSecond = mUnitSeconds[mPerTextCountIndex];
                mUnitGap = mScale * mOneSecondGap * mUnitSecond;
                logD("onScale: mScale=%f, mPerTextCountIndex=%d, mUnitSecond=%d, mUnitGap=%f",
                        mScale, mPerTextCountIndex, mUnitSecond, mUnitGap);

                mCurrentDistance = (float) currentTime / mUnitSecond * mUnitGap;
                invalidate();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                logD("onScaleBegin...");
                isScaling = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
                logD("onScaleEnd...");
            }
        });

        // 调整最小跨度值。默认值27mm(>=sw600dp的32mm)，太大了，效果不好
        Class clazz = ScaleGestureDetector.class;
        int newMinSpan = ViewConfiguration.get(context).getScaledTouchSlop();
        try {
            Field mMinSpanField = clazz.getDeclaredField("mMinSpan");
            mMinSpanField.setAccessible(true);
            mMinSpanField.set(mScaleGestureDetector, newMinSpan);
            mMinSpanField.setAccessible(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 二分法查找缩放值对应的索引值
     */
    private int findScaleIndex(float scale) {
        final int size = mPerCountScaleThresholds.length;
        int min = 0;
        int max = size - 1;
        int mid = (min + max) >> 1;
        while (!(scale >= mPerCountScaleThresholds[mid] && scale < mPerCountScaleThresholds[mid - 1])) {
            if (scale >= mPerCountScaleThresholds[mid - 1]) {
                // 因为值往小区，index往大取，所以不能为mid -1
                max = mid;
            } else {
                min = mid + 1;
            }
            mid = (min + max) >> 1;
            if (min >= max) {
                break;
            }
            if (mid == 0) {
                break;
            }
        }
        return mid;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        // 只处理wrap_content的高度，设置为80dp
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            mHeight = dp2px(60);
        }
        mHalfWidth = mWidth >> 1;

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        final int actionMasked = event.getActionMasked();
        final int action = event.getAction();
        final int pointerCount = event.getPointerCount();
        logD("onTouchEvent: isScaling=%b, actionIndex=%d, pointerId=%d, actionMasked=%d, action=%d, pointerCount=%d",
                isScaling, actionIndex, pointerId, actionMasked, action, pointerCount);
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        mScaleGestureDetector.onTouchEvent(event);

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                isMoving = false;
                mInitialX = x;
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 只要第二手指按下，就禁止滑动
                isScaling = true;
                isMoving = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isScaling) {
                    break;
                }
                int dx = x - mLastX;
                if (!isMoving) {
                    final int dy = y - mLastY;
                    if (Math.abs(x - mInitialX) <= SCROLL_SLOP || Math.abs(dx) <= Math.abs(dy)) {
                        break;
                    }
                    isMoving = true;
                }
                mCurrentDistance -= dx;
                computeTime();
                break;
            case MotionEvent.ACTION_UP:
                if (isScaling || !isMoving) {
                    break;
                }
                mVelocityTracker.computeCurrentVelocity(1000, MAX_VELOCITY);
                final int xVelocity = (int) mVelocityTracker.getXVelocity();
                if (Math.abs(xVelocity) >= MIN_VELOCITY) {
                    // 惯性滑动
                    final int maxDistance = (int) (MAX_TIME_VALUE / mUnitGap * mUnitGap);
                    mScroller.fling((int) mCurrentDistance, 0, -xVelocity, 0, 0, maxDistance, 0, 0);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // 两个中的有一个手指被抬起，允许滑动。同时把未抬起的手机当前位置赋给初始X
                isScaling = false;
                int restIndex = actionIndex == 0 ? 1 : 0;
                mInitialX = (int) event.getX(restIndex);
                break;
            default: break;
        }
        mLastX = x;
        mLastY = y;
        return true;
    }

    private void computeTime() {
        // 不用转float，肯定能整除
        float maxDistance = MAX_TIME_VALUE / mUnitSecond * mUnitGap;
        // 限定范围
        mCurrentDistance = Math.min(maxDistance, Math.max(0, mCurrentDistance));
        currentTime = (int) (mCurrentDistance / mUnitGap * mUnitSecond);
        if (mListener != null) {
            mListener.onTimeChanged(currentTime);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 背景
        canvas.drawColor(bgColor);

        // 刻度
        drawRule(canvas);

        // 时间段
        drawTimeParts(canvas);

        // 当前时间指针
        drawTimeIndicator(canvas);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mCurrentDistance = mScroller.getCurrX();
            computeTime();
        }
    }

    /**
     * 绘制刻度
     */
    private void drawRule(Canvas canvas) {
        // 移动画布坐标系
        canvas.save();
        canvas.translate(0, partHeight);
        mPaint.setColor(gradationColor);
        mPaint.setStrokeWidth(gradationWidth);

        // 刻度
        int start = 0;
        float offset = mHalfWidth - mCurrentDistance;
        final int perTextCount = mPerTextCounts[mPerTextCountIndex];
        while (start <= MAX_TIME_VALUE) {
            // 刻度
            if (start % 3600 == 0) {
                // 时刻度
                canvas.drawLine(offset, 0, offset, hourLen, mPaint);
            } else if (start % 60 == 0) {
                // 分刻度
                canvas.drawLine(offset, 0, offset, minuteLen, mPaint);
            } else{
                // 秒刻度
                canvas.drawLine(offset, 0, offset, secondLen, mPaint);
            }

            // 时间数值
            if (start % perTextCount == 0) {
                String text = formatTimeHHmm(start);
                canvas.drawText(text, offset - mTextHalfWidth, hourLen + gradationTextGap + gradationTextSize, mTextPaint);
            }

            start += mUnitSecond;
            offset += mUnitGap;
        }
        canvas.restore();
    }

    /**
     * 绘制当前时间指针
     */
    private void drawTimeIndicator(Canvas canvas) {
        // 指针
        mPaint.setColor(indicatorColor);
        mPaint.setStrokeWidth(indicatorWidth);
        canvas.drawLine(mHalfWidth, 0, mHalfWidth, mHeight, mPaint);

        // 正三角形
        if (mTrianglePath.isEmpty()) {
            //
            final float halfSideLen = indicatorTriangleSideLen * .5f;
            mTrianglePath.moveTo(mHalfWidth - halfSideLen, 0);
            mTrianglePath.rLineTo(indicatorTriangleSideLen, 0);
            mTrianglePath.rLineTo(-halfSideLen, (float) (Math.sin(Math.toRadians(60)) * halfSideLen));
            mTrianglePath.close();
        }
        mPaint.setStrokeWidth(1);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(mTrianglePath, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * 绘制时间段
     */
    private void drawTimeParts(Canvas canvas) {
        if (mTimePartList == null) {
            return;
        }
        // 不用矩形，直接使用直线绘制
        mPaint.setStrokeWidth(partHeight);
        mPaint.setColor(partColor);
        float start, end;
        final float halfPartHeight = partHeight * .5f;
        final float secondGap = mUnitGap / mUnitSecond;
        for (int i = 0, size = mTimePartList.size(); i < size; i++) {
            TimePart timePart = mTimePartList.get(i);
            start = mHalfWidth - mCurrentDistance + timePart.startTime * secondGap;
            end = mHalfWidth - mCurrentDistance + timePart.endTime * secondGap;
            canvas.drawLine(start, halfPartHeight, end, halfPartHeight, mPaint);
        }
    }

    /**
     * 格式化时间 HH:mm
     * @param timeValue 具体时间值
     * @return 格式化后的字符串，eg：3600 to 01:00
     */
    public static String formatTimeHHmm(@IntRange(from = 0, to = MAX_TIME_VALUE) int timeValue) {
        if (timeValue < 0){
            timeValue=0;
        }
        int hour = timeValue / 3600;
        int minute = timeValue % 3600 / 60;
        StringBuilder sb = new StringBuilder();
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour).append(':');
        if (minute < 10) {
            sb.append('0');
        }
        sb.append(minute);
        return sb.toString();
    }

    /**
     * 格式化时间 HH:mm:ss
     * @param timeValue 具体时间值
     * @return 格式化后的字符串，eg：3600 to 01:00:00
     */
    public static String formatTimeHHmmss(@IntRange(from = 0, to = MAX_TIME_VALUE) int timeValue) {
        int hour = timeValue / 3600;
        int minute = timeValue % 3600 / 60;
        int second = timeValue % 3600 % 60;
        StringBuilder sb = new StringBuilder();

        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour).append(':');

        if (minute < 10) {
            sb.append('0');
        }
        sb.append(minute);
        sb.append(':');

        if (second < 10) {
            sb.append('0');
        }
        sb.append(second);
        return sb.toString();
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    @SuppressWarnings("all")
    private void logD(String format, Object... args) {
        if (LOG_ENABLE) {
            Log.d("MoneySelectRuleView", String.format("zjun@" + format, args));
        }
    }

    /**
     * 设置时间变化监听事件
     * @param listener 监听回调
     */
    public void setOnTimeChangedListener(OnTimeChangedListener listener) {
        this.mListener = listener;
    }

    /**
     * 设置时间块（段）集合
     * @param timePartList 时间块集合
     */
    public void setTimePartList(List<TimePart> timePartList) {
        this.mTimePartList = timePartList;
        postInvalidate();
    }

    /**
     * 设置当前时间
     * @param currentTime 当前时间
     */
    public void setCurrentTime(@IntRange(from = 0, to = MAX_TIME_VALUE) int currentTime) {
        this.currentTime = currentTime;
        calculateValues();
        postInvalidate();
    }
    
}
