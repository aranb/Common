package com.eyalzo.common.misc;

import static org.junit.Assert.*;

import java.util.regex.Matcher;

import org.junit.Test;

public class DateUtilsTest
{

	@Test
	public void testIsDate()
	{
		Matcher result;

		DateUtils.printDatePatterns();

		//
		// False
		//
		result = DateUtils.containsDate(null);
		assertNull(result);

		result = DateUtils.containsDate("");
		assertNull(result);

		result = DateUtils.containsDate(" abc ");
		assertNull(result);

		//
		// True
		//
		testDate("Oct-20-13 12:33:09 PDT", 9);
		testDate("Arrive on: Oct-20-13 12:33:09 PDT", 9);
		testDate("Fri, April 4, 2014");
		testDate("Arrive on: Fri, April 4, 2014", 18);
		testDate("Arrive on: Fri, April 4", 12);
		testDate("Fri April 4, 2014");
		testDate("Fri Apr 30 2014");
		testDate("Friday, April 4, 2014");
		testDate("Friday, Apr 4, 2014");
		testDate("Apr 4");
		testDate("Tue, May 20");
		testDate(" Tue, May 20",11);
		testDate(" Tue, May 20\t",11);
		testDate(" Tue, May 20\t\n",11);
		testDate(" Tue, May 20\n",11);
		testDate("\n Tue, May 20\n",11);
		testDate("\n\t\t\t   Tue, May 20   \t  \n ",11);
	}

	private void testDate(String date)
	{
		testDate(date, date.length());
	}

	private void testDate(String date, int expectedMatchLen)
	{
		Matcher result = DateUtils.containsDate(date);
		assertNotNull(result);
		assertEquals(1, result.groupCount());
		assertEquals(expectedMatchLen, result.group(1).length());
		System.out.println(date + "\t=>\t" + result.group(1));
	}
}
