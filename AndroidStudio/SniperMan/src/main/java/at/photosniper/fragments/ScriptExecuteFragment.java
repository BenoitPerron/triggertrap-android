package at.photosniper.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import at.photosniper.PhotoSniperApp;
import at.photosniper.R;
import at.photosniper.widget.OngoingButton;


public class ScriptExecuteFragment extends PhotoSniperBaseFragment {

    private static final String TAG = ScriptExecuteFragment.class.getSimpleName();
    private OngoingButton mShutterButton;
    private Button mFileOpenButton;
    private Button mFileSaveButton;
    private EditText mScript;
    private ScriptExecutionListener mListener = null;

    public ScriptExecuteFragment() {

    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (ScriptExecutionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ScriptExecutionListener");
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.script_mode, container, false);
        mShutterButton = (OngoingButton) rootView.findViewById(R.id.shutterButton);
//        TextView title = (TextView) rootView.findViewById(R.id.releaseTitle);
//        title.setTypeface(SAN_SERIF_LIGHT);

        mFileOpenButton = (Button) rootView.findViewById(R.id.button1);
        mFileSaveButton = (Button) rootView.findViewById(R.id.button2);

        mScript = (EditText) rootView.findViewById(R.id.editText);

        mFileOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // try to load file
                Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_OPEN_DOCUMENT); // Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select a script"), PhotoSniperApp.OnGoingAction.SCRIPT);

            }
        });
        mFileSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // try to save file
//                Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_);
//                startActivityForResult(Intent.createChooser(intent, "Save script as..."), PhotoSniperApp.OnGoingAction.SCRIPT);

            }
        });

        mShutterButton.setTouchListener(new OngoingButton.OnTouchListener() {
            @Override
            public void onTouchUp() {
                //Do nothing
            }

            @Override
            public void onTouchDown() {
                //startStopwatch();
                if (mListener != null) {
                    mListener.onExecuteScript(getValidatedCmd());

                }
            }
        });

        Editable text = mScript.getText();
        if ((text == null) || (text.length() < 2)) {
            mScript.setText("<        // start tag\n" + "B,0,0    // close shutter\n" + "D,100,1  // for 1 x 100ms\n" + "C,0,0    // open shutter again\n" + "D,1000,5 // wait for 5 x 1000 ms = 5 sec\n" + "K,10,0   // loop 10x\n" + ">        ");
        }

        return rootView;
    }

    @Override
    public void onStop() {
        Log.d(TAG, "Stopping");
        super.onStop();

    }

    @Override
    public void setActionState(boolean actionState) {
        if (actionState) {
            mState = State.STARTED;
        } else {
            mState = State.STOPPED;
        }
        setInitialUiState();
    }

    private void setInitialUiState() {
        if (mState == State.STARTED) {

        } else {
            mShutterButton.stopAnimation();
        }
    }

    private String getValidatedCmd() {
        String text = mScript.getText().toString();

        // validate
        String cmd = "";
        try {
            InputStream is = new ByteArrayInputStream(text.getBytes("UTF-8"));
            InputStreamReader inputStreamReader = new InputStreamReader(is);

            BufferedReader br = new BufferedReader(inputStreamReader);


            String line;
            boolean inCode = false;

            while ((line = br.readLine()) != null) {
                line = line.replace(" ", "");
                if (!inCode) {
                    inCode = (line.indexOf("<") >= 0);
                }
                if (inCode) {
                    int cmmt = line.indexOf("//");

                    if (cmmt >= 0) {
                        line = line.substring(0, cmmt);
                    }
                    cmd += line + ";";
                }
                if (line.indexOf(">") >= 0) {
                    break;
                }
            }

            br.close();
        } catch (Exception x) {
            Log.d(TAG, "script parsing failed");
        }
        cmd = cmd.replace(";;", ";");
        cmd = cmd.replace(">;", ">");
        cmd = cmd.replace("<;", "<");

        return cmd;

    }

    public void setScriptFile(Uri selectedFile) {
        try {
//            InputStreamReader inputStreamReader = new InputStreamReader(getActivity().getContentResolver().openInputStream(selectedFile));


//            BufferedReader br = new BufferedReader(inputStreamReader);
//
            String cmd = "";
//            String line;
//            boolean inCode = false;
//
//            while ((line = br.readLine()) != null) {
//                line = line.replace(" ", "");
//                if (!inCode) {
//                    inCode = (line.indexOf("<") > 0);
//                }
//                if (inCode) {
//                    int cmmt = line.indexOf("//");
//
//                    if (cmmt >= 0) {
//                        line = line.substring(0, cmmt);
//                    }
//                    cmd += line + ";";
//                }
//                if (line.indexOf(">") > 0) {
//                    break;
//                }
//            }
//
//            br.close();
//
//            cmd = cmd.replace(";;", ";");

            cmd = convertStreamToString(getActivity().getContentResolver().openInputStream(selectedFile));

            mScript.setText(cmd);

        } catch (Exception x) {

            Log.d(TAG, "file parsing failed");

        }

    }


    public interface ScriptExecutionListener {
        void onExecuteScript(final String cmdSequence);
    }
}
