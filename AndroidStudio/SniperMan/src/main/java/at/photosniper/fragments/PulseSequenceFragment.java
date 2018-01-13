package at.photosniper.fragments;

import android.app.Activity;

import at.photosniper.util.PulseGenerator;

public class PulseSequenceFragment extends TriggertrapFragment {

    protected PulseSequenceListener mPulseSeqListener;
    protected PulseGenerator mPulseGenerator;
    protected long[] mPulseSequence;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPulseGenerator = new PulseGenerator(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPulseSeqListener = (PulseSequenceListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PulseSequenceListener");
        }


    }

    public interface PulseSequenceListener {
        void onPulseSequenceCreated(int ongoingAction, long[] sequence, boolean repeat);

        void onPulseSequenceCancelled();
    }

}
