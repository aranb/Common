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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StringUtils File - contains general static utility functions for string handling <BR>
 * <BR>
 * identText - returns a string with appeneded white spaces at asked length<BR>
 * getWordAt - returns the i-th word from a string
 */
public class StringUtils
{
	public static SimpleDateFormat			dateFormatDateTime	= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static final String[]			stopWordsArray		= { "is", "a", "of", "for", "at", "in", "to", "with",
			"de", "and", "on", "as"							};
	private static final HashSet<String>	stopWords			= new HashSet<String>(Arrays.asList(stopWordsArray));
	private static final Pattern			patternCurrency		= Pattern
																		.compile("\\$( )?[0-9]+(\\,[0-9]{3})*(\\.[0-9]([0-9])?)?");
	/**
	 * Country names, where the full names are converted to lowercase ("israel") and the 2-char ISO names are kept in
	 * uppercase ("IL").
	 */
	private static HashSet<String>			countryNames;

	/**
	 * Check if text looks like a payment in USD in the format like "$15.01", "$15.1", "$1,024", etc.
	 * 
	 * @param text
	 *            The given text, after trim. May be null.
	 * @return True if the given text looks like a payment in USD.
	 */
	public static boolean isTextCurrency(String text)
	{
		if (text == null)
			return false;

		return patternCurrency.matcher(text).matches();
	}

	/**
	 * Check if text looks like a full country name according to the ISO 3166.
	 * 
	 * @param text
	 *            The given text, after trim. May be null.
	 * @return True if the given text looks like a payment in USD.
	 */
	public static boolean isTextCountry(String text)
	{
		if (text == null)
			return false;

		// Initialize the list
		if (countryNames == null)
		{
			countryNames = new HashSet<String>();
			String[] locales = Locale.getISOCountries();
			for (String countryCode : locales)
			{
				Locale obj = new Locale("", countryCode);
				String curCountry = obj.getDisplayCountry();
				countryNames.add(curCountry.toLowerCase());
				curCountry = obj.getCountry();
				countryNames.add(curCountry);
			}
			// Specific common cases
			countryNames.add("USA");
		}

		// Check if the lower-case trimmed version is found in the list
		String trim = text.trim();
		if (countryNames.contains(trim.toLowerCase()))
			return true;
		// Now try the 2-char uppercase
		return countryNames.contains(trim);
	}

	public static boolean isTextContainsCreditCardProvider(String text)
	{
		return text.contains("Visa") || text.contains("MasterCard") || text.contains("Master Card")
				|| text.contains("Master-Card") || text.contains("AMEX") || text.contains("Amex")
				|| text.contains("American Express") || text.contains("Diners");
	}

	/**
	 * This method receives a variable size string, and returns a new string with desired length <BR>
	 * if s.length < len the function returns the string with appeneded white spaces if s.length > len function will
	 * trunc the string to desired length
	 * 
	 * @param s
	 *            - string to ident
	 * @param len
	 *            - desired length
	 * @return the string at desired length
	 */
	public static String identText(String s, int len)
	{
		StringBuffer tmp = new StringBuffer(s);
		for (int i = s.length(); i < len; ++i)
		{
			tmp.append(" ");
		}
		return tmp.toString().substring(0, len);
	}

	/**
	 * This method receives a variable size string, and returns a new string with desired length <BR>
	 * if s.length < len the function returns the string with appeneded white spaces if s.length > len function will
	 * trunc the string to desired length, if asked to (i.e. if trunc = true), otherwise - return the original string
	 * 
	 * @param s
	 *            - string to ident
	 * @param len
	 *            - desired length
	 * @param trunc
	 *            - do we truncate the string if longer than desired length or not
	 * @return the string at desired length
	 */
	public static String identText(String s, int len, boolean trunc)
	{
		if (!trunc && s.length() >= len)
		{
			return s;
		}
		return identText(s, len);
	}

	/**
	 * getLastWord: Returns the right part of a *char* separated String (anything after last *char*)
	 * 
	 * @param s
	 *            Space delimited string containing a path
	 * @param c
	 *            Delimiter
	 * @return Right part of the string
	 */
	public static String getLastWord(String s, char c)
	{
		if (s == null || s.equals(""))
			return "";
		int index = s.lastIndexOf(c);
		if (index == -1)
		{
			return s;
		}
		return s.substring(index + 1);
	}

