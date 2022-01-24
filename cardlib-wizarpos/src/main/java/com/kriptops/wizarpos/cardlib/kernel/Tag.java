package com.kriptops.wizarpos.cardlib.kernel;

import com.kriptops.wizarpos.cardlib.tools.Util;

public class Tag {

    private String name;
    private String value;

    public Tag(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    private String compile() {
        return name + Util.toHexString(new byte[]{(byte) (value.length() / 2)}) + value;
    }

    public static String compile(Tag[] tags) {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) {
            sb.append(tag);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return compile();
    }
}
