package at.photosniper.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;

import at.photosniper.TTApp;

public class TriggertrapFragment extends Fragment {

    Typeface SAN_SERIF_LIGHT = null;
    Typeface SAN_SERIF_THIN = null;
    int mRunningAction = TTApp.OnGoingAction.NONE;
    Bundle mStateBundle;
    int mState = State.STOPPED;

    public TriggertrapFragment() {


    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        SAN_SERIF_LIGHT = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Light.ttf");
        SAN_SERIF_THIN = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Thin.ttf");
    }

    @Override
    public void onPause() {
        super.onPause();
        resetVolumeWarning();
    }

    public int getRunningAction() {
        return mRunningAction;
    }

    Bundle getStateBundle() {
        mStateBundle = new Bundle();
        mStateBundle.putString(TriggertrapFragment.BundleKey.FRAGMENT_TAG, getTag());
        return mStateBundle;
    }

    /*After the volume warning has been shown we need to reset to show again*/
    void resetVolumeWarning() {
//		  Intent intent = new Intent(WarningMessageManager.ACTION);
//          intent.putExtra(WarningMessageManager.ACTION_TYPE, WarningMessageManager.Action.RESET);
//          LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    void checkVolume() {
//		  Intent intent = new Intent(WarningMessageManager.ACTION);
//          intent.putExtra(WarningMessageManager.ACTION_TYPE, WarningMessageManager.Action.SHOW);
//          LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    public void setActionState(boolean actionState) {

    }

    public void dismissError() {
    }


    public interface BundleKey {
        String FRAGMENT_TAG = "fragment_tag";
        String IS_ACTION_ACTIVE = "is_action_active";
        String PULSE_INTERVAL = "pulse_interval";
        String MIDDLE_EXPOSURE = "middle_exposure";
        String NUMBER_EXPOSURES = "number_exposures";
        String EV_VALUE = "ev_value";
        String EASING = "easing";
        String WIFI_SLAVE_INFO = "wifi_slave_info";
    }

    interface State {
        int STARTED = 0;
        int STOPPED = 1;
    }
}
