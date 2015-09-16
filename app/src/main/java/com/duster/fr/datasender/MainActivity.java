package com.duster.fr.datasender;

import android.app.Activity;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import android.os.Handler;

public class MainActivity extends Activity {


    private  static final String TAG ="MainActivity";
    private BluetoothService bluetoothService;
    private TextView textView;

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
        if (id == R.id.action_settings) {
            return true;
        }else if(id == R.id.action_make_discoverable) {
            bluetoothService.makeDiscoverable(this);
            bluetoothService.accept();
            return true;
        }else if(id == R.id.action_send){
            bluetoothService.sendOrStop();
            return true;

        }else if(id == R.id.action_change){
            bluetoothService.change();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void printValue(int value){
        textView.setText(Integer.toString(value));
    }
}
