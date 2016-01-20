package com.psgod.ui.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;

import com.psgod.Constants;
import com.psgod.R;
import com.psgod.Utils;

import java.util.jar.Attributes;

/**
 * Created by pires on 16/1/20.
 */
public class RechargeTypeDialog  extends Dialog {
    private static final String TAG = RechargeTypeDialog.class.getSimpleName();
    private Context mContext;

    public RechargeTypeDialog(Context context){
        super(context, R.style.ActionSheetDialog);
        this.mContext = context;
        setContentView(R.layout.dialog_recharge_type);

        getWindow().getAttributes().width = Constants.WIDTH_OF_SCREEN;
        setCanceledOnTouchOutside(true);
    }

    @Override
    public void show() {
        super.show();
        getWindow().setGravity(Gravity.BOTTOM);
        getWindow().setWindowAnimations(R.style.popwindow_anim_style);
    }

}
