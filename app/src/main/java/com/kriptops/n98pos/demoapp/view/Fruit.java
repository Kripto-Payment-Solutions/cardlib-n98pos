package com.kriptops.n98pos.demoapp.view;

/**
 * Created by javaee on 2017/12/26.
 */

public class Fruit {
    private String name;
    private String  mdevice;

    public Fruit() {

    }

    public String getMdevice() {
        return mdevice;
    }

    public void setMdevice(String mdevice) {
        this.mdevice = mdevice;
    }

    public Fruit(String name, String mdevice) {
        this.name = name;
        this.mdevice = mdevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
