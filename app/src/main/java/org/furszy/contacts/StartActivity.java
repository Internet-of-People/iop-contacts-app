package org.furszy.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.furszy.contacts.R;

/**
 * Created by Neoperol on 6/20/17.
 */

public class StartActivity extends BaseActivity {
    private Button buttonRestore;
    private Button buttonCreate;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getSupportActionBar().hide();
        getLayoutInflater().inflate(R.layout.start_activity, container);

        // Open Create Profile
        buttonCreate = (Button) findViewById(R.id.btnCreate);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), CreateProfileActivity.class);
                startActivity(myIntent);
                finish();
            }
        });

        // Open Restore Profile
        buttonRestore = (Button) findViewById(R.id.btnRestore);
        buttonRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), RestoreActivity.class);
                startActivity(myIntent);
                finish();
            }
        });
    }


}
