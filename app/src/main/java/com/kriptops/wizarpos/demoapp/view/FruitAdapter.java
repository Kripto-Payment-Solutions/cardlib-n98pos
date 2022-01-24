package com.kriptops.wizarpos.demoapp.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.kriptops.wizarpos.demoapp.R;

import java.util.List;

/**
 * Created by javaee on 2017/12/26.
 */

public class FruitAdapter extends ArrayAdapter<Fruit>{
    private int resourceId;

    public FruitAdapter(Context context, int resource, List<Fruit> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Fruit fruit = getItem(position);
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.name = view.findViewById(R.id.device_name);
            viewHolder.mdevice = view.findViewById(R.id.device_address);
            view.setTag(viewHolder);
        } else{
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.name.setText(fruit.getName());
        viewHolder.mdevice.setText(fruit.getMdevice());
        return view;
    }

    class ViewHolder{
        TextView name;
        TextView  mdevice;
    }
}
