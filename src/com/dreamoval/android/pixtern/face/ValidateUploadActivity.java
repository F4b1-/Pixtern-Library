package com.dreamoval.android.pixtern.face;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.dreamoval.android.pixtern.realtime.R;
import com.todddavies.components.progressbar.ProgressWheel;
/**
 * This class provides functionality to upload a picture of a card and validate it.
 */
public class ValidateUploadActivity extends Activity {
	private CascadeClassifier cascadeClassifier;
	private CascadeClassifier cascadeClassifier2;
	private CascadeClassifier cascadeClassifier3;
	private Mat origIm;
	private Mat image;
	private int incremented;

	private String picturePath;
	private String picturePath2;
	private int facesNumber;
	private int eyesNumber;
	private int mouthNumber;

	private boolean faceDetect;
	private boolean eyesDetect;
	private boolean mouthDetect;
	private boolean brightDetect;
	private int lowerLimit1;
	private int lowerLimit2;
	private int upperLimit1;
	private int upperLimit2;
	private float brightness;
	private static int RESULT_LOAD_IMAGE = 1;

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
		}
	}

	/** Loads all the OpenCV Dependencies and set ups the library for usage**/
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				initializeOpenCVDependencies();
				
			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
			
		}
	};

	@Override
	public void onResume()
	{
		super.onResume();
//		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
//		mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
	}
	/** Loads the Classifier file for face detection and initializes a Classifier Variable **/
	private void initializeOpenCVDependencies() {
		try {
			// Setting up the cascade classifiers
			InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
			File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
			File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
			FileOutputStream os = new FileOutputStream(mCascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();
			// Load the cascade classifier
			cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
		} catch (Exception e) {
			Log.e("OpenCVActivity", "Error loading cascade", e);
		}

		try {
			// Copy the resource into a temp file so OpenCV can load it
			InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
			File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
			File mCascadeFile = new File(cascadeDir, "lbpcascade_eye.xml");
			FileOutputStream os = new FileOutputStream(mCascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();
			// Load the cascade classifier
			cascadeClassifier2 = new CascadeClassifier(mCascadeFile.getAbsolutePath());
		} catch (Exception e) {
			Log.e("OpenCVActivity", "Error loading cascade", e);
		}
		try {
			// Copy the resource into a temp file so OpenCV can load it
			InputStream is = getResources().openRawResource(R.raw.haarcascade_mcs_mouth);
			File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
			File mCascadeFile = new File(cascadeDir, "haarcascade_mcs_mouth.xml");
			FileOutputStream os = new FileOutputStream(mCascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();
			// Load the cascade classifier
			cascadeClassifier3 = new CascadeClassifier(mCascadeFile.getAbsolutePath());
		} catch (Exception e) {
			Log.e("OpenCVActivity", "Error loading cascade", e);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		incremented = 0;
		facesNumber = 0;
		eyesNumber = 0;
		mouthNumber = 0;
		faceDetect = false;
		eyesDetect = false;
		mouthDetect = false;
		brightDetect = false;
		lowerLimit1 = 0;
		lowerLimit2 = 0;
		upperLimit1 = 400;
		upperLimit2 = 400;
		brightness = 0;
		
		setContentView(R.layout.main);
		mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
		Intent i = new Intent(
				Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, RESULT_LOAD_IMAGE);

		View buttonLoadImage = findViewById(R.id.buttonLoadPicture);
		buttonLoadImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(
						Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				startActivityForResult(i, RESULT_LOAD_IMAGE);
			}
		});
		buttonLoadImage.setEnabled(false);

		View buttonBack = findViewById(R.id.back);
		buttonBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				arg0.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				finish();
			}
		});

		ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
		pBar.setVisibility(View.GONE);
	}
	
	/** Receives the path of the uploaded image and starts the background thread for Validation**/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//	 initializeOpenCVDependencies();
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaColumns.DATA };
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			picturePath = cursor.getString(columnIndex);
			cursor.close();
			picturePath2 = picturePath;

			new LongOperation().execute("");
		}
	}
	/** Loads the template file and sets the detection variables accordingly **/
	public void getTemplate() {
		BufferedReader reader;
		try{
			final InputStream file = getResources().openRawResource(R.raw.rules);
			reader = new BufferedReader(new InputStreamReader(file));
			String line = reader.readLine();
			while(line != null){
				Log.d("RulesTemplate", line);
				try{
					String[] parts = line.split("=");
					String first = parts[0];
					String second = parts[1];
					if(first.equals("face.detection")) faceDetect = Boolean.valueOf(second);
					if(first.equals("eyes.detection")) eyesDetect = Boolean.valueOf(second);
					if(first.equals("mouth.detection")) mouthDetect = Boolean.valueOf(second);
					if(first.equals("brightness.detection")) brightDetect = Boolean.valueOf(second);

					if(first.equals("distance.limit.lower")) {
						String[] substr = second.split(",");
						lowerLimit1 = Integer.valueOf(substr[0]); 
						lowerLimit2 = Integer.valueOf(substr[1]);
					}

					if(first.equals("distance.limit.upper")) {
						String[] substr = second.split(",");
						upperLimit1 = Integer.valueOf(substr[0]); 
						upperLimit2 = Integer.valueOf(substr[1]);
					}
				}
				catch(Exception e) {
				}
				line = reader.readLine();
			}
		} catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Detects the traits and returns the number of detections
	 * @param cascade Classifier for the Detection
	 * @param trait Name of the trait to be detected
	 **/
	public int traitDetection(CascadeClassifier cascade, String trait) {
		MatOfRect traits = new MatOfRect();
		// Classifier used for Trait Detection
		if (cascade != null) {
			//	if(cascade == cascadeClassifier2) cascade.detectMultiScale(image, traits, 1.05, 20, 0, new Size(0, 0), new Size(200, 200)); //if eyes
			if(trait.equals("eyes"))  cascade.detectMultiScale(image, traits, 1.05, 10, 0, new Size(0, 0), new Size(300, 300));//	else if(cascade == cascadeClassifier3) cascade.detectMultiScale(image, traits, 1.04, 80, 0, new Size(0, 0), new Size(300, 300)); //if mouth
			else if(trait.equals("mouths")) cascade.detectMultiScale(image, traits, 1.45, 10, 0, new Size(0, 0), new Size(200, 200)); 
			else cascade.detectMultiScale(image, traits, 1.05, 6, 0, new Size(lowerLimit1, lowerLimit2), new Size(upperLimit1, upperLimit2)); //faces
		}

		Rect[] facesArray =traits.toArray();
		for (int i = 0; i <facesArray.length; i++) {
			Core.rectangle(origIm, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 2);
		}
		return facesArray.length;
	}

	/** Changes image to grayscale, equalises its histogram and resizes it **/
	public void imageProc() {
		Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
		Imgproc.equalizeHist(image, image);
		int heightNew = 400;
		int widthNew = ((image.width() * heightNew) / image.height());
		Imgproc.resize(image, image, new Size(widthNew, heightNew));
		Imgproc.resize(origIm, origIm, new Size(widthNew, heightNew));

	}

	/** Runs the background thread for validations.**/
	private class LongOperation extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			View buttonLoadImage = findViewById(R.id.buttonLoadPicture);
			publishProgress("10");
			try {
			Bitmap bimage2 = BitmapFactory.decodeFile(picturePath2);
			origIm = new Mat ( bimage2.getHeight(), bimage2.getWidth(), CvType.CV_8U, new Scalar(4));
			Utils.bitmapToMat(bimage2, origIm);		
			Bitmap bimage = BitmapFactory.decodeFile(picturePath);
			image = new Mat ( bimage.getHeight(), bimage.getWidth(), CvType.CV_8U, new Scalar(4));
			Utils.bitmapToMat(bimage, image);
			
			// to grayscale and resize
			imageProc();
			// Template Validation
			getTemplate();
			// Perform Validation
			publishProgress("25");
			if(faceDetect == true) {
				facesNumber = traitDetection(cascadeClassifier, "faces");
			}	
			publishProgress("50");
			if(eyesDetect == true) {
				eyesNumber = traitDetection(cascadeClassifier2, "eyes");
			}
			publishProgress("75");
			if(mouthDetect == true) {
				mouthNumber = traitDetection(cascadeClassifier3, "mouths");
			}
			publishProgress("95");
			if(brightDetect) brightness = getBright();
			
			return "success";
			
			} catch(Exception e) {
				Log.w("Error", "Loading image error");
				return "failure";
			}
		}

		/** Finishes activity and returns validation results to calling application**/
		@Override
		protected void onPostExecute(String result) {
			if(result.equals("success")) {
			HashMap<String, Integer> theText = new HashMap<String, Integer>(); 
			theText.put("Faces", facesNumber);
			theText.put("Eyes", eyesNumber);
			theText.put("Mouths", mouthNumber);
			theText.put("Brightness", (int)brightness);
			
			//textView.setText(theText);  
			Bitmap resultBitmap = Bitmap.createBitmap(origIm.cols(),  origIm.rows(),Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(origIm, resultBitmap);
			ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
			pBar.setVisibility(View.GONE);
//			ImageView imageView = (ImageView) findViewById(R.id.imgView);
//			imageView.setVisibility(View.VISIBLE);
////		imageView.setImageBitmap(resultBitmap);
			
			//Reset ProgressBar 
			incremented = 0;
			pBar.setProgress(360);
			//prepare return values and end the activity	
			Intent theResult = new Intent();
			theResult.putExtra("theSelfie",picturePath);
//			ByteArrayOutputStream stream = new ByteArrayOutputStream();
//			resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//			byte[] byteArray = stream.toByteArray();
//			theResult.putExtra("valIm", byteArray);
			theResult.putExtra("theValidation", theText);
			setResult(Activity.RESULT_OK, theResult);
			finish();
			} else {
				Log.w("Error", "Loading image error");
				HashMap<String, String> theText = new HashMap<String, String>(); 
				theText.put("Error", "Image Loading");
				Intent theResult = new Intent();
				theResult.putExtra("theValidation", theText);
				setResult(Activity.RESULT_OK, theResult);
				finish();
			}
		}

		@Override
		protected void onPreExecute() {}

		@Override
		protected void onProgressUpdate(String... values) {
			ImageView imageView = (ImageView) findViewById(R.id.imgView);
			imageView.setVisibility(View.GONE);
			ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
			pBar.setVisibility(View.VISIBLE);
			//Calculation to update progressBar
			double val = Integer.valueOf(values[0]) / 100.0;	
			val = val * 360;
			double nowToIncr = val - incremented;
			for(int i= 0; i<= nowToIncr; i++) pBar.incrementProgress();
			incremented = (int)val;
		}
	}

	/** Calculates brightness of image **/
	public float getBright() {
		float brightness = 0;
		Mat n = new Mat();
		// Convert it to  YCrCb 
		Imgproc.cvtColor(origIm, n, Imgproc.COLOR_BGR2YCrCb);
		Mat y = new Mat();
		// Only use Y channel and calculate mean brightness
		Core.extractChannel(n, y, 0);
		for(int j = 0;j < y.rows();j++){
			for(int i = 0;i < y.cols();i++){
				brightness += y.get(j, i)[0];
			}
		}
		return (brightness / (y.rows() * y.cols()) );
	}
}