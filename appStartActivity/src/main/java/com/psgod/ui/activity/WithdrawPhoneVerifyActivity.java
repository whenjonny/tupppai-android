package com.psgod.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.psgod.CustomToast;
import com.psgod.R;
import com.psgod.Utils;
import com.psgod.WeakReferenceHandler;
import com.psgod.model.LoginUser;
import com.psgod.model.MoneyTransfer;
import com.psgod.network.request.GetVerifyCodeRequest;
import com.psgod.network.request.MoneyTransferRequest;
import com.psgod.network.request.PSGodErrorListener;
import com.psgod.network.request.PSGodRequestQueue;

/**
 * Created by pires on 16/1/21.
 */
public class WithdrawPhoneVerifyActivity extends PSGodBaseActivity{
    private static final String TAG = WithdrawPhoneVerifyActivity.class.getSimpleName();

    public static final String AMOUNT_DOUBLE = "amount";
    public static final int RESEND_TIME_IN_SEC = 60;
    private int mLeftTime = RESEND_TIME_IN_SEC;
    private static final int MSG_TIMER = 0x3315;

    private WeakReferenceHandler mHandler = new WeakReferenceHandler(this);

    private double amount;

    private TextView mPhoneTxt;
    private TextView mVerifyTxt;
    private EditText mVerifyEdit;
    private Button mSure;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.addActivity(this);
        setContentView(R.layout.activity_withdraw_phone_verify);

        Intent intent = getIntent();
        amount = intent.getDoubleExtra(AMOUNT_DOUBLE, 0);

        initView();
        initListener();

    }

    private void initView() {
        mPhoneTxt = (TextView) findViewById(R.id.withdraw_phone_verify_phone);
        mVerifyTxt = (TextView) findViewById(R.id.withdraw_phone_verify_verify);
        mVerifyEdit = (EditText) findViewById(R.id.withdraw_phone_verify_edit);
        mSure = (Button) findViewById(R.id.withdraw_phone_verify_sure);
        
        mPhoneTxt.setText(LoginUser.getInstance().getPhoneNum());
    }

    private void initListener() {
        mVerifyTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLeftTime = 60;
                if (mLeftTime > 1) {
                    mLeftTime--;
                    mVerifyTxt.setEnabled(false);
                    mVerifyTxt.setText(mLeftTime + "s后重发");
                    mVerifyTxt.setTextColor(Color.parseColor("#66090909"));
                    mHandler.sendEmptyMessageDelayed(MSG_TIMER, 1000);
                } else {
                    mLeftTime = RESEND_TIME_IN_SEC;
                    mVerifyTxt.setEnabled(true);
                    mVerifyTxt.setText("获取验证码");
                    mVerifyTxt.setTextColor(Color.parseColor("#090909"));
                }

                GetVerifyCodeRequest.Builder builder = new GetVerifyCodeRequest.Builder()
                        .setErrorListener(
                                errorListener);
                GetVerifyCodeRequest request = builder
                        .build();
                request.setTag(TAG);
                RequestQueue requestQueue = PSGodRequestQueue
                        .getInstance(
                                WithdrawPhoneVerifyActivity.this)
                        .getRequestQueue();
                requestQueue.add(request);
            }
        });

        mSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showProgressDialog(WithdrawPhoneVerifyActivity.this);
                MoneyTransferRequest request = new MoneyTransferRequest.Builder().
                        setErrorListener(new PSGodErrorListener(this) {
                            @Override
                            public void handleError(VolleyError error) {

                            }
                        }).
                        setListener(new Response.Listener<MoneyTransfer>() {
                            @Override
                            public void onResponse(MoneyTransfer response) {
                                Utils.hideProgressDialog();
                                if(response != null) {
                                    Intent intent = new Intent(WithdrawPhoneVerifyActivity.this,
                                            WithdrawSuccessActivity.class);
                                    intent.putExtra(WithdrawSuccessActivity.RESULT, response);
                                    WithdrawPhoneVerifyActivity.this.startActivity(intent);
                                }
                            }
                        }).
                        setAmount(String.valueOf(amount)).
                        setCode(mVerifyEdit.getText().toString()).
                        build();
                RequestQueue requestQueue = PSGodRequestQueue
                        .getInstance(WithdrawPhoneVerifyActivity.this).getRequestQueue();
                requestQueue.add(request);
            }
        });
    }

    private PSGodErrorListener errorListener = new PSGodErrorListener(this) {

        @Override
        public void handleError(VolleyError error) {
            CustomToast.show(WithdrawPhoneVerifyActivity.this
                    , "验证码获取失败", Toast.LENGTH_SHORT);
            mLeftTime = RESEND_TIME_IN_SEC;
            mVerifyTxt.setEnabled(true);
            mVerifyTxt.setText("获取验证码");
            mVerifyTxt.setTextColor(Color.parseColor("#090909"));
        }

    };

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_TIMER:
                // 重发倒计时
                if (mLeftTime > 1) {
                    mLeftTime--;
                    mVerifyTxt.setEnabled(false);
                    mVerifyTxt.setText(mLeftTime + "s后重发");
                    mHandler.sendEmptyMessageDelayed(MSG_TIMER, 1000);
                } else {
                    mLeftTime = RESEND_TIME_IN_SEC;
                    mVerifyTxt.setEnabled(true);
                    mVerifyTxt.setText("获取验证码");
                    mVerifyTxt.setTextColor(Color.parseColor("#090909"));
                }
                break;
            default:
                break;

        }
        return true;
    }

}