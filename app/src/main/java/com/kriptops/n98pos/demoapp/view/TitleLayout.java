package com.kriptops.n98pos.demoapp.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kriptops.n98pos.demoapp.R;

/**
 * Created by javaee on 2018/1/3.
 */

public class TitleLayout extends LinearLayout{

    public TitleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.title,this);
        Button button = findViewById(R.id.back);
        Button mbuttion = findViewById(R.id.ok);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        mbuttion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(),getContext().getString(R.string.cancel),Toast.LENGTH_SHORT).show();
            }
        });
    }
}
