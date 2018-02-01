package at.photosniper.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;

import at.photosniper.PhotoSniperApp;

public class PhotoSniperBaseFragment extends Fragment {

    Typeface SAN_SERIF_LIGHT = null;
    Typeface SAN_SERIF_THIN = null;
    int mRunningAction = PhotoSniperApp.OnGoingAction.NONE;
    Bundle mStateBundle;
    int mState = State.STOPPED;

    public PhotoSniperBaseFragment() {


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
//        resetVolumeWarning();
    }

    public int getRunningAction() {
        return mRunningAction;
    }

    Bundle getStateBundle() {
        mStateBundle = new Bundle();
        mStateBundle.putString(PhotoSniperBaseFragment.BundleKey.FRAGMENT_TAG, getTag());
        return mStateBundle;
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
