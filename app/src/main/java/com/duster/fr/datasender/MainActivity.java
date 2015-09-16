package com.duster.fr.datasender;

import android.app.Activity;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.os.Handler;

public class MainActivity extends ActionBarActivity {


    private  static final String TAG ="MainActivity";
    private BluetoothService bluetoothService;
    private TextView textView;
    //Button btnD,btnS,btnC,btnH;

    private Handler handler = new Handler(){
        private byte[] readBuf;
        public void handleMessage(Message msg){
            if(msg.what==0){
                readBuf = (byte[]) msg.obj;
                Log.i(TAG, "message received =" + new String(readBuf,0,msg.arg1));
            }

        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*btnD = (Button) findViewById(R.id.btnD);
        btnH = (Button) findViewById(R.id.btnH);
        btnS = (Button) findViewById(R.id.btnS);
        btnC = (Button) findViewById(R.id.btnC);*/
        bluetoothService = new BluetoothService(handler,this);
        textView = (TextView) findViewById(R.id.textView);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()){
            case  R.id.action_settings:
                return true;

            case  R.id.action_make_discoverable:
                bluetoothService.makeDiscoverable(this);
                bluetoothService.accept();
                return true;
            case  R.id.action_make_hidden:
                bluetoothService.stop();
                return true;

            case  R.id.action_send:
                bluetoothService.change();
                return true;

            case  R.id.action_change:
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    public void printValue(int value){
        textView.setText(Integer.toString(value));
    }
}
