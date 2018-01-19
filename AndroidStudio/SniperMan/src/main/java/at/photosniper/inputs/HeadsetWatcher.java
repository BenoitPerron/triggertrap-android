package at.photosniper.inputs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;

public class HeadsetWatcher {

    public static final int HEADSET_PLUGGED = 1;
    public static final int HEADSET_UNPLUGGED = 0;

    private static final String TAG = HeadsetWatcher.class.getSimpleName();

    private final HeadsetBroadcastReceiver headsetReceiver;

    //Support two listeners Primary is always set secondary is transient
    private ArrayList<HeadsetListener> mListeners = null;

    private HeadsetWatcher(Context ctx, HeadsetListener listener) {
        headsetReceiver = new HeadsetBroadcastReceiver(this);
        ctx.registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        mListeners = new ArrayList<>();
        mListeners.add(listener);
    }

    private void changed(int state) {
        Log.d(TAG, "State changed " + state);
        if (mListeners != null) {
            for (HeadsetListener listener : mListeners) {
                if (listener != null) {
                    listener.onHeadsetChanged(state);
                }
            }
        }
    }

    public void addSecondryListener(HeadsetListener listener) {
        mListeners.add(1, listener);
    }

    public void unregister(Context ctx) {
        ctx.unregisterReceiver(headsetReceiver);
    }

    public interface HeadsetListener {
        void onHeadsetChanged(int state);
    }

    public class HeadsetBroadcastReceiver extends BroadcastReceiver {
        final HeadsetWatcher watcher;

        public HeadsetBroadcastReceiver(HeadsetWatcher watcher) {
            super();
            this.watcher = watcher;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Broadcast Receiver", action);
            if ((action.compareTo(Intent.ACTION_HEADSET_PLUG)) == 0) {
                int headsetState = intent.getIntExtra("state", 0);
                watcher.changed(headsetState);
            }

        }

    }


}

