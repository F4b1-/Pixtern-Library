#Pixtern#


Pixtern is an open-source, template based face and card validation library for Android.



Libraries needed
-------------
You will need the [tesseract](https://code.google.com/p/tesseract-ocr/), [OpenCV](http://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.0.0/OpenCV-3.0.0-android-sdk-1.zip/download) and [ProgressWheel](https://github.com/Todd-Davies/ProgressWheel) libraries if you want to use Pixtern as a part of your own application.

Project Structure
-------------
**Face**
The face recognition enables the user to get realtime feedback(currently: Face and Eyes) while taking a selfie. It is also possible to upload a Picture for validation(currently: Face, Eyes, Mouth, Brightness). Beheaviour of the activity can easily be modified by changing the templates file.

**Card**
The card validation enables the user to take a picture of a card that is validated against a template. It is possible to specify certain pattern (e.g. symbols or emblems) that all the cards of this type have to meet. Then, card information is beeing fetched from the text regions that have been specified in the tempate file.


How to use Pixtern within your Project (Android Studio)
-------------
Click [here ](https://bitbucket.org/F4b1/pixternonstudio/overview)for the Android Studio Version.

How to use Pixtern within your Project (Eclipse)
-------------

1. Clone the Library on your machine
2. Download the libraries needed (see below)
3. Open Eclipse
4. File > Import > Existing Projects into Workspace > Select root directory > link to the Pixtern Library directory
5. Rightclick on your Project > Properties > Android > Library > Add > Pixtern Library
6. Add the tesseract, OpenCV and progressWheel libraries to your workspace 
7. Rightclick on Pixtern Library and add the libraries just like in step 6


You can use the sample app to get an idea how to use the library activities. If you want to call one of the Activities in your own code, use the following: 
```
startActivityForResult(new Intent(MainActivity.this, com.dreamoval.android.pixtern.face.FdActivity.class), 888);	
```

Don't forget to specify the called activities in your manifest file!

Remember that the Activity returns the result of the validation or the path to the picture that has been taken. To access the information(here: the path of the image) use:

```
@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			if(requestCode == 888) {
				String path = data.getStringExtra("theSelfie"));
                        }
                 }
        }   
```

Configure the face recognition
-------------
If you want an overview over the possible configurations please have a look at the sample rules file in this repo.
You can turn the detection on/off for each trait and the brightness:
```
face.detection=true
eyes.detection=true
mouth.detection=true
brightness.detection=true
```
If you want the app to automatically take a picture when a certain detection percentage has been passed, this is what you want to add to the rules file:
```
percentage.to.pass=50
auto.snap=true
```
If the user should only be able to take a picture when a certain percentage has been passed:
```
snap.when.detected=true
```
You can specify the allowed size of faces that are detections:
```
distance.limit.lower=0,0
distance.limit.upper=400,400
```
How to set up a card template
-------------

Create a new file called "rules.txt" in the res/raw folder of your application. If you want to create a new card use:
```
-CARDNAME-                                 
```
If your card has a noisy background Pixtern can try to remove it:
```
remove.background=false
```
You can show an outline of the text pattern on the preview. This can make it easier for users to take a good picture. It also helps with setting up new patterns:
```
show.outline=false
```

If you want to enable face detection specify the region of the face on the card like this:
```  
CARDFACE
card.face.detection=true
face.area.tl=0.7,0.15
face.area.br=0.99,0.85
CARDFACEEND                                
```
Regular patterns are added as follows:
```
PATTERN
pattern.resource=patternName
pattern.area.tl=0.0,0.0
pattern.area.br=0.2,0.2
pattern.threshold=0.4
PATTERNEND
```
The region of your pattern is represented by a rectangle whose top left(tl) and bottom right(br) corners can be set.
Do not forget to add a sample image of the pattern to the your res/mipmap folder. Pixtern/OpenCV will then try to find this pattern in the specified region of interest.

Note: If you want to add a text Pattern, add "text_" before the name name of the pattern:
```
pattern.resource=text_YourPattername
```

Tesseract which is a part of Pixtern will try to recognize the text and return it to your calling application.