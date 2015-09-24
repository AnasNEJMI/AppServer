package com.duster.fr.datasender;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anas on 24/09/2015.
 */
public class SettingsFragment extends DialogFragment {

    //For debugging purposes
    private final static String TAG = "SettingsFragment";
    private static boolean DEBUG = true;

    //Layout elements for name changing
    private EditText nameNbr;
    private Spinner nameSide;
    private EditText nameFootsize;
    private Button nameChangeBtn;
    private String side;

    //Layout elements for changing the size of the foot
    private EditText editFootsize;
    private Button footsizeChangeBtn;

    //layouts for footsize changing



    //Communication interface
    Communicator communicator;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        communicator = (Communicator) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.settings,container,false);
        getDialog().setTitle("Settings");


        //Linking layout elements to their attributes

        /* -- name changing -- */

        nameNbr = (EditText) view.findViewById(R.id.nameNbr);
        nameSide = (Spinner) view.findViewById(R.id.nameSide);
        nameFootsize = (EditText) view.findViewById(R.id.nameFootsize);
        nameChangeBtn = (Button) view.findViewById(R.id.nameChangeBtn);


        nameChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String nbr = nameNbr.getText().toString();
                String foot = nameFootsize.getText().toString();
                if ((nbr.equals("") || nbr.isEmpty()) || (foot.equals("") || foot.isEmpty())) {
                    Toast.makeText(view.getContext(), "Make sure all values are correctly submitted", Toast.LENGTH_SHORT).show();
                } else {

                    int intNbr = Integer.parseInt(nbr);
                    int intFoot = Integer.parseInt(foot);
                    communicator.NameMessage(intNbr,side, intFoot);

                    /** -----  Return the values submitted to the MainActivity ----- **/
                }
            }
        });


        List<String> insoleS = new ArrayList<String>();
        insoleS.add("L");
        insoleS.add("R");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, insoleS);


        //Setting up the spinner
        //nameSide = (Spinner) view.findViewById(R.id.data);
        //ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(getActivity(),R.array.insole_side,android.R.layout.simple_spinner_item);
        dataAdapter.setDropDownViewResource(R.layout.spinner_textview);
        nameSide.setAdapter(dataAdapter);

        nameSide.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String itemAtPosition = new String(parent.getItemAtPosition(position).toString());
                if(itemAtPosition.equals("L")){
                    side = new String("L");
                    if (DEBUG) Log.i(TAG,"side update successful");
                }else if(itemAtPosition.equals("R")){
                    side = new String("R");
                    if (DEBUG) Log.i(TAG,"side update successful");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        /* -- footsize changing -- */
        footsizeChangeBtn = (Button) view.findViewById(R.id.footsizeChangeBtn);
        editFootsize = (EditText) view.findViewById(R.id.editFootsize);
        footsizeChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String foot = editFootsize.getText().toString();

                if (foot.equals("") || foot.isEmpty()){
                    Toast.makeText(view.getContext(), "Make sure all values the footsize is submitted", Toast.LENGTH_SHORT).show();
                } else {

                    int intNbr = Integer.parseInt(foot);
                    int intFoot = Integer.parseInt(foot);
                    communicator.FootsizeMessage(intFoot);

                    /** -----  Return the values submitted to the MainActivity ----- **/
                }
            }
        });



        return view;
    }

    interface Communicator{
        public void NameMessage(int nbr,String s, int foot);
        public void VersionMessage(String message);
        public void FootsizeMessage(int foot);
    }
}
