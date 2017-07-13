package com.example.furszy.contactsapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.BaseDrawerActivity;
import com.example.furszy.contactsapp.App;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.StartActivity;
import com.example.furszy.contactsapp.ui.home.contacts.ContactsFragment;
import com.example.furszy.contactsapp.ui.home.requests.RequestsFragment;
import com.example.furszy.contactsapp.ui.send_request.SendRequestActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by furszy on 6/20/17.
 */

public class HomeActivity extends BaseDrawerActivity {

    public static final String INIT_REQUESTS = "in_req";
    private View root;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab_add;
    private ViewPagerAdapter adapter;

    private boolean initInRequest;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        if(!App.getInstance().createProfSerConfig().isIdentityCreated()){
            Intent intent = new Intent(this, StartActivity.class);
            startActivity(intent);
            finish();
        }else {
            root = getLayoutInflater().inflate(R.layout.home_main, container);
            setTitle("IoP Connections");

            if (getIntent()!=null){
                if (getIntent().hasExtra(INIT_REQUESTS)){
                    initInRequest = true;
                }
            }

            viewPager = (ViewPager) root.findViewById(R.id.viewpager);
            setupViewPager(viewPager);

            tabLayout = (TabLayout) root.findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);
            fab_add = (FloatingActionButton) root.findViewById(R.id.fab_add);
            fab_add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(v.getContext(), SendRequestActivity.class));
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(0);
        if (initInRequest){
            viewPager.setCurrentItem(1);
            initInRequest = false;
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ContactsFragment(), "Contacts");
        adapter.addFragment(new RequestsFragment(), "Requests");
        viewPager.setAdapter(adapter);
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    public void refreshContacts(){
        final Fragment fragment = adapter.getItem(0);
        if (fragment!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ContactsFragment)fragment).refresh();
                }
            });

        }
    }
}
