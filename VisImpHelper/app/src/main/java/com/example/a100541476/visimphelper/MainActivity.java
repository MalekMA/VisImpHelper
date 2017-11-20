package com.example.a100541476.visimphelper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String CLOUD_VISION_API_KEY = "AIzaSyCqc_Vyuz1Dx0sP1EMeaI9MPNAvMOVh8RM";

    private static final int SPEECH_REQUEST_CODE = 0;
    private static final int IDENTIFY_REQUEST_CODE = 1;
    private static final int READ_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSIONS_REQUEST = 3;
    private static final int CAMERA_IMAGE_REQUEST = 4;

    private int cloudType = 0;

    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FILE_NAME = "temp.jpg";
    private String mostRecent = "";
    private TextToSpeech txtToSpch;

    public TextView txtView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView = (TextView) findViewById(R.id.textView);
    }

    public void mainButtonClicked(View view){
        displaySpeechRecognizer();
    }

    private void displaySpeechRecognizer(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            txtToSpch = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    txtToSpch.setLanguage(Locale.CANADA);
                    txtToSpch.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null);
                }
            });
            performCommand(spokenText);
        }
        else if(requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK){
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
        }
    }

    private void performCommand(String command){
        if(command.equals("identify")){
            cloudType = IDENTIFY_REQUEST_CODE;
            startCamera();
        }
        else if(command.equals("read")){
            cloudType = READ_REQUEST_CODE;
            startCamera();
        }
        else if(command.equals("repeat")){
            readOutResponse(mostRecent);
        }
        else if(command.equals("battery")){
            getBatteryLevel();
        }
        else if(command.equals("time")){
            getTime();
        }
        else {
            readOutResponse("Invalid command");
        }
    }

    public void startCamera(){
        if(PermissionUtils.requestPermission(this, CAMERA_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile(){
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    public void uploadImage(Uri uri){
        if(uri != null){
            try{
                Bitmap bitmap = scaleBitmapDown(MediaStore.Images.Media.getBitmap(getContentResolver(), uri), 1200);
                readOutResponse("Analyzing");
                callCloudVision(bitmap);
            } catch(IOException e){
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave null image.");
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException{

       Toast.makeText(this, "Analyzing image", Toast.LENGTH_LONG).show();
        readOutResponse("Analyzing");

       new AsyncTask<Object, Void, String>(){
           protected String doInBackground(Object... params){
               try{
                   HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY){
                        protected void initializeVisionRequest(VisionRequest<?> visionRequest) throws IOException {
                            super.initializeVisionRequest(visionRequest);

                            String packageName = getPackageName();
                            visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                            String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                            visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                        }
                    };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        Image base64EncodedImage = new Image();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        if(cloudType == IDENTIFY_REQUEST_CODE) {
                            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                                Feature labelDetection = new Feature();
                                labelDetection.setType("LABEL_DETECTION");
                                labelDetection.setMaxResults(100);
                                add(labelDetection);
                            }});
                        }
                        else if(cloudType == READ_REQUEST_CODE){
                            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                                Feature labelDetection = new Feature();
                                labelDetection.setType("TEXT_DETECTION");
                                labelDetection.setMaxResults(100);
                                add(labelDetection);
                            }});
                        }
                        add(annotateImageRequest);
                    }});


                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    Log.d(TAG, "created vision request, sending");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);
               } catch(GoogleJsonResponseException e){
                   Log.d(TAG, e.getContent());
               } catch(IOException e){
                   Log.d(TAG, e.getMessage());
               }
               return "Cloud vision api request failed.";
        }

        protected void onPostExecute(String result) {txtView.setText(result);}
       }.execute();
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public String convertResponseToString(BatchAnnotateImagesResponse response){
        String output = "I found: ";

        if(cloudType == IDENTIFY_REQUEST_CODE) {

            List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();

            if (labels != null) {
                for (EntityAnnotation label : labels) {
                    output += ", "+label.getDescription() ;
                }
            } else {
                output += "nothing";
            }
            readOutResponse(output);
            mostRecent = output;
            return output;
        }
        else if(cloudType == READ_REQUEST_CODE){
            List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();

            output = labels.get(0).getDescription();

            readOutResponse(output);
            mostRecent = output;
            return output;
        }
        return output;
    }

    private void getBatteryLevel(){
        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        mostRecent = Integer.toString(batLevel) + " percent.";
        readOutResponse(Integer.toString(batLevel) + " percent.");
    }

    private void getTime(){
        Date currentTime = Calendar.getInstance().getTime();
        mostRecent = currentTime.toString();
        readOutResponse(currentTime.toString());
    }

    private void readOutResponse(final String toRead){

        txtToSpch = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                txtToSpch.setLanguage(Locale.CANADA);
                txtToSpch.speak(toRead, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }
}
