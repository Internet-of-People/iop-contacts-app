package com.example.furszy.contactsapp.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * FermatViewHolder Base Class
 *
 * @author Francisco Vásquez & Matias Furszyfer
 * @version 2.0
 */
public abstract class FermatViewHolder extends RecyclerView.ViewHolder {

    private int holderId = 0;
    private int holderType;
    private int holderLayoutRes;

    /**
     * Constructor
     *
     * @param itemView
     */
    @Deprecated
    protected FermatViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * @param itemView
     * @param holderType
     */
    protected FermatViewHolder(View itemView, int holderType) {
        super(itemView);
        this.holderType = holderType;
    }

    protected FermatViewHolder(View itemView, int holderId, int holderType) {
        super(itemView);
        this.holderId = holderId;
        this.holderType = holderType;
    }

    public FermatViewHolder(View itemView, int holderId, int holderType, int holderLayoutRes) {
        super(itemView);
        this.holderId = holderId;
        this.holderType = holderType;
        this.holderLayoutRes = holderLayoutRes;
    }

    public int getHolderId() {
        return holderId;
    }

    public int getHolderType() {
        return holderType;
    }

    public int getHolderLayoutRes() {
        return holderLayoutRes;
    }

}