	/**
	 * getLastWord: Returns the right part of a space seperated String (anything after last space)
	 * 
	 * @param s
	 *            - space delimited string containing a path
	 * @return Right part of the string
	 */
	public static String getLastWord(String s)
	{
		return getLastWord(s, ' ');
	}

	/**
	 * deleteExtraBlanks: Converts all places with >= 2 blanks to 1 blank space
	 * 
	 * @param s
	 *            - string to work on
	 * @return Parameter string without any sequence of 2 spaces or more
	 */
	public static String deleteExtraBlanks(String s)
	{
		if (s == null)
			return null;
		//
		// While we still have double blanks, replace them with one blank
		// (Can be done more efficiently by scanning the String, but will take
		// more code lines,
		// and will not occur much anyway, so speed doesn't matter)
		//
		while (s.indexOf("  ") != -1 && !s.trim().equals(""))
		{
			s = s.replaceAll("  ", " ");
		}

		return s;
	}

	/**
	 * getWordAt: Returns the a certain word at a *char* seperated String according to a param index
	 * 
	 * @param s
	 *            - space delimited string containing a path
	 * @param c
	 *            - delimiter
	 * @param index
	 *            - index of the word we wish to receive, 1 being the most left word
	 * @param fromStart
	 *            - do we want the index-th word from the beginning or the end
	 * @return The word at the given index (null if index invalid)
	 */
	public static String getWordAt(String s, char c, int index, boolean fromStart)
	{
		if (s == null || s.equals(""))
			return null;

		String[] tmp = s.split(String.valueOf(c));
		if (index < 1 || index > tmp.length)
			return null;

		if (!fromStart)
			index = tmp.length - index + 1;

		return tmp[index - 1];
	}

	public static String createLink(String command, String text, String param)
	{
		return "<a href='/" + command + "?key=" + param + "'>" + text + "</a>";
	}

	public static String createLink(String command, String text, String paramName, String paramVal)
	{
		return "<a href='/" + command + "?" + paramName + "=" + paramVal + "'>" + text + "</a>";
	}

	/**
	 * getLeft: Returns the left part of a space separated String (anything before last space)
	 * 
	 * @param s
	 *            - space delimited string containing a path
	 * @return Right part of the string
	 */
	public static String getLeft(String s)
	{
		return getLeft(s, ' ');
	}

	/**
	 * getLeft: Returns the left part of a *char* separated String (anything before last space)
	 * 
	 * @param s
	 *            - space delimited string containing a path
	 * @param c
	 *            - delimiter
	 * @return Right part of the string
	 */
	public static String getLeft(String s, char c)
	{
		if (s == null || s.equals(""))
			return "";
		int index = s.lastIndexOf(c);
		if (index == -1)
		{
			return "";
		}
		return s.substring(0, index);
	}

	/**
	 * getLeftFileName: Returns the left part of a char seperated String (anything before last space) or the string
	 * itself incase the delimiter doesn't exist.
	 * 
	 * @param s
	 *            - space delimited string containing a path
	 * @param c
	 *            - delimiter
	 * @return Left part of the string or the String itself ( if the delimiter doesn't exist)
	 */
	public static String getLeftFileName(String s, char c)
	{
		if (s == null || s.equals(""))
			return "";
		int index = s.lastIndexOf(c);
		if (index == -1)
		{
			return s;
		}
		return s.substring(0, index);
	}

	public static String createDateFormat(long time)
	{
		Date date = new Date(time);
		return dateFormatDateTime.format(date);
	}

	/**
	 * Returns a date format by the given format param.
	 * <p>
	 * If an error exists, the method will return null. <br>
	 * <u>Pay attention</u> - this method creats a SimpleDateFormat instance and is therefor not recommended if you use
	 * this repeatedly
	 * 
	 * @param time
	 *            - The time as long
	 * @param format
	 *            - The date format
	 * @return - The formated date or null if an error occurred
	 */
	public static String createDateFormat(long time, String format)
	{
		try
		{
			SimpleDateFormat tempDateFormatDateTime = new SimpleDateFormat(format);
			Date date = new Date(time);
			return tempDateFormatDateTime.format(date);
		} catch (RuntimeException e)
		{
			return null;
		}
	}

