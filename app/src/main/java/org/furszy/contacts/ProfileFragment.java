package org.furszy.contacts;

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
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import org.furszy.contacts.App;
import org.furszy.contacts.R;
import com.squareup.picasso.Picasso;

import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.model.Profile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;

/**
 * Created by mati on 16/04/17.
 */

public class ProfileFragment extends Fragment implements View.OnClickListener{


    private static final String TAG = "ProfileActivity";

    private final int destWidth = 400;

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
    private ProgressBar loading_img;

    private Profile profile;
    byte[] profImgData;
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

        getActivity().setTitle("Edit Profile");

        root = inflater.inflate(R.layout.profile_main,container);

        isRegistered = module.isProfileRegistered();

        imgProfile = (CircleImageView) root.findViewById(R.id.profile_image);
        txt_name = (EditText) root.findViewById(R.id.txt_name);
        txt_name.setFilters(new InputFilter[]{filter,new InputFilter.LengthFilter(14)});
        progressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        loading_img = (ProgressBar) root.findViewById(R.id.loading_img);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
        loading_img.getIndeterminateDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
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
        if (module!=null) {
            profile = module.getProfile();
            if (profile != null) {
                txt_name.setText(profile.getName());
                if (profile.getImg() != null && profile.getImg().length > 0 && profImgData == null) {
                    imgProfile.setImageBitmap(BitmapFactory.decodeByteArray(profile.getImg(), 0, profile.getImg().length));
                }
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
                    changeScreenState(UPDATE_SCREEN_STATE);
                }else {
                    changeScreenState(DONE_SCREEN_STATE);
                }
            }
        });
    }

    /**
     * Execute
     *
     * @param runnable
     */
    private void execute(Runnable runnable){
        if (executor==null)
            executor = Executors.newSingleThreadExecutor();
        executor.execute(runnable);
    }


    private void buildFailDialog(String message) {
        Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {

            loading_img.setVisibility(View.VISIBLE);

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getActivity().getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            final String picturePath = cursor.getString(columnIndex);
            cursor.close();

            // scale image
            //imgProfile.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 1024, 1024, false));

            if( ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

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

            execute(new Runnable() {
                @Override
                public void run() {
                    //BitmapFactory.Options options = new BitmapFactory.Options();
                    //options.inSampleSize = 4;
                    //Bitmap bitmap = compressImageFileIntoBitmap(new File(picturePath));
                    Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
                    Log.i(TAG, "Original Width " + bitmap.getWidth());
                    Log.i(TAG, "Original Height " + bitmap.getHeight());
                    // compress and do it array
                    ByteArrayOutputStream out = null;
                    int origWidth = bitmap.getWidth();
                    int origHeight = bitmap.getHeight();
                    int outWidth = 0;
                    int outHeight = 0;
                    try {
                        if(origWidth > destWidth){
                            // picture is wider than we want it, we calculate its target height
                            if(origWidth > origHeight){
                                outWidth = destWidth;
                                outHeight = (origHeight * destWidth) / origWidth;
                            } else {
                                outHeight = destWidth;
                                outWidth = (origWidth * destWidth) / origHeight;
                            }
                            //int destHeight = origHeight/( origWidth / destWidth ) ;
                            Log.i(TAG, "outHeight " + outHeight);
                            Log.i(TAG, "outWidth " + outWidth);
                            // we create an scaled bitmap so it reduces the image, not just trim it
                            Bitmap b2 = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
                            out = new ByteArrayOutputStream();
                            bitmap = b2;
                            b2.compress(Bitmap.CompressFormat.JPEG,70 , out);
                            Log.i(TAG, "Scale Width " + b2.getWidth());
                            Log.i(TAG, "Scale Height " + b2.getHeight());
                        } else {
                            out = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
                        }
//                      out = new ByteArrayOutputStream();
//                      bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
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


                    final Bitmap finalBitmap = bitmap;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("ProfileFragment","setting bitmap profile");
                            //imgProfile.setImageBitmap(Bitmap.createScaledBitmap(finalBitmap, 1024, 1024, false));
                            imgProfile.setImageBitmap(finalBitmap);
                            loading_img.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });


            if (isRegistered){
                btn_create.setText("Save");
                screenState = UPDATE_SCREEN_STATE;
            }
        }
    }

    private Bitmap compressImageArrayIntoBitmap(byte[] array){
        try {
            File file = new File(getActivity().getCacheDir(),"compressed_image");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(array);
            fileOutputStream.close();
            return compressImageFileIntoBitmap(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap compressImageFileIntoBitmap(File pictureFile) {
        Bitmap bitmap = null;
        try {
            bitmap = new Compressor(getActivity())
                    .setMaxWidth(441)
                    .setMaxHeight(315)
                    .setQuality(75)
                    .setCompressFormat(Bitmap.CompressFormat.PNG)
                    .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES).getAbsolutePath())
                    .compressToBitmap(pictureFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
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
            execute(new Runnable() {
                @Override
                public void run() {
                    boolean res = false;
                    String detail = null;
                    try {
                        MsgListenerFuture<Boolean> listenerFuture = new MsgListenerFuture();
                        module.updateProfile(
                                name,
                                profImgData,
                                listenerFuture);
                        listenerFuture.get();
                        res = listenerFuture.getStatusDetail()==null;
                        if (!res){
                            detail = "Fail, error: "+listenerFuture.getStatusDetail();
                        }
                    } catch (Exception e) {
                        Log.e(TAG," exception updating the profile\n" +e.getMessage());
                        detail = "Cant update profile, send report please";
                    }
                    final String finalDetail = detail;
                    final boolean finalRes = res;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity()!=null) {
                                if (!finalRes) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getActivity(), finalDetail, Toast.LENGTH_LONG).show();
                                } else {
                                    progressBar.setVisibility(View.VISIBLE);
                                    Toast.makeText(getActivity(), "Saved", Toast.LENGTH_LONG).show();
                                    getActivity().onBackPressed();
                                }
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
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

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
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {

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
    public InputFilter filter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; ++i)
            {
                if (!Pattern.compile("[ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890]*").matcher(String.valueOf(source.charAt(i))).matches())
                {
                    return "";
                }
            }

            return null;
        }
    };


}

