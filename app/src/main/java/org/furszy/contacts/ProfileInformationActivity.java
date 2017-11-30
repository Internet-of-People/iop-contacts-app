package org.furszy.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.libertaria.world.crypto.CryptoBytes;
import org.libertaria.world.profile_server.CantConnectException;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.engine.futures.BaseMsgFuture;
import org.libertaria.world.profile_server.engine.futures.MsgListenerFuture;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.hdodenhof.circleimageview.CircleImageView;
import iop.org.iop_sdk_android.core.service.device_state.LocationUtil;
import world.libertaria.shared.library.util.OpenApplicationsUtil;

import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_PAIR_DISCONNECTED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_PROFILE_UPDATED_CONSTANT;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;
import static world.libertaria.shared.library.services.chat.ChatIntentsConstants.ACTION_OPEN_CHAT_APP;
import static world.libertaria.shared.library.services.chat.ChatIntentsConstants.EXTRA_INTENT_LOCAL_PROFILE;
import static world.libertaria.shared.library.services.chat.ChatIntentsConstants.EXTRA_INTENT_REMOTE_PROFILE;

/**
 * Created by furszy on 5/27/17.
 */
public class ProfileInformationActivity extends BaseActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger(ProfileInformationActivity.class);

    private static final String TAG = "ProfInfoActivity";

    public static final String IS_MY_PROFILE = "extra_is_my_profile";

    public static final String INTENT_EXTRA_PROF_SERVER_ID = "prof_ser_id";
    public static final String INTENT_EXTRA_SEARCH = "prof_search";
    private static final int OPTIONS_DELETE = 1;

    ProfileInformation profileInformation;

    private View root;
    private CircleImageView imgProfile;
    private TextView txt_name;
    private TextView locationText;
    private Button btn_action;
    private ProgressBar progress_bar;
    private LinearLayout userApps;

    private TextView txt_chat, disconnected_message;

    private ExecutorService executor;
    private AtomicBoolean flag = new AtomicBoolean(false);

    private boolean isMyProfile;
    private boolean searchForProfile = false;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_PROFILE_UPDATED_CONSTANT)) {
                if (isMyProfile) {
                    profileInformation = profilesModule.getProfile(selectedProfPubKey);
                    loadProfileData();
                }
            } else if (action.equals(ACTION_ON_PAIR_DISCONNECTED)) {
                if (profileInformation != null) {
                    String pubKey = profileInformation.getHexPublicKey();
                    profileInformation = profilesModule.getKnownProfile(selectedProfPubKey, pubKey);
                    loadProfileData();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getIntent() != null && getIntent().hasExtra(IS_MY_PROFILE)) {
            getMenuInflater().inflate(R.menu.my_profile_menu, menu);
        } else {
            MenuItem menuItem = menu.add(0, OPTIONS_DELETE, 0, R.string.delete_contact);
            return super.onCreateOptionsMenu(menu);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.editProfile:
                Intent myIntent = new Intent(this, ProfileActivity.class);
                startActivity(myIntent);
                return true;
            case OPTIONS_DELETE:
                tappedDeleteButton();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2998ff")));
        }
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(ACTION_ON_PAIR_DISCONNECTED));
        root = getLayoutInflater().inflate(R.layout.profile_information_main, container);
        imgProfile = (CircleImageView) root.findViewById(R.id.profile_image);
        txt_name = (TextView) root.findViewById(R.id.txt_name);
        locationText = (TextView) root.findViewById(R.id.txt_location);
        btn_action = (Button) root.findViewById(R.id.btn_action);
        progress_bar = (ProgressBar) root.findViewById(R.id.progress_bar);
        txt_chat = (TextView) root.findViewById(R.id.txt_chat);
        userApps = (LinearLayout) root.findViewById(R.id.userApps);
        txt_chat.setOnClickListener(this);
        disconnected_message = (TextView) root.findViewById(R.id.disconnected_message);
        disconnected_message.setVisibility(View.GONE);
        btn_action.setEnabled(false);
        btn_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tappedActionButton();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(INTENT_EXTRA_PROF_KEY)) {
                byte[] pubKey = extras.getByteArray(INTENT_EXTRA_PROF_KEY);
                profileInformation = profilesModule.getKnownProfile(selectedProfPubKey, CryptoBytes.toHexString(pubKey));
                // and schedule to try to update this profile information..
                searchForProfile = true;
            } else if (extras.containsKey(IS_MY_PROFILE)) {
                isMyProfile = true;
                profileInformation = profilesModule.getProfile(selectedProfPubKey);
            }
        }

        if (isMyProfile) {
            userApps.setVisibility(View.GONE);
        }

        if (profileInformation == null && !searchForProfile) {
            onBackPressed();
            return;
        }

        Address address = LocationUtil.getLastKnownAddress(getApplicationContext());
        String displayText = getApplicationContext().getString(R.string.my_location);

        if (address != null) {
            displayText = address.getSubAdminArea() + ", " + address.getCountryName();
        }
        locationText.setText(displayText);
    }

    private void tappedActionButton() {
        if (flag.compareAndSet(true, true)) {
            return;
        }
        flag.set(true);
        showLoading();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                MsgListenerFuture<ProfileInformation> future = new MsgListenerFuture<>();
                future.setListener(new BaseMsgFuture.Listener<ProfileInformation>() {
                    @Override
                    public void onAction(int messageId, ProfileInformation object) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                flag.set(false);
                                hideLoading();
                                Toast.makeText(ProfileInformationActivity.this, R.string.pairing_success, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onFail(int messageId, final int status, final String statusDetail) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                flag.set(false);
                                hideLoading();
                                String baseMsg = getResources().getString(R.string.pairing_fail);
                                Toast.makeText(ProfileInformationActivity.this, baseMsg + ": Remote profile not available", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                try {
                    pairingModule.requestPairingProfile(
                            selectedProfPubKey,
                            CryptoBytes.fromHexToBytes(profileInformation.getHexPublicKey()),
                            profileInformation.getName(),
                            profileInformation.getHomeHost(),
                            future
                    );
                } catch (Exception e) {
                    String baseMsg = getResources().getString(R.string.pairing_fail);
                    Toast.makeText(ProfileInformationActivity.this, baseMsg + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void tappedDeleteButton() {
        if (flag.compareAndSet(true, true)) {
            return;
        }
        flag.set(true);
        showLoading();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                MsgListenerFuture<Boolean> readyListener = new MsgListenerFuture<Boolean>();
                readyListener.setListener(new BaseMsgFuture.Listener<Boolean>() {

                    @Override
                    public void onAction(int messageId, Boolean object) {
                        flag.set(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoading();
                                Toast.makeText(ProfileInformationActivity.this, "User has been disconnected", Toast.LENGTH_LONG).show();
                                onBackPressed();
                            }
                        });
                    }

                    @Override
                    public void onFail(int messageId, int status, String statusDetail) {
                        flag.set(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoading();
                                Toast.makeText(ProfileInformationActivity.this, "Fail disconnecting", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                if (profileInformation.getPairStatus().equals(ProfileInformationImp.PairStatus.DISCONNECTED)) {
                    pairingModule.disconnectPairingProfile(selectedProfPubKey, profileInformation, false, readyListener);
                } else {
                    pairingModule.disconnectPairingProfile(selectedProfPubKey, profileInformation, true, readyListener);
                }
            }
        });
    }

    private void loadProfileData() {
        if (profileInformation != null) {
            if (profileInformation.getPairStatus().equals(ProfileInformationImp.PairStatus.DISCONNECTED)) {
                btn_action.setEnabled(true);
                btn_action.setText(R.string.send_request);
                btn_action.setBackgroundColor(getResources().getColor(R.color.bgBlue, null));
                btn_action.setTextColor(getResources().getColor(R.color.white, null));
                disconnected_message.setVisibility(View.VISIBLE);
            }
            txt_name.setText(profileInformation.getName());
            if (profileInformation.getImg() != null && profileInformation.getImg().length > 1) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(profileInformation.getImg(), 0, profileInformation.getImg().length);
                imgProfile.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMyProfile) {
            profileInformation = profilesModule.getProfile(selectedProfPubKey);
        }
        loadProfileData();
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        if (searchForProfile) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        MsgListenerFuture<ProfileInformation> msgListenerFuture = new MsgListenerFuture();
                        msgListenerFuture.setListener(new BaseMsgFuture.Listener<ProfileInformation>() {
                            @Override
                            public void onAction(int messageId, final ProfileInformation object) {
                                profileInformation = object;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadProfileData();
                                    }
                                });
                            }

                            @Override
                            public void onFail(int messageId, int status, String statusDetail) {
                                logger.info("Search profile on network fail, detail:" + statusDetail);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        logger.info("Search profile on network fail...");
                                        hideLoading();
                                        //onBackPressed();
                                    }
                                });
                            }
                        });
                        profilesModule.getProfileInformation(selectedProfPubKey, profileInformation.getHexPublicKey(), true, msgListenerFuture);
                    } catch (CantSendMessageException e) {
                        e.printStackTrace();
                    } catch (CantConnectException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        flag.set(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void showLoading() {
        progress_bar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progress_bar.setVisibility(View.GONE);
    }

    @Override
    public void onClick(final View v) {
        int id = v.getId();
        switch (id) {
            case R.id.txt_chat:
                if (isMyProfile) {
                    OpenApplicationsUtil.openChatContacts(getApplicationContext());
                }

                if (profileInformation.getPairStatus().equals(ProfileInformationImp.PairStatus.DISCONNECTED)) {
                    Toast.makeText(v.getContext(), "You need connect with " + profileInformation.getName() + " in order to send messages", Toast.LENGTH_SHORT).show();
                    return;
                }

                OpenApplicationsUtil.startNewChat(getApplicationContext(), profileInformation.getHexPublicKey());
                break;
            case R.id.pairingStatus:
                if (isMyProfile) {
                    OpenApplicationsUtil.openRequestsScreen(getApplicationContext());
                } else {
                    String toastMessage = profileInformation.getPairStatus().toString();
                    switch (profileInformation.getPairStatus()) {
                        case PAIRED:
                            toastMessage = "You are already paired with " + profileInformation.getName();
                            break;
                        case WAITING_FOR_MY_RESPONSE:
                            toastMessage = "Sending approval to " + profileInformation.getName();
                            //TODO: APPROVE
                            break;
                        case WAITING_FOR_RESPONSE:
                            toastMessage = "Waiting approval from " + profileInformation.getName();
                            break;
                        case BLOCKED:
                            toastMessage = profileInformation.getName() + " is blocked.";
                            break;
                        case DISCONNECTED:
                            toastMessage = profileInformation.getName() + " is disconnected.";
                            break;
                        case NOT_PAIRED:
                            //TODO: SEND PAIRING REQUEST
                            toastMessage = "Sending pairing request to " + profileInformation.getName();
                            break;
                    }
                    Toast.makeText(v.getContext(), toastMessage, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
