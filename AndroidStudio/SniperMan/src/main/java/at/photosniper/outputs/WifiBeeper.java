package at.photosniper.outputs;

import android.content.Context;

import at.photosniper.PhotoSniperApp;
import at.photosniper.wifi.MasterServer;

public class WifiBeeper implements IBeeper {

    private final Context mAppContext;

    public WifiBeeper(Context context) {
        mAppContext = context.getApplicationContext();
    }

    public void play(long length) {
        //Check this is not a bulb mode trigger
        if (PhotoSniperApp.getInstance(mAppContext).getBeepLength() == length) {
            MasterServer.getInstance().beep();
        }
    }

    public void play(long length) {
        //Check this is not a bulb mode trigger
        if (PhotoSniperApp.getInstance(mAppContext).getBeepLength() == length) {
            MasterServer.getInstance().beep();
        }
        //Don't need callback after pause, all that is handled in the Audio Beeper
    }

    public void stop() {
        // TODO Auto-generated method stub
    }

}
