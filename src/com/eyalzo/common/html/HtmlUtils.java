package com.eyalzo.common.html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils
{
	private static final Pattern	patternHtmlTag	= Pattern.compile("<\\s*([a-zA-Z]+).*");

	/**
	 * Find the "ltrim()" inverse length, which is the length of the white spaces and "&nbsp;" characters found in
	 * prefix.
	 * 
	 * @param text
	 *            The given HTML text. Can be null.
	 * @return The number of subsequent spaces from the left until the first symbol.
	 */
	public static int getHtmlTextSpacesPrefixLen(String text)
	{
		// Sanity check
		if (text == null)
			return 0;

		int len = text.length();
		for (int i = 0; i < len;)
		{
			char c = text.charAt(i);
			if (Character.isWhitespace(c))
			{
				i++;
				continue;
			}
			// Looking for "&nbsp;"
			if (c == '&')
			{
				// If it's towards the end, it can't be
				if (i > (len - 6))
					return i;
				if (text.charAt(i + 1) == 'n' && text.charAt(i + 2) == 'b' && text.charAt(i + 3) == 's'
						&& text.charAt(i + 4) == 'p' && text.charAt(i + 5) == ';')
				{
					i += 6;
					continue;
				}
			}
			// Not a space, so quit now
			return i;
		}
		// Found only spaces
		return len;
	}

	/**
	 * Find the "rtrim()" inverse length, which is the length of the white spaces and "&nbsp;" characters found in
	 * suffix.
	 * 
	 * @param text
	 *            The given HTML text. Can be null.
	 * @return The number of subsequent spaces from the right backwards until the first symbol.
	 */
	public static int getHtmlTextSpacesSuffixLen(String text)
	{
		// Sanity check
		if (text == null)
			return 0;

		int len = text.length();
		for (int i = len - 1; i >= 0;)
		{
			char c = text.charAt(i);
			if (Character.isWhitespace(c))
			{
				i--;
				continue;
			}
			// Looking for "&nbsp;"
			if (c == ';')
			{
				// If it's towards the end, it can't be
				if (i < 5)
					return i;
				if (text.charAt(i - 5) == '&' && text.charAt(i - 4) == 'n' && text.charAt(i - 3) == 'b'
						&& text.charAt(i - 2) == 's' && text.charAt(i - 1) == 'p')
				{
					i -= 6;
					continue;
				}
			}
			// Not a space, so quit now
			return len - i - 1;
		}
		// Found only spaces
		return len;
	}

	public static String getHtmlTagName(String htmlText)
	{
		if (htmlText == null)
			return null;

		Matcher matcher = patternHtmlTag.matcher(htmlText);
		if (!matcher.matches())
			return null;
		String result = matcher.group(1);
		return result.toLowerCase();
	}

	/**
	 * Get text or HTML tag converted to HTML displayable string, where special symbols like &lt; are converted to their
	 * safe form (as &amp;lt;). Also converts the newlines to visible two characters "\n".
	 * 
	 * @return Display safe HTML representation of the text.
	 */
	public static String getTextAsHtmlDisplayable(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;")
				.replace("\n", "\\n").replace("\"", "&quot;");
	}
}
