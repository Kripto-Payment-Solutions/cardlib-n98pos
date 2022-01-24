package com.kriptops.wizarpos.cardlib.crypto;

/**
 * Define el modo en que se completa una cadena para corresponder a un formato valido de Hex.
 */
public enum FitMode {
    ZERO_FIT("0"),
    F_FIT("F");

    private String fit;

    FitMode(String fit) {
        this.fit = fit;
    }

    public String fit(String hex) {
        if (hex.length() % 2 == 1) {
            hex += fit;
        }
        return hex;
    }
}
