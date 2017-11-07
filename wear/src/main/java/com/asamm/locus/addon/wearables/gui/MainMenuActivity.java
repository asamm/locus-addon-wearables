package com.asamm.locus.addon.wearables.gui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.asamm.locus.addon.wearables.DeviceCommunication;
import com.asamm.locus.addon.wearables.R;
import com.asamm.locus.addon.wearables.gui.custom.InfoPanel;
import com.assam.locus.addon.wearables.common.Const;
import com.assam.locus.addon.wearables.common.DataContainer;

import locus.api.utils.Logger;

/**
 * Created by menion on 11/08/15.
 * Asamm Software, s. r. o.
 */
public class MainMenuActivity extends CustomActivity {

    // tag for logger
    private static final String TAG = "MainMenu";

    @Override
    public void onStart() {
        super.onStart();

        // display loading system
        displayLoadingFragment();
    }

    // LOADING FRAGMENT

    /**
     * Display fragment with loading layout.
     */
    private void displayLoadingFragment() {
        // initialize fragment
        FragmentLoading fragment = new FragmentLoading();

        // display it
        displayFragment(fragment);
    }

    /**
     * Get visible loading fragment, otherwise 'null' if is not available.
     * @return loading fragment or 'null'
     */
    private FragmentLoading getLoadingFragment() {
        // get current attached fragment
        Fragment fragment = getDisplayedFragment();

        // test it and return if valid
        if (fragment != null && fragment instanceof FragmentLoading) {
            return (FragmentLoading) fragment;
        }

        // fragment is not attached
        return null;
    }

    // MAIN MENU FRAGMENT

    private void displayMainMenuFragment() {
        // initialize fragment
        FragmentMainMenu fragment = new FragmentMainMenu();

        // display it
        displayFragment(fragment);
    }

    // ABSTRACT FUNCTIONS

    @Override
    public void refreshLayout() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // get and refresh loading fragment
                FragmentLoading fragLoading = getLoadingFragment();
                if (fragLoading != null) {
                    fragLoading.doRefresh();
                    return;
                }

                Logger.logW(TAG, "refreshLayout(), " +
                        "no fragment to refresh");
            }
        });
    }

    /**************************************************/
    // LOADING FRAGMENT
    /**************************************************/

    /**
     * Basic loading fragment.
     */
    public static class FragmentLoading extends AFragmentBase {

        // info panel for data
        private InfoPanel mPanel;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // inflate layout
            View view = inflater.inflate(R.layout.layout_info_panel, container, false);

            // set panel
            mPanel = new InfoPanel((LinearLayout) view);

            // refresh layout
            doRefresh();

            // return inflated view
            return view;
        }

        /**
         * Refresh content of fragment based on situation.
         */
        void doRefresh() {
            // get connection to device
            DeviceCommunication devComm = act.getDeviceComm();
            Logger.logD(TAG, "FragmentLoading - doRefresh(), " +
                    "isApiConnected:" + devComm.isApiConnected() + ", " +
                    "isNodeConnected:" + devComm.isNodeConnected());

            // check connection to API
            if (!devComm.isApiConnected()) {
                Logger.logD(TAG, "doRefresh(), " +
                        "API is not yet connected");
                mPanel.displayProgress(
                        getString(R.string.init),
                        getString(R.string.init_connecting_to_device));
                return;
            }

            // check connection to node
            if (!devComm.isNodeConnected()) {
                Logger.logD(TAG, "doRefresh(), " +
                        "device is not yet connected");
                mPanel.displayError(R.string.init_node_not_connected);
                return;
            }

            // get and check container
            DataContainer container = devComm.getDataContainer();
            if (container.getLocusVersion() == null) {
                Logger.logD(TAG, "doRefresh(), " +
                        "Locus version not known");
                mPanel.displayProgress(
                        getString(R.string.init),
                        getString(R.string.init_contacting_locus));
                return;
            }

            // check Locus version
            if (container.getLocusVersion().getVersionCode() < Const.LOCUS_VERSION_CODE ||
                    container.getLocusInfo() == null) {
                Logger.logD(TAG, "doRefresh(), " +
                        "version of Locus is too old");
                mPanel.displayError(R.string.invalid_locus_version);
                return;
            }

            // check periodic updates
            if (!container.getLocusInfo().isRunning()) {
                Logger.logD(TAG, "doRefresh(), " +
                        "Locus is not running");
                mPanel.displayError(R.string.locus_not_running);
                return;
            }

            // check periodic updates
            if (!container.getLocusInfo().isPeriodicUpdatesEnabled()) {
                Logger.logD(TAG, "doRefresh(), " +
                        "periodic updates not enabled");
                mPanel.displayError(R.string.periodic_updates_not_enabled);
                return;
            }

            // check loaded data
            if (devComm.getLastUpdate() == null) {
                Logger.logD(TAG, "doRefresh(), " +
                        "last update not exists");
                mPanel.displayProgress(
                        getString(R.string.init),
                        getString(R.string.waiting_on_new_data));
                return;
            }

            // finally display main menu
            Logger.logD(TAG, "doRefresh(), " +
                    "finally display main menu");
            ((MainMenuActivity) act).displayMainMenuFragment();
        }
    }

    /**************************************************/
    // MAIN MENU FRAGMENT
    /**************************************************/

    /**
     * Basic fragment with main menu.
     */
    public static class FragmentMainMenu extends AFragmentBase implements View.OnClickListener {

        // reference to buttons
        private ImageButton ibMenu01;
        private ImageButton ibMenu02;
        private ImageButton ibMenu03;
        private ImageButton ibMenu04;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // inflate layout
            View view = inflater.inflate(R.layout.layout_main_menu, container, false);

            // get reference to buttons
            ibMenu01 = (ImageButton)
                    view.findViewById(R.id.button_main_menu_01);
            ibMenu01.setOnClickListener(this);
            ibMenu02 = (ImageButton)
                    view.findViewById(R.id.button_main_menu_02);
            ibMenu02.setOnClickListener(this);
            ibMenu03 = (ImageButton)
                    view.findViewById(R.id.button_main_menu_03);
            ibMenu03.setOnClickListener(this);
            ibMenu04 = (ImageButton)
                    view.findViewById(R.id.button_main_menu_04);
            ibMenu04.setOnClickListener(this);

            // return inflated view
            return view;
        }

        @Override
        public void onClick(View v) {
            if (v == ibMenu01) {
                Intent intent = new Intent(act, MapActivity.class);
                startActivity(intent);
            } else if (v == ibMenu02) {
                Intent intent = new Intent(act, TrackRecordActivity.class);
                startActivity(intent);
            } else if (v == ibMenu03) {
                // TODO: 17/09/15 not yet implemented
            } else if (v == ibMenu04) {
                // TODO: 17/09/15 not yet implemented
            }
        }
    }
}
