package com.example.furszy.contactsapp.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * BaseViewHolder Base Class
 *
 * @author Francisco Vásquez & Matias Furszyfer
 * @version 2.0
 */
public abstract class BaseViewHolder extends RecyclerView.ViewHolder {

    private int holderId = 0;
    private int holderType;
    private int holderLayoutRes;

    /**
     * Constructor
     *
     * @param itemView
     */
    @Deprecated
    protected BaseViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * @param itemView
     * @param holderType
     */
    protected BaseViewHolder(View itemView, int holderType) {
        super(itemView);
        this.holderType = holderType;
    }

    protected BaseViewHolder(View itemView, int holderId, int holderType) {
        super(itemView);
        this.holderId = holderId;
        this.holderType = holderType;
    }

    public BaseViewHolder(View itemView, int holderId, int holderType, int holderLayoutRes) {
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
