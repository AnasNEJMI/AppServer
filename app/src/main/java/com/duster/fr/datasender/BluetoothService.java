package com.duster.fr.datasender;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
//import java.util.logging.Handler;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Anas on 15/09/2015.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothService";
    protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int mState;

    //To indicate the state of the current connection
    public static final int STATE_NONE = 0;       // nothing is done
    public static final int STATE_LISTEN = 1;     // currently listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


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
    public void setBluetoothServiceName(String s){
        mAdapter.setName(s);
    }

    private synchronized void setState(int state){
        if(MainActivity.DEBUG) Log.d(TAG, "setState" + mState +"-->"+state);
        mState=state;
    }

    public synchronized int getState(){
        return mState;
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
    // To write to the ConnectedThread in an unsynchronized manner

    public void write(byte[] outMsg){
        ConnectedThread r;

        synchronized (this){
            if (mState!=STATE_CONNECTED) return;
            r=mConnectedThread;
        }

        //unsynchronized wrinting

        r.write(outMsg);


    }
    public synchronized void disconnect() {
        if(MainActivity.DEBUG) Log.d(TAG, "disconnect");
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        setState(STATE_NONE);
    }

    /* Starting the thread to enable connection to device*/
    public synchronized void accept() {
        Log.d(TAG, "accept");
        stop();
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    /*Starting the tread to manage the connection and enable transmissions*/

    public synchronized void Connected(BluetoothSocket socket,BluetoothDevice device) {
        Log.d(TAG, "connected");
        stop();

        mConnectedThread = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.insName, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);

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

    public void sleep(int t){
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                if(MainActivity.DEBUG) Log.e(TAG, e.toString());
            }
            mmServerSocket = tmp;

        }

        public void run() {
            /* Connection through a socket*/

            while (mState != STATE_CONNECTED) {
                try {
                    mbtSocket = mmServerSocket.accept(); // listening to connection requests
                    // not to be used in the main thread ( because it's a blocking call)
                    Log.i(TAG, "connection accepted");
                    mmServerSocket.close(); // Used to not accept additional connections

                } catch (IOException connectException) {
                /*enabling the connection and closing the socket*/
                    if (MainActivity.DEBUG) Log.e(TAG, "close socket");
                    try {
                        mmServerSocket.close();
                    } catch (IOException closeException) {
                        if (MainActivity.DEBUG) Log.e(TAG, closeException.toString());
                    }

                }
                if (mbtSocket != null) {
                    BluetoothService.this.Connected(mbtSocket, mbtSocket.getRemoteDevice());
                }
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

    private void connectionLost(){
        setState(STATE_LISTEN);

        // send failure message back to the UI Activity

        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST,"The connection with the device was last");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
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
                if(MainActivity.DEBUG) Log.e(TAG, "Could not get any data from the socket");
                BluetoothService.this.stop();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if(MainActivity.DEBUG) Log.i(TAG,"Begin mConnected");
            int bytes =0;
            int b=0;
            byte[] buffer = new byte[1024];
            send = false;
            testInt = 0;
            while(true){
                try{
                    b = mmInStream.available();

                } catch (IOException e) {
                    if(MainActivity.DEBUG) Log.e(TAG,"disconnected",e);
                    BluetoothService.this.disconnect();
                }

                if(b>0){
                    try {
                        bytes = mmInStream.read(buffer,0,b);
                        mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    } catch (IOException e) {
                        BluetoothService.this.disconnect();
                    }
                }else{
                    //Log.i(TAG,"no stream found");
                }
                /*if(bytes >0){
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
                }*/

            }

        }

        public void write(byte[] buffer){

            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e("writing", "unable to write data in the device", e);
                cancel();
            }

        }

        public void read(byte[] bytes){
            try {
                mmInStream.read(bytes);
            }catch (IOException e){
                Log.e("Reading", "Unable to read data on the stream", e);
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
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        activity.startActivity(discoverableIntent);
    }

}