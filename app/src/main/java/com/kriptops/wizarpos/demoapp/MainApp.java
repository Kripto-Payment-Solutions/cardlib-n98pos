package com.kriptops.wizarpos.demoapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.kriptops.n98pos.cardlib.Defaults;
import com.kriptops.n98pos.cardlib.Pos;
import com.kriptops.n98pos.cardlib.PosOptions;
import com.kriptops.n98pos.cardlib.android.PosApp;
import com.kriptops.n98pos.cardlib.crypto.FitMode;
import com.kriptops.n98pos.cardlib.crypto.PaddingMode;
import com.kriptops.n98pos.cardlib.db.MapIVController;

public class MainApp extends Application implements PosApp {

    private Pos pos;

    public Pos getPos() {
        return pos;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // ACA ESTA EL SETEO DEL IV CONTROLLER PARA MANTENER UN HISTORICO DEL IV EN LA CARDLIB
        // PARA CAMBIARLO SE DEBE IMPLEMENTAR LA INTERFAZ IVController Y ALIMENTARLO EN LA CREACION
        // DEL POS
//        PosOptions posOptions = new PosOptions();
//        posOptions.setIvController(new MapIVController());
//        posOptions.setTrack2FitMode(FitMode.F_FIT);
//        posOptions.setTrack2PaddingMode(PaddingMode.PKCS5);
//        posOptions.setAuthProcessingCode((byte) 0x00);
//        posOptions.setReverseProcessingCode((byte) 0x00);
//        posOptions.setAidTables(Defaults.AID_TABLES);
//
//        this.pos = new Pos(this, posOptions);
//        this.pos.setPinLength(4);
        //this.pos.setPinLength(4, 6);
    }

}
