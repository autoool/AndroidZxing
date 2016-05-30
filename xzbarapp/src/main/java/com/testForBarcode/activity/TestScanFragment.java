package com.testForBarcode.activity;

import android.os.Bundle;
import android.widget.Toast;

import com.ZbarZxing.XZbar.ScanBarFragment;
import com.google.zxing.Result;

/**
 * Created by zchao on 2016/5/30.
 */
public class TestScanFragment extends ScanBarFragment {

    public TestScanFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCallBack(new IResultCallback() {
            @Override
            public void result(String lastResult) {
                Toast.makeText(getActivity(), "Scan: "
                        + lastResult.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
