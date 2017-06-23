package com.example.furszy.contactsapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.BaseDrawerActivity;
import com.example.furszy.contactsapp.App;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.StartActivity;
import com.example.furszy.contactsapp.ui.home.contacts.ContactsFragment;
import com.example.furszy.contactsapp.ui.home.requests.RequestsFragment;
import com.example.furszy.contactsapp.ui.new_contact.NewContactActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by furszy on 6/20/17.
 */

public class HomeActivity extends BaseDrawerActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private FloatingActionButton fab_add;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        if(!App.getInstance().createProfSerConfig().isIdentityCreated()){
            Intent intent = new Intent(this, StartActivity.class);
            startActivity(intent);
            finish();
        }else {
            getLayoutInflater().inflate(R.layout.home_main, container);
            setTitle("IoP Connections");

            viewPager = (ViewPager) findViewById(R.id.viewpager);
            setupViewPager(viewPager);

            tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);
            fab_add = (FloatingActionButton) findViewById(R.id.fab_add);
            fab_add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(v.getContext(), NewContactActivity.class));
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(0);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ContactsFragment(), "Contacts");
        adapter.addFragment(new RequestsFragment(), "Requests");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
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
}
