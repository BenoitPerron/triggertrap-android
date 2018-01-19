package at.photosniper.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import antistatic.spinnerwheel.AbstractWheel;
import antistatic.spinnerwheel.OnWheelScrollListener;
import antistatic.spinnerwheel.adapters.ArrayWheelAdapter;
import at.photosniper.R;

/**
 * Fragment that handles the display of the cable selector and navigates users to the store.
 *
 * @author scottmellors
 * @since 2.3
 */
public class CableSelectorFragment extends TriggertrapFragment {

    private static final String CABLE_CB1 = "CB1";
    private static final String CABLE_DC0 = "DC0";
    private static final String CABLE_DC1 = "DC1";
    private static final String CABLE_DC2 = "DC2";
    private static final String CABLE_E3 = "E3";
    private static final String CABLE_L1 = "L1";
    private static final String CABLE_N3 = "N3";
    private static final String CABLE_NX = "NX";
    private static final String CABLE_R9 = "R9";
    private static final String CABLE_S1 = "S1";
    private static final String CABLE_S2 = "S2";
    private static final String CABLE_UC1 = "UC1";

    private final HashMap<String, LinkedHashMap<String, String>> mCableChooserData = new HashMap<>();
    private Button mBuyCableBtn;
    private String[] mCurrentCameraArray;
    private ImageView mCableImageView;

    private final HashMap<String, String> mStoreLinks = new HashMap<>();

