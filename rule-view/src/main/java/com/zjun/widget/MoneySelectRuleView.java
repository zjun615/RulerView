package com.zjun.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

/**
 * MoneySelectRuleView
 * 金额选择卷尺控件
 *
 * 每隔固定100
 *
 * 参考：{@link RuleView}
 *
 * Author: Ralap
 * Description:
 * Date 2018/8/9
 */
public class MoneySelectRuleView extends View {

    private static final boolean LOG_ENABLE = BuildConfig.DEBUG;

    /**
     * 滑动阈值
     */
    private final int TOUCH_SLOP;
    /**
     * 惯性滑动最小、最大速度
     */
    private final int MIN_FLING_VELOCITY;
    private final int MAX_FLING_VELOCITY;
    
    private int bgColor;
    private int gradationColor;
    private float gradationHeight;
    private float gradationShortLen;
    private float gradationLongLen;
    private float gradationShortWidth;
    private float gradationLongWidth;
    private float gradationValueGap;
    private float gradationTextSize;
    private int gradationTextColor;

    private float balanceTextSize;
    private int indicatorColor;
    private float unitGap;

    private String balanceText;
    private float balanceGap;

    private int maxValue;
    private int currentValue;
    private int balanceValue;
    private int valueUnit;
    private int valuePerCount;

    
    private float mCurrentDistance;
    private int mWidthRangeValue;
    private int mRangeDistance;

    private int mWidth, mHeight, mHalfWidth;
    private Paint mPaint;
    private TextPaint mTextPaint;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private OnValueChangedListener mListener;

    public interface OnValueChangedListener {
        /**
         * 当值变化时调用
         * @param newValue 变化后的新值
         */
        void onValueChanged(int newValue);

    }

    public MoneySelectRuleView(Context context) {
        this(context, null);
    }

