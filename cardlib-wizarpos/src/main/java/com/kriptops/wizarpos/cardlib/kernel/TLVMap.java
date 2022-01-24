package com.kriptops.wizarpos.cardlib.kernel;

import java.util.LinkedHashMap;

public class TLVMap extends LinkedHashMap<String, Tag> {

    public TLVMap() {

    }

    public TLVMap(String tlv) {
        super();

        // this.put("FFFF", new Tag("FFFF", tlv));
        TLVTagTokenizer st = new TLVTagTokenizer(tlv);

        for (Tag tag = st.nextTag(); tag != null; tag = st.nextTag()) {
            this.put(tag.getName(), tag);
        }

    }

    public String getValue(String tagName) {
        return this.containsKey(tagName) ? this.get(tagName).getValue() : null;
    }

    private class TLVTagTokenizer {
        private int bit8on = 0b10000000;
        private int bits1to5on = 0b00011111;
        private int bits1to7on = 0b01111111;
        private String tlv;
        private int offset;

        public TLVTagTokenizer(String tlv) {
            this.tlv = tlv;
            this.offset = 0;
        }

        public Tag nextTag() {
            String name = getTagName();
            if (name == null) return null;
            int length = getTagLength();
            if (length < 0) return null;
            String value = remove(length * 2);//multiplica por 2 porque son octetos
            if (value == null) return null;
            if (name == null || length < 0 || value == null) return null;
            return new Tag(name, value);
        }


        private String getTagName() {
            if (overflows(2)) return null;
            String part = remove(2);
            String name = part;
            int b = Integer.parseInt(part, 16);
            //how to know if there are more
            boolean more = checkMask(bits1to5on, b);
            while (more) {
                if (overflows(2)) return null;
                part = remove(2);
                name += part;
                b = Integer.parseInt(part);
                more = checkMask(bit8on, b);
            }
            return name;
        }

        private int getTagLength() {
            if (overflows(2)) return -1;
            int b = removeInt(2);
            boolean simple = !checkMask(bit8on, b);
            int length = bits1to7on & b;
            if (simple) return length;
            if (overflows(length)) return -1;
            return removeInt(length * 2);//multiplica por 2 porque son octetos
        }

        private String remove(int length) {
            if (length < 0) return null; //invalid length
            if (length == 0) return ""; //no need to read data
            if (overflows(length)) return null;  //not enough data to read
            return tlv.substring(offset, offset += length);
        }

        private int removeInt(int length) {
            if (length < 0) return -1; //invalid length
            if (length == 0) return 0; //invalid TLV!
            if (overflows(length)) return -1; //not enough data to read
            return Integer.parseInt(remove(length), 16);
        }

        private boolean checkMask(int mask, int value) {
            return (mask & value) == mask;
        }

        private String peek(int skip, int length) {
            if (overflows(length + skip)) return null;
            return tlv.substring(offset + skip, offset + skip + length);
        }

        private boolean overflows(int size) {
            return tlv.length() < (offset + size);
        }

        @Override
        public String toString() {
            return "StringTokenizer{remains: " + tlv.substring(offset) + "}";
        }
    }

}
