/**
 * Copyright 2012 Eyal Zohar. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY EYAL ZOHAR ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * EYAL ZOHAR OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed or implied, of Eyal Zohar.
 */
package com.eyalzo.common.misc;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author Eyal Zohar
 * 
 */
public class DateUtils
{
	/**
	 * Date format that serves well as sortable string for file names etc. It always takes 19 characters.
	 */
	public static final String				DATETIME_FORMAT_SORTABLE	= "yyyy-MM-dd.HH-mm-ss";
	public static final String				DATE_FORMAT_SORTABLE		= "yyyy-MM-dd";
	private static final SimpleDateFormat	formatterDateTime			= new SimpleDateFormat(DATETIME_FORMAT_SORTABLE);
	private static final SimpleDateFormat	formatterDate				= new SimpleDateFormat(DATE_FORMAT_SORTABLE);
	private static final DateFormat			formatterDateTimeShort		= DateFormat.getDateTimeInstance(
																				DateFormat.SHORT, DateFormat.MEDIUM,
																				Locale.UK);

	private static final long				MILLIS_SECOND				= 1000L;
	private static final long				MILLIS_MINUTE				= 60L * MILLIS_SECOND;
	private static final long				MILLIS_HOUR					= 60L * MILLIS_MINUTE;
	private static final long				MILLIS_DAY					= 24L * MILLIS_HOUR;
	private static final long				MILLIS_YEAR					= 365L * MILLIS_DAY;

	/**
	 * Date formats for fast inaccurate validation. Examples: "Friday, April 4, 2014".
	 */
	private static final String				DATE_VALIDATOR_DAY_NAME		= "(?:(?:Sun|Mon|Fri)(?:day)?|Tue(?:sday)?|Wed(?:nesday)?|Thu(?:rsday)?|Sat(?:urday)?)";
	private static final String				DATE_VALIDATOR_MONTH_NAME	= "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)";
	private static final String[]			DATE_VALIDATOR_FORMATS		= {
			"[0-9][0-9][\\-/][0-9][0-9][\\-/](?:20|19)[0-9][0-9]",
			"(?:20|19)[0-9][0-9][\\-/][0-9][0-9][\\-/][0-9][0-9]",
			"\\b" + DATE_VALIDATOR_DAY_NAME + "[, ]+\\b" + DATE_VALIDATOR_MONTH_NAME
					+ "[, ]+\\b[1-9](?:[0-9])?(?:[, ]+20[0-9][0-9])?"	};
	private static LinkedList<Pattern>		patternsDate;

	/**
	 * @param text
	 *            Given text to be examined. Can be null.
	 * @return True if the string looks like a complete date.
	 */
	public static boolean isDate(String text)
	{
		if (text == null)
			return false;

		// Init the patterns if needed
		if (patternsDate == null)
		{
			patternsDate = new LinkedList<Pattern>();
			for (String curStr : DATE_VALIDATOR_FORMATS)
			{
				Pattern curPattern;
				try
				{
					curPattern = Pattern.compile(curStr);
				} catch (Exception e)
				{
					// Just to be on the safe side, in case someone adds a broken string
					continue;
				}
				patternsDate.add(curPattern);
			}
		}

		for (Pattern curPattern : patternsDate)
			if (curPattern.matcher(text).matches())
				return true;

		return false;
	}

	public static String getCurrentDateAndTimeAsSortableString()
	{
		return getDateAndTimeAsSortableString(new Date());
	}

	public static String getDateAndTimeAsSortableString(Date dateTime)
	{
		if (dateTime == null)
			return null;

		return formatterDateTime.format(dateTime);
	}

	/**
	 * @return Format "dd-MM-yy hh:mm".
	 */
	public static String getDateAndTimeAsShortString(Date dateTime)
	{
		if (dateTime == null)
			return null;

		return formatterDateTimeShort.format(dateTime);
	}

	/**
	 * 
	 * @param dateTimeString
	 *            Input date time string in the format "yyyy-MM-dd.HH-mm-ss".
	 * @return Valid date, or null on error.
	 */
	public static Date parseDateAndTime(String dateTimeString)
	{
		if (dateTimeString == null)
			return null;

		Date result;
		try
		{
			result = formatterDateTime.parse(dateTimeString);
		} catch (ParseException e)
		{
			return parseDate(dateTimeString);
		}
		return result;
	}

	/**
	 * 
	 * @param dateTimeString
	 *            Input date time string in the format "yyyy-MM-dd.HH-mm-ss".
	 * @return Valid date, or null on error.
	 */
	public static Date parseDate(String dateTimeString)
	{
		if (dateTimeString == null)
			return null;

		Date result;
		try
		{
			result = formatterDate.parse(dateTimeString);
		} catch (ParseException e)
		{
			return null;
		}
		return result;
	}

	public static String getHumanReadableMillis(long millis)
	{
		if (millis <= 0)
			return "0";

		// 10 mSec
		if (millis <= MILLIS_SECOND)
			return String.format("%,d mSec", millis);

		// Minute
		if (millis <= MILLIS_MINUTE)
			return String.format("%.1f sec", (double) millis / MILLIS_SECOND);

		// Hour
		if (millis <= MILLIS_HOUR)
			return String.format("%.1f min", (double) millis / MILLIS_MINUTE);

		// Day
		if (millis <= MILLIS_DAY)
			return String.format("%.1f hours", (double) millis / MILLIS_HOUR);

		// Year
		if (millis <= MILLIS_YEAR)
			return String.format("%.1f days", (double) millis / MILLIS_DAY);

		return String.format("%.1f years", (double) millis / MILLIS_YEAR);
	}
}
