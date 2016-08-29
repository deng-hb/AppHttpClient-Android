package com.denghb.example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by denghb on 16/8/29.
 */

public class TextAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private String[] array;

    public TextAdapter(Context context,String[] data){
        mInflater = LayoutInflater.from(context);
        array = data;
    }

    @Override
    public int getCount() {
        return array.length;
    }

    @Override
    public Object getItem(int position) {
        return array[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_text, null);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.text);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(array[position]);

        return convertView;
    }

    static class ViewHolder {
        TextView text;
    }
}
