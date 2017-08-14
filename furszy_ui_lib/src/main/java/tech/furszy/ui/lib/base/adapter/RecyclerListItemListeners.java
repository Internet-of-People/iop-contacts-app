package tech.furszy.ui.lib.base.adapter;

/**
 * Created by francisco on 25/08/15.
 */
public interface RecyclerListItemListeners<M> {

    /**
     * onItem click listener event
     *
     * @param data
     * @param position
     */
    void onItemClickListener(M data, int position);

    /**
     * On Long item Click Listener
     *
     * @param data
     * @param position
     */
    void onLongItemClickListener(M data, int position);

}
