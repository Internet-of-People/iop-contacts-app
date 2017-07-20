package org.furszy.contacts.requests;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.furszy.contacts.App;
import org.furszy.contacts.BaseActivity;
import org.furszy.contacts.R;
import org.furszy.contacts.adapter.FermatListItemListeners;

import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profiles_manager.PairingRequest;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mati on 03/03/17.
 */

public class PairingRequestsActivity extends BaseActivity {

    private static final String TAG = "PairingRequestsActivity";

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RequestsAdapter adapter;
    private ProgressBar loading_bar;
    private TextView txt_empty;
    private View container_empty_screen;

    private List<PairingRequest> requests;

    private Handler handler = new Handler();

    private ModuleRedtooth module;

    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setTitle("Pairing requests");

        module = ((App)getApplication()).getAnRedtooth().getRedtooth();

        setContentView(R.layout.profiles_information_main);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_proposals);
        container_empty_screen = findViewById(R.id.container_empty_screen);
        loading_bar = (ProgressBar) findViewById(R.id.loading_bar);
        txt_empty = (TextView) findViewById(R.id.txt_empty);
        txt_empty.setText("No requests yet");

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        adapter = new RequestsAdapter(this, module,new FermatListItemListeners<PairingRequest>() {
            @Override
            public void onItemClickListener(PairingRequest data, int position) {
                //Intent intent1 = new Intent(PairingRequestsActivity.this, ProfileInformationActivity.class);
                //intent1.putExtra(ProfileInformationActivity.INTENT_EXTRA_PROF_KEY, data.getPublicKey());
                //PairingRequestsActivity.this.startActivity(intent1);
            }

            @Override
            public void onLongItemClickListener(PairingRequest data, int position) {

            }
        });
        recyclerView.setAdapter(adapter);

//        container_empty_screen.findViewById(R.id.create_contract).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });

    }

    private void updateView() {
        if (!requests.isEmpty()) {
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
                requests = module.getPairingRequests();
                res = true;
            } catch (Exception e){
                e.printStackTrace();
                res = false;
                Log.e(TAG,"CanGetProfileException: "+e.getMessage());
            }

            final boolean finalRes = res;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (finalRes) {
                        if (!requests.isEmpty()) {
                            hideEmptyScreen();
                            adapter.changeDataSet(requests);
                        } else {
                            showEmptyScreen();
                            txt_empty.setText("No requests yet..");
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
