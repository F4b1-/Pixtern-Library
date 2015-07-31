// http://snippi.com/s/2kjhhbh
package com.dreamoval.android.pixtern.face;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
















import com.dreamoval.android.pixtern.realtime.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Opens the OpenCV Java Camera and provides the functionality to take a picture for the face validation.
 */
public class FdActivity extends Activity implements 
SensorEventListener {

	private static final String TAG = "OCVSample::Activity";
	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private CascadeClassifier mJavaDetectorE;

	private int percentageToPass;
	private boolean faceDetect;
	private boolean eyesDetect;
	private boolean autoSnap;
	private boolean snapOnlyWhenDet;
	private String ErrorMess = "Eyes not detected";

	private Mat cropedImage;
	private String[] mDetectorName;
	private float mRelativeFaceSize;
	private int mAbsoluteFaceSize;
	private int counter;
	private LongOperation op;
	boolean[] dRateFace;
	private int height;
	private int width;
	private int totalDRate;
	private SensorManager sMgr;
	private Sensor axis;
	private float rotation;
	private CameraBridgeViewBase mOpenCvCameraView;
	private int currentCam;
	private boolean flashOn;

	private SurfaceHolder sHolder; 
	private Camera mCamera;
	private Parameters parameters;


	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
		}
	}

	/** Loads all the OpenCV Dependencies and set ups the library for usage. */
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				//Load native library after(!) OpenCV initialization
				try {
					//load cascade file from application resources
					InputStream is = getResources().openRawResource(
							R.raw.haarcascade_frontalface_default);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir,
							"haarcascade_frontalface_default.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();
					mJavaDetector = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());
					if (mJavaDetector.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetector = null;
					} else
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

					cascadeDir.delete();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}

				try {
					// load cascade file from application resources
					InputStream is = getResources().openRawResource(
							R.raw.haarcascade_eye);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir, "lbpcascade_eye.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					mJavaDetectorE = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());
					if (mJavaDetectorE.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetectorE = null;
					} else
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());
					cascadeDir.delete();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}
			}
			break;
			default: {
				super.onManagerConnected(status);
			}
			break;
			}
		}
	};

	public FdActivity() {
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Initialising the default variables
		faceDetect = false;
		eyesDetect = false;
		autoSnap = false;
		dRateFace = new boolean[5];
		op = new LongOperation();
		snapOnlyWhenDet = true;
		flashOn = false;
		percentageToPass = 50;
		currentCam = 1;
		totalDRate = 0;
		mRelativeFaceSize = 0.2f;
		mAbsoluteFaceSize = 0;

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		//check for CPU of phone
		if(android.os.Build.CPU_ABI.matches("[a][r][m].*")) {
			new AlertDialog.Builder(this)
			.setTitle("Face Validation")
			.setMessage("Place your face in the rectangle")
			.setPositiveButton("Open", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 
					openCamera();
				}
			})
			.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 
					finish();
				}
			})
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();

			//Set up a sensor
			sMgr = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
			axis = sMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			setContentView(R.layout.face_detect_surface_view);
			//mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
			//Gets the display dimensions and sets the ratio of the frame size accordingly
			getTemplate();
			findViewById(R.id.buttonLoadPicture).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Toast.makeText(findViewById(R.id.fd_activity_surface_view).getContext(), "Picture is being taken", Toast.LENGTH_LONG).show();
					arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					takePicture();
				}
			});
			if(snapOnlyWhenDet)findViewById(R.id.buttonLoadPicture).setEnabled(false);
			View buttonBack = findViewById(R.id.back);
			buttonBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					finish();
				}
			});
			//Rotate Button Functionality
			View buttonRotate = findViewById(R.id.rotate);
			buttonRotate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					if(currentCam == 1) currentCam = -1;
					else currentCam = 1;
					openCamera();
				}
			});
			//Flash Button Functionality
			View buttonFlash = findViewById(R.id.flash);
			buttonFlash.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					if(flashOn == false) {
						flashOn = true;
						Toast.makeText(findViewById(R.id.frame_det).getContext(), "Flash: on", Toast.LENGTH_LONG).show();
						openCamera();
					}
					else if (flashOn == true){
						flashOn = false;
						Toast.makeText(findViewById(R.id.frame_det).getContext(), "Flash: off", Toast.LENGTH_LONG).show();
						openCamera();
					}

				}
			});
			TextView textV =	(TextView) findViewById(R.id.textView);
			textV.setText(ErrorMess);
			counter = 0;	
		}  else {
			Intent laresult = new Intent();
			laresult.putExtra("theSelfie", "CPU Error");
			setResult(Activity.RESULT_OK, laresult);
			finish();
		}
	}

	public void takePicture() {
		mCamera.setPreviewCallback(null);
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


	/**
	 * Closes the OpenCV camera and opens the android camera to take a picture.
	 * @param percent - current detection rate.
	 */
	private void isitOK(int percent){
		if(percent >= percentageToPass)
		{
			takePicture();
		} 
	}


	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
		rotation = event.values[2];
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		//		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
		//				mLoaderCallback); 
		mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		sMgr.registerListener(this, axis, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			mCamera.release();
			mCamera.setPreviewCallback(null);
		} catch(Exception e) {

		}
	}

	/** Reads the rules template file and sets up card object accordingly. */
	public void getTemplate() {
		BufferedReader reader;
		try {
			final InputStream file = getResources()
					.openRawResource(R.raw.rules);
			reader = new BufferedReader(new InputStreamReader(file));
			String line = reader.readLine();
			while (line != null) {
				Log.d("RulesTemplate", line);
				try {
					String[] parts = line.split("=");
					String first = parts[0];
					String second = parts[1];
					if (first.equals("face.detection"))
						faceDetect = Boolean.valueOf(second);
					if (first.equals("eyes.detection"))
						eyesDetect = Boolean.valueOf(second);
					if (first.equals("relative.face.size"))
						mRelativeFaceSize = Float.valueOf(second);
					if (first.equals("percentage.to.pass"))
						percentageToPass = Integer.valueOf(second);
					if (first.equals("auto.snap"))
						autoSnap = Boolean.valueOf(second);
					if (first.equals("snap.when.detected"))
						snapOnlyWhenDet = Boolean.valueOf(second);
					if (first.equals("error.message"))
						ErrorMess = second;
				}
				catch(Exception e) {
				}
				line = reader.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Calculates the detection rate.
	 * @param theEyes Number of eye detections 
	 */
	public void calcRate(int theEyes) {
		float countTrue = 0;
		for (int i=0; i<dRateFace.length; i++) {
			if (dRateFace[i]) countTrue++;
		}
		if(theEyes > 2) theEyes = 2;
		totalDRate = Math.round(((countTrue + theEyes) / (dRateFace.length + 2)) * 100);
		if(autoSnap == true) isitOK(totalDRate);
	}

	/**
	 * Turns Mat for OpenCV detection.
	 */
	public void turnMat(Mat mGray) {
		Mat mRgbaT = mGray;
		Core.flip(mGray.t(), mRgbaT, -1);
		mGray = mRgbaT;
	}

	/**
	 * Turns Mat for OpenCV to detection.
	 */
	public void turnMatLandscape(Mat mGray) {
		Mat mRgbaT = mGray.t();
		Core.flip(mGray.t(), mRgbaT, 1);
		Imgproc.resize(mRgbaT, mRgbaT, mGray.size());
		mGray = mRgbaT;
	}

	/**
	 * Enables and disables the ability to take pictures by clicking on the button
	 * @param enabled True or false for Button is enabled/disabled
	 */
	public void accessView(final boolean enabled) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				findViewById(R.id.buttonLoadPicture).setEnabled(enabled); //Unable to push to take pic
			}
		});
	}

	/**
	 * Executes the validation Thread and sets the UI accordingly.
	 */
	private class LongOperation extends AsyncTask<byte[], Integer, String> {
		@Override
		protected String doInBackground(byte[]... params) {
			Mat mRgba = new Mat();
			Mat mGray = new Mat();
			//Turn bite[] into Mat and resize, turn to grayscale
			Camera.Size previewSize = mCamera.getParameters().getPreviewSize(); 
			YuvImage yuvimage=new YuvImage(params[0], ImageFormat.NV21, previewSize.width, previewSize.height, null);
			OutputStream baos = new ByteArrayOutputStream();
			yuvimage.compressToJpeg(new android.graphics.Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
			byte[] jdata = ((ByteArrayOutputStream) baos).toByteArray();
			Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
			mRgba = new Mat ( bmp.getHeight(), bmp.getWidth(), CvType.CV_8U, new Scalar(4));
			Utils.bitmapToMat(bmp, mRgba);
			int heightNew = 300;
			int widthNew = ((mRgba.width() * heightNew) / mRgba.height());
			Imgproc.resize(mRgba, mRgba, new Size(widthNew, heightNew));
			mGray = mRgba;
			Imgproc.cvtColor(mGray, mGray, Imgproc.COLOR_RGB2GRAY);

			//Rotate mGray for openCV to detect.
			if(rotation > -20 && rotation < 20) {
				turnMat(mGray);
			}
			else if(rotation < -20 && rotation > -60) {
				turnMatLandscape(mGray);
				turnMatLandscape(mGray);
			}
			try {
				Rect roi = new Rect(new Point(mGray.cols() * 0.1 , mGray.rows() * 0.1), new Point(mGray.cols() * 0.9, mGray.rows() * 0.9));
				cropedImage = mGray.submat(roi);	
			}
			catch(Exception e) {
				Log.w("ROI", "There was an ROI error");
				cropedImage = mGray;
			}

			if (mAbsoluteFaceSize == 0) {
				int height = mGray.rows();
				if (Math.round(height * mRelativeFaceSize) > 0) {
					mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
				}

			}
			//Trait Detection
			MatOfRect faces = new MatOfRect();
			MatOfRect eyes = new MatOfRect();
			//Face Detection
			if (faceDetect == true) {
				if (mJavaDetector != null)
					mJavaDetector.detectMultiScale(cropedImage, faces, 1.1, 1, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
				if (faces.toArray().length >= 1) {
					dRateFace[counter] = true;
					if(totalDRate > percentageToPass && snapOnlyWhenDet == true) {
						accessView(true);
					}
					//Eye Detection
					Rect eyesRoi = new Rect(new Point(0, cropedImage.rows() * 0.2), new Point(cropedImage.cols(), cropedImage.rows() * 0.8));
					Mat eyesM = cropedImage.submat(eyesRoi);

					if (eyesDetect == true) {
						if (mJavaDetectorE != null)
							//							mJavaDetectorE.detectMultiScale(eyesM, eyes, 1.1, 2, 0, new Size(0, 0), new Size(height * 0.3, height * 0.3));		
							mJavaDetectorE.detectMultiScale(eyesM, eyes, 1.1, 20, 0, new Size(0, 0), new Size(height * 0.3, height * 0.3));
					}
				}
				//No faces found
				else {
					dRateFace[counter] = false;
					if(snapOnlyWhenDet && totalDRate > percentageToPass ) {
						accessView(false);
					}
				}
				Rect[] facesArray = faces.toArray();
				//Calculate Detection Rate
				calcRate(eyes.toArray().length);	
				//Change UI according to Detections 
				if(facesArray.length > 0) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ImageView imageView = (ImageView) findViewById(R.id.frame_not);
							imageView.setVisibility(View.GONE);
							ImageView imageView2 = (ImageView) findViewById(R.id.frame_det);
							imageView2.setVisibility(View.VISIBLE);
							JavaCameraView javaView = (JavaCameraView) findViewById(R.id.fd_activity_surface_view);
							javaView.setBackgroundColor(Color.TRANSPARENT);
						}
					});
				}
				//No faces found
				else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ImageView imageView2 = (ImageView) findViewById(R.id.frame_det);
							imageView2.setVisibility(View.GONE);
							ImageView imageView = (ImageView) findViewById(R.id.frame_not);
							imageView.setVisibility(View.VISIBLE);
							JavaCameraView javaView = (JavaCameraView) findViewById(R.id.fd_activity_surface_view);
							javaView.setBackgroundColor(0x32FF1019);
						}
					});
				}
				//Eyes were deteced
				if(eyes.toArray().length >= 2) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							TextView textView = (TextView) findViewById(R.id.textView);
							textView.setText("Eyes are detected");
							ImageView x = (ImageView)findViewById(R.id.x);
							x.setVisibility(View.INVISIBLE);
						}
					});
				}
				//Eyes were not detected
				else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							TextView textView = (TextView) findViewById(R.id.textView);
							textView.setText(ErrorMess);
							ImageView x = (ImageView)findViewById(R.id.x);
							x.setVisibility(View.VISIBLE);
						}
					});
				}
			}
			counter++;
			if(counter >= 5) counter = 0;
			return "sucess";
		}

		@Override
		protected void onPostExecute(String result) {
		}

		@Override
		protected void onPreExecute() {}

		@Override
		protected void onProgressUpdate(Integer... values) {
		}
	}

	/**
	 * Opens android camera.
	 */
	@SuppressWarnings("deprecation")
	public void openCamera() {
		if(mCamera != null) {
			//Unset Callbacl so it's not called after camera has been released
			mCamera.setPreviewCallback(null);
			mCamera.release();
		}
		//Open Camera
		if(currentCam == 1)	mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
		else mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
		SurfaceView sv = (SurfaceView) findViewById(R.id.fd_activity_surface_view);
		mCamera.setDisplayOrientation(90);
		try {
			mCamera.setPreviewDisplay(sv.getHolder());
			//Change Rotation of picture that is about to be taken.
			Camera.CameraInfo info = new Camera.CameraInfo();
			if(currentCam == 1) Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info);
			else Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
			int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
			int degrees = 0;
			switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break; //Natural orientation
			case Surface.ROTATION_90: degrees = 90; break; //Landscape left
			case Surface.ROTATION_180: degrees = 180; break;//Upside down
			case Surface.ROTATION_270: degrees = 270; break;//Landscape right
			}
			int rotate = (info.orientation - degrees + 360) % 360;

			Camera.Parameters params = mCamera.getParameters();
			params.setRotation(rotate); 

			//Enables flash if specified in rules file.
			if(flashOn == true) {
				if(params.getFlashMode() != null) {
					params.setFlashMode(Parameters.FLASH_MODE_TORCH);	
				}
			}
			//Enables focus on preview.
			try {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);	
			} catch(Exception e) {
				Log.w("Focus", "Continuous picture not supported");
			}
			try {
				mCamera.setParameters(params);
			}catch(Exception e) {

			}
			mCamera.startPreview();
			mCamera.setPreviewCallback(previewCallback);


		} catch (IOException e) {
			e.printStackTrace();
		}
		//Get a surface
		sHolder = sv.getHolder();
		//tells Android that this surface will have its data constantly replaced
		sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	Camera.PictureCallback mCall = new Camera.PictureCallback()
	{
		public void onPictureTaken(byte[] data, Camera camera)
		{
			//start new Thread to save picture
			new SavePhotoTask().execute(data);
		}
	};

	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Saves the Picture in the gallery.
	 */
	class SavePhotoTask extends AsyncTask<byte[], String, String> {
		@Override
		protected String doInBackground(byte[]... data) {
			Bitmap bm = BitmapFactory.decodeByteArray(data[0] , 0, data[0] .length);
			String success = "";
			//Rotate picture if front camera is used.
			if(android.os.Build.VERSION.SDK_INT>13 && currentCam == 1)
			{
				Matrix rotateRight = new Matrix();
				rotateRight.preRotate(90);
				float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
				rotateRight = new Matrix();
				Matrix matrixMirrorY = new Matrix();
				matrixMirrorY.setValues(mirrorY);
				rotateRight.postConcat(matrixMirrorY);
				rotateRight.preRotate(270);
				final Bitmap rImg= Bitmap.createBitmap(bm, 0, 0,
						bm.getWidth(), bm.getHeight(), rotateRight, true);

				success = MediaStore.Images.Media.insertImage(getContentResolver(), rImg, "PixternImg", "Picture taken by Pixtern");
			} else {
				success = MediaStore.Images.Media.insertImage(getContentResolver(), bm, "PixternImg", "Picture taken by Pixtern");
			}
			return success;
		}

		@Override
		protected void onPostExecute(String result) {
			Intent laresult = new Intent();
			laresult.putExtra("theSelfie", result);
			setResult(Activity.RESULT_OK, laresult);
			mCamera.release();
			finish(); 
		}
	}

	/**
	 * Camera preview Callback. Receives single frame of preview and starts validation.
	 */
	Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			if(op.getStatus() == AsyncTask.Status.FINISHED || op.getStatus() == AsyncTask.Status.PENDING) {
				op = new LongOperation(); 
				op.execute(data);
			}
			ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
			pb.setProgress(totalDRate);
			if(totalDRate >= percentageToPass) accessView(true);

		}
	};

}






