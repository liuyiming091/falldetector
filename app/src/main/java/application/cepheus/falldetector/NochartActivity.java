package application.cepheus.falldetector;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.exception.DbException;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class NochartActivity extends Activity {
    public SensorManager sensorManager;
    public Sensor accelSensor ;
    TextView xText ;
    TextView yText ;
    TextView zText ;
    TextView sumText;
    TextView danWei ;
    ShimmerTextView title;
    private ImageButton button;
    private Vibrator vibrator;

    SensorEventListener threeParamListener;
    SensorEventListener oneParamListener;
    SensorEventListener twoParamListener;
    Handler avgHandler;
    Thread avgThread;
    int sensor_id = 0;
    public double ax,ay,az;
    public double a_norm;
    public  int i=0;
    static int BUFF_SIZE=500;
    static public double[] win= new double[BUFF_SIZE];
    private String[] mPlanetTitles;
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    //图表相关
    private XYSeries series;
    private XYMultipleSeriesDataset mDataset;
    private GraphicalView chart;
    private XYMultipleSeriesRenderer renderer;
    private Context context;
    private int yMax = 20;//y轴最大值，根据不同传感器变化
    private int xMax = 50;//一屏显示测量次数
    private int yMin = 0;
    DbUtils db ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accel);
        button=(ImageButton)findViewById(R.id.quit);
        button.setOnClickListener(ButtonListener);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        db = DbUtils.create(getApplicationContext());
        //给控件实例化
        if(xText==null){
            findViews();
        }
        Intent intent = getIntent();
        int wtd = intent.getIntExtra("wtd", Sensor.TYPE_ACCELEROMETER);
        intialize();


        //初始化各个监听器
        initListeners();

        switch (wtd) {
            case Sensor.TYPE_ACCELEROMETER:
                title.setText("Fall Detector");
                danWei.setText("");
                accelSensor = sensorManager.getDefaultSensor(wtd);
                sensorManager.registerListener(threeParamListener, accelSensor, sensorManager.SENSOR_DELAY_UI);
                yMax = 20;
                sensor_id = wtd;
                break;
            default:
                break;
        }

        //初始化图表
        mTitle="Fall Detector";
        mPlanetTitles=getResources().getStringArray(R.array.p_array);
        mDrawerList=(ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        mDrawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        // set a custom shadow that overlays the main content when the drawer opens

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        //mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) ;/*{
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};*/
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        //getActionBar().setTitle("fall");


    }
	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}*/


    @Override
    protected void onPause() {

        super.onPause();
        if(avgThread!=null)
            avgThread.interrupt();
    }

    Shimmer shimmer;
    //FButton fullScreen;


    /**
     * 抓取view中文本控件的函数
     */
    private void findViews(){
        xText = (TextView) findViewById(R.id.xAxis);
        yText = (TextView) findViewById(R.id.yAxis);
        zText = (TextView) findViewById(R.id.zAxis);
        sumText = (TextView) findViewById(R.id.sum);
        danWei = (TextView) findViewById(R.id.danWei);
        title = (ShimmerTextView) findViewById(R.id.title);
        //fullScreen = (FButton) findViewById(R.id.bigImg);
        shimmer = new Shimmer();
        shimmer.start(title);
    }

    /**
     * 初始化各类监听器
     */
    private void initListeners() {

        threeParamListener = new SensorEventListener() {//有三个返回参数的监听器

            @Override
            public void onSensorChanged(SensorEvent event) {

                xText.setText(event.values[0]+"");
                yText.setText(event.values[1]+"");
                zText.setText(event.values[2]+"");
                double sum = threeDimenToOne(event.values[0], event.values[1], event.values[2]);

                giveAverage(sum);//将当前测量的结果写入buffer，然后定期求buffer里面的平均值，并显示
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


    public static double threeDimenToOne(double x,double y,double z){
        return Math.sqrt(x*x+y*y+z*z);
    }
    public  int index = 0;//指示这段时间一共写入了多少个数据
    //在这里可以设置缓冲区的长度，用于求平均数
    double[] buffer = new double[500];//半秒钟最多放500个数
    double[] diff=new double[501];
    public int INTERVAL = 250;//每半秒求一次平均值
    public double AVERAGE = 0;//存储平均值


    /**
     * 接受当前传感器的测量值，存到缓存区中去，并将下标加一
     */
    public void giveAverage(double data){
        buffer[index]=data;
        index++;
    }


    private void AddData(double ax2, double ay2, double az2){
        a_norm= Math.sqrt(ax2*ax2+ay2*ay2+az2*az2);
        for(i=0;i<=500-2;i++){
            win[i]=win[i+1];
        }
        win[500-1]=a_norm;
    }
    private void intialize(){
        for(int i=0;i<501;i++)
            diff[i]=0;
    }
    private void Fall(double[] window2) {
        int tmax = 1;
        int tmin = 1;
        int i = 1;
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
        double cha = window2[tmax - 1] - window2[tmin - 1];
        if ((cha < 2 * 9.8) || tmax < tmin) {
            tmin = 0;
            tmax = 0;
        } else if (cha > 2 * 9.8) {

            sumText.setText("FALL!");
			/*vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			long [] pattern = {100,400,100,400};   // 停止 开启 停止 开启
			vibrator.vibrate(pattern,2);           //重复两次上面的pattern 如果只想震动一次，index设*/
            Intent it=new Intent(NochartActivity.this,TestActivity.class);
            startActivity(it);
			/*KeyguardManager km= (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
			KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
			//解锁
			kl.disableKeyguard();
			//获取电源管理器对象
			PowerManager pm=(PowerManager) context.getSystemService(Context.POWER_SERVICE);
			//获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK,"bright");
			//点亮屏幕
			wl.acquire();*/


        }
    }

    //Deal with the side menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
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
