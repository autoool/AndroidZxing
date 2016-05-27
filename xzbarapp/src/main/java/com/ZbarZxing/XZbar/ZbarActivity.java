/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ZbarZxing.XZbar;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class ZbarActivity extends Activity implements SurfaceHolder.Callback {

	private static final String TAG = ZbarActivity.class.getSimpleName();
    private static final int capture_crop_view_id=0x7f080002;
    private static final int capture_mask_top_id=0x7f080004;
    private static final int capture_mask_bottom_id = 0x7f080006;
    private static final int restart_preview_id = 0x44;
    private int PreviewWidth;
    private int PreviewHeight;

	private CameraManager cameraManager;
	private CaptureActivityHandler handler;
	private InactivityTimer inactivityTimer;
	private BeepManager beepManager;

	private SurfaceView scanPreview = null;
	private RelativeLayout scanContainer;
	private RelativeLayout scanCropView;
	private ImageView scanLine;
	private Button scanCancel ;
	private Button flashswitch;
	private boolean isTrue = false;

	private Rect mCropRect = null;

	public Handler getHandler() {
		return handler;
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	private boolean isHasSurface = false;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		Window window = getWindow();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		Intent it = getIntent();
		
		if(it.getBooleanExtra("isLandSpace", false))
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		
		
		initUI();
		addLisenter();
		inactivityTimer = new InactivityTimer(this);
		beepManager = new BeepManager(this);

		TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.05f, Animation.RELATIVE_TO_PARENT,
				0.85f);
		animation.setDuration(4500);
		animation.setRepeatCount(-1);
		animation.setRepeatMode(Animation.REVERSE);
		scanLine.startAnimation(animation);
	}
	
	

	
	public final void initUI() {
		// 思路： 先创建父布局的参数对象，初始化上 子布局的属性，之后设置参数。

		// 1. main 根布局
		scanContainer = new RelativeLayout(this);
		// 设置参数
		scanContainer.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		// 2.第一层布局
		// 2.1 framelayout
		scanPreview = new SurfaceView(this);
		scanPreview.setLayoutParams(new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		// 添加到父控件
		scanContainer.addView(scanPreview);

		// 2.2 RelativeLayout
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int screenHeigh = dm.heightPixels;

		Log.i("zbh", "width=" + screenWidth + ",height = " + screenHeigh);
		// 根据屏幕指定扫描框的大小
		PreviewWidth = dm.widthPixels;
    	PreviewHeight = dm.heightPixels;
    	
    	//根据屏幕指定扫描框的大小
    	int x=PreviewWidth/2;
    	int y=PreviewHeight/2*2/3;
    	
    	if(PreviewWidth > PreviewHeight)
    	{
    		//根据屏幕指定扫描框的大小
	    	x=PreviewHeight/2;
	    	y=PreviewWidth/2*2/3;
    	}

		scanCropView = new RelativeLayout(this);
		scanCropView.setId(capture_crop_view_id);
		RelativeLayout.LayoutParams relativepaLayoutParams = new RelativeLayout.LayoutParams(
				x, y);
		relativepaLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				RelativeLayout.TRUE);
		relativepaLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL,
				RelativeLayout.TRUE);
		scanCropView.setBackgroundResource(getResourceId(this, "qr_code_bg",
				"drawable"));
		// 添加到父控件
		scanContainer.addView(scanCropView, relativepaLayoutParams);

		// 2.2.1 image
		scanLine = new ImageView(this);
		RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		imageParams.setMargins(dip2px(this, 10), dip2px(this, 5),
				dip2px(this, 10), dip2px(this, 5));
		imageParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
				RelativeLayout.TRUE);
		scanLine.setImageResource(getResourceId(this, "scan_line", "drawable"));

		// 添加到父控件
		scanCropView.addView(scanLine, imageParams);

		// 2.3 RelativeLayout
		RelativeLayout capture_mask_top = new RelativeLayout(this);
		capture_mask_top.setId(capture_mask_top_id);
		capture_mask_top.setBackgroundResource(getResourceId(this, "shadow",
				"drawable"));
		RelativeLayout.LayoutParams Params = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		Params.addRule(RelativeLayout.ABOVE, capture_crop_view_id);
		Params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

		// 添加到父控件
		scanContainer.addView(capture_mask_top, Params);

		// 2.3.1 button
		scanCancel = new Button(this);
		scanCancel.setText("取消");
		scanCancel.setTextSize(18);
		scanCancel.setTextColor(Color.rgb(0xFF, 0xFA, 0xFA));
		scanCancel.getBackground().setAlpha(0x00);
		scanCancel.setGravity(Gravity.CENTER);

		RelativeLayout.LayoutParams cancelParams = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		cancelParams.leftMargin = dip2px(this, 10);
		cancelParams.topMargin = dip2px(this, 10);
		cancelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				RelativeLayout.TRUE);

		// 添加到父控件
		scanCancel.setLayoutParams(cancelParams);
		capture_mask_top.addView(scanCancel);

		// 2.3.2 button
		flashswitch = new Button(this);
		flashswitch.setText("闪光灯");
		flashswitch.setTextSize(18);
		flashswitch.setTextColor(Color.rgb(0xFF, 0xFA, 0xFA));
		flashswitch.getBackground().setAlpha(0x00);
		flashswitch.setGravity(Gravity.CENTER);

		RelativeLayout.LayoutParams flashlightingParams = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		flashlightingParams.topMargin = dip2px(this, 10);
		flashlightingParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				RelativeLayout.TRUE);

		// 添加到父控件
		flashswitch.setLayoutParams(flashlightingParams);
		capture_mask_top.addView(flashswitch);

		// 2.3.3 TextView
		TextView tip2text = new TextView(this);
		tip2text.setText("将二维码或条码放入框内，即可自动扫描");
		tip2text.setTextSize(18);
		tip2text.setTextColor(Color.rgb(248, 29, 56));// red

		RelativeLayout.LayoutParams tiptextParams = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tiptextParams.bottomMargin = dip2px(this, 5);
		tiptextParams.leftMargin = dip2px(this, 10);
		tiptextParams.rightMargin = dip2px(this, 10);
		tiptextParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				RelativeLayout.TRUE);
		tiptextParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				RelativeLayout.TRUE);

		// 添加到父控件
		capture_mask_top.addView(tip2text, tiptextParams);

		// 2.4 RelativeLayout
		RelativeLayout capture_mask_bottom = new RelativeLayout(this);
		capture_mask_bottom.setId(capture_mask_bottom_id);
		capture_mask_bottom.setBackgroundResource(getResourceId(this, "shadow",
				"drawable"));
		RelativeLayout.LayoutParams bottomParams = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				RelativeLayout.TRUE);
		bottomParams.addRule(RelativeLayout.BELOW, capture_crop_view_id);
		// 添加到父控件
		scanContainer.addView(capture_mask_bottom, bottomParams);

		// 2.5 imageView
		ImageView leftimage = new ImageView(this);
		leftimage.setBackgroundResource(getResourceId(this, "shadow",
				"drawable"));
		RelativeLayout.LayoutParams leftimageParams = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		leftimageParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				RelativeLayout.TRUE);
		leftimageParams.addRule(RelativeLayout.ABOVE, capture_mask_bottom_id);
		leftimageParams.addRule(RelativeLayout.BELOW, capture_mask_top_id);
		leftimageParams.addRule(RelativeLayout.LEFT_OF, capture_crop_view_id);
		// 添加到父控件
		scanContainer.addView(leftimage, leftimageParams);

		// 2.6 imageView
		ImageView rightimage = new ImageView(this);
		rightimage.setBackgroundResource(getResourceId(this, "shadow",
				"drawable"));
		RelativeLayout.LayoutParams rightimageParams = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		rightimageParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
				RelativeLayout.TRUE);
		rightimageParams.addRule(RelativeLayout.ABOVE, capture_mask_bottom_id);
		rightimageParams.addRule(RelativeLayout.BELOW, capture_mask_top_id);
		rightimageParams.addRule(RelativeLayout.RIGHT_OF, capture_crop_view_id);
		// 添加到父控件
		scanContainer.addView(rightimage, rightimageParams);

		setContentView(scanContainer);
	}
	
	private void addLisenter() {
		flashswitch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(isTrue)
				{
					turnLightOff(cameraManager.getCamera());
					isTrue=false;
					flashswitch.setTextColor(Color.rgb(0xFA, 0xFA, 0xFF));
				}
				else
				{
					turnLightOn(cameraManager.getCamera());
					isTrue = true;
					flashswitch.setTextColor(Color.rgb(0xFF, 0x00, 0x00));
				}
			}
		});
		
		scanCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Vibrator mVibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);  
                mVibrator.vibrate(50);
                
				finish();
			}
		});
		
		scanCancel.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction()==MotionEvent.ACTION_DOWN){  
	                ((Button)v).setTextColor(Color.rgb(0xe1, 0x69, 0x41));
	            }else if(event.getAction()==MotionEvent.ACTION_UP){  
	            	((Button)v).setTextColor(Color.rgb(0xFA, 0xFA, 0xFF));  
	            }
				return false;
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// CameraManager must be initialized here, not in onCreate(). This is
		// necessary because we don't
		// want to open the camera driver and measure the screen size if we're
		// going to show the help on
		// first launch. That led to bugs where the scanning rectangle was the
		// wrong size and partially
		// off screen.
		System.out.println("begin new cameraManger");
		cameraManager = new CameraManager(getApplication());
		beepManager = new BeepManager(this);
		handler = null;

		if (isHasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			System.out.println("surfaceCreated() won't be called，initCamera");
			initCamera(scanPreview.getHolder());
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			System.out.println("wait for surfaceCreated() to init Camera");
			scanPreview.getHolder().addCallback(this);
		}

		inactivityTimer.onResume();
	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		inactivityTimer.onPause();
		if(isTrue)
		{
			turnLightOff(cameraManager.getCamera());
			flashswitch.setTextColor(Color.rgb(0xFA, 0xFA, 0xFF));
		}
		beepManager.close();
		cameraManager.closeDriver();
		if (!isHasSurface) {
			scanPreview.getHolder().removeCallback(this);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (holder == null) {
			Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
		}
		if (!isHasSurface) {
			isHasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		isHasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * 
	 * @param bundle
	 *            The extras
	 */
	public void handleDecode(String rawResult, Bundle bundle) {
		inactivityTimer.onActivity();
		beepManager.playBeepSoundAndVibrate();
		Intent dataStr = new Intent();
		
		bundle.putInt("width", mCropRect.width());
		bundle.putInt("height", mCropRect.height());
		bundle.putString("result", rawResult);
		dataStr.putExtra("data", bundle);
		setResult(Activity.RESULT_OK, dataStr);
		finish();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
			Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				handler = new CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE);
			}

			initCrop();
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializing camera", e);
			displayFrameworkBugMessageAndExit();
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		// camera error
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("条码扫描器");
		builder.setMessage("相机打开出错，请稍后重试");
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}

		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		builder.show();
	}

	public void restartPreviewAfterDelay(long delayMS) {
		if (handler != null) {
			handler.sendEmptyMessageDelayed(restart_preview_id, delayMS);
		}
	}

	public Rect getCropRect() {
		return mCropRect;
	}

	/**
	 * 初始化截取的矩形区域
	 */
	private void initCrop() {
		int cameraWidth = cameraManager.getCameraResolution().x;
		int cameraHeight = cameraManager.getCameraResolution().y;

		/** 获取布局中扫描框的位置信息 */
		int[] location = new int[2];
		scanCropView.getLocationInWindow(location);

		int cropLeft = location[0];
		int cropTop = location[1];

		int cropWidth = scanCropView.getWidth();
		int cropHeight = scanCropView.getHeight();

		/** 获取布局容器的宽高 */
		int containerWidth = scanContainer.getWidth();
		int containerHeight = scanContainer.getHeight();

		/** 计算最终截取的矩形的左上角顶点x坐标 */
		int x = cropLeft * cameraWidth / containerWidth;
		/** 计算最终截取的矩形的左上角顶点y坐标 */
		int y = cropTop * cameraHeight / containerHeight;

		/** 计算最终截取的矩形的宽度 */
		int width = cropWidth * cameraWidth / containerWidth;
		/** 计算最终截取的矩形的高度 */
		int height = cropHeight * cameraHeight / containerHeight;

		/** 生成最终的截取的矩形 */
		mCropRect = new Rect(x, y, width + x, height + y);
	}

	
    public static int getResourceId(Context context,String name,String type){
   	 
        Resources themeResources=null;
        PackageManager pm=context.getPackageManager();
         try {
        	 String packageName = context.getApplicationContext().getPackageName();
        	 themeResources=pm.getResourcesForApplication(packageName);
            return themeResources.getIdentifier(name, type, packageName);
         } catch(NameNotFoundException e) {

           e.printStackTrace();
         }
         return 0;
   }
    
    /** 
     * 通过设置Camera打开闪光灯 
     * @param mCamera 
     */  
    public static void turnLightOn(Camera mCamera) {  
        if (mCamera == null) {  
            return;  
        }  
        Parameters parameters = mCamera.getParameters();  
        if (parameters == null) {  
            return;  
        }  
    List<String> flashModes = parameters.getSupportedFlashModes();  
        // Check if camera flash exists  
        if (flashModes == null) {  
            // Use the screen as a flashlight (next best thing)  
            return;  
        }  
        String flashMode = parameters.getFlashMode();  
        Log.i(TAG, "Flash mode: " + flashMode);  
        Log.i(TAG, "Flash modes: " + flashModes);  
        if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {  
            // Turn on the flash  
            if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {  
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);  
                mCamera.setParameters(parameters);  
            } else {
            	Log.e(TAG, "FLASH_MODE_TORCH not supported"); 
            }  
        }  
    }
    
    /** 
     * 通过设置Camera关闭闪光灯 
     * @param mCamera 
     */  
    public static void turnLightOff(Camera mCamera) {  
        if (mCamera == null) {  
            return;  
        }  
        Parameters parameters = mCamera.getParameters();  
        if (parameters == null) {  
            return;  
        }  
        List<String> flashModes = parameters.getSupportedFlashModes();  
        String flashMode = parameters.getFlashMode();  
        // Check if camera flash exists  
        if (flashModes == null) {
            return;  
        }  
        Log.i(TAG, "Flash mode: " + flashMode);  
        Log.i(TAG, "Flash modes: " + flashModes);  
        if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {  
            // Turn off the flash  
            if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {  
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);  
                mCamera.setParameters(parameters);  
            } else {  
                Log.e(TAG, "FLASH_MODE_OFF not supported");  
            }  
        }  
    }
	
    /** 
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素) 
     */  
    public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }  
  
    /** 
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp 
     */  
    public static int px2dip(Context context, float pxValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (pxValue / scale + 0.5f);  
    }
    
    @Override 
    public void onBackPressed() { 
    	
		finish();
    }
}