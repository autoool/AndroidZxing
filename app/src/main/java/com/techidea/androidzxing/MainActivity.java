package com.techidea.androidzxing;

import android.app.DownloadManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button mButtonZxing;
    DownloadManager mDownloadManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButtonZxing = (Button) findViewById(R.id.button_zxing);
        mButtonZxing.setOnClickListener(this);
        ButterKnife.bind(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_zxing:
                startActivity(new Intent(MainActivity.this, ZxingActivity.class));
                break;
        }
    }

    @OnClick(R.id.button_launch)
    void buttonLaunch() {
    }


}
