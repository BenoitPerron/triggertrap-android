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

import at.photosniper.inputs.MicVolumeMonitor.VolumeListener;
import at.photosniper.view.ArcProgress;
import at.photosniper.widget.OngoingButton;
import at.photosniper.widget.SeekArc;

public class SoundSensorFragment extends PhotoSniperBaseFragment implements VolumeListener {

    private static final String TAG = SoundSensorFragment.class.getSimpleName();

    private ArcProgress mProgressArc;
    private SeekBar mMicSensitivity;
    private TextView mBangText;
    private OngoingButton mButton;
    private int mState = States.MIC_CLOSED;

    private int mThresholdProgress;
    private int mSensitivityProgress;

    private SoundSensorListener mListener = null;

    public SoundSensorFragment() {
        mRunningAction = PhotoSniperApp.OnGoingAction.BANG;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (SoundSensorListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement SoundSensorListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThresholdProgress = PhotoSniperApp.getInstance(getActivity()).getSoundSensorThreshold();
        mSensitivityProgress = PhotoSniperApp.getInstance(getActivity()).getSoundSensorSensitivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.bang, container, false);

        TextView title = (TextView) rootView.findViewById(R.id.bangText);
        title.setTypeface(SAN_SERIF_THIN);

        TextView sensitivity = (TextView) rootView.findViewById(R.id.sensitivityBangText);
        sensitivity.setTypeface(SAN_SERIF_LIGHT);

        mProgressArc = (ArcProgress) rootView.findViewById(R.id.bangVolumeDisplay);
        mMicSensitivity = (SeekBar) rootView.findViewById(R.id.bangSensitivity);
        SeekArc mThreshold = (SeekArc) rootView.findViewById(R.id.bangThreshold);

        //thresholdIndicator = (ImageButton) rootView.findViewById(R.id.thresholdIndicator);
        mBangText = (TextView) rootView.findViewById(R.id.bangText);

        mButton = (OngoingButton) rootView.findViewById(R.id.bangButton);
        mButton.setToggleListener(new OngoingButton.OnToggleListener() {

            @Override
            public void onToggleOn() {
                if (mState == States.MIC_CLOSED) {
                    mState = States.MIC_OPEN;
                    Log.d(TAG, "Enabling threshold....");
                    if (mListener != null) {
                        mListener.onEnableSoundThreshold();
                        //volMonitor.enabledThreshold();
                    }
                }
            }

            @Override
            public void onToggleOff() {
                if (mState == States.MIC_OPEN) {
                    mState = States.MIC_CLOSED;
                    Log.d(TAG, "Disabling threshold....");
                    if (mListener != null) {
                        //volMonitor.disableThreshold();
                        mListener.onDisableSoundThreshold();
                    }
                }
            }

        });

        mMicSensitivity.setProgress(mSensitivityProgress);
        mThreshold.setProgress(mThresholdProgress);

        mMicSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //micSensValue.setText(String.valueOf(progress));

                if (mListener != null) {
                    //volMonitor.setMicSensitivity(mMicSensitivity.getProgress());
                    mListener.onSetMicSensitivity(mMicSensitivity.getProgress());
                    mSensitivityProgress = mMicSensitivity.getProgress();

                }
                //mProgressBar.setMax(volMonitor.getVolumeRange());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
        });

        mThreshold.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
                if (mListener != null) {
                    //volMonitor.setThreshold(progress);
                    mListener.onSetSoundThreshold(progress);
                    mThresholdProgress = progress;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {
            }
        });

        //volMonitor = new MicVolumeMonitor(this);
        if (mListener != null) {
            //volMonitor.setMicSensitivity(mMicSensitivity.getProgress());
            mListener.onSetMicSensitivity(mMicSensitivity.getProgress());
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
        startVolumeMonitor();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onstop");
        super.onStop();
        stopVolumeMonitor();
        PhotoSniperApp.getInstance(getActivity()).setSoundSensorSensitivity(mSensitivityProgress);
        PhotoSniperApp.getInstance(getActivity()).setSoundSensorThreshold(mThresholdProgress);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //volMonitor.release();
    }

    private void startVolumeMonitor() {
        if (mListener != null) {
            //volMonitor.start();
            mListener.onStartSoundSensor();
        }

    }

    private void stopVolumeMonitor() {
        if (mListener != null) {
            //volMonitor.stop();
            mListener.onStopSoundSensor();
        }
        mProgressArc.setProgress(0);
    }

    boolean textUpdated = false;

    @Override
    public void onVolumeUpdate(int amplitude) {
        if (mProgressArc != null) {
            mProgressArc.setProgress(amplitude);
            if (textUpdated)
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mBangText.setTextColor(Color.BLACK);
                    textUpdated = false;
                }
            });
        }

    }

    @Override
    public void onExceedVolumeThreshold(int amplitude) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mBangText.setTextColor(Color.RED);
                textUpdated = true;
            }
        });
    }

    @Override
    public void setActionState(boolean actionState) {
        if (actionState) {
            mState = States.MIC_OPEN;
        } else {
            mState = States.MIC_CLOSED;
        }
        setInitialUiState();
    }

    private void setInitialUiState() {
        if (mState == States.MIC_OPEN) {
            mButton.startAnimation();
        } else {
            if (mButton != null) {
                mButton.stopAnimation();
            }
        }
    }


    private interface States {
        int MIC_OPEN = 0;
        int MIC_CLOSED = 1;
    }

    public interface SoundSensorListener {
        void onStartSoundSensor();

        void onStopSoundSensor();

        void onEnableSoundThreshold();

        void onDisableSoundThreshold();

        void onSetMicSensitivity(int sensitivity);

        void onSetSoundThreshold(int threshold);
    }
}
