package at.photosniper.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import at.photosniper.PhotoSniperApp;
import at.photosniper.R;
import at.photosniper.widget.OngoingButton;


public class ScriptExecuteFragment extends PhotoSniperBaseFragment {

    private static final String TAG = ScriptExecuteFragment.class.getSimpleName();
    private OngoingButton mShutterButton;
    private Button mFileButton;
    private EditText mScript;
    private ScriptExecutionListener mListener = null;

    public ScriptExecuteFragment() {

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

        mFileButton = (Button) rootView.findViewById(R.id.button);
        mScript = (EditText) rootView.findViewById(R.id.editText);

        mFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // try to load file
                Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select a script"), PhotoSniperApp.OnGoingAction.SCRIPT);

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

        return text;

    }

    public void setScriptFile(Uri selectedFile) {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(getActivity().getContentResolver().openInputStream(selectedFile));

            BufferedReader br = new BufferedReader(inputStreamReader);

            String cmd = "";
            String line;
            boolean inCode = false;

            while ((line = br.readLine()) != null) {
                line = line.replace(" ", "");
                if (!inCode) {
                    inCode = (line.indexOf("<") > 0);
                }
                if (inCode) {
                    int cmmt = line.indexOf("//");

                    if (cmmt >= 0) {
                        line = line.substring(0, cmmt);
                    }
                    cmd += line + ";";
                }
                if (line.indexOf(">") > 0) {
                    break;
                }
            }

            br.close();

            cmd = cmd.replace(";;", ";");

            mScript.setText(cmd);

        } catch (Exception x) {


        }

    }


    public interface ScriptExecutionListener {
        void onExecuteScript(final String cmdSequence);
    }
}
