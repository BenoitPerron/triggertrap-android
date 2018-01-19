package at.photosniper.fragments.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import at.photosniper.PhotoSniperApp;
import at.photosniper.R;

import at.photosniper.fragments.WifiMasterFragment;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;
import eu.inmite.android.lib.dialogs.SimpleDialogFragment;


/**
 * Dialog which appears if the app is currently running as a WiFi master and asks if the user wants
 * to disable it.
 *
 * @author scottmellors
 * @since 2.1
 */
public class MasterRunningDialog extends SimpleDialogFragment {
    public void show(Activity activity) {
        new MasterRunningDialog().show(activity.getFragmentManager(), "master_running");
    }

    @Override
    public BaseDialogFragment.Builder build(BaseDialogFragment.Builder builder) {
        builder.setView(LayoutInflater.from(getActivity()).inflate(R.layout.running_action_dialog, null));
        builder.setTitle(getResources().getString(R.string.running_wifi_master));
        builder.setMessage(getResources().getString(R.string.would_you_like_to_stop_this_running_));
        builder.setPositiveButton(getResources().getString(R.string.yes), new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                WifiMasterFragment fragment = (WifiMasterFragment) getActivity().getFragmentManager().findFragmentByTag(PhotoSniperApp.FragmentTags.WIFI_MASTER);

                fragment.stopWifiMaster();
                dismiss();
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.no), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return builder;
    }
}
