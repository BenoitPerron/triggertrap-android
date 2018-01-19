package at.photosniper.outputs;

import android.content.Context;

public class OutputDispatcher implements PulseHandler.PulseListener {

    private static final String TAG = OutputDispatcher.class.getSimpleName();
    private final PulseHandler mPulseHandler;

    private OutputListener mListener;

    public OutputDispatcher(OutputListener listener, Context context) {
        mListener = listener;
        mPulseHandler = new PulseHandler(this, context);
    }

    public void trigger(long length) {
        mPulseHandler.playPulse(length);
    }

    public void trigger(long length, long pauseLength) {
        mPulseHandler.playPulse(length, pauseLength);
    }

    public void start() {
        mPulseHandler.playPulse(PulseHandler.FOREVER);
    }

    public void stop() {
        mPulseHandler.stop();
    }

    public void close() {
        mPulseHandler.close();
    }

    public void setOutputListener(OutputListener listener) {
        mListener = listener;
    }

    //Listener methods for the Pulse Handler
    @Override
    public void onPulseStart() {
        if (mListener != null) {
            mListener.onOutputStart();
        }

    }

    @Override
    public void onPulseStop() {
        if (mListener != null) {
            mListener.onOutputStop();
        }

    }

    @Override
    public void onPulsePauseDone() {
        if (mListener != null) {
            mListener.onOutputPauseDone();
        }

    }

    public interface OutputListener {
        void onOutputStart();

        void onOutputStop();

        void onOutputPauseDone();
    }

}
