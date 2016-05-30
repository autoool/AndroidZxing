/*
 * Copyright (C) 2010 ZXing authors
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

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class DecodeHandlerFragment extends Handler {

    private static final int decode_id = 0xEE;
    private static final int decode_failed_id = 0x11;
    private static final int decode_succeeded_id = 0x22;
    private static final int quit_id = 0x33;

    private final ScanBarFragment mScanBarFragment;
    private final MultiFormatReader multiFormatReader;
    private boolean running = true;
    private ImageScanner scanner;

    static {
        System.loadLibrary("iconv");
    }

    public DecodeHandlerFragment(ScanBarFragment fragment, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.mScanBarFragment = fragment;
        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
            case decode_id:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case quit_id:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        Size size = mScanBarFragment.getCameraManager().getPreviewSize();
        Log.w("PreView size:", "size : w " + size.width + " h:" + size.height);

        Rect rect = mScanBarFragment.getCropRect();
        Log.i("CropRect", "左上点:（" + rect.left + "," + rect.top + "） 右下点:（" + rect.right + "," + rect.bottom + "）宽高：width：" + rect.width() + " height:" + rect.height());

        byte[] rotatedData = new byte[data.length];
        if (mScanBarFragment.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
            rotatedData = new byte[data.length];
            for (int y = 0; y < size.height; y++) {
                for (int x = 0; x < size.width; x++)
                    rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
            }

            // 宽高也要调整
            int tmp = size.width;
            size.width = size.height;
            size.height = tmp;
        } else {
            System.arraycopy(data, 0, rotatedData, 0, data.length);
        }

        //zbar解码
        Image barcode = new Image(size.width, size.height, "Y800");
        barcode.setData(rotatedData);
        barcode.setCrop(rect.left, rect.top, rect.width(), rect.height());
        int ret = scanner.scanImage(barcode);

        if (ret != 0) {
            if (null != mScanBarFragment.getHandler()) {

                SymbolSet syms = scanner.getResults();
                String resultString = "";
                for (Symbol sym : syms) {
                    resultString = "" + sym.getData();
                }
                if (null == resultString || resultString.equals("")) {
                    //继续扫描
                } else {
                    PlanarYUVLuminanceSource source = buildLuminanceSource(rotatedData, size.width, size.height);
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundleThumbnail(source, bundle);
                    msg.setData(bundle);
                    msg.obj = resultString;
                    msg.what = decode_succeeded_id;
                    mScanBarFragment.getHandler().sendMessage(msg);
                }
            }
        } else {
            if (null != mScanBarFragment.getHandler()) {
                mScanBarFragment.getHandler().sendEmptyMessage(decode_failed_id);
            }
        }
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on
     * the format of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = mScanBarFragment.getCropRect();
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
    }

}
