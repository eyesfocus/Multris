package de.medieninf.mobcomp.multris.network.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 
 * @author marcel bechtold
 * 
 *         Class to convert Objects (usually BTMessages) to an array of Bytes and vice versa
 * */
public class MessageConverter {
	final static int BYTESOFINT = 4;

	/**
	 * Converts an Object to a byte-Array
	 * 
	 * @param o
	 *            Object that has to be serialized
	 * @return converted object (byte-array)
	 * */
	public static byte[] serializeObject(Object o) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.close();
			byte[] buffer = baos.toByteArray();
			byte[] retbuffer = new byte[buffer.length + BYTESOFINT];
			byte[] ibuffer = IntegerConverter.intToBytearray(buffer.length);
			if (ibuffer.length != BYTESOFINT) {
				throw new RuntimeException("BYTESOFINT NOT 4");
			}
			System.arraycopy(ibuffer, 0, retbuffer, 0, BYTESOFINT);
			System.arraycopy(buffer, 0, retbuffer, BYTESOFINT, buffer.length);
			return retbuffer;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts an byte-Array to an Object
	 * 
	 * @param b
	 *            byte-array that has to be desezialized
	 * @return converted byte-array (Object)
	 * 
	 * */
	public static Object deserializeObject(byte[] b) {
		Object o = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(b, 0, b.length);
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(bais);
			o = in.readObject();
			in.close();
			return o;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
			throw new RuntimeException(c);
		}
	}
}
