package com.testForBarcode.activity;

import com.ZbarXing.xzbar.R;
import com.ZbarZxing.XZbar.DecodeThread;
import com.ZbarZxing.XZbar.HxBarcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

public class MainActivity extends Activity {

    private ImageView mResultImage;
    private TextView mResultText;
    private Button LandspaceScan;
    private Button PortraitScan;
    private Button buttonScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mResultImage = (ImageView) findViewById(R.id.result_image);
        mResultText = (TextView) findViewById(R.id.result_text);
        LandspaceScan = (Button) findViewById(R.id.result_button);
        PortraitScan = (Button) findViewById(R.id.result_button2);
        buttonScan = (Button) findViewById(R.id.button_scan);
        LandspaceScan.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                HxBarcode hxBarcode = new HxBarcode();
                hxBarcode.scan(MainActivity.this, 501, true);
            }
        });
        PortraitScan.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                HxBarcode hxBarcode = new HxBarcode();
                hxBarcode.scan(MainActivity.this, 501, false);
            }
        });

        buttonScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TestScanActivity.class));
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        System.out.printf("onActivityResult ： %s,%s\n", requestCode, resultCode);
        switch (requestCode) {
            case 501:
                if (data != null) {
                    Bundle extras = data.getBundleExtra("data");


                    if (null != extras) {
                        int width = extras.getInt("width");
                        int height = extras.getInt("height");
                        String result = extras.getString("result");
                        mResultText.setText(result);


                        //以下只是为了显示图片。
                        LayoutParams lps = new LayoutParams(width, height);
                        lps.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
                        lps.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                        lps.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

                        mResultImage.setLayoutParams(lps);
                        Bitmap barcode = null;
                        byte[] compressedBitmap = extras.getByteArray(DecodeThread.BARCODE_BITMAP);
                        if (compressedBitmap != null) {
                            barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                            // Mutable copy:
                            barcode = barcode.copy(Bitmap.Config.RGB_565, true);
                        }
                        mResultImage.setImageBitmap(barcode);
                    }
                }
                break;
        }
    }
}