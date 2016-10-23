package application.cepheus.falldetector;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.TextView;

public class TestActivity extends Activity {
    SQLiteDatabase db;
    static final String db_name="Fall";
    static final String tb_name="detail";
    TextView txv,txv2,txv3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        txv=(TextView)findViewById(R.id.textView5);
        txv2=(TextView)findViewById(R.id.textView6);
        txv3=(TextView)findViewById(R.id.textView7);
        db=openOrCreateDatabase(db_name, Context.MODE_PRIVATE,null);
        Cursor c=db.rawQuery("SELECT * FROM "+tb_name,null);
        if(c.moveToFirst()){
            String name=c.getString(0);
            String contact=c.getString(1);
            String phone=c.getString(2);
            txv.setText(name);
            txv2.setText(contact);
            txv3.setText(phone);
        }

    }

}
