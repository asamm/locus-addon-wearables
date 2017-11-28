package com.asamm.locus.addon.wear.gui.trackrec.viewpager;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState;

import java.util.ArrayList;
import java.util.List;

public class TrackRecordPagerAdapter extends PagerAdapter implements TrackRecordingUpdatable{

	private static final int NUM_FAKE_SCREENS = 2;
	private static final int MAX_SCREENS = 3 + NUM_FAKE_SCREENS;
	private List<TrackRecordingUpdatable> screens = new ArrayList<>(MAX_SCREENS);

	public TrackRecordPagerAdapter(ViewGroup rootView) {
		screens.add(new BlankScreenController(rootView)); // first fake screen for cyclic scrolling
		screens.add(new MainScreenController(rootView));
		screens.add(new StatsScreenController(rootView, 1));
		screens.add(new BlankScreenController(rootView)); // last fake screen for cyclic scrolling
	}

	@Override
	public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {
		for (TrackRecordingUpdatable scr : screens) {
			scr.onTrackActivityStateChange(context, newState);
		}
	}

	@Override
	public void onNewTrackRecordingData(Activity context, TrackRecordingValue newData) {
		for (TrackRecordingUpdatable scr : screens) {
			scr.onNewTrackRecordingData(context, newData);
		}
	}

	@Override
	public ViewGroup getRootView() {
		return null; // formal implementation, adapter has no directly accessible root view
	}

	//view inflating..
	@Override
	public Object instantiateItem(ViewGroup collection, int position) {
		final ViewGroup layout = screens.get(position).getRootView();
		collection.addView(layout);
		return layout;
	}

	@Override
	public void destroyItem(ViewGroup collection, int position, Object view) {
		collection.removeView((View) view);
	}

	@Override
	public int getCount() {
		return screens.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}
}