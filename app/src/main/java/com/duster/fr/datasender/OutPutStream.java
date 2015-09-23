package com.duster.fr.datasender;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Anas on 23/09/2015.
 */
public class OutPutStream extends ByteArrayOutputStream {


    private static final String TAG = "OutPutStream";
    private static final boolean DEBUG = true;


    public byte[] concatenateData(ByteArrayOutputStream outputStream,byte[] n ,byte[] t , byte[] d){

        if(DEBUG) Log.i(TAG,"Attempting to concatenate");

        try {
            outputStream.write(n);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.write(t);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.write(d);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] c = outputStream.toByteArray();
        String concat = Arrays.toString(c);
        byte[] concatByte = concat.getBytes();

        return concatByte;
    }


    public byte[] concatenateTwoBytes(ByteArrayOutputStream outputStream,byte[] n ,byte[] t ){

        if(DEBUG) Log.i(TAG,"Attempting to concatenate");

        try {
            outputStream.write(n);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.write(t);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] c = outputStream.toByteArray();
        String concat = Arrays.toString(c);
        byte[] concatByte = concat.getBytes();

        return concatByte;
    }



}
