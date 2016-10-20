package application.cepheus.falldetector;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SettingActivity extends Activity {
    private EditText mEditText,mEditText2,mEditText3;
    //private CharSequence mCrime;
    private Button button;
    private SharedPreferences savednotes,savednotes2,savednotes3;
    SQLiteDatabase db;
    static final String db_name="Fall";
    static final String tb_name="detail";
   // private  static final String TEMP_INFO="temp_info";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        db=openOrCreateDatabase(db_name,Context.MODE_PRIVATE,null);
        String createTable = "CREATE TABLE IF NOT EXISTS " +
                tb_name + "(name VARCHAR(32), " +
                "contact VARCHAR(32), " +
                "phone VARCHAR(16), " +
                "latitude VARCHAR(16), " +
                "longitude VARCHAR(16))";
        db.execSQL(createTable);
        mEditText=(EditText)findViewById(R.id.editText);
        mEditText2=(EditText)findViewById(R.id.editText2);
        mEditText3=(EditText)findViewById(R.id.editText3);
        button=(Button)findViewById(R.id.button);
        savednotes=getSharedPreferences("notes1",MODE_PRIVATE);
        savednotes2=getSharedPreferences("notes2",MODE_PRIVATE);
        savednotes3=getSharedPreferences("notes3",MODE_PRIVATE);
        mEditText.setText(savednotes.getString("tag1",""));
        mEditText2.setText(savednotes2.getString("tag2",""));
        mEditText3.setText(savednotes3.getString("tag3",""));
        button.setOnClickListener(ButtonListener);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }



    private void makeTag1(String tag){
        //String or = savednotes.getString(tag, null);
        SharedPreferences.Editor preferencesEditor = savednotes.edit();
        preferencesEditor.putString("tag1",tag); //change this line to this
        preferencesEditor.commit();
    }
    private void makeTag2(String tag){
        //String or = savednotes.getString(tag, null);
        SharedPreferences.Editor preferencesEditor = savednotes2.edit();
        preferencesEditor.putString("tag2",tag); //change this line to this
        preferencesEditor.commit();
    }
    private void makeTag3(String tag){
        //String or = savednotes.getString(tag, null);
        SharedPreferences.Editor preferencesEditor = savednotes3.edit();
        preferencesEditor.putString("tag3",tag); //change this line to this
        preferencesEditor.commit();
    }

    public View.OnClickListener ButtonListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            if(mEditText.getText().length()>0){
                makeTag1(mEditText.getText().toString());
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mEditText.getWindowToken(),0);

            }
            if(mEditText2.getText().length()>0){
                makeTag2(mEditText2.getText().toString());
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mEditText2.getWindowToken(),0);

            }
            if(mEditText3.getText().length()>0){
                makeTag3(mEditText3.getText().toString());
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mEditText3.getWindowToken(),0);

            }
            String name=mEditText.getText().toString();
            String contact=mEditText2.getText().toString();
            String phone=mEditText3.getText().toString();
            String sql="delete from "+tb_name;
            db.execSQL(sql);
            ContentValues cv=new ContentValues();
            cv.put("name",name);
            cv.put("contact",contact);
            cv.put("phone",phone);
            db.insert(tb_name,null,cv);
            db.close();
            Intent it= new Intent(SettingActivity.this,MainActivity.class);
            startActivity(it);
        }
    };
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}



