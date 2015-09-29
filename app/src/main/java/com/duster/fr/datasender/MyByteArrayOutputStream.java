package com.duster.fr.datasender;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Anas on 25/09/2015.
 */
public class MyByteArrayOutputStream extends ByteArrayOutputStream{

    private static final String TAG = "OutPutStream";


    public byte[] concatenateThreeBytes(ByteArrayOutputStream outputStream,byte[] n ,byte[] t , byte[] d){

        if(MainActivity.DEBUG) Log.i(TAG, "Attempting to concatenate");

        try {
            outputStream.write(n);
            outputStream.write(t);
            outputStream.write(d);
        } catch (IOException e) {
            if(MainActivity.DEBUG) Log.e(TAG, "error while concatenating the three byte arrays");
        }

        //byte[] c = outputStream.toByteArray();
        //String concat = Arrays.toString(c);
        //byte[] concatByte = concat.getBytes();

        return outputStream.toByteArray();
    }


    public byte[] concatenateTwoBytes(ByteArrayOutputStream outputStream,byte[] n ,byte[] t ){

        if(MainActivity.DEBUG) Log.i(TAG,"Attempting to concatenate");

        try {
            outputStream.write(n);
            outputStream.write(t);
        } catch (IOException e) {
            if(MainActivity.DEBUG) Log.e(TAG, "error while concatenating the two byte arrays");
        }
        //byte[] c = outputStream.toByteArray();
        //String concat = Arrays.toString(c);
        //byte[] concatByte = concat.getBytes();

        return outputStream.toByteArray();
    }
}
