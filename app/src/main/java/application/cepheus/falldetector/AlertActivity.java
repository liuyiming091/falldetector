package application.cepheus.falldetector;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
public class AlertActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{
    //private CountDownTimer time;
    SQLiteDatabase db;
    static final String db_name="Fall";
    static final String tb_name="detail";
    private TextView txv;
    private ImageButton yes,no;
    PowerManager.WakeLock fullWakeLock;
    PowerManager.WakeLock partialWakeLock;
    private Vibrator vibrator;
    String name,contact,phone;
    //location
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    //Number to store the current location with latitude and longitude
    private double currentLatitude;
    private double currentLongitude;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    //Explicitly get the permission of using location
    private static final int REQUEST_ACCESS_FINE_LOCATION = 605;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        txv=(TextView)findViewById(R.id.textView9);
        yes=(ImageButton)findViewById(R.id.yes);
        no=(ImageButton)findViewById(R.id.no);
        db=openOrCreateDatabase(db_name, Context.MODE_PRIVATE,null);
        Cursor c=db.rawQuery("SELECT * FROM "+tb_name,null);
        if(c.moveToFirst()){
            name=c.getString(0);
            contact=c.getString(1);
            phone=c.getString(2);
        }
        if (checkPlayServices()) {

            // If google services can be used, build the GoogleApi client
            buildGoogleApiClient();
        }
        //Get the current location
        getCurrentLocation();
        yes.setOnClickListener(listener1);
        no.setOnClickListener(listener2);
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        long [] pattern = {100,400,100,400};   // 停止 开启 停止 开启
        vibrator.vibrate(pattern,2);           //重复两次上面的pattern 如果只想震动一次，index设为-1
        createWakeLocks();
        wakeDevice();
       // time.start();
    }
    CountDownTimer time = new CountDownTimer(10000,1000) {
        @Override
        public void onTick(long millisUntilFinished) {

            txv.setText(""+millisUntilFinished / 1000);

        }

        @Override
        public void onFinish() {
           vibrator.cancel();
            /* time.cancel();
            String text_Message ="https://maps.google.com/?t=m&q="+currentLatitude+','+currentLongitude+"+(Shared+location)&ll="+currentLatitude+','+currentLongitude+"&z=17";
            String text_Message2="An emergency might occur to your friend, "+name+", here is the location for him/her right now, please help him/her as soon as possible!";
            //SmsManager smsManager = SmsManager.getDefault();
            //smsManager.sendTextMessage(phone, null, text_Message2, null, null);
            //smsManager.sendTextMessage(phone, null, text_Message, null, null);
            vibrator.cancel();
            Intent it=new Intent(AlertActivity.this,MainActivity.class);
            startActivity(it);
            AlertActivity.this.finish();
            Toast.makeText(AlertActivity.this,"Alert Sended!",Toast.LENGTH_SHORT).show();*/
        }
    };
    protected void createWakeLocks(){
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "Loneworker - FULL WAKE LOCK");
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Loneworker - PARTIAL WAKE LOCK");
    }

    // Called implicitly when device is about to sleep or application is backgrounded
    protected void onPause(){
        super.onPause();
        partialWakeLock.acquire();
    }

    // Called implicitly when device is about to wake up or foregrounded
    protected void onResume(){
        super.onResume();
        if(fullWakeLock.isHeld()){
            fullWakeLock.release();
        }
        if(partialWakeLock.isHeld()){
            partialWakeLock.release();
        }
        checkPlayServices();
    }

    // Called whenever we need to wake up the device
    public void wakeDevice() {
        fullWakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }

    public View.OnClickListener listener1=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            db.close();
            time.cancel();
            vibrator.cancel();
            Intent it=new Intent(AlertActivity.this,MainActivity.class);
            startActivity(it);
            finish();
            Toast.makeText(AlertActivity.this,"Alert has been canceled!",Toast.LENGTH_SHORT).show();
        }
    };

    public View.OnClickListener listener2=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            db.close();
            time.cancel();
            //String text_Message ="https://maps.google.com/?t=m&q="+currentLatitude+','+currentLongitude+"+(Shared+location)&ll="+currentLatitude+','+currentLongitude+"&z=17";
            //String text_Message2="An emergency might occur to your friend, "+name+", here is the location for him/her right now, please help him/her as soon as possible!";
           // SmsManager smsManager = SmsManager.getDefault();
            //smsManager.sendTextMessage(phone, null, text_Message2, null, null);
            //smsManager.sendTextMessage(phone, null, text_Message, null, null);
            vibrator.cancel();
            Intent it=new Intent(AlertActivity.this,MainActivity.class);
            startActivity(it);
            finish();
            Toast.makeText(AlertActivity.this,"Alert Sended!",Toast.LENGTH_SHORT).show();

        }
    };

    //Check if the google play services are available on this phone
    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }

        return true;
    }
    //Build the google api client to get the location
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void getCurrentLocation() {

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //Get the permission of the location
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION );
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            currentLatitude = mLastLocation.getLatitude();
            currentLongitude = mLastLocation.getLongitude();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        getCurrentLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }
}



