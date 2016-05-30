package com.testForBarcode.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.FrameLayout;

import com.ZbarXing.xzbar.R;
import com.ZbarZxing.XZbar.ScanBarFragment;

/**
 * Created by zchao on 2016/5/30.
 */
public class TestScanActivity extends FragmentActivity {

    ScanBarFragment mScanBarFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        FragmentManager fm = getSupportFragmentManager();
        mScanBarFragment = (ScanBarFragment)fm.findFragmentById(R.id.fragment_scan);

    }
}
