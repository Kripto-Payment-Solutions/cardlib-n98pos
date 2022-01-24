package com.kriptops.n98pos.cardlib.db;

public interface IVController {

    void saveIv(String usage, String iv);

    String readIv(String usage);

}