    public MoneySelectRuleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoneySelectRuleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);

        // 初始化final常量，必须在构造中赋初值
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        TOUCH_SLOP = viewConfiguration.getScaledTouchSlop();
        MIN_FLING_VELOCITY = viewConfiguration.getScaledMinimumFlingVelocity();
        MAX_FLING_VELOCITY = viewConfiguration.getScaledMaximumFlingVelocity();

        calculateValues();
        init(context);
    }

    private void calculateValues() {
        mCurrentDistance = (float) currentValue / valueUnit * unitGap;
        mRangeDistance = (int) (maxValue / valueUnit * unitGap);
        mWidthRangeValue = (int) (mWidth / unitGap * valueUnit);
    }

    private void init(Context context) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(gradationColor);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(balanceTextSize);
        mTextPaint.setColor(gradationTextColor);

        mScroller = new Scroller(context);

        mVelocityTracker = VelocityTracker.obtain();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MoneySelectRuleView);
        bgColor = ta.getColor(R.styleable.MoneySelectRuleView_zjun_bgColor, Color.parseColor("#F5F5F5"));
        gradationColor = ta.getColor(R.styleable.MoneySelectRuleView_zjun_gradationColor, Color.LTGRAY);
        gradationHeight = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_gradationHeight, dp2px(40));
        gradationShortLen = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_gradationShortLen, dp2px(6));
        gradationLongLen = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_gradationLongLen, gradationShortLen * 2);
        gradationShortWidth = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_gradationShortWidth, 1);
        gradationLongWidth = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_gradationLongWidth, gradationShortWidth);
        gradationValueGap = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_gradationValueGap, dp2px(8));
        gradationTextSize = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_gradationTextSize, sp2px(12));
        gradationTextColor = ta.getColor(R.styleable.MoneySelectRuleView_zjun_textColor, Color.GRAY);
        indicatorColor = ta.getColor(R.styleable.MoneySelectRuleView_zjun_indicatorLineColor, Color.parseColor("#eb4c1c"));
        balanceTextSize = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_balanceTextSize, sp2px(10));
        unitGap = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_unitGap, dp2px(6));
        balanceText = ta.getString(R.styleable.MoneySelectRuleView_msrv_balanceText);
        if (TextUtils.isEmpty(balanceText)) {
            balanceText = context.getString(R.string.balance_text);
        }
        balanceGap = ta.getDimension(R.styleable.MoneySelectRuleView_msrv_balanceGap, dp2px(4));
        maxValue = ta.getInt(R.styleable.MoneySelectRuleView_msrv_maxValue, 50_000);
        currentValue = ta.getInt(R.styleable.MoneySelectRuleView_msrv_currentValue, 0);
        balanceValue = ta.getInt(R.styleable.MoneySelectRuleView_msrv_balanceValue, 0);
        valueUnit = ta.getInt(R.styleable.MoneySelectRuleView_msrv_valueUnit, 100);
        valuePerCount = ta.getInt(R.styleable.MoneySelectRuleView_msrv_valuePerCount, 10);
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHalfWidth = mWidth >> 1;
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.AT_MOST) {
            mHeight = dp2px(60);
            gradationHeight = dp2px(40);
        }

        mWidthRangeValue = (int) (mWidth / unitGap * valueUnit);

        setMeasuredDimension(mWidth, mHeight);
    }

    private int mDownX, mDownY;
    private int mLastX, mLastY;
    private boolean mIsMoving;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        logD("onTouchEvent: action=%d", action);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsMoving = false;
                mDownX = x;
                mDownY = y;
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final int dx = x - mLastX;
                if (!mIsMoving) {
                    final int dy = y - mLastY;
                    if (Math.abs(x - mDownX) <= TOUCH_SLOP || Math.abs(dx) < Math.abs(dy)) {
                        break;
                    }
                    mIsMoving = true;
                }
                mCurrentDistance -= dx;
                computeValue();
                break;
            case MotionEvent.ACTION_UP:
                if (!mIsMoving) {
                    break;
                }
                // 计算速度
                mVelocityTracker.computeCurrentVelocity(1000, MAX_FLING_VELOCITY);
                // 获取当前的水平速度
                int xVelocity = (int) mVelocityTracker.getXVelocity();
                logD("up: xVelocity=%d", xVelocity);
                if (Math.abs(xVelocity) < MIN_FLING_VELOCITY) {
                    // 滑动刻度
                    scrollToGradation();
                } else {
                    // 惯性滑动。
                    mScroller.fling((int) mCurrentDistance, 0, -xVelocity, 0, 0, mRangeDistance, 0, 0);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (!mScroller.isFinished()) {
                    // 结束滑动，并把数值立马置为终点值
                    mScroller.abortAnimation();
                }
                break;
            default: break;
        }
        mLastX = x;
        mLastY = y;
        return true;
    }

    /**
     * 滑动到最近的刻度上
     */
    private void scrollToGradation() {
        // 最近的刻度
        currentValue = Math.round(mCurrentDistance / unitGap) * valueUnit;
        // 校验边界
        currentValue = Math.min(maxValue, Math.max(0, currentValue));
        // 计算新刻度位置
        mCurrentDistance = currentValue / valueUnit * unitGap;
        logD("scrollToGradation: currentValue=%d, mCurrentDistance=%f", currentValue, mCurrentDistance);
        if (mListener != null) {
            mListener.onValueChanged(currentValue);
        }
        invalidate();
    }

    /**
     * 校验距离，并重新计算当前值
     */
    private void computeValue() {
        logD("computeValue: mRangeDistance=%d, mCurrentDistance=%f", mRangeDistance, mCurrentDistance);
        mCurrentDistance = Math.min(mRangeDistance, Math.max(0, mCurrentDistance));
        currentValue = (int)(mCurrentDistance / unitGap) * valueUnit;
        if (mListener != null) {
            mListener.onValueChanged(currentValue);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 背景
        canvas.drawColor(bgColor);
        // 刻度数值
        drawRule(canvas);
        // 绘制指针
        drawIndicator(canvas);
    }

    /**
     * 绘制刻度、金额、及剩余额度
     */
    private void drawRule(Canvas canvas) {
        canvas.save();
        canvas.translate(0, gradationHeight);

        // 参考线
        mPaint.setStrokeWidth(gradationShortWidth);
        canvas.drawLine(0, 0, mWidth, 0, mPaint);

        // 刻度、数值
        final int expend = 3 * valueUnit;
        // 起始绘制刻度
        int start = (int) ((mCurrentDistance - mHalfWidth) / unitGap) * valueUnit;
        start = Math.max(0, start - expend);
        int end = Math.min(maxValue, (start + expend) + mWidthRangeValue + expend);
        float startOffset = mHalfWidth - (mCurrentDistance - start / valueUnit * unitGap);
        final int perCount = valuePerCount * valueUnit;
        // 剩余金额：向下取整
        final int balance = balanceValue / valueUnit * valueUnit;
        logD("drawRule: mCurrentDistance=%f, start=%d, end=%d, startOffset=%f, perCount=%d",
                mCurrentDistance, start, end, startOffset, perCount);
        while (start <= end) {
            if (start % perCount == 0) {
                // 刻度
                mPaint.setStrokeWidth(gradationLongWidth);
                canvas.drawLine(startOffset, 0, startOffset, -gradationLongLen, mPaint);

                // 数值
                mTextPaint.setTextSize(gradationTextSize);
                mTextPaint.setColor(gradationTextColor);
                String text = Integer.toString(start);
                float textWidth = mTextPaint.measureText(text);
                canvas.drawText(text, startOffset - textWidth * .5f, -(gradationLongLen + gradationValueGap), mTextPaint);
            } else {
                mPaint.setStrokeWidth(gradationShortWidth);
                canvas.drawLine(startOffset, 0, startOffset, -gradationShortLen, mPaint);
            }

            // 剩余金额
            if (start == balance) {
                mPaint.setColor(indicatorColor);
                canvas.drawLine(startOffset, 0, startOffset, -gradationLongLen, mPaint);
                mPaint.setColor(gradationColor);

                mTextPaint.setTextSize(balanceTextSize);
                mTextPaint.setColor(indicatorColor);
                float textWidth = mTextPaint.measureText(balanceText);
                canvas.drawText(balanceText, startOffset - textWidth * .5f, balanceGap + balanceTextSize, mTextPaint);
                mTextPaint.setColor(gradationColor);
            }

            start += valueUnit;
            startOffset += unitGap;
        }

        canvas.restore();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScroller.getCurrX() == mScroller.getFinalX()) {
                // 已经达到终点：滑动到刻度线上
                scrollToGradation();
            } else {
                mCurrentDistance = mScroller.getCurrX();
                computeValue();
            }
        }
    }

    /**
     * 绘制指针
     */
    private void drawIndicator(Canvas canvas) {
        mPaint.setColor(indicatorColor);
        canvas.drawLine(mHalfWidth, 0, mHalfWidth, gradationHeight, mPaint);
        mPaint.setColor(gradationColor);
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

    public int getValue() {
        return currentValue;
    }

    /**
     * 设置值
     * 注意：这里不需要回调，否则会改变原数据
     *
     * @param value 当前金额
     */
    public void setValue(float value) {
        // 向下取整
        this.currentValue = (int) value / valueUnit * valueUnit;
        currentValue = Math.min(maxValue, Math.max(0, currentValue));
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        if (mListener != null) {
            mListener.onValueChanged(currentValue);
        }
        calculateValues();
        postInvalidate();
    }

    public int getBalance() {
        return balanceValue;
    }

    public void setBalance(float balance) {
        this.balanceValue = (int) balance / valueUnit * valueUnit;
        postInvalidate();
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.mListener = listener;
    }
}
