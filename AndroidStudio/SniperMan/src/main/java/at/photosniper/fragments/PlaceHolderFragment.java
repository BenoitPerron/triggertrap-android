package at.photosniper.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import at.photosniper.R;


public class PlaceHolderFragment extends TriggertrapFragment {

    public PlaceHolderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.place_holder, container, false);
        return rootView;
    }
}
