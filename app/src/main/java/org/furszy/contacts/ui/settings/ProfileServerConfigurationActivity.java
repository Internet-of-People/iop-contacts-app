package org.furszy.contacts.ui.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.furszy.contacts.BaseActivity;
import org.furszy.contacts.R;
import org.furszy.contacts.contacts.ProfileHolder;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.ProfileServerConfiguration;
import org.libertaria.world.profile_server.model.ProfServerData;

import java.util.ArrayList;
import java.util.List;

public class ProfileServerConfigurationActivity extends BaseActivity {

    private Spinner serverList;
    private ProfileServerConfiguration profileServerConfiguration;
    private List<ProfServerData> registeredServers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_server_configuration);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        profileServerConfiguration = profilesModule.serverConfiguration();
        registeredServers = profileServerConfiguration.getRegisteredServers();
        serverList = (Spinner) findViewById(R.id.selectServerSpinner);
        serverList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                profileServerConfiguration.selectServer(registeredServers.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                serverList.setSelection(registeredServers.indexOf(profileServerConfiguration.getSelectedProfileServer()));
            }

        });
        ArrayAdapter<ProfServerData> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, profileServerConfiguration.getRegisteredServers());
        serverList.setAdapter(adapter); // this will set list of values to spinner
        serverList.setSelection(registeredServers.indexOf(profileServerConfiguration.getSelectedProfileServer()));//set selected value in spinner
    }

}