	/**
	 * @param strings
	 *            a colection of Strings
	 * @return the length of the longest String in the collection
	 */
	public static int getMaxLength(Collection<String> strings)
	{
		int len = 0;
		for (String string : strings)
		{
			if (string.length() > len)
			{
				len = string.length();
			}
		}
		return len;
	}

	/**
	 * Purpose: This method takes the 'fullPath' as supplied, goes over the text and removes any indication to param
	 * arg.
	 * 
	 * @param fullPath
	 * @return new fullPath without the param in it
	 */
	public static String removeOldParam(String param, String fullPath)
	{
		String newFullPath = "";

		int i;
		if ((i = fullPath.indexOf("?" + param + "=")) > 0 || (i = fullPath.indexOf("&" + param + "=")) > 0)
		{
			int j;
			newFullPath = fullPath.substring(0, i)
					+ ((j = fullPath.indexOf("&", i + 1)) > 0 ? fullPath.substring(j) : "");
		} else
		{
			newFullPath = fullPath;
		}

		//
		// fix ? or & positions
		//
		if (newFullPath.indexOf("&") > 0)
		{
			newFullPath = newFullPath.replaceAll("\\?", "&");
			newFullPath = newFullPath.replaceFirst("\\&", "?");
		}

		return newFullPath;
	}

	/**
	 * Purpose: This method takes the 'fullPath' as supplied, goes over the text and removes any indication to
	 * 'oldactions'
	 * 
	 * @param fullPath
	 * @return new fullPath without oldactions in it
	 */
	protected static String removeOldAction(String fullPath)
	{
		return removeOldParam("oldaction", fullPath);
	}

	/**
	 * Purpose: Adds a parameter to a URL depending on if it is the first parameter or additional
	 * 
	 * Note: Passing null as value will append the word "null", in order to pass an empty value, one should send an
	 * empty string ("").
	 * 
	 * @param url
	 * @param param
	 * @param value
	 * @return
	 */
	public static String addURLParam(String url, String param, Object value)
	{
		// first remove any old action
		url = removeOldAction(url);

		// now, remove any parameters as requested (might be the action that was
		// removed before)
		url = removeOldParam(param, url);

		// check if this is not the first parameter of this url
		if (url.contains("?"))
		{
			return url + "&" + param + "=" + String.valueOf(value);
		}

		return url + "?" + param + "=" + String.valueOf(value);
	}

	public static String addURLParams(String url, String[] params, Object... values)
	{
		String result = url;

		int i = 0;
		for (String param : params)
		{
			String value = "";
			if (values.length > i)
			{
				value = String.valueOf(values[i]);
			}

			result = addURLParam(result, param, value);
			i++;
		}

		return result;
	}

	/**
	 * @param str
	 *            - a string ref
	 * @return - true, if str is not null and not empty
	 */
	public static boolean isStrNotEmpty(String str)
	{
		return str != null && str.length() != 0;
	}

	/**
	 * 
	 * Purpose: Check if the text from the offset is same as the compared text
	 * 
	 * @param charArray
	 * @param text
	 *            the compared text
	 * @param offset
	 *            the position in the char buffer
	 * @return boolean true if found
	 */
	public static boolean isStringInArray(char[] charArray, String text, int offset)
	{
		for (int i = 0; i < text.length(); i++)
		{
			if (charArray[offset + i] != text.charAt(i))
				return false;
		}

		return true;
	}

	/**
	 * 
	 * Purpose: Find the position of the text in the char buffer
	 * 
	 * 
	 * @param text
	 * @param buffer
	 * @return int
	 */
	public static int getPosOf(String text, CharBuffer buffer)
	{
		char[] messageBytes = buffer.array();

		for (int i = 0; i < (buffer.position() - text.length()); i++)
		{
			if (isStringInArray(messageBytes, text, i))
			{
				return (i + text.length());
			}
		}

		return -1;
	}

	public static String reverseStr(String source)
	{
		int len = source.length();
		StringBuffer dest = new StringBuffer(len);

		for (int i = (len - 1); i >= 0; i--)
		{
			dest.append(source.charAt(i));
		}

		return dest.toString();
	}

	/**
	 * 
	 * 
	 * @param object
	 * @return String Get the name from the object, i.e. the last string after the last dot
	 */
	public static String extractNameFromClass(Object object)
	{
		if (object != null)
		{
			String name = object.getClass().toString();
			name = name.substring(name.lastIndexOf(".") + 1);

			return name;
		}
		return "";
	}

