package com.zjun.widget;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * GradationView
 * 刻度卷尺控件
 *
 * 思路：
 *  1. 把float类型数据，乘10，转为int类型。之所以把它放在第一位，因为踩过这个坑：不转的话，由于精度问题，会导致计算异常复杂
 *  2. 绘制刻度：
 *     - 根据中间指针位置的数值，来计算最小值位置与中间指针位置的距离
 *     - 为了绘制性能，只绘制控件宽度范围内的刻度。但不出现数值突变（两侧刻度出现突然显示或不显示），两侧各增加2个单位
 *  3. 滑动时，通过移动最小位置与中间指针位置的距离，逆向推算当前刻度值
 *  4. 滑动停止后，自动调整到最近的刻度：使用滑动器Scroller，需要计算出最终要抵达的位置
 *  5. 惯性滑动：使用速度跟踪器VelocityTracker
 *
 * Author: Ralap
 * Description:
 * Date 2018/7/29
 */
public class RuleView extends View {
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

    /**
     * 背景色
     */
    private int bgColor;
    /**
     * 刻度颜色
     */
    private int gradationColor;
    /**
     * 短刻度线宽度
     */
    private float shortLineWidth;
    /**
     * 长刻度线宽度
     * 默认 = 2 * shortLineWidth
     */
    private float longLineWidth ;
    /**
     * 短刻度长度
     */
    private float shortGradationLen;
    /**
     * 长刻度长度
     * 默认为短刻度的2倍
     */
    private float longGradationLen;
    /**
     * 刻度字体颜色
     */
    private int textColor;
    /**
     * 刻度字体大小
     */
    private float textSize;
    /**
     * 中间指针线颜色
     */
    private int indicatorLineColor;
    /**
     * 中间指针线宽度
     */
    private float indicatorLineWidth;
    /**
     * 中间指针线长度
     */
    private float indicatorLineLen;
    /**
     * 最小值
     */
    private float minValue;
    /**
     * 最大值
     */
    private float maxValue;
    /**
     * 当前值
     */
    private float currentValue;
    /**
     * 刻度最小单位
     */
    private float gradationUnit;
    /**
     * 需要绘制的数值
     */
    private int numberPerCount;
    /**
     * 刻度间距离
     */
    private float gradationGap;
    /**
     * 刻度与文字的间距
     */
    private float gradationNumberGap;

    /**
     * 最小数值，放大10倍：minValue * 10
     */
    private int mMinNumber;
    /**
     * 最大数值，放大10倍：maxValue * 10
     */
    private int mMaxNumber;
    /**
     * 当前数值
     */
    private int mCurrentNumber;
    /**
     * 最大数值与最小数值间的距离：(mMaxNumber - mMinNumber) / mNumberUnit * gradationGap
     */
    private float mNumberRangeDistance;
    /**
     * 刻度数值最小单位：gradationUnit * 10
     */
    private int mNumberUnit;
    /**
     * 当前数值与最小值的距离：(mCurrentNumber - minValue) / mNumberUnit * gradationGap
     */
    private float mCurrentDistance;
    /**
     * 控件宽度所占有的数值范围：mWidth / gradationGap * mNumberUnit
     */
    private int mWidthRangeNumber;

    /**
     * 普通画笔
     */
    private Paint mPaint;
    /**
     * 文字画笔
     */
    private TextPaint mTextPaint;
    /**
     * 滑动器
     */
    private Scroller mScroller;
    /**
     * 速度跟踪器
     */
    private VelocityTracker mVelocityTracker;
    /**
     * 尺寸
     */
    private int mWidth, mHalfWidth, mHeight;

    private int mDownX;
    private int mLastX, mLastY;
    private boolean isMoved;

    private OnValueChangedListener mValueChangedListener;

    /**
     * 当前值变化监听器
     */
    public interface OnValueChangedListener{
        void onValueChanged(float value);
    }


    public RuleView(Context context) {
        this(context, null);
    }

