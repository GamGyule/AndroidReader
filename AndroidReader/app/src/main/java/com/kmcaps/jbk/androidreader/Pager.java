package com.kmcaps.jbk.androidreader;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class Pager extends Parent {
    Adapter adapter;
    ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view);
        stateHide();
        viewPager = (ViewPager) findViewById(R.id.view);
        adapter = new Adapter(Pager.this);
        viewPager.setAdapter(adapter);

    }
}