	/**
	 * 
	 * Purpose: Replace the thread name with the class name of the thread (the string after the last dot)
	 * 
	 * @param thread
	 */
	public static void setThreadName(Thread thread)
	{
		if (thread != null)
		{
			thread.setName(extractNameFromClass(thread));
		}
	}

	/**
	 * 
	 * Purpose: Extract the stream output and return it as a new string
	 * 
	 * @param stream
	 * @return String the streamed output as string
	 * @throws IOException
	 */
	public static String streamToString(InputStream stream, int maxSize) throws IOException
	{
		String result = null;
		if (stream != null)
		{
			BufferedReader bufferedReader = null;
			try
			{
				bufferedReader = new BufferedReader(new InputStreamReader(stream));
				StringBuilder stringBuilder = new StringBuilder();
				String line = null;

				while ((line = bufferedReader.readLine()) != null
						&& (maxSize == -1 || stringBuilder.length() <= maxSize))
				{
					stringBuilder.append(line + "\n");
				}

				if (maxSize == -1)
				{
					result = stringBuilder.toString();
				} else
				{
					result = stringBuilder.substring(1, maxSize);
				}
			} finally
			{
				bufferedReader.close();
			}
		}

		return result;
	}

	/**
	 * 
	 * Purpose: Extract the stream output and return it as a new string
	 * 
	 * @param stream
	 * @return String the streamed output as string
	 * @throws IOException
	 */
	public static String streamToString(InputStream stream) throws IOException
	{
		return streamToString(stream, -1);
	}

	/**
	 * @param string
	 *            String to replicate.
	 * @param repeats
	 *            Number of times (1+).
	 * @return Replicated string according to number of times. May be empty but never null.
	 */
	public static String replicate(String string, int repeats)
	{
		if (repeats <= 0 || string == null)
			return "";
		String result = "";
		for (int i = 0; i < repeats; i++)
		{
			result += string;
		}
		return result;
	}

	/**
	 * Count number of words, assuming that words are made of at least one character which is a letter or digit.
	 * 
	 * @param s
	 *            The given string.
	 * @return Number of words or zero if string is null.
	 */
	public static int countWords(String s)
	{
		// Sanity check
		if (s == null)
			return 0;

		int result = 0;

		boolean inWord = false;

		for (char curChar : s.toCharArray())
		{
			// Words are made of letters and digits
			if (curChar == ' ' || curChar == '-' || curChar == ',')
			{
				inWord = false;
				continue;
			}

			// Current character is a letter

			// Check if in word
			if (inWord)
				continue;

			inWord = true;
			result++;
		}

		return result;
	}

	/**
	 * Get words, assuming that words are made of at least one character which is a letter or digit.
	 * 
	 * @param s
	 *            The given string.
	 * @return All words. May be empty but never null.
	 */
	public static LinkedList<String> getWords(String s)
	{
		LinkedList<String> result = new LinkedList<String>();

		// Sanity check
		if (s == null)
			return result;

		String curWord = null;

		for (char curChar : s.toCharArray())
		{
			// Words are made of letters and digits
			if (curChar == ' ' || curChar == '-' || curChar == ',' || curChar == '.' || curChar == ':')
			{
				// If in word, then add it to list and reset
				if (curWord != null)
				{
					result.add(curWord);
					curWord = null;
				}
				continue;
			}

			// Current character is a letter

			// Check if in word
			if (curWord == null)
				curWord = new String();
			curWord += curChar;
		}

		if (curWord != null)
			result.add(curWord);

		return result;
	}

	public static boolean isStopWord(String word)
	{
		if (word == null)
			return false;

		return stopWords.contains(word);
	}

	public static void removeStopWords(Collection<String> words)
	{
		if (words == null || words.isEmpty())
			return;

		for (Iterator<String> it = words.iterator(); it.hasNext();)
		{
			String curWord = it.next();
			if (isStopWord(curWord) || (curWord.length() <= 1 && !"i".equals(curWord) && !"I".equals(curWord)))
				it.remove();
		}
	}

