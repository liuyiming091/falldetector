package application.cepheus.falldetector;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;


import java.util.Timer;
import java.util.TimerTask;

public class NochartActivity extends Activity {
    public SensorManager sensorManager;
    public Sensor accelSensor ;
    TextView xText ;
    TextView yText ;
    TextView zText ;
    TextView sumText;
    ShimmerTextView title;
    private ImageButton button;

    SensorEventListener threeParamListener;
    SensorEventListener oneParamListener;
    SensorEventListener twoParamListener;
    Thread avgThread;
    int sensor_id = 0;
    public double ax,ay,az;
    public double a_norm;
    public  int i=0;
    static int BUFF_SIZE=500;
    static public double[] win= new double[BUFF_SIZE];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nochart);
        button=(ImageButton)findViewById(R.id.quit2);
        button.setOnClickListener(ButtonListener);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(xText==null){
            findViews();
        }
        Intent intent = getIntent();
        int wtd = intent.getIntExtra("wtd", Sensor.TYPE_ACCELEROMETER);
        intialize();


        initListeners();

        switch (wtd) {
            case Sensor.TYPE_ACCELEROMETER:
                title.setText("Fall Detector");
                //danWei.setText("");
                accelSensor = sensorManager.getDefaultSensor(wtd);
                sensorManager.registerListener(threeParamListener, accelSensor, sensorManager.SENSOR_DELAY_UI);
                sensor_id = wtd;
                break;
            default:
                break;
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }



    @Override
    protected void onPause() {

        super.onPause();
    }

    Shimmer shimmer;


    private void findViews(){
        xText = (TextView) findViewById(R.id.xAxis);
        yText = (TextView) findViewById(R.id.yAxis);
        zText = (TextView) findViewById(R.id.zAxis);
        sumText = (TextView) findViewById(R.id.sum);
        title = (ShimmerTextView) findViewById(R.id.title);
        shimmer = new Shimmer();
        shimmer.start(title);
    }


    private void initListeners() {

        threeParamListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {

                xText.setText(event.values[0]+"");
                yText.setText(event.values[1]+"");
                zText.setText(event.values[2]+"");
               // double sum = threeDimenToOne(event.values[0], event.values[1], event.values[2]);

               // giveAverage(sum);
                ax=event.values[0];
                ay=event.values[1];
                az=event.values[2];
                AddData(ax,ay,az);
                Fall(win);

            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {


            }
        };
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
        if(threeParamListener!=null){
            sensorManager.unregisterListener(threeParamListener);
        }
        if(oneParamListener!=null){
            sensorManager.unregisterListener(oneParamListener);
        }
        if(twoParamListener!=null){
            sensorManager.unregisterListener(twoParamListener);
        }
        if(avgThread!=null)
            avgThread.interrupt();



    }


    double[] diff=new double[501];



    private void AddData(final double ax2,final double ay2,final double az2){
        TimerTask task = new TimerTask(){

            public void run(){
                a_norm= Math.sqrt(ax2*ax2+ay2*ay2+az2*az2);
                for(i=0;i<=500-2;i++){
                    win[i]=win[i+1];
                }
                win[500-1]=a_norm;//execute the task

            }

        };

        Timer timer = new Timer();

        timer.schedule(task,100);
    }
    private void intialize(){
        for(int i=0;i<501;i++)
            diff[i]=0;
        for(int i=0;i<499;i++)
            win[i]=0;
    }
    private void Fall(double[] window2) {
        int tmax = 1;
        int tmin = 1;
        int i = 1;
        double cha=0;
        diff[1] = window2[0];
        for (i = 2; i < 500; i++) {
            diff[i] = window2[i - 1] - window2[i - 2];
        }
        double max = diff[1];
        double min = diff[1];
        for (i = 1; i < 500; i++) {
            if (max < diff[i])
                tmax = i;
            if (min > diff[i])
                tmin = i;
        }
        cha = window2[tmax - 1] - window2[tmin - 1];
        if ((cha < 2 * 9.8) || tmax < tmin) {
            tmin = 0;
            tmax = 0;
        } else if (cha > 2 * 9.8) {
            sumText.setText("FALL!");
            sensorManager.unregisterListener(threeParamListener);
           // avgThread.interrupt();
            this.finish();
            Intent it=new Intent(NochartActivity.this,AlertActivity.class);
            startActivity(it);


        }
    }

    //Deal with the side menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        switch (item.getItemId()){
            case android.R.id.home:
                Intent it=new Intent(this,MainActivity.class);
                startActivity(it);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public View.OnClickListener ButtonListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Intent it=new Intent(NochartActivity.this,MainActivity.class);
            startActivity(it);
            finish();
        }
    };



}
