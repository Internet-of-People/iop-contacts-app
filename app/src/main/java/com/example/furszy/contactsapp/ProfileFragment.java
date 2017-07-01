package com.example.furszy.contactsapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.model.Profile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.hdodenhof.circleimageview.CircleImageView;
import static android.app.Activity.RESULT_OK;
import static org.fermat.redtooth.utils.StringUtils.cleanString;

/**
 * Created by mati on 16/04/17.
 */

public class ProfileFragment extends Fragment implements View.OnClickListener{


    private static final String TAG = "ProfileActivity";


    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;

    private static final int UPDATE_SCREEN_STATE = 1;
    private static final int DONE_SCREEN_STATE = 2;

    private static final int RESULT_LOAD_IMAGE = 100;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 500;


    private ModuleRedtooth module;

    // UI
    private View root;
    private CircleImageView imgProfile;
    private EditText txt_name;
    private Switch show_location;
    private Button btn_create;
    private ProgressBar progressBar;

    private Profile profile;
    byte[] profImgData;
    private boolean isUsernameCorrect;
    private AtomicBoolean lock = new AtomicBoolean(false);
    private boolean isRegistered;
    private int screenState;
    private ExecutorService executor;

    private BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(App.INTENT_ACTION_PROFILE_CONNECTED)){
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(),"Profile connected",Toast.LENGTH_LONG).show();
            }
        }
    };


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        checkPermissions();

        module = ((App)getActivity().getApplication()).anRedtooth.getRedtooth();

        getActivity().setTitle("Profile");

        root = inflater.inflate(R.layout.profile_main,container);

        isRegistered = module.isProfileRegistered();

        imgProfile = (CircleImageView) root.findViewById(R.id.profile_image);
        txt_name = (EditText) root.findViewById(R.id.txt_name);
        progressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        progressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
        progressBar.setVisibility(View.GONE);
        btn_create = (Button) root.findViewById(R.id.btn_create);
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        //Show Locaiton

        show_location = (Switch) root.findViewById(R.id.show_location);
        show_location.setChecked(false);
        show_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){

                }else{

                }

            }
        });

        btn_create.setOnClickListener(this);

        if (isRegistered){
            changeScreenState(DONE_SCREEN_STATE);
        }else {
            changeScreenState(UPDATE_SCREEN_STATE);
        }

        try {
            File imgFile = null;// module.getUserImageFile();
            if (imgFile!=null && imgFile.exists())
                Picasso.with(getActivity()).load(imgFile).into(imgProfile);
        }catch (Exception e){
            e.printStackTrace();
        }

        init();

        return root;
    }

    private void changeScreenState(int state){
        screenState = state;
        switch (state){
            case DONE_SCREEN_STATE:
                btn_create.setText("Back");
                break;
            case UPDATE_SCREEN_STATE:
                btn_create.setText("Save");
                break;
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        if (executor==null)
            executor = Executors.newSingleThreadExecutor();
        // init profile
        profile = module.getProfile();
        if (profile!=null) {
            txt_name.setText(profile.getName());
            if(profile.getImg()!=null && profile.getImg().length>0){
                imgProfile.setImageBitmap(BitmapFactory.decodeByteArray(profile.getImg(),0,profile.getImg().length));
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (executor!=null){
            executor.shutdownNow();
            executor = null;
        }
        try{
            ((BaseActivity) getActivity()).localBroadcastManager.unregisterReceiver(connectionReceiver);
        }catch (Exception e){
            // nothing..
        }
    }

    private void init(){

        txt_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString();
                if (name.length()>3 ){
                    isUsernameCorrect = true;
                    changeScreenState(UPDATE_SCREEN_STATE);
                }else {
                    isUsernameCorrect = false;
                    changeScreenState(DONE_SCREEN_STATE);
                }
            }
        });

    }

    private boolean validateMail(CharSequence email){
        String regExpn =
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                        +"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        +"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                        +"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

        Pattern pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);

        if(matcher.matches())
            return true;
        else
            return false;
    }

    private void registerUser(final String username){

        if (!lock.getAndSet(true)) {
            execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String pubKey = module.registerProfile(username, "contactApp",null,0,0,null);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
//                                    buildDialog();
                            }
                        });
                    } catch (final Exception e){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buildFailDialog(cleanString(e.getMessage()));
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }

                    lock.set(false);
                }
            });
        }
    }


    /**
     * Execute
     *
     * @param runnable
     */
    private void execute(Runnable runnable){
        executor.execute(runnable);
    }


    private void buildFailDialog(String message) {
        Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getActivity().getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);

            // scale image
            imgProfile.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 1024, 1024, false));
//            imgProfile.setImageBitmap(bitmap);

            if( ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.READ_CONTACTS)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL);

                }
            }

            // compress and do it array
            ByteArrayOutputStream out = null;
            try {
                out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                profImgData = out.toByteArray();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    // nothing
                }
            }

            if (isRegistered){
                btn_create.setText("Save");
                screenState = UPDATE_SCREEN_STATE;
            }
        }
    }

    private void updateProfile(){
        final String name = txt_name.getText().toString();
        if (!name.equals("")) {
            progressBar.setVisibility(View.VISIBLE);
            final boolean isIdentityCreated = module.isIdentityCreated();
            if (!isIdentityCreated){
                IntentFilter intentFilter = new IntentFilter(App.INTENT_ACTION_PROFILE_CONNECTED);
                ((BaseActivity)getActivity()).localBroadcastManager.registerReceiver(connectionReceiver,intentFilter);
            }
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    boolean res = false;
                    String detail = null;
                    try {
                        if (!isIdentityCreated) {
                            String pk = module.registerProfile(name, "contactApp", profImgData, 0, 0, null);
                            module.connect(pk);
                        } else {
                            MsgListenerFuture listenerFuture = new MsgListenerFuture();
                            module.updateProfile(
                                    name,
                                    profImgData,
                                    listenerFuture);
                            listenerFuture.get();
                        }
                        res = true;
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        detail = "Cant update profile, send report please";
                    }
                    final String finalDetail = detail;
                    final boolean finalRes = res;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!finalRes) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getActivity(), finalDetail, Toast.LENGTH_LONG).show();
                            } else {
                                progressBar.setVisibility(View.VISIBLE);
                                Toast.makeText(getActivity(), "Saved", Toast.LENGTH_LONG).show();
                                getActivity().onBackPressed();
                            }
                        }
                    });

                }
            });
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_create){
            switch (screenState){
                case DONE_SCREEN_STATE:
                    getActivity().onBackPressed();
                    break;
                case UPDATE_SCREEN_STATE:
                    updateProfile();
                    break;
            }
        }
    }


    private void checkPermissions() {
        // Assume thisActivity is the current activity
        if (Build.VERSION.SDK_INT > 22) {

            int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.READ_CONTACTS)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getActivity(), "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


}

