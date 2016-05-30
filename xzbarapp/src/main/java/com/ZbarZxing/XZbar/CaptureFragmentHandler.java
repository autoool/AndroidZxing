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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class CaptureFragmentHandler extends Handler {

	private static final int decode_id = 0xEE;
	private static final int decode_failed_id = 0x11;
	private static final int decode_succeeded_id = 0x22;
	private static final int quit_id = 0x33;
	private static final int restart_preview_id = 0x44;
	private static final int return_scan_result_id = 0x55;


	private final ScanBarFragment mScanBarFragment;
	private final DecodeThreadFragment decodeThread;
	private final CameraManager cameraManager;
	private State state;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	public CaptureFragmentHandler(ScanBarFragment fragment, CameraManager cameraManager, int decodeMode) {
		this.mScanBarFragment = fragment;
		decodeThread = new DecodeThreadFragment(fragment, decodeMode);
		decodeThread.start();
		state = State.SUCCESS;

		// Start ourselves capturing previews and decoding.
		this.cameraManager = cameraManager;
		cameraManager.startPreview();
		restartPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case restart_preview_id:
			restartPreviewAndDecode();
			break;
		case decode_succeeded_id:
			state = State.SUCCESS;
			Bundle bundle = message.getData();
			mScanBarFragment.handleDecode((String) message.obj, bundle);
			break;
		case decode_failed_id:
			// We're decoding as fast as possible, so when one decode fails,
			// start another.
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(), decode_id);
			break;
		case return_scan_result_id:
			break;
		}
	}

	public void quitSynchronously() {
		state = State.DONE;
		cameraManager.stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), quit_id);
		quit.sendToTarget();
		try {
			// Wait at most half a second; should be enough time, and onPause()
			// will timeout quickly
			decodeThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(decode_succeeded_id);
		removeMessages(decode_failed_id);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(), decode_id);
		}
	}

}
