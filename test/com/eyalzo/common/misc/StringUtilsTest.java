package com.eyalzo.common.misc;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

public class StringUtilsTest
{

	@Test
	public void testIsTextCurrency()
	{
		boolean result;

		//
		// False
		//
		result = StringUtils.isTextCountry(null);
		assertFalse(result);

		result = StringUtils.isTextCountry("");
		assertFalse(result);

		result = StringUtils.isTextCountry(" ");
		assertFalse(result);

		//
		// True
		//
		result = StringUtils.isTextCountry("$1");
		assertFalse(result);

		result = StringUtils.isTextCountry(" $1");
		assertFalse(result);

		result = StringUtils.isTextCountry(" $1 ");
		assertFalse(result);

		result = StringUtils.isTextCountry("$1 ");
		assertFalse(result);
	}

	@Test
	public void testIsTextCountry()
	{
		assertEquals(false, StringUtils.isTextCountry(null));
		assertEquals(false, StringUtils.isTextCountry(""));
		assertEquals(false, StringUtils.isTextCountry(" "));
		assertEquals(false, StringUtils.isTextCountry(" fhasdhjkl "));

		assertEquals(true, StringUtils.isTextCountry("Israel"));
		assertEquals(true, StringUtils.isTextCountry(" Israel"));
		assertEquals(true, StringUtils.isTextCountry("Israel "));
		assertEquals(true, StringUtils.isTextCountry("IsRaEl"));
		assertEquals(true, StringUtils.isTextCountry("IL"));

		//
		// USA
		//
		assertEquals(true, StringUtils.isTextCountry("USA"));
		assertEquals(true, StringUtils.isTextCountry("US"));
		assertEquals(false, StringUtils.isTextCountry("Usa"));
		assertEquals(true, StringUtils.isTextCountry("United States"));
		assertEquals(true, StringUtils.isTextCountry("United states"));
		assertEquals(false, StringUtils.isTextCountry("United States of America"));
	}

	@Test
	public void testGetLongestPrefix()
	{
		int result;
		String baseString = "Hello world";
		ArrayList<String> others = new ArrayList<String>();
		others.add("");

		result = StringUtils.getLongestCommonPrefix(baseString, others, 0);
		assertEquals(11, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 1);
		assertEquals(0, result);

		others.add("Second");
		result = StringUtils.getLongestCommonPrefix(baseString, others, 0);
		assertEquals(11, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 1);
		assertEquals(0, result);

		others.add("Hello third");
		result = StringUtils.getLongestCommonPrefix(baseString, others, 0);
		assertEquals(11, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 1);
		assertEquals(6, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 2);
		assertEquals(0, result);

		others.add("Hello word");
		result = StringUtils.getLongestCommonPrefix(baseString, others, 0);
		assertEquals(11, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 1);
		assertEquals(9, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 2);
		assertEquals(6, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 3);
		assertEquals(0, result);

		others.add("Hello world");
		result = StringUtils.getLongestCommonPrefix(baseString, others, 0);
		assertEquals(11, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 1);
		assertEquals(11, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 2);
		assertEquals(9, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 3);
		assertEquals(6, result);
		result = StringUtils.getLongestCommonPrefix(baseString, others, 4);
		assertEquals(0, result);
	}
}
