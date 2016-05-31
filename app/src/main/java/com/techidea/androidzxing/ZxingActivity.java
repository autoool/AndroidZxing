package com.techidea.androidzxing;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.google.zxing.client.android.library.CaptureFragment;

/**
 * Created by zchao on 2016/5/31.
 */
public class ZxingActivity extends AppCompatActivity {

    CaptureFragment mCaptureFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zxing);
        FragmentManager fm = getSupportFragmentManager();
        mCaptureFragment = (CaptureFragment) fm.findFragmentById(R.id.fragment_scan);
    }
}
