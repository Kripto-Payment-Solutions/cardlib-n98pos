package com.kriptops.n98pos.cardlib.android;

import android.content.Context;

import com.kriptops.n98pos.cardlib.Pos;
import com.newpos.mposlib.sdk.NpPosManager;

public interface PosApp {

    Pos getPos();

    Context getApplicationContext();

}
