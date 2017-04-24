package com.shopify.mobilebuy.sample2;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.shopify.buy3.Storefront;

import java.util.List;

/**
 * Created by sammaier on 4/21/17.
 */

public class CollectionsAdapter extends ArrayAdapter<Storefront.Collection> {
    private List<Storefront.Collection> collections;
    private Context context;

    public CollectionsAdapter(Context context, int resource, List<Storefront.Collection> objects) {
        super(context, resource, objects);
        collections = objects;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new TextView(context);
        }
        ((TextView)convertView).setText(collections.get(position).getTitle());
        return convertView;
    }
}
