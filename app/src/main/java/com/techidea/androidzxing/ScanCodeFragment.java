package com.techidea.androidzxing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.zxing.Result;
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
            public void result(Result lastResult) {
                Toast.makeText(getActivity().getApplicationContext(), lastResult.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
