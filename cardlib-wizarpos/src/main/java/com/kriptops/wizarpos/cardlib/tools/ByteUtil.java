package com.kriptops.wizarpos.cardlib.tools;

public class ByteUtil
{
	public ByteUtil(){
	}
  
	/**
	 *
	 * Method Concatenates the specified byte[].
	 *
	 * @param number The int value to be converted.
	 *
	 */
	public static byte[] concatByteArray(byte[] a, byte[] b)
	{
		int aL = a.length;
		int bL = b.length;
		int len = aL + bL;
		byte[] c = new byte[len];
    
		System.arraycopy(a, 0, c, 0, aL);
		System.arraycopy(b, 0, c, aL, bL);
  
		return c;
	}

	public static void asciiToBCD(byte[] ascii_buf, int asc_offset, byte[] bcd_buf, int bcd_offset, int conv_len, int type)
	{
	    int cnt;
	    byte ch, ch1;
	    int bcdOffset = bcd_offset;
	    int asciiOffset = asc_offset;

	    if (((conv_len & 0x01) > 0) && (type > 0)){
	    	ch1 = 0;
	    }else{
	    	ch1 = 0x55;
	    }
	    for (cnt = 0; cnt < conv_len; asciiOffset++, cnt++)
	    {
	    	if (ascii_buf[asciiOffset] >= 97) // 97 = 'a'
	    	{
	    		ch = (byte) (ascii_buf[asciiOffset] - 97 + 10); // 97 = 'a'
	    	} else{
	    		if (ascii_buf[asciiOffset] >= 65) // 65 = 'A'
	    		{
	    			ch = (byte) ((ascii_buf[asciiOffset]) - 65 + 10); // 65 = 'A'
	    		}
	    		else{
	    			if (ascii_buf[asciiOffset] >= 48) // 48 = '0'
	    			{
	    				ch = (byte) ((ascii_buf[asciiOffset]) - 48); // 48 = '0'
	    			}else{
	    				ch = 0;
	    			}
	    		}
	    	}
	      	if (ch1 == 0x55){
	      		ch1 = ch;
	      	}else{
	      		// *bcd_buf++=ch1<<4 | ch;
	      		bcd_buf[bcdOffset++] = (byte) ((ch1 << 4) | ch);
	        	ch1 = 0x55;
	      	}
	    }
	    if (ch1 != 0x55){
	    	bcd_buf[bcdOffset] = (byte) (ch1 << 4);
	    }
	}
  
	public static byte[] bcdToAscii(byte[] bcdByte)
	{
	  	byte[] returnByte = new byte[bcdByte.length * 2];
		byte value;
		for(int i = 0; i < bcdByte.length; i++)
		{
    		value = (byte)(bcdByte[i] >> 4 & 0xF);
    		if( value > 9){
    			returnByte[i*2] = (byte)(value + (byte)0x37);
    		}
    		else{
    			returnByte[i*2] = (byte)(value + (byte)0x30);
    		}
    		value = (byte)(bcdByte[i] & 0xF);
    		if( value > 9){
    			returnByte[i*2+1] = (byte)(value + (byte)0x37);
    		}
    		else{
    			returnByte[i*2+1] = (byte)(value + (byte)0x30);
    		}
		}
		return returnByte;
	}

	public static byte[] bcdToAscii(byte[] bcdByte, int offset, int length)
	{
	  	byte[] returnByte = new byte[length * 2];
		byte value;
		for(int i = offset; i < length; i++)
		{
    		value = (byte)(bcdByte[i] >> 4 & 0xF);
    		if( value > 9){
    			returnByte[i*2] = (byte)(value + (byte)0x37);
    		}
    		else{
    			returnByte[i*2] = (byte)(value + (byte)0x30);
    		}
    		value = (byte)(bcdByte[i] & 0xF);
    		if( value > 9){
    			returnByte[i*2+1] = (byte)(value + (byte)0x37);
    		}
    		else{
    			returnByte[i*2+1] = (byte)(value + (byte)0x30);
    		}   	
		}
		return returnByte;
	}
	
	public static void bcdToAscii(byte[] bcd_buf, int offset, byte[] ascii_buf, int asc_offset,	int conv_len, int type)
	{
		int cnt;
		int bcdOffset = offset;
		int asciiOffset = asc_offset;
		if (conv_len > (bcd_buf.length * 2)){
			conv_len = (bcd_buf.length * 2);
		}
		if (((conv_len & 0x01) > 0) && (type > 0)) {
			cnt = 1;
			conv_len++;
		}else{
			cnt = 0;
		}
		for (; cnt < conv_len; cnt++, asciiOffset++) 
		{
			ascii_buf[asciiOffset] = (byte) (((cnt & 0x01) > 0) ? (bcd_buf[bcdOffset++] & 0x0f)	: ((bcd_buf[bcdOffset] & 0xFF) >>> 4));
			ascii_buf[asciiOffset] += (byte) ((ascii_buf[asciiOffset] > 9) ? (65 - 10) : 48); // 65 = 'A' 48 = '0'
		}
	}
  
	public static int bcdToInt(byte[] bcdByte)	// 嵌套调用的处理过程太复杂... DuanCS@[20140625]
	{
		return NumberUtil.parseInt(bcdToAscii(bcdByte),0,10,false);
	}
  
	/**
	 * Compares this byte arrary to the specified object.
	 * The result is <code>true</code> if and only if the argument is not
	 * <code>null</code> and is a <code>String</code> object that represents
	 * the same sequence of characters as this object.
	 *
	 * @param src
	 *            the first byte array
	 * @param tag
	 *            the second byte array
	 *                     against.
	 * @return  <code>true</code> if the <code>String </code>are equal;
	 *          <code>false</code> otherwise.
	 */
	public static boolean equalByteArray(byte[] src, byte[] dst) 
	{
		return equalByteArray(src, 0, src.length, dst, 0, dst.length);
	}
  
