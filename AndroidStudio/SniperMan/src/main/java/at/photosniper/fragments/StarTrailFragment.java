package at.photosniper.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import at.photosniper.PhotoSniperApp;
import at.photosniper.R;
import at.photosniper.util.DialpadManager;
import at.photosniper.util.PulseGenerator;
import at.photosniper.view.CircleTimerView;
import at.photosniper.view.CountingTimerView;
import at.photosniper.view.SimpleTimerView;
import at.photosniper.widget.NumericView;
import at.photosniper.widget.OngoingButton;
import at.photosniper.widget.TimerView;


public class StarTrailFragment extends PulseSequenceFragment {

    private static final String TAG = StarTrailFragment.class.getSimpleName();
    private DialpadManager.InputSelectionListener mInputListener = null;
    private View mRootView;
    private OngoingButton mButton;
    private TimerView mExposureTimeInput;
    private TimerView mGapTimeInput;

    private NumericView mNrOfPicturesInput;
    private View mButtonContainer;
    private View mCountDownLayout;

    private int mInitialIterations = 0;
    private long mExposure;
    private long mGap;
    private boolean mSyncCircle = false;

    private CircleTimerView mCircleTimerView;
    private CountingTimerView mTimerText;
    private SimpleTimerView mExposureTimerText;
    private SimpleTimerView mGapTimerText;
    private TextView mSequenceCountText;

    private Animation mSlideInFromTop;
    private Animation mSlideOutToTop;