    public RuleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RuleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);

        // 初始化final常量，必须在构造中赋初值
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        TOUCH_SLOP = viewConfiguration.getScaledTouchSlop();
        MIN_FLING_VELOCITY = viewConfiguration.getScaledMinimumFlingVelocity();
        MAX_FLING_VELOCITY = viewConfiguration.getScaledMaximumFlingVelocity();

        convertValue2Number();
        init(context);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RuleView);
        bgColor = ta.getColor(R.styleable.RuleView_zjun_bgColor, Color.parseColor("#f5f8f5"));
        gradationColor = ta.getColor(R.styleable.RuleView_zjun_gradationColor, Color.LTGRAY);
        shortLineWidth = ta.getDimension(R.styleable.RuleView_gv_shortLineWidth, dp2px(1));
        shortGradationLen = ta.getDimension(R.styleable.RuleView_gv_shortGradationLen, dp2px(16));
        longGradationLen = ta.getDimension(R.styleable.RuleView_gv_longGradationLen, shortGradationLen * 2);
        longLineWidth = ta.getDimension(R.styleable.RuleView_gv_longLineWidth, shortLineWidth * 2);
        textColor = ta.getColor(R.styleable.RuleView_zjun_textColor, Color.BLACK);
        textSize = ta.getDimension(R.styleable.RuleView_zjun_textSize, sp2px(14));
        indicatorLineColor = ta.getColor(R.styleable.RuleView_zjun_indicatorLineColor, Color.parseColor("#48b975"));
        indicatorLineWidth = ta.getDimension(R.styleable.RuleView_zjun_indicatorLineWidth, dp2px(3f));
        indicatorLineLen = ta.getDimension(R.styleable.RuleView_gv_indicatorLineLen, dp2px(35f));
        minValue = ta.getFloat(R.styleable.RuleView_gv_minValue, 0f);
        maxValue = ta.getFloat(R.styleable.RuleView_gv_maxValue, 100f);
        currentValue = ta.getFloat(R.styleable.RuleView_gv_currentValue, 50f);
        gradationUnit = ta.getFloat(R.styleable.RuleView_gv_gradationUnit, .1f);
        numberPerCount = ta.getInt(R.styleable.RuleView_gv_numberPerCount, 10);
        gradationGap = ta.getDimension(R.styleable.RuleView_gv_gradationGap, dp2px(10));
        gradationNumberGap = ta.getDimension(R.styleable.RuleView_gv_gradationNumberGap, dp2px(8));
        ta.recycle();
    }

    /**
     * 初始化
     */
    private void init(Context context) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(shortLineWidth);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(textColor);

        mScroller = new Scroller(context);
    }

    /**
     * 把真实数值转换成绘制数值
     * 为了防止float的精度丢失，把minValue、maxValue、currentValue、gradationUnit都放大10倍
     */
    private void convertValue2Number() {
        mMinNumber = (int) (minValue * 10);
        mMaxNumber = (int) (maxValue * 10);
        mCurrentNumber = (int) (currentValue * 10);
        mNumberUnit = (int) (gradationUnit * 10);
        mCurrentDistance = (mCurrentNumber - mMinNumber) / mNumberUnit * gradationGap;
        mNumberRangeDistance = (mMaxNumber - mMinNumber) / mNumberUnit * gradationGap;
        if (mWidth != 0) {
            // 初始化时，在onMeasure()里计算
            mWidthRangeNumber = (int) (mWidth / gradationGap * mNumberUnit);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = calculateSize(true, widthMeasureSpec);
        mHeight = calculateSize(false, heightMeasureSpec);
        mHalfWidth = mWidth >> 1;
        if (mWidthRangeNumber == 0) {
            mWidthRangeNumber = (int) (mWidth / gradationGap * mNumberUnit);
        }
        setMeasuredDimension(mWidth, mHeight);
    }

    /**
     * 计算宽度或高度的真实大小
     *
     * 宽或高为wrap_content时，父控件的测量模式无论是EXACTLY还是AT_MOST，默认给的测量模式都是AT_MOST，测量大小为父控件的size
     * 所以，我们宽度不管，只处理高度，默认80dp
     * @see ViewGroup#getChildMeasureSpec(int, int, int)
     *
     * @param isWidth 是不是宽度
     * @param spec    测量规则
     * @return 真实的大小
     */
    private int calculateSize(boolean isWidth, int spec) {
        final int mode = MeasureSpec.getMode(spec);
        final int size = MeasureSpec.getSize(spec);

        int realSize = size;
        switch (mode) {
            // 精确模式：已经确定具体数值：layout_width为具体值，或match_parent
            case MeasureSpec.EXACTLY:
                break;
            // 最大模式：最大不能超过父控件给的widthSize：layout_width为wrap_content
            case MeasureSpec.AT_MOST:
                if (!isWidth) {
                    int defaultContentSize = dp2px(80);
                    realSize = Math.min(realSize, defaultContentSize);
                }
                break;
            // 未指定尺寸模式：一般父控件是AdapterView
            case MeasureSpec.UNSPECIFIED:
            default:

        }
        logD("isWidth=%b, mode=%d, size=%d, realSize=%d", isWidth, mode, size, realSize);
        return realSize;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        logD("onTouchEvent: action=%d", action);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScroller.forceFinished(true);
                mDownX = x;
                isMoved = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final int dx = x - mLastX;

                // 判断是否已经滑动
                if (!isMoved) {
                    final int dy = y - mLastY;
                    // 滑动的触发条件：水平滑动大于垂直滑动；滑动距离大于阈值
                    if (Math.abs(dx) < Math.abs(dy) || Math.abs(x - mDownX) < TOUCH_SLOP) {
                        break;
                    }
                    isMoved = true;
                }

                mCurrentDistance += -dx;
                calculateValue();
                break;
            case MotionEvent.ACTION_UP:
                // 计算速度：使用1000ms为单位
                mVelocityTracker.computeCurrentVelocity(1000, MAX_FLING_VELOCITY);
                // 获取速度。速度有方向性，水平方向：左滑为负，右滑为正
                int xVelocity = (int) mVelocityTracker.getXVelocity();
                // 达到速度则惯性滑动，否则缓慢滑动到刻度
                if (Math.abs(xVelocity) >= MIN_FLING_VELOCITY) {
                    // 速度具有方向性，需要取反
                    mScroller.fling((int)mCurrentDistance, 0, -xVelocity, 0,
                            0, (int)mNumberRangeDistance, 0, 0);
                    invalidate();
                } else {
                    scrollToGradation();
                }
                break;
            default:
                break;
        }
        mLastX = x;
        mLastY = y;
        return true;
    }

    /**
     * 根据distance距离，计算数值
     */
    private void calculateValue() {
        // 限定范围：在最小值与最大值之间
        mCurrentDistance = Math.min(Math.max(mCurrentDistance, 0), mNumberRangeDistance);
        mCurrentNumber = mMinNumber + (int)(mCurrentDistance / gradationGap) * mNumberUnit;
        currentValue = mCurrentNumber / 10f;
        logD("calculateValue: mCurrentDistance=%f, mCurrentNumber=%d, currentValue=%f",
                mCurrentDistance, mCurrentNumber, currentValue);
        if (mValueChangedListener != null) {
            mValueChangedListener.onValueChanged(currentValue);
        }
        invalidate();
    }

    /**
     * 滑动到最近的刻度线上
     */
    private void scrollToGradation() {
        mCurrentNumber = mMinNumber + Math.round(mCurrentDistance / gradationGap) * mNumberUnit;
        mCurrentNumber = Math.min(Math.max(mCurrentNumber, mMinNumber), mMaxNumber);
        mCurrentDistance = (mCurrentNumber - mMinNumber) / mNumberUnit * gradationGap;
        currentValue = mCurrentNumber / 10f;
        logD("scrollToGradation: mCurrentDistance=%f, mCurrentNumber=%d, currentValue=%f",
                mCurrentDistance, mCurrentNumber, currentValue);
        if (mValueChangedListener != null) {
            mValueChangedListener.onValueChanged(currentValue);
        }
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScroller.getCurrX() != mScroller.getFinalX()) {
                mCurrentDistance = mScroller.getCurrX();
                calculateValue();
            } else {
                scrollToGradation();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1 绘制背景色
        canvas.drawColor(bgColor);
        // 2 绘制刻度、数字
        drawGradation(canvas);
        // 3 绘制指针
        drawIndicator(canvas);
    }

    /**
     * 绘制刻度
     */
    private void drawGradation(Canvas canvas) {
        // 1 顶部基准线
        mPaint.setColor(gradationColor);
        mPaint.setStrokeWidth(shortLineWidth);
        canvas.drawLine(0, shortLineWidth * .5f, mWidth, 0, mPaint);

        /*
         2 左侧刻度
         2.1 计算左侧开始绘制的刻度
          */
        int startNum = ((int) mCurrentDistance - mHalfWidth) / (int) gradationGap * mNumberUnit + mMinNumber;
        // 扩展2个单位
        final int expendUnit = mNumberUnit << 1;
        // 左侧扩展
        startNum -= expendUnit;
        if (startNum < mMinNumber) {
            startNum = mMinNumber;
        }
        // 右侧扩展
        int rightMaxNum = (startNum + expendUnit) + mWidthRangeNumber + expendUnit;
        if (rightMaxNum > mMaxNumber) {
            rightMaxNum = mMaxNumber;
        }
        // 当前绘制刻度对应控件左侧的位置
        float distance = mHalfWidth - (mCurrentDistance - (startNum - mMinNumber) / mNumberUnit * gradationGap);
        final int perUnitCount = mNumberUnit * numberPerCount;
        logD("drawGradation: startNum=%d, rightNum=%d, perUnitCount=%d",
                startNum, rightMaxNum, perUnitCount);
        while (startNum <= rightMaxNum) {
            logD("drawGradation: startNum=%d", startNum);
            if (startNum % perUnitCount == 0) {
                // 长刻度：刻度宽度为短刻度的2倍
                mPaint.setStrokeWidth(longLineWidth);
                canvas.drawLine(distance, 0, distance, longGradationLen, mPaint);

                // 数值
                float fNum = startNum / 10f;
                String text = Float.toString(fNum);
                logD("drawGradation: text=%s", text);
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length() - 2);
                }
                final float textWidth = mTextPaint.measureText(text);
                canvas.drawText(text, distance - textWidth * .5f, longGradationLen + gradationNumberGap + textSize, mTextPaint);
            } else {
                // 短刻度
                mPaint.setStrokeWidth(shortLineWidth);
                canvas.drawLine(distance, 0, distance, shortGradationLen, mPaint);
            }
            startNum += mNumberUnit;
            distance += gradationGap;
        }
    }

    /**
     * 绘制指针
     */
    private void drawIndicator(Canvas canvas) {
        mPaint.setColor(indicatorLineColor);
        mPaint.setStrokeWidth(indicatorLineWidth);
        // 圆头画笔
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        //
        canvas.drawLine(mHalfWidth, 0, mHalfWidth, indicatorLineLen, mPaint);
        // 默认形状画笔
        mPaint.setStrokeCap(Paint.Cap.BUTT);
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
            Log.d("GradationView", String.format("zjun@" + format, args));
        }
    }

    /**
     * 设置新值
     */
    public void setCurrentValue(float currentValue) {
        if (currentValue < minValue || currentValue > maxValue) {
            throw new IllegalArgumentException(String.format("The currentValue of %f is out of range: [%f, %f]",
                    currentValue, minValue, maxValue));
        }
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        this.currentValue = currentValue;
        mCurrentNumber = (int) (this.currentValue * 10);
        final float newDistance = (mCurrentNumber - mMinNumber) / mNumberUnit * gradationGap;
        final int dx = (int) (newDistance - mCurrentDistance);
        // 最大2000ms
        final int duration = dx * 2000 / (int)mNumberRangeDistance;
        // 滑动到目标值
        mScroller.startScroll((int) mCurrentDistance, 0, dx, duration);
        postInvalidate();
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    /**
     * 获取当前值
     */
    public float getCurrentValue() {
        return this.currentValue;
    }

    /**
     * 重新配置参数
     *
     * @param minValue  最小值
     * @param maxValue  最大值
     * @param curValue  当前值
     * @param unit      最小单位所代表的值
     * @param perCount  相邻两条长刻度线之间被分成的隔数量
     */
    public void setValue(float minValue, float maxValue, float curValue, float unit, int perCount) {
        if (minValue > maxValue || curValue < minValue || curValue > maxValue) {
            throw new IllegalArgumentException(String.format("The given values are invalid, check firstly: " +
                    "minValue=%f, maxValue=%f, curValue=%s", minValue, maxValue, curValue));
        }
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = curValue;
        this.gradationUnit = unit;
        this.numberPerCount = perCount;
        convertValue2Number();
        if (mValueChangedListener != null) {
            mValueChangedListener.onValueChanged(currentValue);
        }
        postInvalidate();
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.mValueChangedListener = listener;
    }
}
