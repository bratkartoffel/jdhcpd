package eu.fraho.jdhcpd;

/*
This file is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This file is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this file. If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Abstract class with only static methods. These methods are pretty generic
 * and are used in different locations in the program.
 * 
 * @author sfrankenberger
 */
public abstract class Tools {
	/**
	 * Defines the format how dates are formated
	 */
	private static final String FORMAT_DATE = "yyyy-MM-dd";
	
	/**
	 * Defines the format how times are formated
	 */
	private static final String FORMAT_TIME = "HH:mm:ss";
	
	/**
	 * Object which formats dates
	 */
	private static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat(
	    FORMAT_DATE);
	
	/**
	 * Object which formats times
	 */
	private static final SimpleDateFormat TIME_FORMATER = new SimpleDateFormat(
	    FORMAT_TIME);
	
	/**
	 * Helper methods to convert 4 bytes into one integer.
	 * 
	 * @param buffer the bytes to convert
	 * @return an integer representing the 4 bytes
	 * 
	 * @see #intToByte(int)
	 */
	public static int byteToInt(byte[] buffer) {
		if (buffer.length != 4) {
			throw new IllegalArgumentException("buffer length must be 4 bytes!");
		}
		
		int value = (0xFF & buffer[0]) << 24;
		value |= (0xFF & buffer[1]) << 16;
		value |= (0xFF & buffer[2]) << 8;
		value |= (0xFF & buffer[3]);
		
		return value;
	}
	
	/**
	 * Method to convert 4 bytes to a textual ip adress representation.
	 * 
	 * @param b the bytes to convert
	 * @return the bytes as an ip adress
	 */
	public static String byteToIp(byte[] b) {
		StringBuilder back = new StringBuilder();
		
		for (int i = 0; i < 4; i++) {
			if (i > 0)
				back.append(".");
			String neu = String.valueOf(0xFF & b[i]);
			
			back.append(neu);
		}
		
		return back.toString();
	}
	
	/**
	 * Helper method to convert 6 bytes to a textual mac adress representation.
	 * 
	 * @param b the bytes to convert
	 * @return the bytes as a mac adress
	 */
	public static String byteToMac(byte[] b) {
		StringBuilder back = new StringBuilder();
		
		for (int i = 0; i < 6; i++) {
			if (i > 0)
				back.append(":");
			String neu = Integer.toHexString(0xFF & b[i]);
			if (neu.length() == 1)
				neu = "0" + neu;
			
			back.append(neu);
		}
		
		return back.toString();
	}
	
	/**
	 * Closes the given Object and catches the exceptions.
	 * 
	 * @param c the object to close
	 */
	public static void close(Closeable c) {
		if (c == null) {
			return;
		}
		
		try {
			c.close();
		}
		catch (IOException e) {
			Application.getInstance().update(null, e);
		}
	}
	
	/**
	 * @return current formatted date
	 * @see #DATE_FORMATER
	 */
	public static String currentDate() {
		return DATE_FORMATER.format(new Date());
	}
	
	/**
	 * @return current formatted date and time, separated by a blank character
	 */
	public static String currentDateTime() {
		return currentDate() + " " + currentTime();
	}

	/**
	 * @return current formatted time
	 * @see #TIME_FORMATER
	 */
	public static String currentTime() {
		return TIME_FORMATER.format(new Date());
	}
	
	/**
	 * @param s the timestamp to format
	 * @return The formatted date and time of the timestamp
	 */
	public static String dateTime(long s) {
		Date d = new Date(s);
		return DATE_FORMATER.format(d) + " " + TIME_FORMATER.format(d);
	}
	
	/**
	 * Splits the given int into 4 bytes
	 * 
	 * @param val the int to convert
	 * @return a byte array containing 4 elements
	 * 
	 * @see #byteToInt(byte[])
	 */
	public static byte[] intToByte(int val) {
		byte[] buffer = new byte[4];
		
		buffer[0] = (byte) (val >>> 24);
		buffer[1] = (byte) (val >>> 16);
		buffer[2] = (byte) (val >>> 8);
		buffer[3] = (byte) val;
		
		return buffer;
	}
	
