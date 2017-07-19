package org.furszy.contacts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.furszy.contacts.R;
import org.furszy.contacts.ui.home.HomeActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;
import iop.org.iop_sdk_android.core.profile_server.BigImageException;

/**
 * Created by Neoperol on 6/20/17.
 */

public class CreateProfileActivity extends BaseActivity {
    private static final Logger log = LoggerFactory.getLogger(CreateProfileActivity.class);
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;
    private static final int RESULT_LOAD_IMAGE = 100;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 500;

    private View root;
    private Button buttonCreate;
    private EditText edit_name;
    private CircleImageView img_profile_image;
    private byte[] profImgData;
    private ExecutorService executor;
    private final int destWidth = 400;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.create_profile_activity, container);
        edit_name = (EditText) root.findViewById(R.id.edit_name);
        edit_name.setFilters(new InputFilter[]{filter,new InputFilter.LengthFilter(14)});
        img_profile_image = (CircleImageView) root.findViewById(R.id.img_profile_image);
        buttonCreate = (Button) root.findViewById(R.id.btnCreate);
        setTitle("Create profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        img_profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                try {
                    final String name = edit_name.getText().toString();
                    if (name.length() > 0) {
                        if (executor==null) executor = Executors.newSingleThreadExecutor();
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //todo: make this connect non blocking.
                                    anRedtooth.connect(anRedtooth.registerProfile(name, profImgData));
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent myIntent = new Intent(v.getContext(), HomeActivity.class);
                                            startActivity(myIntent);
                                            finish();
                                        }
                                    });
                                } catch (final BigImageException e){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(v.getContext(), "Registration fail, big image.", Toast.LENGTH_LONG).show();
                                            e.printStackTrace();
                                            log.error("Profile registration fail",e);
                                        }
                                    });
                                }catch (final Exception e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(v.getContext(), "Registration fail,Please try again later", Toast.LENGTH_LONG).show();
                                            e.printStackTrace();
                                            log.error("Profile registration fail",e);
                                        }
                                    });
                                }
                            }
                        });
                    } else
                        Toast.makeText(v.getContext(), "Please write your profile name", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Registration fail,Please try again later", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    log.error("Profile registration fail",e);
                }
            }
        });

        checkPermissions();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (executor!=null){
            executor.shutdown();
            executor = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = this.getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            if (bitmap == null) {
                return;
            }

            ByteArrayOutputStream out = null;
            int origWidth = bitmap.getWidth();
            int origHeight = bitmap.getHeight();
            int outWidth = 0;
            int outHeight = 0;

            try {
                out = new ByteArrayOutputStream();

                if(origWidth > destWidth){
                    // picture is wider than we want it, we calculate its target height
                    if(origWidth > origHeight){
                        outWidth = destWidth;
                        outHeight = (origHeight * destWidth) / origWidth;
                    } else {
                        outHeight = destWidth;
                        outWidth = (origWidth * destWidth) / origHeight;
                    }
                    // we create an scaled bitmap so it reduces the image, not just trim it
                    Bitmap b2 = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
                    out = new ByteArrayOutputStream();
                    bitmap = b2;
                    b2.compress(Bitmap.CompressFormat.JPEG,70 , out);
                } else {
                    out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
                }

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
            // scale image
            final Bitmap finalBitmap = bitmap;
            img_profile_image.setImageBitmap(bitmap);
//            imgProfile.setImageBitmap(bitmap);

            if( ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL);

                }
            }

            // compress and do it array
            //ByteArrayOutputStream out = null;

        }
    }


    private void checkPermissions() {
        // Assume thisActivity is the current activity
        if (Build.VERSION.SDK_INT > 22) {

            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Log.i("CreateProfileActivity", "no hay permiso");
                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
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
            case RESULT_LOAD_IMAGE: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
