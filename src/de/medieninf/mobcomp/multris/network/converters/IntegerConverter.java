package de.medieninf.mobcomp.multris.network.converters;

/**
 * Converter Class for Integer<>byte[] Conversion
 * */
public class IntegerConverter {

	/**
	 * converts a byte-Array to an Integer number
	 * @param byteArray
	 * @returns converted byteArray as Integer
	 * */
	public static int bytearrayToInt(byte[] byteArray) {
		// byte[] -> int
		int number = 0;
		for (int i = 0; i < 4; ++i) {
			number |= (byteArray[3 - i] & 0xff) << (i << 3);
		}
		return number;
	}

	/**
	 * converts an Integer number to a byte-Array
	 * @param number
	 * @returns converted number as byte-Array
	 * */
	public static byte[] intToBytearray(int number) {
		byte[] byteArray = new byte[4];

		// int -> byte[]
		for (int i = 0; i < 4; ++i) {
			int shift = i << 3; // i * 8
			byteArray[3 - i] = (byte) ((number & (0xff << shift)) >>> shift);
		}
		return byteArray;
	}
}
