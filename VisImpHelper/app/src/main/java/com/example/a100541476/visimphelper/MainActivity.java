package com.example.a100541476.visimphelper;
//@TODO SCALE BITMAP DOWN, FILTER RESULTS, DIRECTIONS, CALL, TEXT
import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements RetrieveDirectionsListener, LoaderManager.LoaderCallbacks<Cursor>, LocationListener{

    private static final String CLOUD_VISION_API_KEY = "AIzaSyCqc_Vyuz1Dx0sP1EMeaI9MPNAvMOVh8RM";

    private static final int SPEECH_REQUEST_CODE = 0;
    private static final int IDENTIFY_REQUEST_CODE = 1;
    private static final int READ_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSIONS_REQUEST = 3;
    private static final int CAMERA_IMAGE_REQUEST = 4;
    private static final int CONTACT_NAME_REQUEST_CODE = 5;
    private static final int BT_REQUEST_CODE = 6;
    private static final int LOC_PERMISSION_REQUEST = 7;

    private final String SENT_CODE = "SMS_SENT";
    private final String DELIVERED_CODE = "SMS_DELIVERED";

    private int cloudType = 0;
    private boolean analyzeOn = false;

    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FILE_NAME = "temp.jpg";
    private String mostRecent = "";
    private TextToSpeech txtToSpch;
    private String noteTitle = "";

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device;
    InputStream inputStream;
    OutputStream outputStream;
    private boolean running = false;

    String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
    String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
    String contactIn = "";
    String numIn = "";
    HashMap<String, String> contacts = new HashMap<>();

    private SavedDBHelper dbHelper;

    public String dataName = "";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public TextView txtView;

    public static double mLatitude;
    public static double mLongitutde;
    public static double dLatitude;
    public static double dLongitutde;
    public static String destination = "";
    private LocationManager locationManager;
    public static Location location;
    ArrayList<Step> theSteps = new ArrayList<>();
    public int currentStep = 1;
    public float results[] = new float[10];

    Camera mCamera;
    private Handler handler = new Handler();

    // Define the code block to be executed
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Insert custom code here
            Log.d("in handler", "handler");
            mCamera = Camera.open();
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(800,600);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            params.setPictureFormat(ImageFormat.JPEG);
            SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
            try {
                mCamera.setPreviewTexture(st);
            }catch(IOException e){
                Log.d("IOEXCEPTION", e.getMessage());
            }
            mCamera.setParameters(params);
            mCamera.startPreview();
            Log.d("COUNTDOWN", "COUNTING DOWN...");
            mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Toast.makeText(getApplicationContext(), "Image snapshot Done",Toast.LENGTH_LONG).show();
                    try {
                        bitmap = scaleBitmapDown(bitmap, 1200);
                        callCloudVision(bitmap);
                    }catch(IOException e){
                        Log.d("Cloud Vision", e.getMessage());
                    }
                    mCamera.stopPreview();
                    mCamera.release();

                }
            });
            // Repeat every 7 seconds
            handler.postDelayed(runnable, 7000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView = (TextView) findViewById(R.id.textView);
        checkBluetooth();
        dbHelper = new SavedDBHelper(this);
        getSupportLoaderManager().initLoader(1, null, this);
        PermissionUtils.requestPermission(this, LOC_PERMISSION_REQUEST, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS);

        SMSListener.bindListener(new SMSListenerInterface() {
            @Override
            public void messageReceived(String sender, String messageText) {
                String contact = getContact(sender);
                String toRead = contact + "Message: " + messageText;
                mostRecent = toRead;
                readOutResponse(toRead);
            }
        });

    }

    protected void onStart(){
        super.onStart();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    public void checkBluetooth(){
        if(mBluetoothAdapter==null){
            //Device doesn't support bluetooth
        }

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, BT_REQUEST_CODE);
        }
        else {
            setUpBTConn();
        }
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
            Toast.makeText(this, spokenText, Toast.LENGTH_LONG).show();
