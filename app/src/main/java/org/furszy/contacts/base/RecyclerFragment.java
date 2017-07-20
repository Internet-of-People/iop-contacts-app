package org.furszy.contacts.base;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.furszy.contacts.R;
import org.furszy.contacts.adapter.BaseAdapter;
import org.furszy.contacts.adapter.BaseViewHolder;
import org.furszy.contacts.ui.home.contacts.ContactsFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by furszy on 6/20/17.
 */

public abstract class RecyclerFragment<T> extends BaseAppFragment {

    private static final Logger log = LoggerFactory.getLogger(ContactsFragment.class);

    private View root;
    protected RecyclerView.LayoutManager layoutManager;
    protected RecyclerView recycler;
    protected SwipeRefreshLayout swipeRefreshLayout;
    private View container_empty_screen;
    private TextView txt_empty;
    private ImageView img_empty_view;

    protected BaseAdapter adapter;
    protected List<T> list;
    protected ExecutorService executor;

    protected LocalBroadcastManager localBroadcastManager;

    private String emptyText;

    public RecyclerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        root = inflater.inflate(R.layout.contacts_fragment, container, false);
        recycler = (RecyclerView) root.findViewById(R.id.recycler_contacts);
        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefresh);
        container_empty_screen = root.findViewById(R.id.container_empty_screen);
        txt_empty = (TextView) root.findViewById(R.id.txt_empty);
        img_empty_view = (ImageView) root.findViewById(R.id.img_empty_view);
        recycler.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recycler.setLayoutManager(layoutManager);
        adapter = initAdapter();
        if (adapter==null) throw new IllegalStateException("Base adapter cannot be null");
        recycler.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        load();
                    }
                }
        );
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executor==null){
            executor = Executors.newSingleThreadExecutor();
        }
        load();
    }

    /**
     * Method to override
     */
    private void load() {
        swipeRefreshLayout.setRefreshing(true);
        executor.execute(loadRunnable);
    }

    public void refresh(){
        load();
    }


    @Override
    public void onStop() {
        super.onStop();
        if (executor!=null){
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     *
     * @return list of items
     */
    protected abstract List<T> onLoading();

    /**
     *
     * @return the main adapter
     */
    protected abstract BaseAdapter<T,? extends BaseViewHolder> initAdapter();

    protected Runnable loadRunnable = new Runnable() {
        @Override
        public void run() {
            boolean res = false;
            try {
                list = onLoading();
                res = true;
            } catch (Exception e){
                e.printStackTrace();
                res = false;
                log.info("cantLoadListException: "+e.getMessage());
            }
            final boolean finalRes = res;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(false);
                    if (finalRes) {
                        adapter.changeDataSet(list);
                        if (list!=null && !list.isEmpty()) {
                            hideEmptyScreen();
                        } else {
                            showEmptyScreen();
                            txt_empty.setText(emptyText);
                            txt_empty.setTextColor(Color.BLACK);
                        }
                    }
                }
            });
        }
    };

    protected void setEmptyText(String text){
        this.emptyText = text;
        if (txt_empty!=null){
            txt_empty.setText(emptyText);
        }
    }

    protected void setEmptyTextColor(int color){
        if (txt_empty!=null){
            txt_empty.setTextColor(color);
        }
    }

    public void setEmptyView(int imgRes) {
        if (img_empty_view!=null){
            img_empty_view.setImageResource(imgRes);
        }
    }


    private void showEmptyScreen(){
//        if (container_empty_screen!=null)
//            AnimationUtils.fadeInView(container_empty_screen,300);
        container_empty_screen.setVisibility(View.VISIBLE);

    }

    private void hideEmptyScreen(){
        container_empty_screen.setVisibility(View.GONE);
//        if (container_empty_screen!=null)
//            AnimationUtils.fadeOutView(container_empty_screen,300);
    }

}
