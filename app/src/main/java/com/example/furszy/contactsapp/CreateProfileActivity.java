package com.example.furszy.contactsapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.furszy.contactsapp.ui.home.HomeActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Neoperol on 6/20/17.
 */

public class CreateProfileActivity extends BaseActivity {
    private static final Logger log = LoggerFactory.getLogger(CreateProfileActivity.class);
    private Button buttonCreate;
    private EditText edit_name;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.create_profile_activity, container);
        edit_name = (EditText) findViewById(R.id.edit_name);
        setTitle("Create profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Open Create Profile
        buttonCreate = (Button) findViewById(R.id.btnCreate);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String name = edit_name.getText().toString();
                    if (name.length() > 0) {
                        //todo: make this connect non blocking.
                        anRedtooth.connect(anRedtooth.registerProfile(name));
                        Intent myIntent = new Intent(v.getContext(), HomeActivity.class);
                        startActivityForResult(myIntent, 0);
                    } else
                        Toast.makeText(v.getContext(), "Please write your profile name", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Registration fail,Please try again later", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    log.error("Profile registration fail",e);
                }
            }
        });

    }
}
