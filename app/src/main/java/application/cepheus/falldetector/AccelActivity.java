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

public class AccelActivity extends Activity {
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

	//graph related
	 private XYSeries series;
	 private XYMultipleSeriesDataset mDataset;
	 private GraphicalView chart;
	 private XYMultipleSeriesRenderer renderer;
	 private Context context;
	 private int yMax = 20;
	 private int xMax = 50;
	 private int yMin = 0;
	 DbUtils db ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accel);
		button=(ImageButton)findViewById(R.id.quit);
		button.setOnClickListener(ButtonListener);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		avgHandler = new AveHandler();
		db = DbUtils.create(getApplicationContext());

		if(xText==null){
			findViews();
		}
		Intent intent = getIntent();
		int wtd = intent.getIntExtra("wtd", Sensor.TYPE_ACCELEROMETER);
		avgThread = new Thread(runnable);//thread begins
		avgThread.start();
		intialize();

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

        //initialize chart
        initChart("Times", danWei.getText().toString(),0,xMax,yMin,yMax);
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
	}

	@Override
	protected void onPause() {

		super.onPause();
		if(avgThread!=null)
		avgThread.interrupt();
	}
	
	Shimmer shimmer;

	/**
	 * findViewById
	 */
	private void findViews(){
		xText = (TextView) findViewById(R.id.xAxis);
		yText = (TextView) findViewById(R.id.yAxis);
		zText = (TextView) findViewById(R.id.zAxis);
		sumText = (TextView) findViewById(R.id.sum);
		danWei = (TextView) findViewById(R.id.danWei);
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
					double sum = threeDimenToOne(event.values[0], event.values[1], event.values[2]);
						
					giveAverage(sum);
					ax=event.values[0];
					ay=event.values[1];
					az=event.values[2];
					AddData(ax,ay,az);
					Fall(win);//fall detector
				}
				
				
				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {

					
				}
			};
	}


	private void initChart(String xTitle, String yTitle, int minX, int maxX, int minY, int maxY){
        LinearLayout layout = (LinearLayout)findViewById(R.id.chart);
        series = new XYSeries("history");
        mDataset = new XYMultipleSeriesDataset();
        mDataset.addSeries(series);

        int lineColor = Color.GREEN;
        PointStyle style = PointStyle.CIRCLE;
        renderer = buildRenderer(lineColor, style, true);

        setChartSettings(renderer, xTitle,yTitle, 
        		minX, maxX,
        		minY, maxY,
        		Color.BLACK, //color of lines
        		Color.WHITE//color of label
        );
       
        //generate chart
        chart = ChartFactory.getLineChartView(this, mDataset, renderer);
        
        //add chart to View
        layout.addView(chart, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
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
	public  int index = 0;
	//Buffer size
	double[] buffer = new double[500];//500 in 0.5s
	double[] diff=new double[501];
	public int INTERVAL = 250;
	public double AVERAGE = 0;
	
	
	/**
	 * a thread,calculate every fixed time
	 */
	Runnable runnable = new Runnable() {
		
		@Override
		public void run() {

			System.out.println("Thread Begins!");
			while(true){
			try {
				Thread.sleep(INTERVAL);//calculate the average
			} catch (InterruptedException e) {

				e.printStackTrace();
				avgThread = new Thread(runnable);
				avgThread.start();
			}
			if(index!=0){
			double sum = 0;
			for (int i=0;i<index;i++) {
				sum+=buffer[i];
			}
			AVERAGE = sum/new Double(index);
			index=0;
			}
			avgHandler.sendEmptyMessage(1);

			}
		}
	};

	/**
	 * update the value and the chart
	 *
	 */
	class AveHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {


			updateChart();

			Accelerate_info accelerate_info = new Accelerate_info(System.currentTimeMillis(), AVERAGE, sensor_id);
			try {
				db.save(accelerate_info);
			} catch (DbException e) {

				e.printStackTrace();
				System.out.println("fail");
			}
		}
	}

	public void giveAverage(double data){
		buffer[index]=data;
		index++;
	}
	
	
	protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill) {
	     XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	    
	     //set the chart view
	     XYSeriesRenderer r = new XYSeriesRenderer();
	     r.setColor(color);
	     r.setPointStyle(style);
	     r.setFillPoints(fill);
	     r.setLineWidth(2);//这是线宽
	     renderer.addSeriesRenderer(r);
	    
	     return renderer;
	    }
	
	
	/**
	 * @param renderer
	 * @param xTitle
	 * @param yTitle
	 * @param xMin
	 * @param xMax
	 * @param yMin
	 * @param yMax
	 * @param axesColor
	 * @param labelsColor
	 */
	  protected void setChartSettings(XYMultipleSeriesRenderer renderer, String xTitle, String yTitle,
									  double xMin, double xMax, double yMin, double yMax, int axesColor, int labelsColor) {

			     renderer.setChartTitle(title.getText().toString());//title
			     renderer.setChartTitleTextSize(0);
			     renderer.setXAxisMin(xMin);//begin of X axis
			     renderer.setXAxisMax(xMax);//how many points
			     renderer.setYAxisMin(yMin);
			     renderer.setYAxisMax(yMax);
			     renderer.setBackgroundColor(Color.BLACK);
			     renderer.setLabelsColor(Color.YELLOW);
			     renderer.setAxesColor(axesColor);
			     renderer.setLabelsColor(labelsColor);
			     renderer.setShowGrid(true);
			     renderer.setGridColor(Color.WHITE);
			     renderer.setXLabels(20);
			     renderer.setYLabels(20);
			     renderer.setLabelsTextSize(0);
			     renderer.setXTitle("");
		         renderer.setYTitle("");
			     renderer.setAxisTitleTextSize(30);
			     renderer.setYLabelsAlign(Align.RIGHT);
			     renderer.setPointSize((float) 2);
			     renderer.setShowLegend(false);
			     renderer.setLegendTextSize(20);
			    }
	  
//
		  private int addX = -1;
		  private double addY = 0;
		  /**
		   * update the chart
		   */
	    private void updateChart() {


	        addY = AVERAGE;


	        mDataset.removeSeries(series);


	        int length = series.getItemCount();
	        if (length > 5000) {//maximum 5000
	         length = 5000;
	        }


	     series.add(addX++, addY);//IMPORTANT!!!!
	     if(addX>xMax){
	    	 renderer.setXAxisMin(addX-xMax);
	    	 renderer.setXAxisMax(addX);
	     }


	     //Add point
	     mDataset.addSeries(series);

	     //update
	     chart.invalidate();
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
			Intent it=new Intent(AccelActivity.this,AlertActivity.class);
			startActivity(it);
			finish();
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
			Intent it=new Intent(AccelActivity.this,MainActivity.class);
			startActivity(it);
			finish();
		}
	};



}
