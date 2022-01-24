package com.kriptops.wizarpos.cardlib.db;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapIVController implements IVController {

    private Map<String, String> map = new LinkedHashMap<>();


    @Override
    public void saveIv(String usage, String iv) {
        this.map.put(usage, iv);
    }

    @Override
    public String readIv(String usage) {
        return this.map.get(usage);
    }
}