	public static String getHumanReadableBytes(long bytes)
	{
		if (bytes <= 0)
			return "0";

		if (bytes <= 1000L)
			return String.format("%,d bytes");

		if (bytes <= 1000000L)
			return String.format("%,.2f KB", (double) bytes / 1000);

		if (bytes <= 1000000000L)
			return String.format("%,.2f MB", (double) bytes / 1000000L);

		if (bytes <= 1000000000000L)
			return String.format("%,.2f GB", (double) bytes / 1000000000L);

		return String.format("%,.2f TB", (double) bytes / 1000000000000L);
	}

	/**
	 * Compare a given base string to a subset of other strings, to find the longest common prefix.
	 * 
	 * @param baseString
	 *            The base string to compare with all others.
	 * @param others
	 *            The list of other strings.
	 * @param minOthers
	 *            The number of "best" others to compare with the base string. If zero, the result will be the length of
	 *            the base string.
	 * @return The longest common prefix found between the base string and a minimal number of other strings that are
	 *         most similar to the base.
	 */
	public static int getLongestCommonPrefix(String baseString, Collection<String> others, int minOthers)
	{
		// Sanity check
		if (minOthers <= 0)
			return baseString.length();
		if (others == null || others.isEmpty() || minOthers > others.size() || minOthers <= 0)
			return 0;

		LinkedList<String> othersDup = new LinkedList<String>(others);
		for (int i = 0; i < baseString.length(); i++)
		{
			char c = baseString.charAt(i);
			int matched = 0;
			for (Iterator<String> it = othersDup.iterator(); it.hasNext();)
			{
				// Fast quit when there is no chance to get any better result
				if (othersDup.size() + matched < minOthers)
					return i;
				String curOther = it.next();
				// If the other string is too short
				if (curOther == null || curOther.length() <= i)
				{
					it.remove();
					continue;
				}
				// The other string is long enough, so compare character
				char o = curOther.charAt(i);
				if (c == o)
				{
					matched++;
				} else
				{
					it.remove();
				}
			}
			// Fast quit when there is no chance to get any better result
			if (matched < minOthers)
				return i;
		}

		return baseString.length();
	}

	/**
	 * Compare a given base string to a subset of other strings, to find the longest common prefix for all.
	 * 
	 * @param baseString
	 *            The base string to compare with all others.
	 * @param others
	 *            The list of other strings. Null others are ignored.
	 * @param terminators
	 *            Optional - can be null or empty (ignored). The result will be shorter than the actual common prefix if
	 *            the base string is longer than the common part, and the common part does not end with this terminator
	 *            (or the character after it).
	 * @return The longest common prefix found between the base string and all others. Returns 0 if no others are given
	 *         (other that is null is just ignored).
	 */
	public static int getLongestCommonPrefixWithTerminators(String baseString, Collection<String> others,
			String terminators)
	{
		if (others == null || others.isEmpty())
			return 0;

		int result;
		boolean mismatch = false;

		for (result = 0; result < baseString.length(); result++)
		{
			char c = baseString.charAt(result);
			for (String curOther : others)
			{
				// Ignore null others
				if (curOther == null)
					continue;
				// If the other string is too short
				if (curOther.length() <= result)
				{
					mismatch = true;
					break;
				}
				// The other string is long enough, so compare character
				char o = curOther.charAt(result);
				if (c != o)
				{
					mismatch = true;
					break;
				}
			}
			// Check if need to return
			if (mismatch)
				break;
		}

		// Check if need to retreat now to terminator limits
		if (result == 0 || terminators == null || terminators.isEmpty() || result == baseString.length())
			return result;

		//
		// Look for a terminator, starting at the character after the common part
		//
		for (int i = result; i >= 0; i--)
		{
			char c = baseString.charAt(i);
			if (terminators.contains("" + c))
				return i == result ? result : i + 1;
		}

		return 0;
	}