    private static SortedMap<String, String> listFromJsonSorted(JSONObject json) {
        if (json == null)
            return null;
        SortedMap<String, String> map = new TreeMap<>();
        Iterator<String> i = json.keys();
        while (i.hasNext()) {
            try {
                String key = i.next();
                String j = json.getString(key);
                map.put(key, j);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.cable_chooser, container, false);

        final String[] manufacturers = getResources().getStringArray(R.array.tt_camera_manufacturers);

        mCableImageView = (ImageView) rootView.findViewById(R.id.cableImage);

        JSONObject obj;
        try {
            obj = new JSONObject(loadJSONFromAsset("StoreLinks.json"));

            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                while (keys.hasNext()) {
                    String cable = keys.next();
                    mStoreLinks.put(cable, obj.getString(cable));
                    keys.remove();
                }
            }

            obj = new JSONObject(loadJSONFromAsset("CameraChooser.json"));

            for (String manufacturer : manufacturers) {

                JSONObject tempObj = obj.getJSONObject(manufacturer);
                LinkedHashMap<String, String> temp = new LinkedHashMap<>();

                SortedMap<String, String> map = listFromJsonSorted(tempObj);

                map.entrySet();

                for (Object entry : map.entrySet()) {
                    Map.Entry<String, String> tempEntry = (Map.Entry<String, String>) entry;
                    String key = tempEntry.getKey();
                    String value = tempEntry.getValue();
                    temp.put(key, value);
                }

                mCableChooserData.put(manufacturer, temp);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        mBuyCableBtn = (Button) rootView.findViewById(R.id.buy_cable_btn);

        final AbstractWheel cameraManufacturers = (AbstractWheel) rootView.findViewById(R.id.manufacturerWheel);
        final AbstractWheel cameraModelWheel = (AbstractWheel) rootView.findViewById(R.id.cameraWheel);

        cameraManufacturers.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(AbstractWheel wheel) {
                ArrayWheelAdapter<String> cameraAdapter = new ArrayWheelAdapter<>(getActivity(), new String[]{"- - -"});
                cameraAdapter.setItemResource(R.layout.wheel_text_stretch);
                cameraAdapter.setItemTextResource();
                cameraModelWheel.setViewAdapter(cameraAdapter);
                cameraModelWheel.setCyclic();
            }

            @Override
            public void onScrollingFinished(AbstractWheel wheel) {

                mCurrentCameraArray = getCamerasFor(cameraManufacturers.getCurrentItem());

                ArrayWheelAdapter<String> cameraAdapter = new ArrayWheelAdapter<>(getActivity(), mCurrentCameraArray);
                cameraAdapter.setItemResource(R.layout.wheel_text_stretch);
                cameraAdapter.setItemTextResource();
                cameraModelWheel.setViewAdapter(cameraAdapter);
                cameraModelWheel.setCyclic();
                cameraModelWheel.setCurrentItem(0);

                updateUIForCamera(cameraManufacturers.getCurrentItem(), 0);
            }
        });

        cameraModelWheel.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(AbstractWheel wheel) {
                //hide camera
            }

            @Override
            public void onScrollingFinished(AbstractWheel wheel) {
                //set image
                updateUIForCamera(cameraManufacturers.getCurrentItem(), cameraModelWheel.getCurrentItem());
            }
        });

        ArrayWheelAdapter<String> manufacturerAdapter = new ArrayWheelAdapter<>(getActivity(), manufacturers);
        manufacturerAdapter.setItemResource(R.layout.wheel_text_stretch);
        manufacturerAdapter.setItemTextResource();
        cameraManufacturers.setViewAdapter(manufacturerAdapter);
        cameraManufacturers.setCyclic();


        ArrayWheelAdapter<String> cameraAdapter = new ArrayWheelAdapter<>(getActivity(), getCamerasFor(cameraManufacturers.getCurrentItem()));
        cameraAdapter.setItemResource(R.layout.wheel_text_stretch);
        cameraAdapter.setItemTextResource();
        cameraModelWheel.setViewAdapter(cameraAdapter);
        cameraModelWheel.setCyclic();
        //Set initial state
        mCurrentCameraArray = getCamerasFor(0);
        updateUIForCamera(0, 0);

        return rootView;
    }

    private String[] getCamerasFor(int manufacturer) {

        final String[] manufacturers = getResources().getStringArray(R.array.tt_camera_manufacturers);

        HashMap<String, String> tempMap = mCableChooserData.get(manufacturers[manufacturer]);

        return tempMap.keySet().toArray(new String[tempMap.keySet().size()]);
    }

    private void updateUIForCamera(int manufacturer, int cameraModel) {
        final String[] manufacturers = getResources().getStringArray(R.array.tt_camera_manufacturers);
        HashMap<String, String> tempMap = mCableChooserData.get(manufacturers[manufacturer]);
        String cableType = tempMap.get(mCurrentCameraArray[cameraModel]);

        if (cableType != null) {
            switch (cableType) {
                case CABLE_CB1:
                    setupButton(R.string.buy_cable_cb1, CABLE_CB1, R.drawable.cable_cb1);
                    break;
                case CABLE_DC0:
                    setupButton(R.string.buy_cable_dc0, CABLE_DC0, R.drawable.cable_dc0);
                    break;
                case CABLE_DC1:
                    setupButton(R.string.buy_cable_dc1, CABLE_DC1, R.drawable.cable_dc1);
                    break;
                case CABLE_DC2:
                    setupButton(R.string.buy_cable_dc2, CABLE_DC2, R.drawable.cable_dc2);
                    break;
                case CABLE_E3:
                    setupButton(R.string.buy_cable_e3, CABLE_E3, R.drawable.cable_e3);
                    break;
                case CABLE_L1:
                    setupButton(R.string.buy_cable_l1, CABLE_L1, R.drawable.cable_l1);
                    break;
                case CABLE_N3:
                    setupButton(R.string.buy_cable_n3, CABLE_N3, R.drawable.cable_n3);
                    break;
                case CABLE_NX:
                    setupButton(R.string.buy_cable_nx, CABLE_NX, R.drawable.cable_nx);
                    break;
                case CABLE_R9:
                    setupButton(R.string.buy_cable_r9, CABLE_R9, R.drawable.cable_r9);
                    break;
                case CABLE_S1:
                    setupButton(R.string.buy_cable_s1, CABLE_S1, R.drawable.cable_s1);
                    break;
                case CABLE_S2:
                    setupButton(R.string.buy_cable_s2, CABLE_S2, R.drawable.cable_s2);
                    break;
                case CABLE_UC1:
                    setupButton(R.string.buy_cable_uc1, CABLE_UC1, R.drawable.cable_uc1);
                    break;
            }
        }
    }

    private void setupButton(int stringResource, final String storeLink, int drawable) {
        mBuyCableBtn.setText(getResources().getString(stringResource));
        mCableImageView.setImageDrawable(getResources().getDrawable(drawable));
        mBuyCableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(mStoreLinks.get(storeLink)));
                startActivity(i);
            }
        });
    }

    private String loadJSONFromAsset(String fileName) {
        String json;
        try {
            InputStream is = getActivity().getAssets().open(fileName);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }
}
