package com.eyalzo.common.misc;

import static org.junit.Assert.*;

import org.junit.Test;

public class DateUtilsTest
{

	@Test
	public void testIsDate()
	{
		boolean result;

		//
		// False
		//
		result = DateUtils.isDate(null);
		assertFalse(result);

		result = DateUtils.isDate("");
		assertFalse(result);

		result = DateUtils.isDate(" abc ");
		assertFalse(result);

		//
		// True
		//
		result = DateUtils.isDate("Fri, April 4, 2014");
		assertTrue(result);

		result = DateUtils.isDate("Fri April 4, 2014");
		assertTrue(result);

		result = DateUtils.isDate("Fri Apr 30 2014");
		assertTrue(result);

		result = DateUtils.isDate("Fri, April 4");
		assertTrue(result);

		result = DateUtils.isDate("Friday, April 4, 2014");
		assertTrue(result);

		result = DateUtils.isDate("10/12/2002");
		assertTrue(result);

		result = DateUtils.isDate("2002/12/20");
		assertTrue(result);

		result = DateUtils.isDate("2002-12-20");
		assertTrue(result);

		result = DateUtils.isDate("32/32/2002");
		assertTrue(result);
	}
}