	/**
	 * Compare a given base string to a subset of other strings, to find the longest common suffix for all.
	 * 
	 * @param baseString
	 *            The base string to compare with all others.
	 * @param others
	 *            The list of other strings. Null others are ignored.
	 * @param terminators
	 *            Optional - can be null or empty (ignored). The result will be shorter than the actual common suffix if
	 *            the base string is longer than the common part, and the common part does not start with this
	 *            terminator (or the character before it).
	 * @return The longest common suffix found between the base string and all others. Returns 0 if no others are given
	 *         (other that is null is just ignored).
	 */
	public static int getLongestCommonSuffixWithTerminators(String baseString, Collection<String> others,
			String terminators)
	{
		if (others == null || others.isEmpty())
			return 0;

		int result;
		boolean mismatch = false;

		for (result = 0; result < baseString.length(); result++)
		{
			char c = baseString.charAt(baseString.length() - 1 - result);
			for (String curOther : others)
			{
				// Ignore null others
				if (curOther == null)
					continue;
				// If the other string is too short
				if (curOther.length() <= result)
				{
					mismatch = true;
					break;
				}
				// The other string is long enough, so compare character
				char o = curOther.charAt(curOther.length() - 1 - result);
				if (c != o)
				{
					mismatch = true;
					break;
				}
			}
			// Check if need to return
			if (mismatch)
				break;
		}

		// Check if need to retreat now to terminator limits
		if (result == 0 || terminators == null || terminators.isEmpty() || result == baseString.length())
			return result;

		//
		// Look for a terminator, starting at the character after the common part
		//
		for (int i = result; i >= 0; i--)
		{
			char c = baseString.charAt(baseString.length() - 1 - i);
			if (terminators.contains("" + c))
				return i == result ? result : i + 1;
		}

		return 0;
	}

	/**
	 * Compare a given base string to a subset of other strings, to find the longest common suffix.
	 * 
	 * @param baseString
	 *            The base string to compare with all others.
	 * @param others
	 *            The list of other strings.
	 * @param minOthers
	 *            The number of "best" others to compare with the base string. If zero, the result will be the length of
	 *            the base string.
	 * @return The longest common prefix found between the base string and a minimal number of other strings that are
	 *         most similar to the base.
	 */
	public static int getLongestCommonSuffix(String baseString, Collection<String> others, int minOthers, int maxResult)
	{
		// Sanity check
		if (minOthers <= 0)
			return baseString.length();
		if (others == null || others.isEmpty() || minOthers > others.size() || minOthers <= 0 || maxResult <= 0)
			return 0;

		LinkedList<String> othersDup = new LinkedList<String>(others);
		for (int i = 0; i < baseString.length(); i++)
		{
			// Enough?
			if (i >= maxResult)
				return i;
			// Start comparing with others
			char c = baseString.charAt(baseString.length() - i - 1);
			int matched = 0;
			for (Iterator<String> it = othersDup.iterator(); it.hasNext();)
			{
				// Fast quit when there is no chance to get any better result
				if (othersDup.size() - 1 + matched < minOthers)
					return i;
				String curOther = it.next();
				// If the other string is too short
				if (curOther == null || curOther.length() <= i)
				{
					it.remove();
					continue;
				}
				// The other string is long enough, so compare character
				char o = curOther.charAt(curOther.length() - 1 - i);
				if (c == o)
				{
					matched++;
				} else
				{
					it.remove();
				}
			}
			// Fast quit when there is no chance to get any better result
			if (matched < minOthers)
				return i;
		}

		return baseString.length();
	}

	/**
	 * Tells if a string contains at least one character or digits. Supports unicode.
	 * 
	 * @param baseString
	 *            The string to examine. Can be null or empty.
	 * @return True if there is at least one character or digit in the string.
	 */
	public static boolean containsAlphaOrDigit(String baseString)
	{
		if (baseString == null)
			return false;
		for (int i = baseString.length() - 1; i >= 0; i--)
		{
			char c = baseString.charAt(i);
			if (Character.isLetterOrDigit(c))
				return true;
		}
		return false;
	}

	/**
	 * @param emailAddress
	 *            Given email address with the host part.
	 * @param possibleName
	 *            Any string. Can be null or empty (then the result is false).
	 * @return True if the email address's left part is at least 3 characters long, and the given name has at least one
	 *         token (at least 3 characters long) that is contained in the email address.
	 */
	public static boolean emailMatchesName(String emailAddress, String possibleName)
	{
		if (emailAddress == null || possibleName == null)
			return false;

		String split[] = emailAddress.split("@");
		if (split.length != 2)
			return false;

		emailAddress = split[0].toLowerCase().trim();
		if (emailAddress.length() <= 2)
			return false;

		split = possibleName.trim().split("[ ,.-]");
		for (String curString : split)
		{
			if (curString.length() <= 2)
				continue;
			if (emailAddress.contains(curString.toLowerCase()))
				return true;
		}
		return false;
	}
}
