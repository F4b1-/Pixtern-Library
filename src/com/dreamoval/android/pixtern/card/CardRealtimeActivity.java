package com.dreamoval.android.pixtern.card;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import com.dreamoval.android.pixtern.card.model.Card;
import com.dreamoval.android.pixtern.card.utils.DataHolder;
import com.dreamoval.android.pixtern.realtime.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class that opens the Android camera and provides the functionality to take a picture for the card validation.
 */
public class CardRealtimeActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "OCVSample::Activity";
	private boolean matchingCard;
	private Card card;
	private String resource;
	private Point facetl;
	private Point facebr;
	private Point texttl;
	private Point textbr;
	private Point tl;
	private Point br;
	private double patternThresh;
	private SurfaceHolder sHolder; 
	private Camera mCamera;
	private Parameters parameters;
	private boolean firstClick;
	private boolean showOutline;

	private String cardType;

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
		}
	}

	/** Loads all the OpenCV Dependencies and set ups the library for usage. **/
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				drawTemplateOutline();
			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}

		}
	};

	public CardRealtimeActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		firstClick = false;
		showOutline = true;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card_main);
			card = new Card();
			matchingCard = false;
			resource = "";
			facetl = new Point(0, 0);
			facebr = new Point(0, 0);
			texttl = new Point(0, 0);
			textbr = new Point(0, 0);
			tl = new Point(0, 0);
			br = new Point(0, 0);

			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);

			findViewById(R.id.fd_activity_surface_view).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					if(firstClick == false ) setUpCamera();
					else {
						//	mCamera.takePicture(null, null, null, mCall);
						mCamera.autoFocus(new AutoFocusCallback() {
							@Override
							public void onAutoFocus(boolean success, Camera camera) {
								if(success)
								{                           
									Log.d(TAG, "Focusing...successful.");
									camera.takePicture(null, null, null, mCall);
								}
								else
								{
									Log.d(TAG, "Focusing...failed.");
									camera.takePicture(null, null, null, mCall);
								}

							}
						});
					}
				}
			});

			View buttonBack = findViewById(R.id.back);
			buttonBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					finish();
				}
			});

			new AlertDialog.Builder(this)
			.setTitle("Card Validation")
			.setMessage("Turn your phone and click on screen to take picture")
			.setPositiveButton("Open", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 
					setUpCamera();
				}
			})
			.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 
					finish();
				}
			})
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();
	}

	/**
	 * Draws the outline of the Pattern to the preview screen.
	 */
	public void drawTemplateOutline() {
		//Load the template outline 
		cardType = DataHolder.getInstance().getData();
		if(!cardType.equals("-DETECT-")) getTemplate();
		Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.card_frame);
		Mat outline = new Mat ( icon.getHeight(), icon.getWidth(), CvType.CV_8U, new Scalar(0, 0, 0));
		Utils.bitmapToMat(icon, outline);
		Imgproc.cvtColor(outline, outline, Imgproc.COLOR_BGRA2RGBA);
		if(showOutline) {
			for (String key : card.getPatternMap().keySet()) {
				if(card.getPattern(key).getResource().matches("[t][e][x][t].*")) Core.rectangle(outline, new Point(Math.abs(outline.cols() - (card.getPattern(key).getTl().y * outline.cols())), card.getPattern(key).getTl().x * outline.rows()),new Point(Math.abs(outline.cols() - (card.getPattern(key).getBr().y * outline.cols())), card.getPattern(key).getBr().x * outline.rows()), new Scalar(0, 255, 0, 255), 1);
				//Core.rectangle(outline, new Point(Math.abs(outline.cols() - (card.getPattern(key).getTl().y * outline.cols())), card.getPattern(key).getTl().x * outline.rows()),new Point(Math.abs(outline.cols() - (card.getPattern(key).getBr().y * outline.cols())), card.getPattern(key).getBr().x * outline.rows()), new Scalar(255, 0, 0, 0), 1);
			}
			Core.rectangle(outline, new Point(Math.abs(outline.cols() - (facetl.y * outline.cols())), facetl.x * outline.rows()),new Point(Math.abs(outline.cols() - (facebr.y * outline.cols())),facebr.x * outline.rows()), new Scalar(0, 255, 0, 255), 1);
		}
		Bitmap bimage = Bitmap.createBitmap(outline.cols(),  outline.rows(),Bitmap.Config.ARGB_8888);
		Imgproc.cvtColor(outline, outline, Imgproc.COLOR_RGBA2BGRA);
		Utils.matToBitmap(outline, bimage);
		ImageView imgV = (ImageView )findViewById(R.id.frame_det);
		imgV.setImageBitmap(bimage);
	}


	//	public void startCameraPreview() {
	//		setUpCamera();
	//	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		//OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
		//mLoaderCallback); 
		mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try{
			mCamera.release();	
		} catch(Exception e) {
		}
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
	}

	@Override
	public void onCameraViewStopped() {
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		return inputFrame.rgba();
	}

	/**
	 * Starts setting up the camera.
	 */
	private void setUpCamera(){
		openAndroidCamera();
		firstClick = true;
	}

	/**
	 * Enables and disables the ability to take pictures by clicking on the button
	 * @param enabled True or false for Button is enabled/disabled
	 */
	public void accessView(final boolean enabled) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				findViewById(R.id.buttonLoadPicture).setEnabled(enabled);
			}
		});
	}
	/**
	 * Opens the Android Camera with the specified parameters.
	 */
	@SuppressWarnings("deprecation")
	public void openAndroidCamera() {
		// TODO Auto-generated method stub 
		mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
		SurfaceView sv = (SurfaceView) findViewById(R.id.fd_activity_surface_view);
		//Set correct width/height ratio of preview
		Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		float newProportion = (float) width / (float) height;
		// Get the width of the screen
		int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
		int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
		float screenProportion = (float) screenWidth / (float) screenHeight;
		mCamera.setDisplayOrientation(90);
		try {
			mCamera.setPreviewDisplay(sv.getHolder());
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
			Camera.Parameters params = mCamera.getParameters();
			//		set Size relative to screen size
			List<android.hardware.Camera.Size> sizes = params.getSupportedPictureSizes();
			List<android.hardware.Camera.Size> previewSize = params.getSupportedPreviewSizes();
			float bestProportion = (float) previewSize.get(0).width /(float) previewSize.get(0).height;
			android.hardware.Camera.Size bestSize = previewSize.get(0);
			for(android.hardware.Camera.Size theSize: previewSize) {
				float sizeProportion = (float) theSize.width /(float) theSize.height;
				if(Math.abs(sizeProportion - screenProportion) < bestProportion && theSize.height > 700) {
					bestSize = theSize;
					bestProportion = Math.abs(sizeProportion - screenProportion);
					params.setPreviewSize(bestSize.width, bestSize.height);
					params.setPictureSize(bestSize.width, bestSize.height);
				}
			} 

			//	params.setPictureSize(params.getPreviewSize().width, params.getPreviewSize().height);
			//	set flash for Voter's ID
			//			if(cardType.equals("-VOTERSID-")) {
			//				if(params.getFlashMode() != null) {
			//					params.setFlashMode(Parameters.FLASH_MODE_AUTO);	
			//				}
			//			}
			try {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);	
				mCamera.setParameters(params);
			} catch(Exception e) {
				Log.w("Focus", "Continuous picture not supported");
			}

			mCamera.startPreview();
			//			mCamera.setPreviewCallback(previewCallback);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Get a surface
		sHolder = sv.getHolder();
		//tells Android that this surface will have its data constantly replaced
		sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		try {
			mCamera.setPreviewDisplay(sHolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	Camera.PictureCallback mCall = new Camera.PictureCallback()
	{
		@Override
		public void onPictureTaken(byte[] data, Camera camera)
		{
			//start new Thread to save Picture
			new SavePhotoTask().execute(data);
		}
	};

	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * Thread to save the photo that has been taken.
	 */
	class SavePhotoTask extends AsyncTask<byte[], String, String> {
		@Override
		protected String doInBackground(byte[]... data) {
			Bitmap bm = BitmapFactory.decodeByteArray(data[0] , 0, data[0] .length);
			String success = MediaStore.Images.Media.insertImage(getContentResolver(), bm, "PixternImg", "Picture taken by Pixtern");
			return success;
		}

		@Override
		protected void onPostExecute(String result) {
			Intent laresult = new Intent();
			DataHolder.getInstance().setCardPath(result);
			laresult.putExtra("theSelfie", result);
			setResult(Activity.RESULT_OK, laresult);
			mCamera.release();
			finish();  
		}
	}

	/**
	 * Camera preview Callback. Realtime validation can be added later.
	 */
	Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {

		}
	};

	/** Reads the rules template file for the Pattern outline that has to be drawn*/
	public void getTemplate() {
		BufferedReader reader;
		try {
			final InputStream file = getResources()
					.openRawResource(R.raw.rules);
			reader = new BufferedReader(new InputStreamReader(file));
			String line = reader.readLine();
			while (line != null) {
				Log.d("RulesTemplate", line);
				if (line.equals(cardType)) matchingCard = true;
				if(matchingCard) {
					if (line.equals("CARDEND")) matchingCard = false;	
					if (line.equals("PATTERNEND")) {card.addPattern(resource, new Point(tl.x, tl.y), new Point(br.x, br.y), patternThresh); }
					else {
						try {
							String[] parts = line.split("=");
							String first = parts[0];
							String second = parts[1];
							if(first.equals("face.area.tl")) {
								String[] substr = second.split(",");
								facetl.x = Double.valueOf(substr[0]); 
								facetl.y = Double.valueOf(substr[1]);
							}
							if(first.equals("face.area.br")) {
								String[] substr = second.split(",");
								facebr.x = Double.valueOf(substr[0]); 
								facebr.y = Double.valueOf(substr[1]);
							}
							if(first.equals("text.area.tl")) {
								String[] substr = second.split(",");
								texttl.x = Double.valueOf(substr[0]); 
								texttl.y = Double.valueOf(substr[1]);
							}
							if(first.equals("text.area.br")) {
								String[] substr = second.split(",");
								textbr.x = Double.valueOf(substr[0]); 
								textbr.y = Double.valueOf(substr[1]);
							}
							if (first.equals("pattern.resource")) resource = second;
							if (first.equals("pattern.threshold")) patternThresh = Double.valueOf(second);
							if(first.equals("pattern.area.tl")) {
								String[] substr = second.split(",");
								tl.x = Double.valueOf(substr[0]); 
								tl.y = Double.valueOf(substr[1]);
							}
							if(first.equals("pattern.area.br")) {
								String[] substr = second.split(",");
								br.x = Double.valueOf(substr[0]); 
								br.y = Double.valueOf(substr[1]);
							}
							if(first.equals("show.outline")) {
								showOutline = Boolean.valueOf(second);
								Log.w("OUTLINE", "" + Boolean.valueOf(second));
							}
						}
						catch(Exception e) {
						}
					}
				}
				line = reader.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}



}