//            txtToSpch = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
//                @Override
//                public void onInit(int i) {
//                    txtToSpch.setLanguage(Locale.CANADA);
//                    txtToSpch.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null);
//                }
//            });
            performCommand(spokenText);
        }
        else if(requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK){
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
        //Make Call
        else if(requestCode == CONTACT_NAME_REQUEST_CODE && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            if(spokenText.equals("saved")){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                startActivityForResult(intent, 74);
            }
            else {
//                txtToSpch = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
//                    @Override
//                    public void onInit(int i) {
//                        txtToSpch.setLanguage(Locale.CANADA);
//                        txtToSpch.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null);
//                    }
//                });
                //makeCall(spokenText.substring(0,1).toUpperCase()+ spokenText.substring(1));
                contactIn = spokenText.substring(0, 1).toUpperCase() + spokenText.substring(1);
                Log.d("toCall", contactIn);
                getPhoneNum();
                makeCall();
            }
        }
        else if(requestCode == 74 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            numIn = dbHelper.getSavedData(spokenText);
            makeCall();
        }
        //Send Text
        else if(requestCode == 72 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            if(spokenText.equals("saved")){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                startActivityForResult(intent, 75);
            }
            else {
//                txtToSpch = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
//                    @Override
//                    public void onInit(int i) {
//                        txtToSpch.setLanguage(Locale.CANADA);
//                        txtToSpch.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null);
//                    }
//                });
                //makeCall(spokenText.substring(0,1).toUpperCase()+ spokenText.substring(1));
                contactIn = spokenText.substring(0, 1).toUpperCase() + spokenText.substring(1);
                Log.d("toCall", contactIn);
                getPhoneNum();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                startActivityForResult(intent, 73);
            }

        }
        else if(requestCode == 75 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            numIn = dbHelper.getSavedData(spokenText);
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 73);
        }
        else if(requestCode == 73 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            sendSMS(spokenText);
        }
        else if(requestCode == BT_REQUEST_CODE && resultCode == RESULT_OK){
            setUpBTConn();
        }
        else if(requestCode == 70 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String in = results.get(0);
            dbHelper.addNew(in, mostRecent);
        }
        else if(requestCode == 71 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String in = results.get(0);
            Log.d("TO LOOK UP", in);
            String dataIn = dbHelper.getSavedData(in);
            readOutResponse(dataIn);
            Log.d("DATA IN", dataIn);
        }
        else if(requestCode == 69 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String in = results.get(0);
            if(in.equals("saved")){
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                startActivityForResult(intent, 76);
            }
            else{
                destination = in;
                startDirections();
            }

        }
        else if(requestCode == 76 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            destination = dbHelper.getSavedData(spokenText);
            Log.d("DESTINATION", destination);
            startDirections();
        }
        else if(requestCode == 101 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            noteTitle = spokenText;
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 102);
        }
        else if(requestCode == 102 && resultCode == RESULT_OK){
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            Log.d("RECOGNIZED TEXT: ", spokenText);
            dbHelper.addNew(noteTitle, spokenText);
        }



    }

    public void startDirections(){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        criteria.setAltitudeRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        String provider = locationManager.getBestProvider(criteria, true);

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            String locConfig = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
            Intent enableGPS = new Intent(locConfig);
            startActivity(enableGPS);
        }
        try{
            location = locationManager.getLastKnownLocation(provider);
            mLatitude = location.getLatitude();
            mLongitutde = location.getLongitude();
            Log.d("mLat mLong", Double.toString(mLatitude) + " " + Double.toString(mLongitutde));

            Geocoder gc = new Geocoder(this);
            if(gc.isPresent()){
                try {
                    List<Address> list = gc.getFromLocationName(destination, 1);
                    Address address = list.get(0);
                    dLatitude = address.getLatitude();
                    dLongitutde = address.getLongitude();
                    Log.d("dLat dLong", Double.toString(dLatitude) + " " + Double.toString(dLongitutde));
                    double[] coords = {mLatitude, mLongitutde, dLatitude, dLongitutde};
                    new RetrieveDirections(MainActivity.this).execute(coords);
                    locationManager.requestLocationUpdates(provider, 5000, 5, this);
                }catch(IOException e){
                    Log.d("IOException Address", e.getMessage());
                    readOutResponse("Failed to get address");
                }
            }
        } catch(SecurityException e){
            Log.d("Security Exception", e.getMessage());
        }
    }

    public void getCurrentLocation(){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        criteria.setAltitudeRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        String provider = locationManager.getBestProvider(criteria, true);

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            String locConfig = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
            Intent enableGPS = new Intent(locConfig);
            startActivity(enableGPS);
        }
        try{
            location = locationManager.getLastKnownLocation(provider);
            mLatitude = location.getLatitude();
            mLongitutde = location.getLongitude();
            Log.d("mLat mLong", Double.toString(mLatitude) + " " + Double.toString(mLongitutde));

            Geocoder gc = new Geocoder(this);
            if(gc.isPresent()){
                try {
                    List<Address> list = gc.getFromLocation(mLatitude, mLongitutde, 1);
                    Address address = list.get(0);
                    readOutResponse(address.getAddressLine(0));
                    mostRecent = address.getAddressLine(0);
                }catch(IOException e){
                    Log.d("IOException Address", e.getMessage());
                    readOutResponse("Failed to get address");
                }
            }
        } catch(SecurityException e){
            Log.d("Security Exception", e.getMessage());
        }
    }

    public void setUpBTConn(){
        BluetoothSocket  socket = null;
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty()){
            Toast.makeText(this, "No paired Bluetooth Devices", Toast.LENGTH_LONG).show();
        }
        else{
            for(BluetoothDevice iterator: bondedDevices){
                if(iterator.getName().equals("HC-05")){
                    device = iterator;
                }
            }
        }

        try{
            socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        }
        catch(IOException e){
            Log.d("SETUP BT", e.getMessage());
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
            case LOC_PERMISSION_REQUEST:
                if(PermissionUtils.permissionGranted(requestCode, LOC_PERMISSION_REQUEST, grantResults)){
                    Log.d("Permissions", "Permission granted");
                }
                break;
        }
    }

    private void performCommand(String command){
        if(command.equals("identify")){
            cloudType = IDENTIFY_REQUEST_CODE;
            analyzeOn = false;
            mCamera = Camera.open();
            new CountDownTimer(5000, 1000){
                public void onFinish(){
                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_CDMA_CONFIRM);
                    Camera.Parameters params = mCamera.getParameters();
                    params.setPreviewSize(800,600);
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    params.setPictureFormat(ImageFormat.JPEG);
                    params.set("orientation", "portrait");
                    SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
                    try {
                        mCamera.setPreviewTexture(st);
                    }catch(IOException e){
                        Log.d("IOEXCEPTION", e.getMessage());
                    }
                    mCamera.setParameters(params);
                    mCamera.startPreview();
                    Log.d("COUNTDOWN", "COUNTING DOWN...");
                    mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            Toast.makeText(getApplicationContext(), "Image snapshot Done",Toast.LENGTH_LONG).show();
                            mCamera.stopPreview();
                            mCamera.release();
                            try {
                                Log.d("CLOUD", "calling cloud vision?");
                                bitmap = scaleBitmapDown(bitmap, 1200);
                                callCloudVision(bitmap);
                            }catch(IOException e){
                                Log.d("MAIN IDENTIFY", e.getMessage());
                            }

                        }
                    });
                }
                public void onTick(long millisUntilFinished){

                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
                    Log.d("Time left", Long.toString(millisUntilFinished));

                }
            }.start();
            //startCamera();
        }
        else if(command.equals("read")){
            cloudType = READ_REQUEST_CODE;
            mCamera = Camera.open();
            new CountDownTimer(5000, 1000){
                public void onFinish(){
                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_CDMA_CONFIRM);
                    Camera.Parameters params = mCamera.getParameters();
                    params.setPreviewSize(800,600);
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    params.setPictureFormat(ImageFormat.JPEG);
                    params.set("orientation", "portrait");
                    SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
                    try {
                        mCamera.setPreviewTexture(st);
                    }catch(IOException e){
                        Log.d("IOEXCEPTION", e.getMessage());
                    }
                    mCamera.setParameters(params);
                    mCamera.startPreview();
                    Log.d("COUNTDOWN", "COUNTING DOWN...");
                    mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            Toast.makeText(getApplicationContext(), "Image snapshot Done",Toast.LENGTH_LONG).show();
                            mCamera.stopPreview();
                            mCamera.release();
                            try {
                                Log.d("CLOUD", "calling cloud vision?");
                                bitmap = scaleBitmapDown(bitmap, 1200);
                                callCloudVision(bitmap);
                            }catch(IOException e){
                                Log.d("MAIN IDENTIFY", e.getMessage());
                            }

                        }
                    });
                }
                public void onTick(long millisUntilFinished){

                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
                    Log.d("Time left", Long.toString(millisUntilFinished));

                }
            }.start();

        }
        else if(command.equals("test read")){
            cloudType = READ_REQUEST_CODE;
            startCamera();
        }
        else if(command.equals("analyze")){
            cloudType = IDENTIFY_REQUEST_CODE;
            analyzeOn = true;
            handler.post(runnable);
//
//            Intent intent = new Intent(this, CameraActivity.class);
//            startActivity(intent);
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
        else if(command.equals("call")){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, CONTACT_NAME_REQUEST_CODE);
        }
        else if(command.equals("message")){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 72);
        }
        else if(command.equals("guide")){
            setUpBTConn();
            running = !running;
            sensorGuide();
        }
        else if(command.equals("stop")){
            handler.removeCallbacks(runnable);
        }
        else if(command.equals("directions")){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 69);
        }
        else if(command.equals("save")){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 70);
        }
        else if(command.equals("get saved")){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 71);
        }
        else if(command.equals("current location")){
            getCurrentLocation();
        }
        else if(command.equals("voice note")){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 101);
        }
        else if(command.equals("emergency")){
            numIn = "+911";
            makeCall();
        }
        else if(command.equals("help")){
            readOutResponse("identify, read, analyze, guide, call, message, battery, time, save, emergency, voice note, current location");
        }
        else if(command.equals("right")){
            readOutResponse("turn left");
        }
        else if(command.equals("stairs")){
            readOutResponse("caution, stairs ahead!");
        }
        else {
            readOutResponse("Invalid command");
        }
    }

    public void startCamera(){
        if(PermissionUtils.requestPermission(this, CAMERA_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS)){
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
                //readOutResponse("Analyzing");
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
        //readOutResponse("Analyzing");

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
                                labelDetection.setMaxResults(10000);
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
        String output = "";

        if(cloudType == IDENTIFY_REQUEST_CODE) {

            List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();

            if (labels != null) {
                for (EntityAnnotation label : labels) {
                    if(analyzeOn) {
                        if (label.getDescription().equals("angle") || label.getDescription().equals("stairs") || label.getDescription().equals("handrail")) {
                            output += "Caution, uneven surface ahead";
                        } else if (label.getDescription().equals("car") || label.getDescription().equals("vehicle")) {
                            output += "Caution, vehicle ahead";
                        }
                    }else {

                        output += ", " + label.getDescription();
                    }
                }
            } else {
                output += "nothing";
            }
            readOutResponse(output);
            mostRecent = output;
            return output;
        }
        else if(cloudType == READ_REQUEST_CODE){
            try {
                List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
                if (labels.isEmpty()) {
                    output = "nothing";
                } else {
                    output = labels.get(0).getDescription();


                    readOutResponse(output);
                    mostRecent = output;
                    return output;
                }
            }catch(NullPointerException e){
                output = "Unable to read text";
                Log.d("can't read", e.getMessage());
            }
        }
        readOutResponse(output);
        mostRecent = output;
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

    public void readOutResponse(final String toRead){

        txtToSpch = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                txtToSpch.setLanguage(Locale.CANADA);
                txtToSpch.speak(toRead, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }

    private void makeCall(){
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + numIn));
            startActivity(intent);
        }catch(SecurityException e){
            Log.d(TAG, e.getMessage());
        }
    }

    private void sendSMS(String message){

        try {
            PendingIntent sent = PendingIntent.getBroadcast(this, 0, new Intent(SENT_CODE), 0);
            PendingIntent delivered = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED_CODE), 0);

            // when the SMS has been sent
            registerReceiver(new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS sent",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Toast.makeText(getBaseContext(), "Generic failure",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Toast.makeText(getBaseContext(), "No service",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Toast.makeText(getBaseContext(), "Null PDU",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Toast.makeText(getBaseContext(), "Radio off",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(SENT_CODE));

            // when the SMS has been delivered
            registerReceiver(new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS delivered",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case Activity.RESULT_CANCELED:
                            Toast.makeText(getBaseContext(), "SMS not delivered",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(DELIVERED_CODE));

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(numIn, null, message, sent, delivered);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getPhoneNum(){
        for(String s : contacts.keySet()){
            if(s.equals(contactIn)){
                numIn = contacts.get(s);
                Log.d("Num to call", numIn);
            }
        }
    }

    private String getContact(String num){
        int i = 0;
        for(String s : contacts.values()){
            Log.d("getContacts", num + "compared to " +  s);
        }
        return "new";
    }

    private void sensorGuide(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(running){
                    try{
                        int byteCount = inputStream.available();
                        int rate = 0;
                        boolean playSound = false;
                        if(byteCount > 0){
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            String in = new String(rawBytes, "UTF-8");

                            if(in.equals("a")){
                                rate = 1000;
                                playSound = true;
                            }
                            else if(in.equals("b")){
                                rate = 800;
                                playSound = true;
                            }
                            else if(in.equals("c")){
                                rate = 500;
                                playSound = true;
                            }
                            else if(in.equals("d")){
                                rate = 300;
                                playSound = true;
                            }
                            else if(in.equals("x")){
                                playSound = false;
                            }
                        }

                        if(playSound == true){
                            final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(300);
                            SystemClock.sleep(rate);
                        }
                    }
                    catch(IOException e){
                        Log.d("SENSOR GUIDE", e.getMessage());
                    }
                }
            }
        }).start();
    }

    public void directionsRetrieved(ArrayList<Step> steps){
        theSteps = steps;
        String toRead = "Here are the steps: ";
        String first = "First step is: ";
        for(int i = 0; i < theSteps.size(); i++){
            toRead += theSteps.get(i).getStepAction() + "\n";
        }
        mostRecent = toRead;
        readOutResponse(toRead);

    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Uri CONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        return new CursorLoader(this, CONTENT_URI, null,null, null, null);
    }
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String cName = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
            String cNum = cursor.getString(cursor.getColumnIndex(NUMBER));
            //Log.d("Name", cName);
            //Log.d("Number", cNum);
            contacts.put(cName, cNum);
            cursor.moveToNext();
        }

    }
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onLocationChanged(Location loc) {
        if(currentStep == theSteps.size()){
            currentStep = 1;
        }
        else {
            location = loc;
            double dLat = theSteps.get(currentStep).getStepLat();
            double dLon = theSteps.get(currentStep).getStepLon();
            location.distanceBetween(location.getLatitude(), location.getLongitude(), dLat, dLon, results);
            if (results[0] <= 5) {
                readOutResponse(theSteps.get(currentStep).getStepAction());
                currentStep++;
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        //setupLocationServices();
    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
        //setupLocationServices();
    }

}
