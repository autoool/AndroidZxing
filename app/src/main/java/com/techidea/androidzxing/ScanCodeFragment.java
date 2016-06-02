package com.techidea.androidzxing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.zxing.client.android.library.CaptureFragment;


/**
 * Created by zchao on 2016/5/31.
 */
public class ScanCodeFragment extends CaptureFragment {

    public ScanCodeFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCallBack(new IResultCallback() {
            @Override
            public void result(String result) {
                Toast.makeText(getActivity().getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        });
    }
}
