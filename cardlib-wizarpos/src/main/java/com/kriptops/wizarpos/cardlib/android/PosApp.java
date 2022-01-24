package com.kriptops.wizarpos.cardlib.android;

import android.content.Context;

import com.kriptops.wizarpos.cardlib.Pos;

public interface PosApp {

    Pos getPos();

    Context getApplicationContext();

}
