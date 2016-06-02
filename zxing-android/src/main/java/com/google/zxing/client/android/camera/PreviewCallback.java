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

package com.google.zxing.client.android.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.client.android.camera.open.OpenCamera;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

final class PreviewCallback implements Camera.PreviewCallback {

    private static final String TAG = PreviewCallback.class.getSimpleName();

    private final CameraConfigurationManager configManager;
    private OpenCamera camera;
    private ImageScanner mImageScanner;
    private Handler previewHandler;
    private int previewMessage;

    PreviewCallback(CameraConfigurationManager configManager, OpenCamera camera) {
        this.configManager = configManager;
        this.camera = camera;
        mImageScanner = new ImageScanner();
    }

    void setHandler(Handler previewHandler, int previewMessage) {
        this.previewHandler = previewHandler;
        this.previewMessage = previewMessage;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // zbar 解码
        Camera.Size size = camera.getParameters().getPreviewSize();
        byte[] rotatedData = new byte[data.length];
        System.arraycopy(data, 0, rotatedData, 0, data.length);
        Image barcode = new Image(size.width, size.height, "Y800");
        barcode.setData(rotatedData);
        int result = mImageScanner.scanImage(barcode);
        if (result != 0) {
            this.camera.getCamera().setPreviewCallback(null);
            this.camera.getCamera().stopPreview();
            SymbolSet syms = mImageScanner.getResults();
            for (Symbol sym : syms) {
                Log.d("scam: ", sym.getData());
                Handler thePreviewHandler = previewHandler;
                if (thePreviewHandler != null) {
                    Message message = thePreviewHandler.obtainMessage(previewMessage, sym.getData());
                    message.sendToTarget();
                }
            }
        }


        //zxing 解码
       /* Point cameraResolution = configManager.getCameraResolution();
        Handler thePreviewHandler = previewHandler;
        if (cameraResolution != null && thePreviewHandler != null) {
            Message message = thePreviewHandler.obtainMessage(previewMessage, cameraResolution.x,
                    cameraResolution.y, data);
            message.sendToTarget();
            previewHandler = null;
        } else {
            Log.d(TAG, "Got preview callback, but no handler or resolution available");
        }*/

    }

}
