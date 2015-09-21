package com.duster.fr.datasender;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.os.Handler;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends ActionBarActivity {

    // for Debuging purposes
    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = true;

    // Message types sent from BlutoothService handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    //Key names received from BlutoothService handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    //String buffer for outgoing messages
    private StringBuffer mOutStringBuffer = new StringBuffer();

    private String mConnectedDeviceName = null;
    private BluetoothService bluetoothService;

    private DataBuilder dataBuilder = new DataBuilder();


    //Layouts
    private TextView textView;
    private  Button send;
    private  Button abort;
    private EditText sensorNumber;
    private EditText frequency;
    private Thread loop;


    // DataProvider
    private DataProvider dataProvider;

    //Number of data package sent
    private  int d =0;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                            Toast.makeText(getApplicationContext(), "Connected to "
                                    + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            /***********/
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            /**********/
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //textView.setText(writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //dataBuilder.setSensorNumber(Integer.parseInt(readMessage));
                    if(DEBUG)Log.d(TAG,"Message read");
                    Toast.makeText(getApplicationContext(),"message Read",Toast.LENGTH_SHORT).show();
                    textView.setText(readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insole_simulator);
        //ActionBar actionBar = getActionBar();
        //actionBar.setBackgroundDrawable(new ColorDrawable(Color.RED)); // set your desired color

        bluetoothService = new BluetoothService(mHandler,this);
        bluetoothService.accept();
        textView = (TextView) findViewById(R.id.clientMsg);
        sensorNumber = (EditText) findViewById(R.id.editSensorNumber);
        frequency = (EditText) findViewById(R.id.editFrequency);
        send = (Button) findViewById(R.id.sendBtn);
        send.setBackgroundResource(R.drawable.send_selector);



        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageS = sensorNumber.getText().toString();
                String messageF = frequency.getText().toString();

                if (messageF.isEmpty() || messageS.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Make sure you enter both the number of sensors and the frequency ", Toast.LENGTH_SHORT).show();
                } else {
                    int sensorNbr = Integer.parseInt(messageS);
                    final int frq = Integer.parseInt(messageF);

                    dataProvider = new DataProvider(sensorNbr, frq, 0);


                    loop = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {

                                while (dataProvider.getSend() == true) {
                                    String message = Arrays.toString(dataProvider.getData());
                                    //Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                                    message += d;
                                    d++;
                                    sendMessage(message);
                                    bluetoothService.sleep(1000 / frq);
                                    //int dt = dataProvider.getDataType();
                                    // if(dt ==3){ dataProvider.setDataType(0);}
                                    //else{dataProvider.setDataType(dt+1);}

                                }
                            }
                        }
                    });
                    loop.start();

                    //if(dataProvider.getSend()==false){loop.interrupt();}


                    // check if the values entered are integers
                    /*try {
                        int num = Integer.parseInt(messageS);
                        Log.i("", num + " is a number");
                    } catch (NumberFormatException e) {
                        Log.i("", messageS + "is not a number");
                        Toast.makeText(getApplicationContext(), "the number of sensors should be a number", Toast.LENGTH_SHORT);
                    }

                    try {
                        int num = Integer.parseInt(messageF);
                        Log.i("", num + " is a number");
                    } catch (NumberFormatException e) {
                        Log.i("", messageF + "is not a number");
                        Toast.makeText(getApplicationContext(), "the number of sensors should be a number", Toast.LENGTH_SHORT);
                    }*/

                    // send data if connected


                    // clear EditTexts
                    sensorNumber.setText("");
                    frequency.setText("");
                    //sendMessage(messageS);
                    //sendMessage(messageF);

                }
            }
        });

        abort = (Button) findViewById(R.id.abortBtn);
        abort.setBackgroundResource(R.drawable.abort_selector);

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataProvider.abortSend();
                d=0;
                if(DEBUG){Log.d(TAG,"Trying to interrupt the loop");}
                loop.interrupt();
                if(DEBUG){Log.d(TAG,"Trying Interruption successful");}


            }
        });

    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            bluetoothService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //sensorNumber.setText(mOutStringBuffer);
        }
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

            case  R.id.action_send:
                bluetoothService.sendOrStop();
                return true;

            case  R.id.action_change:
                bluetoothService.change();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    public void printValue(int value){
        textView.setText(Integer.toString(value));
    }
}