# RuleView
[ ![Download](https://api.bintray.com/packages/zjun615/zjun/RuleView/images/download.svg) ](https://bintray.com/zjun615/zjun/RuleView/_latestVersion)

一系列卷尺控件，包含：基本卷尺控件（体重尺，或其它）、金额卷尺控件、时间卷尺控件

demo中，分别用三个控件，高仿了薄荷健康的体重尺、团贷网的金额尺、海康威视下萤石云视频的时间尺

## 一、效果图
> 全部效果:

![allGif](https://github.com/zjun615/RuleView/blob/master/img/all.gif)

> RuleView:

![ruleGif](https://github.com/zjun615/RuleView/blob/master/img/ruleView.gif)

> MoneySelectRuleView:

![moneyGif](https://github.com/zjun615/RuleView/blob/master/img/money.gif)

> TimeRuleView:

![timeGif](https://github.com/zjun615/RuleView/blob/master/img/time.gif)


## 二、卷尺功能说明

### 1. RuleView

基本卷尺控件，主要功能：

 - 手动滑动
 - 惯性滑动
 - 停止后，自动定位到最近的刻度
 - 支持动态修改最小值、最大值、当前值

demo中以体重尺为例，但不限于此

### 2. MoneySelectRuleView
金额选择尺，除了基本卷尺的功能外，额外的功能有：

 - 可设置“剩余金额”

### 3. TimeRuleView
时间尺，一天24h内的时间尺，最小刻度是1s

 - 支持惯性滑动
 - 支持缩放时间刻度
 - 支持多个时间段的显示
 
## 三、依赖
> build.gradle in app, the repertory is JCenter:

`implementation 'com.zjun:rule-view:0.0.1'`
 
## 四、使用
 
### 4.1 布局
 
都是最简单的使用，根据需要使用自定义属性即可，详细的自定义属性见五
 
 ```xml
<com.zjun.widget.RuleView
    android:layout_width="match_parent"
    android:layout_height="70dp"
    app:zjun_textSize="18sp" />
  
<com.zjun.widget.MoneySelectRuleView
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
  
<com.zjun.widget.TimeRuleView
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
 ```
 
### 4.2 监听回调
 
主要用来监听当前数值的变化
 ```java
ruleView.setOnValueChangedListener(new RuleView.OnValueChangedListener() {
    @Override
    public void onValueChanged(float value) {
        tvValue.setText(Float.toString(value));
    }
});
  
// 金额的超额判断，不在自定义View中判断，使用时自行判断，可参考demo
moneySelectRuleView.setOnValueChangedListener(new MoneySelectRuleView.OnValueChangedListener() {
    @Override
    public void onValueChanged(int newValue) {
        tvMoney.setText(Integer.toString(newValue));
    }
});
  
timeRuleView.setOnTimeChangedListener(new TimeRuleView.OnTimeChangedListener() {
    @Override
    public void onTimeChanged(int newTimeValue) {
        tvTime.setText(TimeRuleView.formatTimeHHmmss(newTimeValue));
    }
});
 ```

## 五、属性说明
 
### 5.1 RuleView属性
 
属性名 | 说明 | 默认值
:------ | :------ | :------
zjun_bgColor | 背景颜色 | #f5f8f5
zjun_gradationColor | 刻度颜色 | Color.LTGRAY(#CCCCCC)
gv_shortLineWidth | 短刻度线的宽度 | 1dp
gv_shortGradationLen | 短刻度长度 | 16dp
gv_longGradationLen | 长刻度长度 | gv_shortGradationLen * 2
gv_longLineWidth | 长刻度线的宽度 | gv_shortLineWidth * 2
zjun_textColor | 刻度值字体颜色 | Color.BLACK，#000000
zjun_textSize | 刻度值字体大小 | 14sp
zjun_indicatorLineColor | 中间指针线的颜色 | #48b975
zjun_indicatorLineWidth | 中间指针线的宽度 | 3dp
gv_indicatorLineLen | 中间指针线的长度 | 35dp
gv_minValue | 最小值 | 0f
gv_maxValue | 最大值 | 100f
gv_currentValue | 当前值 | 50f
gv_gradationUnit | 刻度间最小单位数值 | .1f
gv_numberPerCount | 两数值间最小单位的个数 | 10
gv_gradationGap | 最小单位的间距 | 10dp
gv_gradationNumberGap | 数值与最长刻度的间距 | 8dp

 
### 5.2 MoneySelectRuleView属性
 
属性名 | 说明 | 默认值
:------ | :------ | :------
zjun_bgColor | 背景色 | #F5F5F5
zjun_gradationColor | 刻度颜色 | Color.LTGRAY(#CCCCCC)
msrv_gradationHeight | 刻度的总高度，刻度基线以上的高度 | 40dp
msrv_gradationShortLen | 短刻度的长度 | 6dp
msrv_gradationLongLen | 长刻度的长度 | msrv_gradationShortLen * 2
msrv_gradationShortWidth | 短刻度的宽度 | 1px
msrv_gradationLongWidth | 短刻度的长度 | msrv_gradationShortWidth
msrv_gradationValueGap | 刻度数值与长刻度的间距 | 8dp
msrv_gradationTextSize | 刻度数值文字的大小 | 12sp
zjun_textColor | 刻度数值文字的颜色 | Color.GRAY
zjun_indicatorLineColor | 中间指针线的颜色 | #eb4c1c
msrv_balanceTextSize | 余额文字的大小 | 10sp
msrv_unitGap | 最小单位的间距 | 6dp
msrv_balanceText | 余额文字内容 | "剩余额度"
msrv_balanceGap | 余额文字与刻度基线的间距 | 4dp
msrv_maxValue | 最大金额 | 50_000
msrv_currentValue | 当前金额 | 0
msrv_balanceValue | 剩余金额 | 0
msrv_valueUnit | 每隔最小单位代表的金额值 | 100
msrv_valuePerCount | 刻度数值间的最小单位数 | 10
 
### 5.3 TimeRuleView属性

属性名 | 说明 | 默认值
:------ | :------ | :------
zjun_bgColor | 背景颜色 | #EEEEEE
zjun_gradationColor | 刻度颜色 | Color.GRAY
trv_partHeight | 时间段的高度 | 20dp
trv_partColor | 时间的颜色 | #F58D24
trv_gradationWidth | 刻度的宽度 | 1px
trv_secondLen | 秒刻度的长度 | 3dp
trv_minuteLen | 分刻度的长度 | 5dp
trv_hourLen | 时刻度的长度 | 10dp
trv_gradationTextColor | 刻度数值颜色 | Color.GRAY
trv_gradationTextSize | 刻度数值字体大小 | 12sp
trv_gradationTextGap | 刻度数值与时刻度的间距 | 2dp
trv_currentTime | 当前时间 | 0（单位：秒，范围∈[0, 24*3600]）
trv_indicatorTriangleSideLen | 中间指针头部三角形的边长 | 15dp
zjun_indicatorLineWidth | 中间指针的宽度 | 1dp
zjun_indicatorLineColor | 中间指针的颜色 | Color.RED
 
## 六、参考

[1] [带你实现漂亮的滑动卷尺](https://mp.weixin.qq.com/s?__biz=MzIyNTY4NjU0OQ==&mid=2247485213&idx=1&sn=97fb5d0645f0bb0eb2c5c51db4454b59&chksm=e87aa267df0d2b71d53fa2267b12afae265df585246e8ce5e2edc32d9c57bad6fbd78ec0469c&mpshare=1&scene=23&srcid=0717uZbDuuzfAGiZKvVaft9k#rd)

[2] [安卓自定义View进阶-缩放手势检测(ScaleGestureDecetor)](http://www.gcssloop.com/customview/scalegesturedetector)

[3] [安卓自定义View进阶-MotionEvent详解](http://www.gcssloop.com/customview/motionevent)

[4] [手把手教你发布自己的开源库到 Jcenter](https://mp.weixin.qq.com/s?__biz=MzIwMTAzMTMxMg==&mid=2649492998&idx=1&sn=015de305fa8cb125caf25d072165f6e8&chksm=8eec85f9b99b0cef30052e7333129ee1ffbba1600ebe5529b05c7d67a7690438ae7180acc804&mpshare=1&scene=23&srcid=0818WK605Hl5ctCkIBFtDP5q#rd)

[5] RecyclerView 的 onTouchEvent(MotionEvent e) 源码

**说明：**

RuleView，针对参考[1]中 TapeView 的源码，进行了一些优化：

 1） 左侧起始绘制刻度：如果当前绘制的刻度 currentCalibration 为0，即第一个刻度，则不绘制。滑动过程中，会发现最左侧的刻度，有时会突然消失。如果此刻度是带数值的长刻度，那数值也不会显示，界面突变会更明显。把控件宽度为 gapWidth 的整数倍，能立马看到这突变效果
 
 优化：左侧起始刻度，往左多绘制2个刻度单位

 2） 右侧结束绘制刻度：一直绘制到当前控件的10倍宽，或最大刻度数（totalCalibration = 最大刻度数 + 1，看的时候掉坑了，觉得最大刻度绘制不出来，原来是加了1）
 
 优化：在最大刻度内，右侧结束刻度，与左侧起始刻度一样，多绘制2个刻度单位即可

 多绘制的刻度单位个数，完全可以根据数值的最大宽度来决定。多了2个刻度单位，因为数值与刻度对称，就有了4个刻度。具体根据项目，多退少补
