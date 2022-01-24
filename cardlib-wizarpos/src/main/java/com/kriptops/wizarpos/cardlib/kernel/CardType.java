package com.kriptops.wizarpos.cardlib.kernel;

import com.cloudpos.jniinterface.EMVJNIInterface;
import com.wizarpos.emvsample.constant.Constant;

public enum CardType {
    MSR("msr"),
    ICC("icc", (byte) Constant.CARD_CONTACT, Constant.CARD_CONTACTLESS),
    NFC("nfc", (byte) Constant.CARD_CONTACTLESS, Constant.CARD_CONTACT);

    public final String tag;
    public final byte set;
    public final int close;

    CardType(String tag, byte set, int close) {
        this.tag = tag;
        this.set = set;
        this.close = close;
    }

    CardType(String tag) {
        this(tag, (byte) 0, 0);
    }

    public static CardType fromEmv(int emvCardConstant) {
        switch (EMVJNIInterface.get_card_type()) {
            case Constant.CARD_CONTACT:
                return CardType.ICC;
            case Constant.CARD_CONTACTLESS:
                return CardType.NFC;
        }
        return null;
    }
}
