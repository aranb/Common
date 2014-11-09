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
import java.util.regex.Matcher;
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
	public static final String				DATETIME_FORMAT_SORTABLE		= "yyyy-MM-dd.HH-mm-ss";
	public static final String				DATE_FORMAT_SORTABLE			= "yyyy-MM-dd";
	private static final SimpleDateFormat	formatterDateTime				= new SimpleDateFormat(
																					DATETIME_FORMAT_SORTABLE);
	private static final SimpleDateFormat	formatterDate					= new SimpleDateFormat(DATE_FORMAT_SORTABLE);
	private static final DateFormat			formatterDateTimeShort			= DateFormat.getDateTimeInstance(
																					DateFormat.SHORT,
																					DateFormat.MEDIUM, Locale.UK);

	private static final long				MILLIS_SECOND					= 1000L;
	private static final long				MILLIS_MINUTE					= 60L * MILLIS_SECOND;
	private static final long				MILLIS_HOUR						= 60L * MILLIS_MINUTE;
	private static final long				MILLIS_DAY						= 24L * MILLIS_HOUR;
	private static final long				MILLIS_YEAR						= 365L * MILLIS_DAY;

	/**
	 * Day names for replace/masking.
	 */
	private static final String				DATE_REPLACE_DAY_NAME_NO_EXT	= "(?:Sun|Mon|Tues|Wednes|Thurs|Fri|Satur)";
	/**
	 * Date formats for fast inaccurate validation. Examples: "Friday, April 4, 2014".
	 */
	private static final String				DATE_VALIDATOR_DAY_NAME			= "(?:(?:Sun|Mon|Fri)(?:day)?|Tue(?:sday)?|Wed(?:nesday)?|Thu(?:rsday)?|Sat(?:urday)?)";
	private static final String				DATE_VALIDATOR_MONTH_NAME		= "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(?:Nov|Dec)(?:ember)?)";
	private static final String				DATE_VALIDATOR_MONTH_NUMBER		= "(?:0[1-9]|1[012])";
	private static final String				DATE_VALIDATOR_YEAR				= "(?:20|19)?\\d\\d";
	private static final String				DATE_VALIDATOR_YEAR_LONG		= "(?:20|19)\\d\\d";
	private static final String				DATE_VALIDATOR_SEP1				= "(?:[ /\\-\\.]|, )";
	private static final String				DATE_VALIDATOR_SEP2				= "[ ,./\\-]";
	private static final String				DATE_VALIDATOR_DAY_OF_MONTH		= "(?:(?:[012])?\\d|3[01])";
	private static final String				DATE_VALIDATOR_TIMEZONE			= "(?: [A-Z]{1-5})?";
	/**
	 * Short month names, to make relatively fast search in a string and determine if there is a potential for a full
	 * date there.
	 */
	private static final String[]			DATE_MONTH_NAME_ABBR			= { "Jan", "Feb", "Mar", "Apr", "May",
			"Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"				};
	// private static final String[] DATE_VALIDATOR_FORMATS = {
	// "[0-9][0-9][\\-/][0-9][0-9][\\-/](?:20|19)[0-9][0-9]",
	// "(?:20|19)[0-9][0-9][\\-/][0-9][0-9][\\-/][0-9][0-9]",
	// "\\b" + DATE_VALIDATOR_DAY_NAME + "[, \\-]+\\b" + DATE_VALIDATOR_MONTH_NAME
	// + "[, \\-]+\\b[1-9](?:[0-9])?(?:[, ]+20[0-9][0-9])?",
	// "\\b" + DATE_VALIDATOR_MONTH_NAME + "[, \\-][0-3][0-9][, \\-]" + DATE_VALIDATOR_YEAR};
	private static final String[]			DATE_VALIDATOR_FORMATS			= {
			/** With day name. **/
			DATE_VALIDATOR_DAY_NAME + DATE_VALIDATOR_SEP1 + DATE_VALIDATOR_MONTH_NAME + DATE_VALIDATOR_SEP1
					+ DATE_VALIDATOR_DAY_OF_MONTH + DATE_VALIDATOR_SEP1 + DATE_VALIDATOR_YEAR,
			DATE_VALIDATOR_MONTH_NAME + DATE_VALIDATOR_SEP1 + DATE_VALIDATOR_DAY_OF_MONTH + DATE_VALIDATOR_SEP1
					+ DATE_VALIDATOR_YEAR,
			/** Without year, with day name. **/
			DATE_VALIDATOR_DAY_NAME + DATE_VALIDATOR_SEP1 + DATE_VALIDATOR_MONTH_NAME + DATE_VALIDATOR_SEP1
					+ DATE_VALIDATOR_DAY_OF_MONTH, /** Without year. **/
			DATE_VALIDATOR_MONTH_NAME + DATE_VALIDATOR_SEP1 + DATE_VALIDATOR_DAY_OF_MONTH };
	private static LinkedList<Pattern>		patternsDateExact;
	private static LinkedList<Pattern>		patternsDateContains;
	private static final Pattern			patternTimeContains				= Pattern
																					.compile(
																							".*\\b((?:[1-9]|1[0-9]|2[0-3]):[0-5][0-9])\\b.*",
																							Pattern.DOTALL);
	private static final Pattern			patternDurationContains			= Pattern
																					.compile(
																							".*\\b([0-9](?:[0-9])?hr (?:0|[0-5][0-9])min)\\b.*",
																							Pattern.DOTALL);
	private static final Pattern			patternMonth					= Pattern.compile(".*\\b("
																					+ DATE_VALIDATOR_MONTH_NAME
																					+ ")\\b.*");
	private static final Pattern			patternDay						= Pattern.compile(".*\\b("
																					+ DATE_VALIDATOR_DAY_NAME
																					+ ")\\b.*");

	private static void initPatterns()
	{
		if (patternsDateContains != null)
			return;

		patternsDateExact = new LinkedList<Pattern>();
		patternsDateContains = new LinkedList<Pattern>();
		for (String curStr : DATE_VALIDATOR_FORMATS)
		{
			Pattern curPatternExact;
			Pattern curPatternContains;
			try
			{
				curPatternExact = Pattern.compile(curStr);
				// The flags are needed here, so .* will capture newlines
				curPatternContains = Pattern.compile(".*\\b(" + curStr + ")\\b.*", Pattern.DOTALL);
				// System.out.println(curPatternContains.pattern());
			} catch (Exception e)
			{
				// Just to be on the safe side, in case someone adds a broken string
				continue;
			}
			patternsDateExact.add(curPatternExact);
			patternsDateContains.add(curPatternContains);
		}

	}

	static void printDatePatterns()
	{
		initPatterns();
		for (Pattern curPattern : patternsDateContains)
			System.out.println(curPattern.pattern());
	}

	/**
	 * Check if a given string contains a date string.
	 * <p>
	 * Positive examples:
	 * <ul>
	 * <li>Oct-20-13 12:33:09 PDT
	 * </ul>
	 * 
	 * @param text
	 *            Given text to be examined. Can be null.
	 * @return Null if the string does not contain a date. If a match is found, it returns a matcher with at least one
	 *         group, so the caller can get the substring with {@link Matcher#group()} or with
	 *         {@link String#substring(int, int)} using start Matcher.start(1) and end at Matcher.end(1).
	 */
	public static Matcher containsDate(String text)
	{
		if (text == null)
			return null;

		initPatterns();

		for (Pattern curPattern : patternsDateContains)
		{
			Matcher matcher = curPattern.matcher(text);
			if (matcher.matches())
				return matcher;
		}

		return null;
	}

	public static Matcher containsTime(String text)
	{
		if (text == null)
			return null;

		Matcher matcher = patternTimeContains.matcher(text);
		if (matcher.matches())
			return matcher;

		return null;
	}

	public static Matcher containsDuration(String text)
	{
		if (text == null)
			return null;

		Matcher matcher = patternDurationContains.matcher(text);
		if (matcher.matches())
			return matcher;

		return null;
	}

	/**
	 * Fast search for short month name, in an inaccurate manner but something that runs relatively fast and can be used
	 * before calling the regular expression methods, like {@link DateUtils#containsDate(String)}.
	 * 
	 * @param text
	 *            Given text to be examined. Can be null.
	 * @return True if the string contains the short form of a month name.
	 */
	public static boolean containsMonthNameShort(String text)
	{
		if (text == null || text.length() < 3)
			return false;

		for (String curMonth : DATE_MONTH_NAME_ABBR)
			if (text.contains(curMonth))
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

	/**
	 * Mask date string (exact) by moving the date to 01-Jan-2011 in the same format given.
	 * 
	 * @param dateStr
	 *            Given date string that has at least the month name in short (Jan) or long (January) format.
	 * @return Date string converted to 01-Jan-2011 in the same format given. If month name is not detected, then the
	 *         same string given is returned.
	 */
	public static String maskDate(String dateStr)
	{
		String result;

		//
		// Month name
		//
		Matcher matcher = patternMonth.matcher(dateStr);
		if (matcher.matches())
		{
			int len = matcher.end(1) - matcher.start(1);
			result = len == 3 ? "Jan" : "January";
			if (matcher.start(1) > 0)
				result = dateStr.substring(0, matcher.start(1)) + result;
			if (matcher.end(1) < dateStr.length())
				result += dateStr.substring(matcher.end(1));
		} else
		{
			result = dateStr;
		}

		//
		// Day name
		//
		matcher = patternDay.matcher(result);
		if (matcher.matches())
		{
			int len = matcher.end(1) - matcher.start(1);
			result = (matcher.start(1) == 0 ? "" : result.substring(0, matcher.start(1)))
					+ (len == 3 ? "Sun" : "Sunday")
					+ (matcher.end(1) == result.length() ? "" : result.substring(matcher.end(1)));
		}

		// Replace digits, and then restore the year (if found)
		return result.replaceAll("\\d", "1").replaceFirst("1111", "2011");
	}
}
