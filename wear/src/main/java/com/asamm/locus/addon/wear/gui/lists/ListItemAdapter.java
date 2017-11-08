package com.asamm.locus.addon.wear.gui.lists;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.R;

import java.util.List;

/**
 * Created by menion on 08/08/15.
 * Asamm Software, s. r. o.
 */
public abstract class ListItemAdapter<T> extends WearableListView.Adapter {

    // current context
    private final Context mCtx;
    // inflater for layout
    private final LayoutInflater mInflater;
    // items to display
    private final List<T> mItems;

    /**
     * Basic constructor.
     * @param ctx current context
     */
    public ListItemAdapter(Context ctx, List<T> items) {
        this.mCtx = ctx;
        this.mItems = items;
        this.mInflater = (LayoutInflater)
                ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public abstract void setItemView(T item, ListItemLayout layout);

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mInflater.
                inflate(R.layout.list_item_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        // retrieve base objects
        T item = mItems.get(position);
        ItemViewHolder itemHolder = (ItemViewHolder) holder;

        // set item view
        setItemView(item, itemHolder.getLayout());

        // replace list item's metadata
        holder.itemView.setTag(item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ItemViewHolder extends WearableListView.ViewHolder {

        // list item container
        private ListItemLayout mListItem;

        public ItemViewHolder(View itemView) {
            super(itemView);
            this.mListItem = (ListItemLayout) itemView;
        }

        /**
         * Get current attached layout.
         * @return layout
         */
        public ListItemLayout getLayout() {
            return mListItem;
        }
    }
}
