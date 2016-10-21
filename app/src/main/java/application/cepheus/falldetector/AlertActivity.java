package application.cepheus.falldetector;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.TextView;

public class AlertActivity extends Activity {
    private CountDownTimer time;
    SQLiteDatabase db;
    static final String db_name="Fall";
    static final String tb_name="detail";
    TextView txv;
    PowerManager.WakeLock fullWakeLock;
    PowerManager.WakeLock partialWakeLock;
    private Vibrator vibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        txv=(TextView)findViewById(R.id.textView9);
        time = new CountDownTimer(100000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                txv.setText((int) (millisUntilFinished/1000));
            }

            @Override
            public void onFinish() {

            }
        };
        /*txv=(TextView)findViewById(R.id.textView5);
        txv2=(TextView)findViewById(R.id.textView6);
        txv3=(TextView)findViewById(R.id.textView7);*/
        db=openOrCreateDatabase(db_name, Context.MODE_PRIVATE,null);
        Cursor c=db.rawQuery("SELECT * FROM "+tb_name,null);
        if(c.moveToFirst()){
            String name=c.getString(0);
            String contact=c.getString(1);
            String phone=c.getString(2);
            /*txv.setText(name);
            txv2.setText(contact);
            txv3.setText(phone);*/
        }
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        long [] pattern = {100,400,100,400};   // 停止 开启 停止 开启
        vibrator.vibrate(pattern,2);           //重复两次上面的pattern 如果只想震动一次，index设为-1
        createWakeLocks();
        wakeDevice();
        time.start();
    }
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
    }

    // Called whenever we need to wake up the device
    public void wakeDevice() {
        fullWakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }
}



