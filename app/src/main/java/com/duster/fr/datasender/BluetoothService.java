package com.duster.fr.datasender;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
//import java.util.logging.Handler;
import android.content.Intent;
import android.os.Handler;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Created by Anas on 15/09/2015.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothService";
    protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private Handler mHandler;
    private Boolean send;
    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;

    private MainActivity activity;
    int testInt;

    public BluetoothService(Handler handler, MainActivity activity) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        this.activity = activity;
    }


    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

    }

    /* Starting the thread to enable connection to device*/
    public synchronized void accept() {
        Log.d(TAG, "accept");
        stop();
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    /*Starting the tread to manage the connection and enable transmissions*/

    public synchronized void Connected(BluetoothSocket socket) {
        Log.d(TAG, "connected");
        stop();

        mConnectedThread = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
    }

    public boolean sendOrStop(){
        if(mConnectedThread!=null){
            send = !send;
        }
        return send;
    }

    public void change(){
        if(mConnectedThread!=null){mConnectedThread.change();}
    }

    private class AcceptThread extends Thread {
        private static final String TAG = "AcceptThread";

        private final BluetoothServerSocket mmServerSocket;
        private BluetoothSocket mbtSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;

            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord("Data Sender for android", MY_UUID); // Unsed to get a BluetoothServerSocket running
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            mmServerSocket = tmp;

        }

        public void run(){
            /* Connection through a socket*/
            try{
                mbtSocket=mmServerSocket.accept(); // listening to connection requests
                                                    // not to be used in the main thread ( because it's a blocking call)
                Log.i(TAG,"connection accepted");
                mmServerSocket.close(); // Used to not accept additional connections

            } catch (IOException connectException) {
                /*enabling the connection and closing the socket*/
                Log.e(TAG,"close socket");
                try {
                    mmServerSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG,closeException.toString());
                }
            }

            if (mbtSocket!=null){
                BluetoothService.this.Connected(mbtSocket);
            }
        }

        public void cancel(){
            try {
                mmServerSocket.close();
                mbtSocket=null;
            } catch (IOException e) {
                Log.i(TAG,"The socket couldn't be closed",e);
            }
        }
    }

    private class ConnectedThread extends Thread {

        private static final String TAG = "ConnectedThread";

        private volatile boolean running = true;

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private DataBuilder dataBuilder = new DataBuilder();

        public ConnectedThread(BluetoothSocket socket, Handler handler) {
            mmSocket = socket;
            mHandler = handler;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Could not get any data from the socket");
                BluetoothService.this.stop();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            int bytes =0;
            byte[] buffer = new byte[500];
            send = false;
            testInt = 0;
            while(running){
                try{
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    bytes = mmInStream.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(bytes >0){
                    try {
                        mmInStream.read(buffer,0,bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(send){
                    write(dataBuilder.getData());
                    if(testInt>100000){
                        testInt=0;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.printValue(testInt++);
                        }
                    });
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.printValue(100000);
                        }
                    });
                }

            }

        }

        public void write(byte[] bytes){

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("writing", "unable to write data in the device", e);
                cancel();
            }

        }

        public void change(){
            dataBuilder.changeType();
        }

        public void cancel() {
            Log.i(TAG, "cancel Thread");
            running = false;

            try {Thread.sleep(100);
            } catch (InterruptedException e){
                Log.e(TAG, "interrupter");
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close socket", e);
            }
        }
    }

    protected void checkBluetoothEnabled(Activity activity){
        if(!mAdapter.isEnabled()){
            Intent enableBtIntent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent,1);
        }
    }

    protected void makeDiscoverable(Activity activity){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,600);
        activity.startActivity(discoverableIntent);
    }

}



