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
	 * Get the index of the first occurance of <code>pattern</code> in <code>input</code> starting at <code>pos</code>
	 * 
	 * @param input
	 * @param pattern
	 * @param pos
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
	
	public static int index(byte[] input, int pos, byte[] pattern) {
		return index(input, pos, input.length-pos, pattern);
	}
	
	private static boolean isMatch(byte[] input, int pos, byte[] pattern) {
	    for(int i=0; i< pattern.length; i++) {
	        if(pattern[i] != input[pos+i]) {
	            return false;
	        }
	    }
	    return true;
	}

	public static List<byte[]> split(byte[] input, byte[] pattern) {
		return split(input, 0, input.length, pattern);
	}
	
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
