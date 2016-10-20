package application.cepheus.falldetector;

import android.app.Activity;
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
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
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
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		avgHandler = new AveHandler();
		db = DbUtils.create(getApplicationContext());
		//给控件实例化
		if(xText==null){
			findViews();
		}
		Intent intent = getIntent();
		int wtd = intent.getIntExtra("wtd", Sensor.TYPE_ACCELEROMETER);
		avgThread = new Thread(runnable);//定期更新平均值的线程启动
		avgThread.start();
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

					
					
//					if(sum>24){
//						 /* 
//				         * 想设置震动大小可以通过改变pattern来设定，如果开启时间太短，震动效果可能感觉不到 
//				         * */  
//				        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);  
//				        long [] pattern = {100,400};   // 停止 开启 停止 开启   
//				        vibrator.vibrate(pattern,-1);           //重复两次上面的pattern 如果只想震动一次，index设为-1
//					}
				}
				
				
				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {

					
				}
			};
	}

	/**
	 * 初始化图表
	 */
	private void initChart(String xTitle, String yTitle, int minX, int maxX, int minY, int maxY){
		//这里获得main界面上的布局，下面会把图表画在这个布局里面
        LinearLayout layout = (LinearLayout)findViewById(R.id.chart);
        //这个类用来放置曲线上的所有点，是一个点的集合，根据这些点画出曲线
        series = new XYSeries("历史曲线");
        //创建一个数据集的实例，这个数据集将被用来创建图表
        mDataset = new XYMultipleSeriesDataset();
        //将点集添加到这个数据集中
        mDataset.addSeries(series);
        
        //以下都是曲线的样式和属性等等的设置，renderer相当于一个用来给图表做渲染的句柄
        int lineColor = Color.GREEN;
        PointStyle style = PointStyle.CIRCLE;
        renderer = buildRenderer(lineColor, style, true);
        
      //设置好图表的样式
        setChartSettings(renderer, xTitle,yTitle, 
        		minX, maxX, //x轴最小最大值
        		minY, maxY, //y轴最小最大值
        		Color.BLACK, //坐标轴颜色
        		Color.WHITE//标签颜色
        );
       
        //生成图表
        chart = ChartFactory.getLineChartView(this, mDataset, renderer);
        
        //将图表添加到布局中去
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
	public  int index = 0;//指示这段时间一共写入了多少个数据
	//在这里可以设置缓冲区的长度，用于求平均数
	double[] buffer = new double[500];//半秒钟最多放500个数
	double[] diff=new double[501];
	public int INTERVAL = 250;//每半秒求一次平均值
	public double AVERAGE = 0;//存储平均值
	
	
	/**
	 * 一个子线程，没隔固定时间计算这段时间的平均值，并给textView赋值
	 */
	Runnable runnable = new Runnable() {
		
		@Override
		public void run() {

			System.out.println("Thread Begins!");
			while(true){
			try {
				Thread.sleep(INTERVAL);//没隔固定时间求平均数
			} catch (InterruptedException e) {

				e.printStackTrace();
				avgThread = new Thread(runnable);
				avgThread.start();
			}
			if(index!=0){
			double sum = 0;
			for (int i=0;i<index;i++) {
				sum+=buffer[i];
				//高精度加法
//				sum = MathTools.add(sum, d);
			}
			AVERAGE = sum/new Double(index);
			index=0;//让下标恢复
			}
			avgHandler.sendEmptyMessage(1);
			//高精度除法，还能四舍五入
//			AVERAGE = MathTools.div(sum, buffer.length, 4);
			}
		}
	};
	
	/**
	 * 更新平均值的显示值
	 */
	/*public void setAverageView(){
		if(sumText==null)return;
		sumText.setText(AVERAGE+"");
	}*/
	/**
	 * 每隔固定时间给平均值赋值，并且更新图表的操作
	 *
	 */
	class AveHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {

			//setAverageView();//显示平均值
			updateChart();//更新图表，非常重要的方法
			//把当前值存入数据库
			
			Accelerate_info accelerate_info = new Accelerate_info(System.currentTimeMillis(), AVERAGE, sensor_id);
			try {
				db.save(accelerate_info);//将当前平均值存入数据库
			} catch (DbException e) {

				e.printStackTrace();
				System.out.println("保存失败");
			}
		}
	}
	/**
	 * 接受当前传感器的测量值，存到缓存区中去，并将下标加一
	 */
	public void giveAverage(double data){
		buffer[index]=data;
		index++;
	}
	
	
	protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill) {
	     XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	    
	     //设置图表中曲线本身的样式，包括颜色、点的大小以及线的粗细等
	     XYSeriesRenderer r = new XYSeriesRenderer();
	     r.setColor(color);
	     r.setPointStyle(style);
	     r.setFillPoints(fill);
	     r.setLineWidth(2);//这是线宽
	     renderer.addSeriesRenderer(r);
	    
	     return renderer;
	    }
	
	
	/**
	 * 初始化图表
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
			     //有关对图表的渲染可参看api文档
			     renderer.setChartTitle(title.getText().toString());//设置标题
			     renderer.setChartTitleTextSize(0);
			     renderer.setXAxisMin(xMin);//设置x轴的起始点
			     renderer.setXAxisMax(xMax);//设置一屏有多少个点
			     renderer.setYAxisMin(yMin);
			     renderer.setYAxisMax(yMax);
			     renderer.setBackgroundColor(Color.BLACK);
			     renderer.setLabelsColor(Color.YELLOW);
			     renderer.setAxesColor(axesColor);
			     renderer.setLabelsColor(labelsColor);
			     renderer.setShowGrid(true);
			     renderer.setGridColor(Color.WHITE);//设置格子的颜色
			     renderer.setXLabels(20);//没有什么卵用
			     renderer.setYLabels(20);//把y轴刻度平均分成多少个
			     renderer.setLabelsTextSize(0);
			     renderer.setXTitle("");
		         renderer.setYTitle("");
		         //renderer.setXTitle(xTitle);//x轴的标题
			     //renderer.setYTitle(yTitle);//y轴的标题
			     renderer.setAxisTitleTextSize(30);
			     renderer.setYLabelsAlign(Align.RIGHT);
			     renderer.setPointSize((float) 2);
			     renderer.setShowLegend(false);//说明文字
			     renderer.setLegendTextSize(20);
			    }
	  
//		  int[] xv = new int[1000];//用来显示的数据
//		  double[] yv = new double[1000];
		  private int addX = -1;
		  private double addY = 0;
		  /**
		   * 更新图表的函数，其实就是重绘
		   */
	    private void updateChart() {

	        //设置好下一个需要增加的节点

	        addY = AVERAGE;//需要增加的值

	        //移除数据集中旧的点集
	        mDataset.removeSeries(series);

	        //判断当前点集中到底有多少点，因为屏幕总共只能容纳100个，所以当点数超过100时，长度永远是100
	        int length = series.getItemCount();
	        if (length > 5000) {//设置最多5000个数
	         length = 5000;
	        }

	        //注释掉的文字为window资源管理器效果

	     //将旧的点集中x和y的数值取出来放入backup中，并且将x的值加1，造成曲线向右平移的效果
//	     for (int i = 0; i < length; i++) {
//		     xv[i] = (int) series.getX(i) + 1;
//		     yv[i] = (int) series.getY(i);
//	     }

	     //点集先清空，为了做成新的点集而准备
//	     series.clear();

	     //将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中
	     //这里可以试验一下把顺序颠倒过来是什么效果，即先运行循环体，再添加新产生的点
	     //每一个新点坐标都后移一位
	     series.add(addX++, addY);//最重要的一句话，以xy对的方式往里放值
//	     for (int k = 0; k < length; k++) {
//	         series.add(xv[k], yv[k]);//把之前的数据放进去
//	     }
	     if(addX>xMax){
	    	 renderer.setXAxisMin(addX-xMax);
	    	 renderer.setXAxisMax(addX);
	     }


	     //重要：在数据集中添加新的点集
	     mDataset.addSeries(series);

	     //视图更新，没有这一步，曲线不会呈现动态
	     //如果在非UI主线程中，需要调用postInvalidate()，具体参考api
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
		}
	}

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

	//Deal with the side menu

}