	/**
	 * Converts the given ip adress into 4 bytes
	 * 
	 * @param ip te ip adress to convert
	 * @return a byte array containing 4 elements
	 */
	public static byte[] ipToByte(String ip) {
		String[] parts = ip.split("\\.");
		byte[] back = new byte[4];
		
		if (parts.length != 4)
			return back;
		
		for (int i = 0; i < 4; i++) {
			int val = Integer.valueOf(parts[i]);
			if(val < 0 || val > 255)
				return new byte[4];
			
			val -= 256;
			back[i] = (byte) val;
		}
		
		return back;
	}
	
	/**
	 * Reads the given file and returns the content
	 * 
	 * @param f the file to read
	 * @return an byte array with the files contents
	 * 
	 * @throws FileNotFoundException if anything goes wrong while reading
	 *         the file.
	 */
	public static byte[] readFile(File f) throws FileNotFoundException {
		if (!f.exists()) {
			throw new FileNotFoundException();
		}
		
		BufferedInputStream in = null;
		ByteArrayOutputStream bs = null;
		BufferedOutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(f));
			bs = new ByteArrayOutputStream();
			out = new BufferedOutputStream(bs);
			
			byte[] ioBuf = new byte[8192];
			int bytesRead;
			
			while ((bytesRead = in.read(ioBuf)) != -1) {
				out.write(ioBuf, 0, bytesRead);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			close(in);
			close(out);
			close(bs);
		}
		
		return bs.toByteArray();
	}
	
	/**
	 * Reads the given file and returns a string with the content
	 * 
	 * @param f the file to read
	 * @return a string with the files contents
	 * 
	 * @throws FileNotFoundException if anything goes wrong while reading
	 *         the file.
	 */
	public static String readTextFile(File f) throws FileNotFoundException {
		return new String(readFile(f));
	}
	
	/**
	 * Checks whether the given byte[] represents a valid netmask.
	 * 
	 * @param nm the netmask to check
	 * @return is the byte[] a netmask?
	 */
	public static boolean checkNetmask(byte[] nm) {
		boolean firstFalse = false;
		
		for (int i = 0; i < 4; i++) {
			boolean[] akt = convertToBits(nm[i]);
			for (int b = 0; b < 8; b++) {
				if (firstFalse && akt[b])
					return false;
				
				if (!akt[b])
					firstFalse = true;
			}
		}
		
		return true;
	}
	
	/**
	 * Method to convert a byte into 8 bits.
	 * 
	 * @param b the byte to convert
	 * @return 8 booleans as an array
	 */
	private static boolean[] convertToBits(byte b) {
		boolean[] bits = new boolean[8];
		
		for (int i = 0; i < 8; i++) {
			bits[7 - i] = ((b & (1 << i)) != 0);
		}
		
		return bits;
	}
	
	/**
	 * Checks wheter the given byte[] represents a valid ip adress which
	 * can propably announced to a client?<br>
	 * Valid adresses range from 1 to 254 (0 is the net itself, 
	 * 255 is the broadcast adress).<br>
	 * <br>
	 * Note that byte is <b>signed</b>, so the following 2 values are
	 * interesting for the comparison:<br>
	 * <code><pre>
	 *   byte    int
	 *   -----------
	 *   -1   =  255
	 *    0   =  0
	 * </pre></code>
	 * 
	 * @param ip the ip adress to check
	 * @return is the byte[] an ip adress?
	 */
	public static boolean checkIp(byte[] ip) {
		return ip.length == 4 && ip[3] != -1 && ip[3] != 0;
	}
	
	/**
	 * Helper method to convert a byte[] into an int[].
	 * 
	 * @param data The byte[] to convert
	 * @return an int[] representing the given byte[]
	 */
	public static int[] byteAToIntA(byte[] data) {
		int[] back = new int[data.length];
		
		for(int i=0; i<data.length; i++)
			back[i] = 0xFF & data[i];
		
		return back;
	}
}
