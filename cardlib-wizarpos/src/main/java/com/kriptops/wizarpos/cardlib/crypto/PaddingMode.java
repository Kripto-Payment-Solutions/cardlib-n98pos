package com.kriptops.wizarpos.cardlib.crypto;

/**
 * Define el modo de completar una cadena para que corresponda a un ancho valido de octetos binarios.
 */
public enum PaddingMode {
    ZERO("00"),
    F("FF"),
    PKCS5(null) {
        @Override
        public String pad(String text) {
            if (text.length() % 2 == 1) {
                throw new IllegalArgumentException("Length must be octect compatible");
            }
            int octect_size = text.length() / 2;
            int pad = 8 - octect_size % 8;
            String padString = "0" + pad;
            for (int i = 0; i < pad; i++) text += padString;
            return text;
        }
    };

    private final String pad;

    PaddingMode(String pad) {
        this.pad = pad;
    }

    public String pad(String text) {
        if (text.length() % 2 == 1)
            throw new IllegalArgumentException("Length must be octect compatible");
        while (text.length() % 16 != 0) {
            text += this.pad;
        }
        return text;
    }
}
