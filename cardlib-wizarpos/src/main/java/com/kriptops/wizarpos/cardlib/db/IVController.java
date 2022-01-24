package com.kriptops.wizarpos.cardlib.db;

public interface IVController {

    void saveIv(String usage, String iv);

    String readIv(String usage);

}
