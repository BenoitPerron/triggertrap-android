

package at.photosniper.fragments;

 

import android.app.Activity;

import android.graphics.Color;

import android.os.Bundle;

import android.util.Log;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.SeekBar;

import android.widget.TextView;

 

import at.photosniper.PhotoSniperApp;

import at.photosniper.R;

 

import at.photosniper.inputs.MicSensorMonitor.SensorListener;

import at.photosniper.view.ArcProgress;

import at.photosniper.widget.OngoingButton;

import at.photosniper.widget.SeekArc;

 

public class LightSensorFragment extends
PhotoSniperBaseFragment implements SensorListener {

 

   
private static final String TAG =
LightSensorFragment.class.getSimpleName();

 

   
private ArcProgress mProgressArc;

   
private SeekBar mLightSensorSensitivity;

   
private TextView mBangText;

   
private OngoingButton mButton;

   
private int mState = States.SENSOR_CLOSED;

 

   
private int mThresholdProgress;

   
private int mSensitivityProgress;

 

   
private LightSensorListener mListener = null;

 

   
public LightSensorFragment() {

       
mRunningAction = PhotoSniperApp.OnGoingAction.BANG2;

    }

 

   
@Override

   
public void onAttach(Activity activity) {

       
super.onAttach(activity);

       
// This makes sure that the container activity has implemented

       
// the callback interface. If not, it throws an exception

        try {

           
mListener = (LightSensorListener) activity;

       
} catch (ClassCastException e) {

           
throw new ClassCastException(activity.toString() + " must implement
LightSensorListener");

       
}

    }

 

   
@Override

   
public void onCreate(Bundle savedInstanceState) {

       
super.onCreate(savedInstanceState);

                                

       
mThresholdProgress =
PhotoSniperApp.getInstance(getActivity()).getLightSensorThreshold();

       
mSensitivityProgress =
PhotoSniperApp.getInstance(getActivity()).getLightSensorSensitivity();

    }

 

   
@Override

   
public View onCreateView(LayoutInflater inflater, ViewGroup container,
Bundle savedInstanceState) {

 

       
View rootView = inflater.inflate(R.layout.bang, container, false);

 

       
TextView title = (TextView) rootView.findViewById(R.id.bangText);

       
title.setTypeface(SAN_SERIF_THIN);

 

       
TextView sensitivity = (TextView)
rootView.findViewById(R.id.sensitivityBangText);

       
sensitivity.setTypeface(SAN_SERIF_LIGHT);

 

       
mProgressArc = (ArcProgress)
rootView.findViewById(R.id.bangSensorDisplay);

       
mLightSensorSensitivity = (SeekBar)
rootView.findViewById(R.id.bangSensitivity);

       
SeekArc mThreshold = (SeekArc)
rootView.findViewById(R.id.bangThreshold);

 

       
//thresholdIndicator = (ImageButton)
rootView.findViewById(R.id.thresholdIndicator);

       
mBangText = (TextView) rootView.findViewById(R.id.bangText);

 

       
mButton = (OngoingButton) rootView.findViewById(R.id.bangButton);

       
mButton.setToggleListener(new OngoingButton.OnToggleListener() {

 

           
@Override

           
public void onToggleOn() {

                if (mState ==
States.SENSOR_CLOSED) {

                    mState =
States.SENSOR_OPEN;

                    Log.d(TAG, "Enabling
threshold....");

                    if (mListener != null) {

                       
mListener.onEnableLightSensorThreshold();

                        //volMonitor.enabledThreshold();

                    }

                }

           
}

 

           
@Override

           
public void onToggleOff() {

                if (mState ==
States.SENSOR_OPEN) {

                    mState =
States.SENSOR_CLOSED;

                    Log.d(TAG, "Disabling
threshold....");

                    if (mListener != null) {

                       
//volMonitor.disableThreshold();

                       
mListener.onDisableLightSensorThreshold();

                    }

                }

            }

 

       
});

 

       
mLightSensorSensitivity.setProgress(mSensitivityProgress);

       
mThreshold.setProgress(mThresholdProgress);

 

       
mLightSensorSensitivity.setOnSeekBarChangeListener(new
SeekBar.OnSeekBarChangeListener() {

           
@Override

           
public void onProgressChanged(SeekBar seekBar, int progress, boolean
fromUser) {

               
//micSensValue.setText(String.valueOf(progress));

 

                if (mListener != null) {

                   
//volMonitor.setMicSensitivity(mLightSensorSensitivity.getProgress());

                   
mListener.onSetLightSensorSensitivity(mLightSensorSensitivity.getProgress());

                    mSensitivityProgress =
mLightSensorSensitivity.getProgress();

 

                }

               
//mProgressBar.setMax(volMonitor.getSensorRange());

           
}

 

           
@Override

           
public void onStopTrackingTouch(SeekBar seekBar) {

           
}

 

           
@Override

           
public void onStartTrackingTouch(SeekBar seekBar) {

           
}

       
});

 

       
mThreshold.setOnSeekArcChangeListener(new
SeekArc.OnSeekArcChangeListener() {

           
@Override

           
public void onProgressChanged(SeekArc seekArc, int progress, boolean
fromUser) {

                if (mListener != null) {

 

                   
mListener.onSetLightSensorThreshold(progress);

                    mThresholdProgress =
progress;

                }

           
}

 

           
@Override

           
public void onStopTrackingTouch(SeekArc seekArc) {

           
}

 

           
@Override

           
public void onStartTrackingTouch(SeekArc seekArc) {

           
}

       
});

 

 

       
if (mListener != null) {

           


           
mListener.onSetLightSensorSensitivity(mLightSensorSensitivity.getProgress());

       
}

 

       
return rootView;

    }

 

   
@Override

   
public void onResume() {

       
super.onResume();

    }

 

   
@Override

   
public void onStart() {

       
Log.d(TAG, "onstart");

       
super.onStart();

       
startSensorMonitor();

    }

 

   
@Override

   
public void onStop() {

       
Log.d(TAG, "onstop");

       
super.onStop();

       
stopSensorMonitor();

                                

       
PhotoSniperApp.getInstance(getActivity()).setLightSensorSensitivity(mSensitivityProgress);

       
PhotoSniperApp.getInstance(getActivity()).setLightSensorThreshold(mThresholdProgress);

    }

 

   
@Override

   
public void onDestroy() {

       
super.onDestroy();

       
//volMonitor.release();

    }

 

   
private void startSensorMonitor() {

       
if (mListener != null) {

           
//volMonitor.start();

           
mListener.onStartLightSensor();

       
}

 

    }

 

   
private void stopSensorMonitor() {

       
if (mListener != null) {

           
//volMonitor.stop();

           
mListener.onStopLightSensor();

       
}

       
mProgressArc.setProgress(0);

    }

 

   
@Override

   
public void onSensorUpdate(int amplitude) {

       
if (mProgressArc != null) {

           
mProgressArc.setProgress(amplitude);

           
getActivity().runOnUiThread(new Runnable() {

                public void run() {

                   
mBangText.setTextColor(Color.BLACK);

                }

            });

       
}

 

    }

 

   
@Override

   
public void onExceedThreshold(int amplitude) {

       
getActivity().runOnUiThread(new Runnable() {

           
public void run() {

               
mBangText.setTextColor(Color.RED);

           
}

       
});

    }

 

   
@Override

   
public void setActionState(boolean actionState) {

       
if (actionState) {

           
mState = States.SENSOR_OPEN;

       
} else {

           
mState = States.SENSOR_CLOSED;

       
}

       
setInitialUiState();

    }

 

   
private void setInitialUiState() {

       
if (mState == States.SENSOR_OPEN) {

           
mButton.startAnimation();

       
} else {

           
if (mButton != null) {

                mButton.stopAnimation();

           
}

       
}

    }

 

 

   
private interface States {

       
int SENSOR_OPEN = 0;

       
int SENSOR_CLOSED = 1;

    }

 

   
public interface LightSensorListener {

       
void onStartLightSensor();

 

       
void onStopLightSensor();

 

       
void onEnableLightThreshold();

 

       
void onDisableLightThreshold();

 

       
void onSetLightSensorSensitivity(int sensitivity);

 

       
void onSetLightSensorThreshold(int threshold);

    }

}

 

