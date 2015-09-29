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
            outputStream.write(t);
            outputStream.write(d);
        } catch (IOException e) {
            Log.e(TAG, "error while concatenating the three byte arrays");
        }


        return Arrays.toString(outputStream.toByteArray()).getBytes();
    }


    public byte[] concatenateTwoBytes(ByteArrayOutputStream outputStream,byte[] n ,byte[] t ){

        if(DEBUG) Log.i(TAG,"Attempting to concatenate");

        try {
            outputStream.write(n);
            outputStream.write(t);
        } catch (IOException e) {
            Log.e(TAG, "error while concatenating the two byte arrays");
        }

        byte[] c = outputStream.toByteArray();
        String concat = Arrays.toString(c);
        byte[] concatByte = concat.getBytes();

        return concatByte;
    }



}
