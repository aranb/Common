package com.eyalzo.common.misc;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ByteArrayUtilsTest {
	
	final byte[] DELIMITER1 = {12, 34};
	final byte[] DELIMITER2 = {12, 7};
	final byte[] INPUT1 = {1, 2, 3, 12, 1, 2, 3, 34, 12, 34, 5, 6, 7};
	final byte[] INPUT2 = {1, 2, 3, 12, 34, 1, 2, 3, 34, 12, 34, 5, 6, 7, 12, 34};
	final byte[] INPUT3 = {12, 34, 1, 2, 3, 12, 34, 1, 2, 3, 34, 12, 34, 5, 6, 7, 12, 34};
	final byte[] INPUT4 = {1, 2, 3, 12, 1, 2, 3, 34, 12, 34, 12, 34, 5, 6, 7};

	@Test
	public void testIndex1() {
		assertEquals(-1, 	ByteArrayUtils.index(INPUT1,0,DELIMITER2));
		assertEquals(3, 	ByteArrayUtils.index(INPUT2,0,DELIMITER1));
		assertEquals(9,		ByteArrayUtils.index(INPUT2,4 ,DELIMITER1));
	}
	
	@Test
	public void testIndex2() {
		assertEquals(-1, ByteArrayUtils.index(INPUT2, 4, 4 ,DELIMITER1));
		assertEquals(-1, ByteArrayUtils.index(INPUT2, 4, 5 ,DELIMITER1));
		assertEquals(-1, ByteArrayUtils.index(INPUT2, 4, 6 ,DELIMITER1));
		assertEquals(9, ByteArrayUtils.index(INPUT2, 4, 7 ,DELIMITER1));
	}

	@Test
	public void testSplitLengths() {
		List<byte[]> list1 = ByteArrayUtils.split(INPUT1, DELIMITER1);
		List<byte[]> list2 = ByteArrayUtils.split(INPUT2, DELIMITER1);
		List<byte[]> list3 = ByteArrayUtils.split(INPUT3, DELIMITER1);
		List<byte[]> list4 = ByteArrayUtils.split(INPUT3, DELIMITER2);
		List<byte[]> list5 = ByteArrayUtils.split(INPUT4, DELIMITER1);
		
		assertEquals(2, list1.size());
		assertEquals(3, list2.size());
		assertEquals(4, list3.size());
		assertEquals(1, list4.size());
		assertEquals(3, list5.size());
	}

	@Test
	public void testSplits() {
		byte[] str1 = {1, 2, 3, 12, 1, 2, 3, 34};
		byte[] str2 = {5, 6, 7};
		List<byte[]> list = ByteArrayUtils.split(INPUT1, DELIMITER1);
		assertTrue(Arrays.equals(list.get(0), str1));
		assertTrue(Arrays.equals(list.get(1), str2));
		
		
		byte[] str3 = {1, 2, 3};
		byte[] str4 = {1, 2, 3, 34};
		List<byte[]> list2 = ByteArrayUtils.split(INPUT2, DELIMITER1);
		assertTrue("First Split of list2 not equal", Arrays.equals(list2.get(0), str3));
		assertTrue("Second Split of list2 not equal",Arrays.equals(list2.get(1), str4));
		assertTrue("Third Split of list2 not equal",Arrays.equals(list2.get(2), str2));
	}
	
	@Test
	public void testEmptySplits() {
		byte[] str1 = {1, 2, 3, 12, 1, 2, 3, 34};
		byte[] emptyByteArray = {};
		byte[] str2 = {5, 6, 7};
		List<byte[]> list = ByteArrayUtils.split(INPUT4, DELIMITER1);
		assertTrue(Arrays.equals(list.get(0), str1));
		assertTrue(Arrays.equals(list.get(2), str2));
		assertTrue(Arrays.equals(list.get(1), emptyByteArray));
		
		byte[] str3 = {1, 2, 3};
		byte[] str4 = {1, 2, 3, 34};
		List<byte[]> list2 = ByteArrayUtils.split(INPUT3, DELIMITER1);
		assertTrue(Arrays.equals(list2.get(0), emptyByteArray));
		assertTrue(Arrays.equals(list2.get(1), str3));
		assertTrue(Arrays.equals(list2.get(2), str4));
		assertTrue(Arrays.equals(list2.get(3), str2));
	}
}