	public static boolean equalByteArray(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset, int dstLen) 
	{
		if (compareByteArray(src, srcOffset, srcLen, dst, dstOffset, dstLen) == 0){
			return true;
		}else{
			return false;
		}
	}
  
	public static int compareByteArray(byte[] src, byte[] dst) 
	{
		return compareByteArray(src, 0, src.length, dst, 0, dst.length);
	}
  
	public static int compareByteArray(byte[] src, int srcOffset, byte[] dst, int dstOffset, int length) 
	{
		return compareByteArray(src, srcOffset, length, dst, dstOffset, length);
	}
  
	/**
	 * Compares two byte array lexicographically..
	 * 
	 * @param src
	 *            the byte array
	 * @param srcOffset
	 *            the start position of the first byte array
	 * @param dst
	 *            the byte array
	 * @param dstOffset
	 *            the start position of the second byte array
	 * @return  the value <code>0</code> if the argument string is equal to
	 *          this string; a value less than <code>0</code> if this string
	 *          is lexicographically less than the string argument; and a
	 *          value greater than <code>0</code> if this string is
	 *          lexicographically greater than the string argument.
	 */
	public static int compareByteArray(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset, int dstLen) 
	{
	    char c1, c2;
	    if (   src == null || srcOffset < 0 || srcLen < 0
	    	|| dst == null || dstOffset < 0 || dstLen < 0
	    ) {
	    	return Integer.MIN_VALUE;
	    }

	    int n = Math.min(srcLen, dstLen);
	    if ((srcOffset + n) > src.length || (dstOffset + n) > dst.length){
	    	return Integer.MIN_VALUE;
	    }

	    for (int i = 0; i < n; i++) {
    		// compare the byte
    		c1 = (char)(src[srcOffset++] & 0xFF);
  	  		c2 = (char)(dst[dstOffset++] & 0xFF);
  	  		if (c1 != c2){
  		  		return c1 - c2;
  	  		}
		}
		return srcLen - dstLen;
	}

	public static void arraycopy(String src, int srcOffset, byte[] dst, int dstOffset, int length) {
		if (src == null || dst ==  null){
			throw new NullPointerException("invalid byte array ");
		}
		if ((src.length() < (srcOffset + length)) || (dst.length < (dstOffset + length))){
			throw new IndexOutOfBoundsException("invalid length: " + length);
		}
		for (int i = 0; i < length; i++) {
			dst[dstOffset++] = (byte)src.charAt(srcOffset++);
		}
	}
	
	public static byte[] getLengthArray(int srcLength) {
		byte[] destArray = null;
		if (srcLength <= 0x7F) {
			destArray = new byte[1];
			destArray[0] = (byte)(srcLength & (byte)0x7F);
		} else {
			int nCount;
			int midLength;
			midLength = srcLength;
			for (nCount = 1; nCount < 20; ++nCount) {
				midLength = midLength / 256;
				if (midLength < 256) {
					break;
				}
			}
			destArray = new byte[nCount+1];
			destArray[0] = (byte)(nCount | 0x80);
			midLength = srcLength;
			for (int idx = nCount; idx >= 1; idx --) {
				destArray[idx] = (byte)(midLength % 256);
				midLength = midLength / 256;
			}
		}
		
		return destArray;
	}

	// 字节数组转成16进制字符串(高效版:只创建至多两个对象, 减少内存占用). DuanCS@[20140610]
	public static final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	public static String arrayToHexStr(byte[] src)
	{
		return arrayToHexStr(null, src, 0, Integer.MAX_VALUE);
	}
	public static String arrayToHexStr(byte[] src, int srcLen)
	{
		return arrayToHexStr(null, src, 0, srcLen);
	}
	public static String arrayToHexStr(byte[] src, int offset, int srcLen)
	{
		return arrayToHexStr(null, src, offset, srcLen);
	}
	public static String arrayToHexStr(String prefix, byte[] src)
	{
		return arrayToHexStr(prefix, src, 0, Integer.MAX_VALUE);
	}
	public static String arrayToHexStr(String prefix, byte[] src, int srcLen)
	{
		return arrayToHexStr(prefix, src, 0, srcLen);
	}
	public static String arrayToHexStr(String prefix, byte[] src, int offset, int srcLen)
	{
		int prefixLen = prefix == null ? 0 : prefix.length();

		srcLen = (src == null || offset < 0 || srcLen < 0) ? offset = 0 : (src.length < srcLen)  ? src.length : srcLen;
		if (0 < srcLen && (src.length - offset) < srcLen)
			srcLen = 0;

		// 尽可能不创建新对象
		if (srcLen == 0) {
			return prefixLen == 0 ? "" : prefix;
		}

		// 事先就创建好大小正好的缓冲区，后续操作会很快且无内存浪费!
		StringBuilder sb = new StringBuilder(prefixLen + (srcLen << 1));
		if (prefixLen > 0)
			sb.append(prefix);

		while(0 < srcLen--)
		{
			int srcI = 0xFF & src[offset++];	// 扩展后 只取 8 BIT
			sb.append(hexChars[srcI >> 4]);
			sb.append(hexChars[srcI & 0x0f]);
		}
		return sb.toString();
	}

	public static int getIndexOf(byte value, byte[] items) {	// 查value在items中的索引
		for (int i = items.length; --i >= 0; ) {
			if (items[i] == value) {
				return i;
			}
		}
		return -1;
	}
}
