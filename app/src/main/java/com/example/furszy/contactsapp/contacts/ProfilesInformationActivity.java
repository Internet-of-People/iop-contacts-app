package com.example.furszy.contactsapp.contacts;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.furszy.contactsapp.App;
import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.ProfileInformationActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.FermatListItemListeners;

import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;

/**
 * Created by mati on 03/03/17.
 */

public class ProfilesInformationActivity extends BaseActivity {

    private static final String TAG = "ProfilesActivity";

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private ProfileAdapter adapter;
    private ProgressBar loading_bar;
    private TextView txt_empty;
    private View container_empty_screen;

    private List<ProfileInformation> profiles;

    private Handler handler = new Handler();

    private ModuleRedtooth module;

    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setTitle("Profiles");

        module = ((App)getApplication()).getAnRedtooth().getRedtooth();

        setContentView(R.layout.profiles_information_main);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_proposals);
        container_empty_screen = findViewById(R.id.container_empty_screen);
        loading_bar = (ProgressBar) findViewById(R.id.loading_bar);
        txt_empty = (TextView) findViewById(R.id.txt_empty);

        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ProfileAdapter(this, module,new FermatListItemListeners<ProfileInformation>() {
            @Override
            public void onItemClickListener(ProfileInformation data, int position) {
                Intent intent1 = new Intent(ProfilesInformationActivity.this, ProfileInformationActivity.class);
                intent1.putExtra(INTENT_EXTRA_PROF_KEY, data.getPublicKey());
                ProfilesInformationActivity.this.startActivity(intent1);
            }

            @Override
            public void onLongItemClickListener(ProfileInformation data, int position) {

            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void updateView() {
        if (!profiles.isEmpty()) {
            hideEmptyScreen();
        } else
            showEmptyScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (executor==null){
            executor = Executors.newSingleThreadExecutor();
        }
        executor.execute(loadProposals);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (executor!=null){
            executor.shutdownNow();
            executor = null;
        }
    }

    Runnable loadProposals = new Runnable() {
        @Override
        public void run() {
            boolean res = false;
            try {
                profiles = module.getKnownProfiles();
                res = true;
            } catch (Exception e){
                e.printStackTrace();
                res = false;
                Log.e(TAG,"CantGetProfileException: "+e.getMessage());
            }

            final boolean finalRes = res;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (finalRes) {
                        if (!profiles.isEmpty()) {
                            hideEmptyScreen();
                            adapter.changeDataSet(profiles);
                        } else {
                            hideEmptyScreen();
                            showEmptyScreen();
                            txt_empty.setText("No profiles yet..");
                            txt_empty.setTextColor(Color.BLACK);
                        }
                    }
                }
            });
        }
    };


    private void showEmptyScreen(){
//        if (container_empty_screen!=null)
//            AnimationUtils.fadeInView(container_empty_screen,300);

    }

    private void hideEmptyScreen(){
        loading_bar.setVisibility(View.GONE);
//        if (container_empty_screen!=null)
//            AnimationUtils.fadeOutView(container_empty_screen,300);
    }

}
