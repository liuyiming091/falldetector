package application.cepheus.falldetector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import application.cepheus.falldetector.data.Channel;
import application.cepheus.falldetector.data.Item;
import application.cepheus.falldetector.service.WeatherServiceCallback;
import application.cepheus.falldetector.service.YahooWeatherService;

public class MainActivity extends Activity implements WeatherServiceCallback{
    //weather
    private ImageView weatherIconImageView;
    private TextView temperatureTextView;
    private TextView conditionTextView;
    private TextView locationTextView;
    private YahooWeatherService service;
    private ProgressDialog dialog;
    //side menu
    private String[] mPlanetTitles;
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weatherIconImageView=(ImageView) findViewById(R.id.weatherIconImageView);
        temperatureTextView=(TextView)findViewById(R.id.temperatureTextView);
        conditionTextView=(TextView)findViewById(R.id.conditionTextView);
        locationTextView=(TextView)findViewById(R.id.locationTextView);


        service = new  YahooWeatherService(this);
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.show();
        service.refreshWeather("Sydney,Australia");

        //mTitle="Fall Detector";
        mPlanetTitles=getResources().getStringArray(R.array.p_array);
        mDrawerList=(ListView) findViewById(R.id.left_drawer2);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        mDrawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout2);

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
        ) {
			public void onDrawerClosed(View view) {
				//getActionBar().setTitle(mTitle);
				//invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				//getActionBar().setTitle(mDrawerTitle);
				//invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }
    public void gotoSensorActivity(View v){
        //Intent it= new Intent(this,AccelActivity.class);
        Intent it=new Intent(this,AccelActivity.class);
        startActivity(it);
        finish();
    }

    //YahooWeather Service
    @Override
    public void serviceSuccess(Channel channel){
        dialog.hide();
        Item item=channel.getItem();
        int resourceId=getResources().getIdentifier("drawable/icon_"+channel.getItem().getCondition().getCode(),null,getPackageName());
        @SuppressWarnings("deprecation")
        Drawable weatherIconDrawble=getResources().getDrawable(resourceId);
        weatherIconImageView.setImageDrawable(weatherIconDrawble);

        temperatureTextView.setText(item.getCondition().getTemperature()+"\u00B0"+channel.getUnits().getTemperature());
        locationTextView.setText(service.getLocation());
        conditionTextView.setText(item.getCondition().getDescription());

    }

    //YahooWeather Service
    @Override
    public void serviceFailure(Exception exception){
        dialog.hide();
        Toast.makeText(this,exception.getMessage(), Toast.LENGTH_LONG).show();
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

    //deal with side menu
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments

        if (position==0){
            Intent it=new Intent(this,SettingActivity.class);

            startActivity(it);
        }
        if (position==1){
            Intent it=new Intent(this,NotificationActivity.class);
            startActivity(it);
        }
        if (position==2){
            Intent it=new Intent(this,NochartActivity.class);
            startActivity(it);
        }
        mDrawerLayout.closeDrawer(mDrawerList);

        // update selected item and title, then close the drawer
        /*mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);*/
    }

}
