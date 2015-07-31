package com.dreamoval.android.pixtern.card;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.dreamoval.android.pixtern.card.model.Card;
import com.dreamoval.android.pixtern.card.utils.DataHolder;
import com.dreamoval.android.pixtern.realtime.R;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.todddavies.components.progressbar.ProgressWheel;
/**
 * This class provides functionality to upload a picture and validate different facial features.
 */
public class CardValidationActivity extends Activity {

	private boolean faceDetect;
	private CascadeClassifier cascadeClassifier;
	private String picturePath2;
	private Bitmap bimage;
	private String displayT;
	private int incremented;
	private String recognizedText;
	private Bitmap resultBitmap;
	private Mat origIm;
	private Mat showBit;

	private String cardType;
	private boolean matchingCard;
	private boolean removeBackground;
	private Card card;
	private String resource;

	private Point facetl;
	private Point facebr;
	private Point texttl;
	private Point textbr;

	private HashMap<String, String> theText;
	private Point tl;
	private Point br;
	private double patternThresh;

	private String extractName;
	private String extractPattern;
	private int extractLength;
	private int failureCount;
	private int patternCount;

	private TessBaseAPI baseApi;
	private static int RESULT_LOAD_IMAGE = 1;

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
				initializeOpenCVDependencies();
			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}

		}
	};

	/** Loads the Classifier file for face detection and initialises a Classifier Variable. **/
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
		setUp();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		//OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
		//mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.main);
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/**
	 * Sets up the activity according to the specified rules in the rules file after OpenCV initialisation has been successful.
	 */
	public void setUp() {
		showBit = new Mat();
		displayT = "";
		card = new Card();
		faceDetect=false;
		matchingCard = false;
		removeBackground = false;
		resource = "";
		facetl = new Point(0, 0);
		facebr = new Point(0, 0);
		texttl = new Point(0, 0);
		textbr = new Point(0, 0);
		tl = new Point(0, 0);
		br = new Point(0, 0);
		extractPattern = "";
		extractLength = 1;
		theText = new HashMap<String, String>();
		cardType = DataHolder.getInstance().getData();
		patternCount = 0;
		failureCount = 0;
		incremented = 0;
		
		if(!cardType.equals("-DETECT-")) getTemplate();
		Log.w("DataHolder", "" + DataHolder.getInstance().getData());
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

		//If activity is not called after image has been taken new intent is created and a new image can be picked from the Mediastore
		if(DataHolder.getInstance().getCardPath() == null) {
			Intent i = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, RESULT_LOAD_IMAGE);
		}
		//	If the application is called after an image has been taken(cardPath in DataHolder is not null) the Validation Thread is started instantly
		else {
			new LongOperation().execute("");
		}
	}

	/** Resolves the path of the image that was picked. */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaColumns.DATA };
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();
			picturePath2 = picturePath;
			new LongOperation().execute("");
		}
	}

	/** Runs the Main Validation Thread. **/
	private class LongOperation extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			copyResources(R.raw.eng);
			updateUI();
			//If activity is running on its own without being called by CardRealtime: Creates a Bitmap of the image picked by the user.
			if(picturePath2 != null) {
				resolveContentForStandAlone();
			}
			//If activity is called by CardRealtimeActivity: Get the bitmap of the picture whose path was stored in the DataHolder Class.
			else {
				resolveContentForCombined();
			}
			publishProgress("10");
			try {
				origIm = new Mat ( bimage.getHeight(), bimage.getWidth(), CvType.CV_8U, new Scalar(4));
				Utils.bitmapToMat(bimage, origIm);
				origIm.copyTo(showBit);
				if(showBit.height() > 1200) {
					int heightNew = 1200;
					int widthNew = ((showBit.width() * heightNew) / showBit.height());
					Imgproc.resize(showBit, showBit, new Size(widthNew, heightNew));
					bimage = Bitmap.createBitmap(showBit.cols(),  showBit.rows(),Bitmap.Config.ARGB_8888);
				}
				//If Card has to be detected: run setDetectionTemplate, reset global variables after detection
				if(cardType.equals("-DETECT-")) {
					setDetectionTemplate();
					card = new Card();
					theText = new HashMap<String, String>();
					getTemplate();
				}
				//If cardType is still -DETECT-, finish the application
				if(cardType.equals("-DETECT-")) {
					Intent theResult = new Intent();
					theText.put("Card", "none");
					theResult.putExtra("theValidation", theText);
					setResult(Activity.RESULT_OK, theResult);
					finish();
				}

				//Face Detection
				publishProgress("25");
				if(faceDetect == true) {
					detectFaces();
				}
				publishProgress("55");
				//Calls the templateMatching method.
				checkForPatterns();
				publishProgress("80");
				theText.put("Card", cardType);
				return "success";
			} catch(Exception e) {
				return "failure";
			}
		}

		/**
		 * Performs OpenCV Face Detection and sets up the results for the calling application.
		 */
		public void detectFaces() {
			Rect roiFace = new Rect(new Point(showBit.cols() * facetl.x , showBit.rows() * facetl.y), new Point(showBit.cols() * facebr.x, showBit.rows() * facebr.y));
			Mat cropedImageFace = showBit.submat(roiFace);
			Imgproc.cvtColor(cropedImageFace, cropedImageFace, Imgproc.COLOR_RGB2GRAY);

			MatOfRect traits = new MatOfRect();
			if (cascadeClassifier != null) {
//				cascadeClassifier.detectMultiScale(cropedImageFace, traits,  1.02, 2, 0, new Size(100, 100), new Size(500, 500)); //faces				
			//	1.05, 6, 0, new Size(lowerLimit1, lowerLimit2), new Size(upperLimit1, upperLimit2)
				cascadeClassifier.detectMultiScale(cropedImageFace, traits);
			}
			Rect[] facesArray =traits.toArray();
			for (int i = 0; i <facesArray.length; i++) {
				Core.rectangle(showBit, new Point(facesArray[i].tl().x + showBit.cols() * facetl.x, facesArray[i].tl().y + showBit.rows() * facetl.y) , new Point(facesArray[i].br().x + showBit.cols() * facetl.x, facesArray[i].br().y + showBit.rows() * facetl.y), new Scalar(0, 255, 0, 255), 4);
			}
			if(traits.toArray().length == 0) {
				failureCount++;
				theText.put("Face", "NOT FOUND");
			}
			else {
				theText.put("Face", "FOUND");
			}
			patternCount++;
		}		
		
		/**
		 * Uses the path to get the image as a Bitmap when the activity is running on its own.
		 */
		public void resolveContentForStandAlone() {
			try {
				bimage = BitmapFactory.decodeFile(picturePath2);	
			}catch(Exception e) {
				Log.w("Upload", "There was an error uploading the file");
				finish();
			}
		}
		
		/**
		 * Uses the path to get the image as a Bitmap when the activity has been called by CardRealtimeActivity.
		 */
		public void resolveContentForCombined() {
			InputStream is;
			try {
				is = getContentResolver().openInputStream(Uri.parse(DataHolder.getInstance().getCardPath()));
				bimage = BitmapFactory.decodeStream(is);
				is.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Executes the template Matching for every non Text Pattern that belongs to the card object.
		 */
		public void checkForPatterns() {
			int match_method = Imgproc.TM_CCOEFF_NORMED;
			for (String key : card.getPatternMap().keySet()) {
				if(!card.getPatternMap().get(key).getResource().matches("[t][e][x][t].*")) {
					try {	
						templateMatching(match_method, getApplicationContext().getResources().getIdentifier(card.getPatternMap().get(key).getResource(), "mipmap", getApplicationContext().getPackageName()), card.getPatternMap().get(key).getResource(), card.getPatternMap().get(key).getThresh());
					} catch (Exception e) {
						Log.w("Pattern", "There has been a problem with the pattern matching");
					}
					patternCount++;
				}
			}
		}		

		/**
		 * Update 'detection in progress' UI.
		 */
		public void updateUI() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					View buttonLoadImage = findViewById(R.id.buttonLoadPicture);
					buttonLoadImage.setEnabled(false);
					ImageView imageView = (ImageView) findViewById(R.id.imgView);
					imageView.setVisibility(View.GONE);
					ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
					pBar.setVisibility(View.VISIBLE);	
				}
			});
		}

		@Override
		protected void onPostExecute(String result) {
			if(result.equals("success")) {
				ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
				pBar.setProgress(0);
				pBar.setVisibility(View.GONE);
				ImageView imageView = (ImageView) findViewById(R.id.imgView);

				imageView.setVisibility(View.VISIBLE);
				Utils.matToBitmap(showBit, bimage); 
				imageView.setImageBitmap(bimage);
				incremented = 0;
				new TextOperation().execute("");
			} else {
				Log.w("Error", "Loading image error");
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
			ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
			double val = Integer.valueOf(values[0]) / 100.0;
			val = val * 360;
			double nowToIncr = val - incremented;
			for(int i= 0; i<= nowToIncr; i++) pBar.incrementProgress();
			incremented = (int)val; 
		} 
	}

	/**
	 * Saves the language data for tesseract in the ExternalDirectory of the phone.
	 * @param resId ID of the language data in the res/raw folder.
	 */
	public void copyResources(int resId){

		File cfgdir = new File(Environment.getExternalStorageDirectory() + File.separator + "tesseract" + File.separator +  "tessdata" + File.separator);
		if(!cfgdir.exists()){
			cfgdir.mkdirs();

			Log.i("Test", "Setup::copyResources");
			InputStream in = getResources().openRawResource(resId);
			String filename = getResources().getResourceEntryName(resId) + ".traineddata";
			File f = new File(filename);
			if(!f.exists()){
				try {
					OutputStream out = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + File.separator + "tesseract" + File.separator + "tessdata", filename));
					byte[] buffer = new byte[1024];
					int len;
					while((len = in.read(buffer, 0, buffer.length)) != -1){
						out.write(buffer, 0, len);
					}
					in.close();
					out.close();
				} catch (FileNotFoundException e) {
					Log.i("Test", "Setup::copyResources - "+e.getMessage());
				} catch (IOException e) {
					Log.i("Test", "Setup::copyResources - "+e.getMessage());
				}
			}
		}
	}

	/**
	 * Checks if the pattern can be found in the given area of interest and sets up feedback.
	 * @param match_method Match Methods supported by OpenCV.
	 * @param res ID of the resource pattern in the res directory.
	 * @param resName Name of the resource to be returned.
	 * @param thresh Threshold the best detection has to pass in order to be a successful detection.
	 */
	public void templateMatching(int match_method, int res, String resName, double thresh) {
		// Pattern Matching
		Point matchLocCode; double matchValCode;
		Log.i("HERE", "" + resName);
		Rect roiCodeArea = new Rect(new Point(showBit.cols() * card.getPattern(resName).getTl().x,showBit.rows() * card.getPattern(resName).getTl().y), new Point(showBit.cols() * card.getPattern(resName).getBr().x, showBit.rows() * card.getPattern(resName).getBr().y));
		Mat cropedCodeArea = showBit.submat(roiCodeArea); 
		Bitmap bmCode = BitmapFactory.decodeResource(getResources(), res);
		Mat cropedCode = new Mat ( bmCode.getHeight(), bmCode.getWidth(), CvType.CV_8U, new Scalar(4));
		Utils.bitmapToMat(bmCode, cropedCode);

		int result_cols_code = cropedCodeArea.cols() - cropedCode.cols() + 1;
		int result_rows_code = cropedCodeArea.rows() - cropedCode.rows() + 1;
		Mat resultCode = new Mat(result_rows_code, result_cols_code, CvType.CV_32FC1);

		Imgproc.matchTemplate(cropedCodeArea, cropedCode, resultCode, match_method);
		MinMaxLocResult mmrCode = Core.minMaxLoc(resultCode);

		if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
			matchLocCode = mmrCode.minLoc;
			matchValCode = mmrCode.minVal;
		} else {
			matchLocCode = mmrCode.maxLoc;
			matchValCode = mmrCode.maxVal;
		}
		Log.w("matchValCode", "" + matchValCode);
		// Pattern passes Detection
		if(matchValCode >= thresh) {
			// If detecting card and pattern passes: get the card that pattern belongs to.
			if(cardType.equals("-DETECT-")) {
				cardType = getFoundCard(resName);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(
								CardValidationActivity.this,
								"Card detected: " + cardType,
								Toast.LENGTH_LONG
								).show();
					}
				});
			}
			else {
				Core.rectangle( cropedCodeArea, matchLocCode, new Point( matchLocCode.x + cropedCode.cols() , matchLocCode.y + cropedCode.rows() ), new Scalar(0, 255, 0, 255), 4 );
				theText.put("Pattern: " + resName, "PASSED");
			}
		}
		else {
			theText.put("Pattern: " + resName, "FAILED");
			failureCount++;
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

				if (line.equals(cardType)) matchingCard = true;
				if(matchingCard) {
					if (line.equals("CARDEND")) matchingCard = false;	
					if (line.equals("PATTERNEND")) {card.addPattern(resource, new Point(tl.x, tl.y), new Point(br.x, br.y), patternThresh); }
					else if (line.equals("EXTRACTEND")) card.addExtractPattern(extractName, extractPattern, extractLength);
					else {
						try {
							String[] parts = line.split("=");
							String first = parts[0];
							String second = parts[1];
							if(first.equals("card.face.detection")) faceDetect = Boolean.valueOf(second);
							if(first.equals("remove.background")) removeBackground = Boolean.valueOf(second);
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
							if (first.equals("extract.name")) extractName = second;
							if (first.equals("extract.pattern")) extractPattern = second;
							if (first.equals("extract.length")) extractLength = Integer.valueOf(second);
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

	/** Sets up a card object with a single Pattern of every Card specified in the rules File. Tries to match every Pattern to the given image.*/
	public void setDetectionTemplate() {
		BufferedReader reader;
		try {
			final InputStream file = getResources()
					.openRawResource(R.raw.rules);
			reader = new BufferedReader(new InputStreamReader(file));
			String line = reader.readLine();
			while (line != null) {
				if(line.matches("[-].*")) matchingCard = true;
				if(matchingCard) {
					if (line.equals("PATTERNEND")) {card.addPattern(resource, new Point(tl.x, tl.y), new Point(br.x, br.y), patternThresh); matchingCard = false; }
					else {
						try {
							String[] parts = line.split("=");
							String first = parts[0];
							String second = parts[1];
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

		int match_method = Imgproc.TM_CCOEFF_NORMED;
		for (String key : card.getPatternMap().keySet()) {
			if(!card.getPatternMap().get(key).getResource().matches("[t][e][x][t].*"))	try {
				templateMatching(match_method, getApplicationContext().getResources().getIdentifier(card.getPatternMap().get(key).getResource(), "mipmap", getApplicationContext().getPackageName()), card.getPatternMap().get(key).getResource(), card.getPatternMap().get(key).getThresh());
			} catch(Exception e) {
				Log.w("Detect Pattern", "A template matching exception occured" );
			}
		}		
	}

	/**
	 * Called after a pattern of the sample Card used for card Detection passes.
	 * Returns the card the pattern is a part of.
	 * @param resName Name of the Pattern that passed the detection.
	 * @return Name of the card that was detected.
	 */
	public String getFoundCard(String resName) {
		BufferedReader reader;
		String currentCard = "";
		try {
			final InputStream file = getResources()
					.openRawResource(R.raw.rules);
			reader = new BufferedReader(new InputStreamReader(file));
			String line = reader.readLine();
			while (line != null) {
				Log.d("RulesTemplate", line);
				if(line.matches("[-].*")) currentCard = line;
				try {
					String[] parts = line.split("=");
					String first = parts[0];
					String second = parts[1];
					if(second.equals(resName)) break; 
				}
				catch(Exception e) {

				}
				line = reader.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return currentCard;
	}

	/** Fetches the text from the specified area.
	 * @param resName name of the text pattern.
	 * **/
	public void fetchTextNew(String resName) {
		Rect roi = new Rect(new Point(origIm.cols() * card.getPattern(resName).getTl().x,origIm.rows() * card.getPattern(resName).getTl().y), new Point(origIm.cols() * card.getPattern(resName).getBr().x, origIm.rows() * card.getPattern(resName).getBr().y));
		Mat cropedIm = new Mat();
		origIm.submat(roi).copyTo(cropedIm);
		Imgproc.cvtColor(cropedIm, cropedIm, Imgproc.COLOR_RGB2GRAY);

		//remove backgorund of card
		if(removeBackground) {
			try {
				Mat image = new Mat();
				Imgproc.adaptiveThreshold(cropedIm, cropedIm, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 29, 10);
//				Imgproc.GaussianBlur(cropedIm, image, new Size(0, 0), 6);
//				Core.addWeighted(cropedIm, 1.5, image, -0.5, 0, image);
//				cropedIm = image;
			} catch(Exception e) {		
			}
		}

		resultBitmap = Bitmap.createBitmap(cropedIm.cols(),  cropedIm.rows(),Bitmap.Config.ARGB_8888);
		//resultBitmap = Bitmap.createBitmap(cropedFlag.cols(),  cropedFlag.rows(),Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(cropedIm, resultBitmap);
		baseApi.setImage(resultBitmap);
		recognizedText = baseApi.getUTF8Text();
		if(cardType.equals("-PASSPORT-")) {
			if (recognizedText.indexOf('\n') != -1){
				recognizedText = recognizedText.substring(recognizedText.indexOf('\n') + 1);
			}
		}
		String key = resName.substring(resName.indexOf("_") + 1);
		theText.put(key, recognizedText);
		displayT = displayT + recognizedText;
	}

	/** Background thread for text recognition. */
	private class TextOperation extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
					pBar.setVisibility(View.VISIBLE);
					pBar.spin();
					pBar.setText("Fetching...");		
				}
			});
			// Sets Up Tesseract for text recognition.
			baseApi = new TessBaseAPI();
			baseApi.init(Environment.getExternalStorageDirectory() +   "/tesseract/", "eng");
			//baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,"ABCDEFGHIJKLMNOPQRSTUVWXYZ");
			baseApi.setVariable("load_system_dawg","F");
			baseApi.setVariable("load_freq_dawg","F");
			baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
			for (String key : card.getPatternMap().keySet()) {
				if(card.getPatternMap().get(key).getResource().matches("[t][e][x][t].*")) fetchTextNew(card.getPattern(key).getResource());
			}
			baseApi.end();
			return "sucess";
		}

		@Override
		protected void onPostExecute(String result) {
			ProgressWheel pBar = (ProgressWheel) findViewById(R.id.progressBar);
			pBar.setVisibility(View.GONE);
			DataHolder.getInstance().setCardPath(null);
			DataHolder.getInstance().setData(null);
			// Prepare return values and end the activity	
			Intent theResult = new Intent();
			theResult.putExtra("theValidation", theText);
			setResult(Activity.RESULT_OK, theResult);
			incremented = 0;
//			ImageView imageView = (ImageView) findViewById(R.id.imgView);
//			imageView.setVisibility(View.VISIBLE);
//			imageView.setImageBitmap(resultBitmap);
			finish();
		}

		@Override
		protected void onPreExecute() {}

		@Override
		protected void onProgressUpdate(String... values) {
		} 
	}
}
