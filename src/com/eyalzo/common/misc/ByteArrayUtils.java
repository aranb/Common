package com.eyalzo.common.misc;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * This class is used for byte array utilities (such as split, index, etc.)
 * 
 * @author Aran Bergman
 *
 */
public class ByteArrayUtils {
	
	/**
	 * Get the index of the first occurrence of <code>pattern</code> in <code>input</code> starting at <code>pos</code>. 
	 * This method is effective only for very short patterns. For long patterns a more efficient method should be used. 
	 * 
	 * @param input the input buffer on which the search is performed
	 * @param pattern the pattern to look for in the input buffer
	 * @param pos zero based position in the input buffer where the search starts 
	 * @param length length of the buffer, starting a <code>pos</code> to search in 
	 * @return -1 if pattern was not found. Otherwise - return the position in input
	 */
	public static int index(byte[] input, int pos, int length, byte[] pattern) {
		if (input == null || pattern == null || pos < 0 || length < 0)
			return -1;
		if (pos > input.length || pattern.length > input.length-pos)
			return -1;
		for (int i=pos; i <= pos+length-pattern.length && i <= input.length-pattern.length; i++)
			if (isMatch(input, i, pattern))
				return i;
		
		return -1;
	}
	
	/**
	 * Get the index of the first occurrence of <code>pattern</code> in <code>input</code> starting at <code>pos</code>. 
	 * This method is effective only for very short patterns. For long patterns a more efficient method should be used. 
	 * 
	 * @param input the input buffer on which the search is performed
	 * @param pattern the pattern to look for in the input buffer
	 * @param pos zero based position in the input buffer where the search starts 
	 * @return -1 if pattern was not found. Otherwise - return the position in input
	 */
	public static int index(byte[] input, int pos, byte[] pattern) {
		return index(input, pos, input.length-pos, pattern);
	}

	/**
	 * Get the index of the first occurrence of <code>pattern</code> in <code>input</code>. 
	 * This method is effective only for very short patterns. For long patterns a more efficient method should be used. 
	 * 
	 * @param input the input buffer on which the search is performed
	 * @param pattern the pattern to look for in the input buffer
	 * @return -1 if pattern was not found. Otherwise - return the position in input
	 */
	public static int index(byte[] input, byte[] pattern) {
		return index(input, 0, input.length, pattern);
	}
	
	private static boolean isMatch(byte[] input, int pos, byte[] pattern) {
	    for(int i=0; i< pattern.length; i++) {
	        if(pattern[i] != input[pos+i]) {
	            return false;
	        }
	    }
	    return true;
	}
	
	public static boolean endsWith(byte[] input, byte[] pattern) {
		// check for valid input
		if (input == null || pattern == null)
			return false;
		int pos = input.length - pattern.length;
		if (pos < 0)
			return false;
		return isMatch(input, pos, pattern);
	}

	public static List<byte[]> split(byte[] input, byte[] pattern) {
		return split(input, 0, input.length, pattern);
	}
	
	/**
	 * A version of {@link String}'s <code>split</code> method for byte arrays, only it does not use regexp.
	 * @param input the input byte arrat
	 * @param pos the zero-based position to start looking for the pattern from 
	 * @param length number of bytes to search, starting at <code>pos</code>
	 * @param pattern the pattern to look for when splitting the array
	 * @return a {@link List} of byte arrays containing the splits of the <code>input</code>
	 */
	public static List<byte[]> split(byte[] input, int pos, int length, byte[] pattern) {
		List<byte[]> list = new LinkedList<byte[]>();
	    int blockStart = pos;
	    int next = index(input, pos, length, pattern);
	    while (next != -1) {
	    	list.add(Arrays.copyOfRange(input, blockStart, next));
	    	length -= next-blockStart+pattern.length;
	    	blockStart = next+pattern.length;
	    	next = index(input, blockStart, length ,pattern);
	    }
	    int lastBlockPos = Math.min(input.length, blockStart+length);
	    if (lastBlockPos > blockStart)
	    	list.add(Arrays.copyOfRange(input, blockStart, Math.min(input.length, blockStart+length)));
	    return list;
	}
}
