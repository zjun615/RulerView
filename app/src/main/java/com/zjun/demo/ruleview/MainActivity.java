package com.zjun.demo.ruleview;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zjun.widget.MoneySelectRuleView;
import com.zjun.widget.RuleView;
import com.zjun.widget.TimeRuleView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvValue;
    private RuleView gvRule;
    private MoneySelectRuleView msrvMoney;
    private TextView tvMoney;
    private EditText etMoney;
    private TimeRuleView trvTime;
    private TextView tvTime;

    private float moneyBalance;
    private boolean isMoneySloped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTime = findViewById(R.id.tv_time);
        trvTime = findViewById(R.id.trv_time);
        tvMoney = findViewById(R.id.tv_money);
        etMoney = findViewById(R.id.et_new_money);
        msrvMoney = findViewById(R.id.msrv_money);
        tvValue = findViewById(R.id.tv_value);
        gvRule = findViewById(R.id.gv_1);


        tvValue.setText(Float.toString(gvRule.getCurrentValue()));
        gvRule.setOnValueChangedListener(new RuleView.OnValueChangedListener() {
            @Override
            public void onValueChanged(float value) {
                tvValue.setText(Float.toString(value));
            }
        });

        tvMoney.setText(Integer.toString(msrvMoney.getValue()));
        moneyBalance = msrvMoney.getBalance();
        msrvMoney.setOnValueChangedListener(new MoneySelectRuleView.OnValueChangedListener() {
            @Override
            public void onValueChanged(int newValue) {
                tvMoney.setText(Integer.toString(newValue));
                if (newValue > moneyBalance) {
                    if (!isMoneySloped) {
                        isMoneySloped = true;
                        Snackbar.make(msrvMoney, "超出额度", Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    if (isMoneySloped) {
                        isMoneySloped = false;
                    }
                }
            }
        });

        trvTime.setOnTimeChangedListener(new TimeRuleView.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(int newTimeValue) {
                tvTime.setText(TimeRuleView.formatTimeHHmmss(newTimeValue));
            }
        });
        // 模拟时间段数据
        List<TimeRuleView.TimePart> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TimeRuleView.TimePart part = new TimeRuleView.TimePart();
            part.startTime = i * 1000;
            part.endTime = part.startTime + new Random().nextInt(1000);
            list.add(part);
        }
        trvTime.setTimePartList(list);

    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_rule_indicator:
                toggleSettingsShow(R.id.ll_rule_settings);
                break;
            case R.id.btn_50:
                gvRule.setCurrentValue(gvRule.getMinValue() == 11 ? 15 : 50);
                break;
            case R.id.btn_change:
                toggleValue();
                break;
            case R.id.tv_money_indicator:
                toggleSettingsShow(R.id.ll_money_settings);
                break;
            case R.id.btn_set_money:
                float money = getMoney();
                msrvMoney.setValue(money);
                break;
            case R.id.btn_set_balance:
                moneyBalance = getMoney();
                msrvMoney.setBalance(moneyBalance);
                isMoneySloped = false;
                break;
            default: break;
        }
    }

    private void toggleSettingsShow(@IdRes int layoutId) {
        LinearLayout llSettings = findViewById(layoutId);
        llSettings.setVisibility(llSettings.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    private void toggleValue() {
        if (gvRule.getMinValue() == 11) {
            gvRule.setValue(0, 100, 50, 0.1f, 10);
        } else {
            gvRule.setValue(11, 20, 15, 0.2f, 5);
        }
    }

    private float getMoney() {
        String moneyStr = etMoney.getText().toString();
        if (moneyStr.isEmpty()) {
            moneyStr = "0";
        }
        return Float.parseFloat(moneyStr);
    }

}