    public StarTrailFragment() {
        mRunningAction = PhotoSniperApp.OnGoingAction.STAR_TRAIL;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mInputListener = (DialpadManager.InputSelectionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DialpadManager.InputSelectionListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.star_trail, container, false);
        mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInputListener != null) {
                    mInputListener.onInputDeSelected();
                    //Set the text on the input just in case the user entered something weird like 88 mins.
                    //mTimeView.setTextInputTime(mTimeView.getTime());

                }

            }
        });
        TextView exposures = (TextView) mRootView.findViewById(R.id.exposuresText);
        TextView gap = (TextView) mRootView.findViewById(R.id.gapText);
        TextView exposureLabel = (TextView) mRootView.findViewById(R.id.exposureLabel);
        TextView pauseLabel = (TextView) mRootView.findViewById(R.id.PauseLabel);


        exposures.setTypeface(SAN_SERIF_LIGHT);
        gap.setTypeface(SAN_SERIF_LIGHT);
        exposureLabel.setTypeface(SAN_SERIF_THIN);
        pauseLabel.setTypeface(SAN_SERIF_THIN);


        setUpIterations();
        setUpTimeInputs();
        setKeyBoardSize();
        setUpButton();
        setUpAnimations();
        setUpCircleTimer();

        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

        //Persist the state of the star trail mode
        PhotoSniperApp.getInstance(getActivity()).setStarTrailIterations(mNrOfPicturesInput.getValue());
        PhotoSniperApp.getInstance(getActivity()).setStarTrailExposure(mExposureTimeInput.getTime());
        PhotoSniperApp.getInstance(getActivity()).setStarTrailGap(mGapTimeInput.getTime());
    }

    private void setUpIterations() {
        mNrOfPicturesInput = (NumericView) mRootView.findViewById(R.id.starIterations);
        mNrOfPicturesInput.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mNrOfPicturesInput.getState() == TimerView.State.UN_SELECTED) {
                            mInputListener.onInputSelected(mNrOfPicturesInput);
                        } else {
                            mInputListener.onInputDeSelected();
                        }
                        break;
                }
                return true;
            }
        });


        Bundle fragmentState = getArguments();
        if (fragmentState != null) {
            //TODO restore state for rotation

        } else {
            //Restore state of time lapse from persistent storage
            mInitialIterations = PhotoSniperApp.getInstance(getActivity()).getStarTrailIterations();
            Log.d(TAG, "Initial Interations: " + mInitialIterations);
            mExposure = PhotoSniperApp.getInstance(getActivity()).getStarTrailExposure();
            mGap = PhotoSniperApp.getInstance(getActivity()).getStarTrailGap();
        }
        mNrOfPicturesInput.initValue(mInitialIterations);

    }


    private void setUpButton() {
        mButton = (OngoingButton) mRootView.findViewById(R.id.starTrailButton);
        mButton.setToggleListener(new OngoingButton.OnToggleListener() {

            @Override
            public void onToggleOn() {
                Log.d(TAG, "onToggleON");
                onStartTimer();

            }

            @Override
            public void onToggleOff() {
                Log.d(TAG, "onToggleOff");
                onStopTimer();
            }
        });
    }

    private void setUpTimeInputs() {
        View mExposureTime = mRootView.findViewById(R.id.starExposure);
        View mGapTime = mRootView.findViewById(R.id.starGap);
        mExposureTimeInput = (TimerView) mExposureTime.findViewById(R.id.timerTimeText);
        mExposureTime.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mExposureTimeInput.getState() == TimerView.State.UN_SELECTED) {
                            mInputListener.onInputSelected(mExposureTimeInput);
                        } else {
                            mInputListener.onInputDeSelected();
                        }
                        break;
                }
                return true;
            }
        });

        mGapTimeInput = (TimerView) mGapTime.findViewById(R.id.timerTimeText);
        mGapTime.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mGapTimeInput.getState() == TimerView.State.UN_SELECTED) {
                            mInputListener.onInputSelected(mGapTimeInput);
                        } else {
                            mInputListener.onInputDeSelected();
                        }
                        break;
                }
                return true;
            }
        });
        mExposureTimeInput.setTextInputTime(mExposure);
        mExposureTimeInput.initInputs(mExposure);
        mGapTimeInput.setTextInputTime(mGap);
        mGapTimeInput.initInputs(mGap);

    }

    private void setKeyBoardSize() {
        mButtonContainer = mRootView.findViewById(R.id.buttonContainer);
        final ViewTreeObserver vto = mRootView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int buttonContainerHeight = mButtonContainer.getHeight();
                int buttonContainerWidth = mButtonContainer.getWidth();

                Log.d(TAG, "Button container height is: " + buttonContainerHeight);
                mInputListener.inputSetSize(buttonContainerHeight, buttonContainerWidth);
                ViewTreeObserver obs = mRootView.getViewTreeObserver();
                obs.removeGlobalOnLayoutListener(this);

            }
        });
    }

    private void setUpAnimations() {
        mSlideInFromTop = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_from_top);
        mSlideOutToTop = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_to_top);

    }

    private void setUpCircleTimer() {
        Log.d(TAG, "Setting up circle timer");
        mCountDownLayout = mRootView.findViewById(R.id.circularTimer);
        mCircleTimerView = (CircleTimerView) mRootView.findViewById(R.id.circleTimer);
        mTimerText = (CountingTimerView) mRootView.findViewById(R.id.countingTimeText);
        mCircleTimerView.setTimerMode(true);
        mExposureTimerText = (SimpleTimerView) mRootView.findViewById(R.id.expsoureTimeText);
        mGapTimerText = (SimpleTimerView) mRootView.findViewById(R.id.gapTimeText);
        mSequenceCountText = (TextView) mRootView.findViewById(R.id.sequenceCount);

        mCountDownLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Consume the touch event when the count down is visible
                return true;
            }
        });

    }

    private void showCountDown() {
        mCountDownLayout.setVisibility(View.VISIBLE);

    }

    private void hideCountDown() {
        if (mCountDownLayout != null) {
            mCountDownLayout.setVisibility(View.GONE);
        }
    }

    private void onStartTimer() {

        if (mState == State.STOPPED) {

            if (mNrOfPicturesInput.getValue() == 0 || mExposureTimeInput.getTime() == 0 || mGapTimeInput.getTime() == 0) {
                mButton.stopAnimation();
                return;
            }

            mState = State.STARTED;

            if (PhotoSniperApp.getInstance(getActivity()).isSynchroneMode()) {
                String cmdSequence = mPulseGenerator.getStarTrailSequenceCommand(mNrOfPicturesInput.getValue(), mExposureTimeInput.getTime(), mGapTimeInput.getTime());
                mPulseSeqListener.onRunBatchInsteadPulse(cmdSequence);

            }

            mCountDownLayout.setVisibility(View.VISIBLE);
            mCountDownLayout.startAnimation(mSlideInFromTop);
            mCircleTimerView.setPassedTime(0, false);

            long totaltime = 0;


            mPulseSequence = mPulseGenerator.getStarTrailSequence(mNrOfPicturesInput.getValue(), mExposureTimeInput.getTime(), mGapTimeInput.getTime());
            mPulseSeqListener.onPulseSequenceCreated(PhotoSniperApp.OnGoingAction.STAR_TRAIL, mPulseSequence, false);

            totaltime = PulseGenerator.getSequenceTime(mPulseSequence);
            mExposureTimerText.setTime(mPulseSequence[0]);
            mGapTimerText.setTime(mPulseSequence[1]);
            String sequenceProgress = 1 + "/" + (mPulseSequence.length / 2);
            mSequenceCountText.setText(sequenceProgress);

            Log.d(TAG, "Total time Circle: " + totaltime);
            mCircleTimerView.setIntervalTime(totaltime);
            mTimerText.setTime(totaltime, false);
            mCircleTimerView.startIntervalAnimation();
            mTimerText.setTime(totaltime, false);

        }
    }

    private void onStopTimer() {
        if (mState == State.STARTED) {

            mButton.stopAnimation();
            mState = State.STOPPED;

            if (PhotoSniperApp.getInstance(getActivity()).isSynchroneMode()) {

                mCircleTimerView.abortIntervalAnimation();
                mCountDownLayout.startAnimation(mSlideOutToTop);
                mCountDownLayout.setVisibility(View.GONE);
                mPulseSeqListener.onPulseSequenceCancelled();
                //mProgressCountText.setText(String.valueOf(0));
            }
        }
    }

    public long getExposureTime() {
        return mExposureTimeInput.getTime();
    }

    public int getExposureCount() {
        return mNrOfPicturesInput.getValue();
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
            mSyncCircle = true;
            showCountDown();
            mButton.startAnimation();
        } else {
            if (mButton != null) {
                hideCountDown();
                mButton.stopAnimation();
            }
        }
    }

    public void onPulseStarted(int currentCount, int totalCount, long timeToNext, long totalTimeMaining) {
        String sequenceProgress = currentCount + "/" + totalCount;
        mSequenceCountText.setText(sequenceProgress);
    }

    public void onPulseUpdate(long[] sequence, int exposures, long timeToNext, long remainingPulseTime, long remainingSequenceTime) {
        //Log.d(TAG,"Pulse update exposures: " +exposures);
        int currentSeq = exposures - 1;
        long currentExposure = sequence[currentSeq * 2];
        long currentGap = sequence[(currentSeq * 2) + 1];
        long remainingExposure = 0;
        long remainingGap = 0;
        if (remainingPulseTime > currentGap) {
            //We need to count down the exposure
            remainingExposure = currentExposure - (timeToNext - remainingPulseTime);
            mExposureTimerText.setTime(remainingExposure);
            if ((remainingGap == 0) && ((currentSeq * 2) + 1) <= sequence.length) {
                long nextGap = sequence[(currentSeq * 2) + 1];
                mGapTimerText.setTime(nextGap);
            } else if (((currentSeq * 2) + 1) > sequence.length) {
                mGapTimerText.setTime(0);
            }
        } else {
            //We need to count down the gap
            remainingGap = remainingPulseTime;
            mGapTimerText.setTime(remainingGap);
            if ((remainingExposure == 0) && ((currentSeq * 2) + 2) < sequence.length) {
                long nextExposure = sequence[(currentSeq * 2) + 2];
                mExposureTimerText.setTime(nextExposure);
            } else if (((currentSeq * 2) + 2) >= sequence.length) {
                mExposureTimerText.setTime(0);
            }

        }

        mTimerText.setTime(remainingSequenceTime, true);
        if (remainingPulseTime != 0) {
            if (mSyncCircle) {
                String sequenceProgress = exposures + "/" + (sequence.length / 2);
                mSequenceCountText.setText(sequenceProgress);
                synchroniseCircle(remainingSequenceTime, sequence);
            }
        }
    }

    private void synchroniseCircle(long remainingTime, long[] sequence) {
        long totaltime = PulseGenerator.getSequenceTime(sequence);
        Log.d(TAG, "Sequence time: " + totaltime);
        Log.d(TAG, "Remaining time: " + remainingTime);
        mCircleTimerView.setIntervalTime(totaltime);
        mCircleTimerView.setPassedTime((totaltime - remainingTime), true);
        mCircleTimerView.startIntervalAnimation();
        mSyncCircle = false;
    }

    public void onPulseStop() {
        mTimerText.setTime(0, true);
        onStopTimer();
    }


}
