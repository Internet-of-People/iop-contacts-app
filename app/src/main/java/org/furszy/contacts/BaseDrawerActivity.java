package org.furszy.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.libertaria.world.profile_server.model.Profile;
import org.furszy.contacts.ui.home.HomeActivity;
import org.furszy.contacts.ui.my_qr.MyQrActivity;
import org.furszy.contacts.ui.settings.SettingsActivity;

import world.libertaria.shared.library.global.client.IntentBroadcastConstants;

import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_IOP_SERVICE_CONNECTED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_CHECK_IN_FAIL;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_PROFILE_DISCONNECTED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_PROFILE_UPDATED_CONSTANT;
import static org.furszy.contacts.App.INTENT_ACTION_PROFILE_CHECK_IN_FAIL;
import static org.furszy.contacts.App.INTENT_ACTION_PROFILE_CONNECTED;
import static org.furszy.contacts.App.INTENT_ACTION_PROFILE_DISCONNECTED;

/**
 * Created by Neoperol on 6/20/17.
 */

public class BaseDrawerActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private NavigationView navigationView;
    private FrameLayout frameLayout;
    private Toolbar toolbar;
    private DrawerLayout drawer;

    private View navHeader;
    //private CircleImageView imgProfile;
    private Profile myProfile;

    private byte[] cachedProfImage;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_PROFILE_UPDATED_CONSTANT)) {
                refreshProfile();
            } else if (action.equals(ACTION_IOP_SERVICE_CONNECTED)) {
                refreshProfile();
                if (profilesModule != null) {
                    if (!profilesModule.isProfileConnectedOrConnecting(selectedProfPubKey)) {
                        btnReload.setVisibility(View.VISIBLE);
                    }
                }
            } else if (action.equals(ACTION_ON_PROFILE_DISCONNECTED)) {
                showConnectionLoose();
            } else if (action.equals(ACTION_ON_CHECK_IN_FAIL)) {
                showConnectionLoose();
            } else if (action.equals(INTENT_ACTION_PROFILE_CONNECTED)) {
                hideConnectionLoose();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beforeCreate();
        setContentView(R.layout.activity_base_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        frameLayout = (FrameLayout) findViewById(R.id.content_frame);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navHeader = navigationView.getHeaderView(0);
        navHeader.setOnClickListener(this);

        //imgProfile = (CircleImageView) navHeader.findViewById(R.id.profile_image);

        //Layout reload
        btnReload = (LinearLayout) findViewById(R.id.btnReload);
        btnReload.setVisibility(LinearLayout.GONE);
        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // nothing yet
            }
        });

        onCreateView(savedInstanceState, frameLayout);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(IntentBroadcastConstants.ACTION_PROFILE_UPDATED_CONSTANT));
        registerReceiver(receiver, new IntentFilter(ACTION_IOP_SERVICE_CONNECTED));
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(INTENT_ACTION_PROFILE_DISCONNECTED));
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(INTENT_ACTION_PROFILE_CHECK_IN_FAIL));
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(INTENT_ACTION_PROFILE_CONNECTED));

        try {
            if (profilesModule != null) {
                if (!profilesModule.isProfileConnectedOrConnecting(selectedProfPubKey)) {
                    btnReload.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshProfile();
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            // nothing..
        }
        try {
            localBroadcastManager.unregisterReceiver(receiver);
        } catch (Exception e) {
            // nothing..
        }
    }

    private void refreshProfile() {

    }

    /**
     * Empty method to check some status before set the main layout of the activity
     */
    protected void beforeCreate() {

    }

    /**
     * Empty method to override.
     *
     * @param savedInstanceState
     */
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {

    }

    @Override
    public void onBackPressed() {
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //to prevent current item select over and over
        if (item.isChecked()) {
            drawer.closeDrawer(GravityCompat.START);
            return false;
        }

        if (id == R.id.nav_contact) {
            startActivity(new Intent(getApplicationContext(), HomeActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        } else if (id == R.id.nav_qr_code) {
            startActivity(new Intent(getApplicationContext(), MyQrActivity.class));
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void setNavigationMenuItemChecked(int pos) {
        navigationView.getMenu().getItem(pos).setChecked(true);
    }

    public void hideConnectionLoose() {
        btnReload.setVisibility(View.GONE);
    }

    public void showConnectionLoose() {
        btnReload.setVisibility(View.VISIBLE);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.container_profile) {
            Intent intent = new Intent(v.getContext(), ProfileInformationActivity.class);
            intent.putExtra(ProfileInformationActivity.IS_MY_PROFILE, true);
            startActivity(intent);
        }
    }
}
