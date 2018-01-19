package at.photosniper.outputs;

import android.content.Context;
import android.util.Log;

public class PulseHandler implements AudioBeeper.AudioBeeperListener {

    public static final long FOREVER = Long.MAX_VALUE;
    private static final String TAG = PulseHandler.class.getSimpleName();
    private int cursor = 0;
    private int count = 0;
    private long[] sequence;

//    private final AudioBeeper mBeeper;

    private PulseListener mListener;

    private PulseHandler() {
//        mBeeper = new AudioBeeper(this, null);
    }

    public PulseHandler(PulseListener listener, Context context) {
//        mBeeper = new AudioBeeper(this, context);
        WifiBeeper mWifiBeeper = new WifiBeeper(context);
        mListener = listener;
    }

    public void setPulseSequence(long[] sequence) {
        cursor = 0;
        count = 0;
        this.sequence = sequence;
    }

    public void playPulse(long length) {
//        mBeeper.play(length, 0);
//        mWifiBeeper.play(length, 0);
    }

    public void playPulse(long length, long pauseLength) {
//        mBeeper.play(length, pauseLength);
//        mWifiBeeper.play(length, pauseLength);
    }

    //TODO remove this no longer needed sequence handling now in service.
    public void playNextPulseInSequence() {

        if (sequence == null || sequence.length < cursor + 2) {
//            mBeeper.stop();
            return;
        }
        if (sequence[cursor] > 0) {
            count++;
        }
        Log.d("PulseSequence", "Sending pulse for " + sequence[cursor] + " then pausing for " + sequence[cursor + 1] + " count: " + count + " sequence length: " + sequence.length);

//        mBeeper.play(sequence[cursor++], sequence[cursor++]);

    }

    public void stop() {
//        mBeeper.stop();
    }

    public void close() {
//        mBeeper.close();
    }

    //ListenerMethods for the Audio Beeper
    @Override
    public void onAudioPlayStart() {
        if (mListener != null) {
            mListener.onPulseStart();
        }

    }

    @Override
    public void onAudioPlayStop() {
        if (mListener != null) {
            mListener.onPulseStop();
        }

    }

    @Override
    public void onAudioPlayPauseDone() {
        if (mListener != null) {
            mListener.onPulsePauseDone();
        }

    }

    public interface PulseListener {
        void onPulseStart();

        void onPulseStop();

        void onPulsePauseDone();
    }
}
 