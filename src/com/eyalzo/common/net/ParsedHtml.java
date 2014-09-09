package com.eyalzo.common.net;

import java.util.LinkedList;

import com.eyalzo.common.misc.DateUtils;
import com.eyalzo.common.misc.MapCounter;
import com.eyalzo.common.misc.StringUtils;
import com.eyalzo.common.webgui.DisplayTable;

/**
 * @author Eyal Zohar
 */
public class ParsedHtml
{
	public enum HtmlPartType
	{
		/**
		 * Everything that starts with < and ends with >.
		 */
		HTML_ELEMENT, /**
		 * Not HTML, but has only white spaces, including "&nbsp;" parts (6 characters).
		 */
		TEXT_EMPTY, /**
		 * Has something meaningful, possibly just a comma or dot, but still somthing that may be worth
		 * treating later.
		 */
		TEXT_REAL, /**
		 * Looks like text, but it is actually an inline style code after a "<style ...".
		 */
		STYLE, /**
		 * Looks like text, but it is actually a script code after a "<script ...".
		 */
		SCRIPT
	}

	public enum HtmlTextType
	{
		GENERAL, DATE, CURRENCY, CITY, COUNTRY, ADDRESS, NAME, PRODUCT;
	}

	/**
	 * The basic parts of an HTML, separated so that each text and tag are separated from each other.
	 */
	public class HtmlPart
	{
		public String		text;
		public HtmlPartType	type;

		/**
		 * Constructor that determines the basic type (not script or style yet).
		 * 
		 * @param text
		 *            The text to store, may be an HTML etc.
		 */
		public HtmlPart(String text)
		{
			this.text = text;
			// Determine the basic type, not handling the style/script case for now
			if (text.startsWith("<"))
				type = HtmlPartType.HTML_ELEMENT;
			// Check if it is only whitespaces or &nbsp;
			else if (text.toLowerCase().replace("&nbsp;", " ").trim().isEmpty())
				type = HtmlPartType.TEXT_EMPTY;
			else
				type = HtmlPartType.TEXT_REAL;
		}

		public HtmlPart(HtmlPart o)
		{
			this.text = new String(o.text);
			this.type = o.type;
		}

		/**
		 * Get text or HTML tag converted to HTML displayable string, where special symbols like &lt; are converted to
		 * their safe form (as &amp;lt;). Also converts the newlines to visiable two characters "\n".
		 * 
		 * @return Display safe HTML representation of the text.
		 */
		public String getTextAsHtmlDisplayable()
		{
			return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;")
					.replace("\n", "\\n");
		}

		/**
		 * Get text or HTML tag converted to HTML displayable string, where special symbols like &lt; are converted to
		 * their safe form (as &amp;lt;). Also converts the newlines to visiable two characters "\n".
		 * 
		 * @return Display safe HTML representation of the text.
		 */
		String getTextAsHtmlDisplayable(int beginIndex, int endIndex)
		{
			return text.substring(beginIndex, endIndex).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
					.replace(" ", "&nbsp;").replace("\n", "\\n");
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == null || !(o instanceof HtmlPart))
				return false;

			HtmlPart oHtmlPart = (HtmlPart) o;

			return this.type == oHtmlPart.type
					&& (this.text == null && oHtmlPart.text == null || this.text != null
							&& this.text.equals(oHtmlPart.text));
		}
	}

	public class HtmlText
	{
		public String		text;
		public HtmlTextType	type;
		/**
		 * 1-based index of the first real text.
		 */
		public int			firstPartIndex;
		/**
		 * 1-based index of the last real text.
		 */
		public int			lastPartIndex;

		public HtmlText(String text)
		{
			this.text = text;
		}
	}

	/**
	 * Total length in bytes of the original byte stream.
	 */
	public final int					length;

	//
	// HTML parts
	//
	/**
	 * The HTML parts, basically separated by types of {@link HtmlPart}.
	 */
	public LinkedList<HtmlPart>			parts;
	/**
	 * Counter per type of HTML part.
	 */
	private MapCounter<HtmlPartType>	statPartsCount	= new MapCounter<ParsedHtml.HtmlPartType>();
	public long							statHtmlParsingNano;

	/**
	 * Duplication of the HTML parts, for processing and output.
	 */
	private LinkedList<HtmlPart>		dupParts;
	/**
	 * True when the dup exists and it was processed to remove images so the style should be modified when
	 * {@link #getBody()} is called.
	 */
	private boolean						dupRemovedImagesSource;
	/**
	 * True when the dup exists and it was processed to remove images so the style should be modified when
	 * {@link #getBody()} is called.
	 */
	private boolean						dupRemovedImagesAltText;

	//
	// Text parts
	//
	/**
	 * Significant text parts, all started as {@link HtmlPartType#TEXT_REAL} and were aggregated if possible, and then
	 * filtered if they ended up being too short or having only symbols.
	 */
	private LinkedList<HtmlText>		textProcessedParts;
	public long							statTextCombiningNano;

	public ParsedHtml(byte[] bodyBytes)
	{
		long before = System.nanoTime();

		length = bodyBytes.length;
		parts = new LinkedList<ParsedHtml.HtmlPart>();

		// Offset and state
		boolean stateHtml = false;
		int startOffset = 0;
		for (int offset = 0; offset < length; offset++)
		{
			byte curChar = bodyBytes[offset];
			if (stateHtml)
			{
				if (curChar == '>')
				{
					int len = offset - startOffset + 1;
					if (len > 0)
					{
						String curPart = new String(bodyBytes, startOffset, len);
						parts.add(new HtmlPart(curPart));
					}
					stateHtml = false;
					startOffset = offset + 1;
				}
				continue;
			}

			// Is non-HTML
			if (curChar == '<')
			{
				int len = offset - startOffset;
				if (len > 0)
				{
					String curPart = new String(bodyBytes, startOffset, len);
					parts.add(new HtmlPart(curPart));
				}
				stateHtml = true;
				startOffset = offset;
			}
		}

		// Go over the parts and mark the styles and scripts
		handleStylesAndScripts();

		statHtmlParsingNano = System.nanoTime() - before;

		// Update stats
		updateStats();
	}

	private void verifyPartsDup()
	{
		if (dupParts != null)
			return;

		dupParts = new LinkedList<ParsedHtml.HtmlPart>();
		for (HtmlPart curPart : parts)
			dupParts.add(new HtmlPart(curPart));
	}

	public void clearPartsDup()
	{
		dupParts = null;
		dupRemovedImagesSource = false;
		dupRemovedImagesAltText = false;
	}

	private void updateStats()
	{
		statPartsCount.clear();

		for (HtmlPart curPart : parts)
		{
			statPartsCount.inc(curPart.type);
		}
	}

	/**
	 * Mark text parts that are actually script or inline style, because these are not really texts.
	 * <p>
	 * The basic assumption is that we first see a (case insensitive) "<style " or "<script " and then the next
	 * non-empty text is marked as such. It does not look for more text blocks nor looks for the closing tag.
	 */
	private void handleStylesAndScripts()
	{
		boolean isStyle = false;
		boolean isScript = false;
		for (HtmlPart curPart : parts)
		{
			// Check if in style or script
			if (curPart.type == HtmlPartType.TEXT_REAL)
			{
				// Style
				if (isStyle)
				{
					curPart.type = HtmlPartType.STYLE;
					isStyle = false;
					continue;
				}
				// Script
				if (isScript)
				{
					curPart.type = HtmlPartType.SCRIPT;
					isScript = false;
					continue;
				}
			}

			// Turn off flags
			isScript = false;
			isStyle = false;

			if (curPart.type == HtmlPartType.HTML_ELEMENT)
			{
				String textStr = curPart.text.toLowerCase();
				if (textStr.startsWith("<style"))
					isStyle = true;
				else if (textStr.startsWith("<script"))
					isScript = true;
			}
		}
	}

	/**
	 * @return HTML body for display as readable HTML, where the body is being colored as follows: 1. HTML code, 2. Real
	 *         text, 3. Other ("text" that contains only white space).
	 */
	public String getBodyAsHtmlTextColored(int widthPixels)
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("<style type=\"text/css\">\n");
		buffer.append("div{ width: ");
		buffer.append(widthPixels);
		buffer.append("px; word-break: break-all; }\n");
		buffer.append(".html     {font-size: 8pt; font-family: courier; background-color: lightgrey; border:1px dotted black;}\n");
		buffer.append(".nonhtml     {font-size: 8pt; font-family: courier; background-color: white; border:1px dotted black;}\n");
		buffer.append(".text {font-size: 8pt; font-family: courier; background-color: yellow; border:1px dotted black; }\n");
		buffer.append(".style {font-size: 8pt; font-family: courier; background-color: darkgrey; border:1px dotted black; }\n");
		buffer.append(".script {font-size: 8pt; font-family: courier; background-color: pink; border:1px dotted black; }\n");
		buffer.append("</style>\n");
		buffer.append("\n\n<!-- body with colored text parts, start-->\n<div><span class=\"body\">\n");

		for (HtmlPart curPart : parts)
		{
			if (curPart.type == HtmlPartType.HTML_ELEMENT)
			{
				buffer.append(curPart.getTextAsHtmlDisplayable());
				continue;
			}

			// Is non-HTML, maybe even actual text
			String wsSuffix = null;
			if (curPart.type == HtmlPartType.TEXT_REAL)
			{
				// Find length of prefix and suffix of white spaces (if)
				int wsPreLen = HtmlUtils.getHtmlTextSpacesPrefixLen(curPart.text);
				int wsSufLen = HtmlUtils.getHtmlTextSpacesSuffixLen(curPart.text);
				if (wsPreLen + wsSufLen >= curPart.text.length())
					continue;

				if (wsPreLen > 0)
				{
					// Output the ws prefix with a different color
					String wsPrefix = curPart.text.substring(0, wsPreLen);
					buffer.append("<span class=\"nonhtml\">");
					// Output the ws string, handling the "&nbsp;" case
					buffer.append(wsPrefix.replace("&", "&amp;").replace("\n", "\\n"));
					buffer.append("</span>");
				}

				// Remove the prefix and/or suffix
				buffer.append("<span class=\"text\">");
				// Output the ws string, handling the "&nbsp;" case
				buffer.append(curPart.getTextAsHtmlDisplayable(wsPreLen, curPart.text.length() - wsSufLen));
				buffer.append("</span>");

				// Suffix
				if (wsSufLen > 0)
				{
					// Output the ws prefix with a different color
					wsSuffix = curPart.text.substring(curPart.text.length() - wsSufLen, curPart.text.length());
					buffer.append("<span class=\"nonhtml\">");
					// Output the ws string, handling the "&nbsp;" case
					buffer.append(wsSuffix.replace("&", "&amp;").replace("\n", "\\n"));
					buffer.append("</span>");
				}
				continue;
			}

			buffer.append("<span class=\""
					+ (curPart.type == HtmlPartType.STYLE ? "style" : curPart.type == HtmlPartType.SCRIPT ? "script"
							: "nonhtml") + "\">");
			buffer.append(curPart.getTextAsHtmlDisplayable());
			buffer.append("</span>");
		}

		buffer.append("</span></div>\n<!-- body with colored text parts, end-->\n\n");

		return buffer.toString();
	}

	/**
	 * Generate the consolidated text parts, based on smart processing. Can be called multiple times because it checks
	 * if this was already done before.
	 */
	private void generateTextProcessedParts()
	{
		if (textProcessedParts != null)
			return;

		long before = System.nanoTime();

		textProcessedParts = new LinkedList<ParsedHtml.HtmlText>();

		// Try to merge
		HtmlText curText = null;
		int curPartIndex = 0;
		for (HtmlPart curPart : parts)
		{
			curPartIndex++;
			// Text - collect the text and HTML elements between
			if (curPart.type == HtmlPartType.TEXT_REAL)
			{
				if (curText == null)
				{
					String temp = curPart.text.replace("&nbsp;", " ").trim();
					curText = new HtmlText(temp);
					curText.firstPartIndex = curPartIndex;
					curText.lastPartIndex = curPartIndex;
				} else
				{
					curText.lastPartIndex = curPartIndex;
					// Add a delimiter, that is needed because of the trims
					curText.text += " ";
					curText.text += curPart.text.replace("&nbsp;", " ").trim();
				}
				continue;
			}

			if (curPart.type == HtmlPartType.HTML_ELEMENT)
			{
				String tagName = HtmlUtils.getHtmlTagName(curPart.text);
				if (tagName != null && curText != null)
				{
					// System.out.println(tagName);
					// Regarding the (rare) "address" tag: Most browsers will add a line break before and after the
					// address element.
					if (tagName.equals("br") || tagName.equals("table") || tagName.equals("tr") || tagName.equals("td")
							|| tagName.equals("p") || tagName.equals("address"))
					{
						textProcessedParts.add(curText);
						curText = null;
					}
				}
			}
		}

		// Last one?
		if (curText != null)
			textProcessedParts.add(curText);

		statTextCombiningNano = System.nanoTime() - before;
	}

	/**
	 * @return Significant text parts, all started as {@link HtmlPartType#TEXT_REAL} and were aggregated if possible,
	 *         and then filtered if they ended up being too short or having only symbols.
	 */
	public LinkedList<HtmlText> getTextProcessedParts()
	{
		generateTextProcessedParts();
		return textProcessedParts;
	}

	/**
	 * @return True if the given text was found in the combined {@link HtmlPartType#TEXT_REAL} parts that were processed
	 *         and merged to sentences.
	 */
	public boolean containsTextProcessed(String text)
	{
		generateTextProcessedParts();

		for (HtmlText curText : textProcessedParts)
			if (curText.text.equals(text))
				return true;

		return false;
	}

	/**
	 * @return Number of HTML elements, which is basically the number of times the parser found a "<...>" in the text.
	 * @see #getStats()
	 */
	public long getStatHtmlElementCount()
	{
		return statPartsCount.get(HtmlPartType.HTML_ELEMENT);
	}

	/**
	 * @return Number of real text (non-empty) parts, which is basically the number of times the parser found a
	 *         non-empty text between HTML elements.
	 * @see #getStats()
	 */
	public long getStatTextRealCount()
	{
		return statPartsCount.get(HtmlPartType.TEXT_REAL);
	}

	/**
	 * @return The complete stats of parts found when parsed the HTML, after some initial processing of style and
	 *         script.
	 * @see #getStatHtmlElementCount()
	 */
	public MapCounter<HtmlPartType> getStats()
	{
		return statPartsCount;
	}

	public DisplayTable webGuiTextProcessed(String link)
	{
		generateTextProcessedParts();

		DisplayTable table = new DisplayTable();

		table.addCol("#", "Sequence of merged text part", true);
		table.addCol("Currency?", "Mark '+' if looks like curreny (USD)", false);
		table.addCol("Date?", "Mark '+' if looks like a date, even if illegal", false);
		table.addCol("Country?", "Mark '+' if looks like a full country name", false);
		table.addCol("From", "Start 1-based index in the full list of HTML parts");
		table.addCol("To", "End 1-based index in the full list of HTML parts");
		table.addCol("Text", "The text itself");

		int i = 0;
		for (HtmlText curText : textProcessedParts)
		{
			table.addRow(null);
			i++;
			table.addCell(i, link + i);
			table.addCell(StringUtils.isTextCurrency(curText.text) ? "+" : null);
			table.addCell(DateUtils.isDate(curText.text) ? "+" : null);
			table.addCell(StringUtils.isTextCountry(curText.text) ? "+" : null);
			table.addCell(curText.firstPartIndex);
			table.addCell(curText.lastPartIndex);
			table.addCell("<div style=\"word-break:break-all;\">" + curText.text + "</div>", null);
		}

		return table;
	}

	public DisplayTable webGuiParts(int fromIndex, int toIndex, String link)
	{
		if (toIndex <= 0)
			toIndex = parts.size();

		DisplayTable table = new DisplayTable();

		table.addCol("#", "Sequence of part", true);
		table.addCol("Type", "Type of part");
		table.addCol("Text", "The text as found in the HTML");

		int i = 0;
		for (HtmlPart curPart : parts)
		{
			// Index
			i++;
			if (i < fromIndex)
				continue;
			if (i > toIndex)
				break;

			table.addRow(null);
			table.addCell(i, link + i);
			table.addCell(curPart.type);
			table.addCell("<div style=\"word-break:break-all;\">"
					+ (curPart.type == HtmlPartType.HTML_ELEMENT ? curPart.getTextAsHtmlDisplayable() : curPart.text)
					+ "</div>", null);
		}

		return table;
	}

	/**
	 * 
	 * @param index
	 *            1-based index of the part to start scanning before it.
	 * @param type
	 *            The type of the searched part.
	 * @return Zero if not found, or the 1-based index if found.
	 */
	public int getPartIndexBefore(int index, HtmlPartType type)
	{
		for (int i = index - 2; i >= 0; i--)
		{
			HtmlPart curPart = parts.get(i);
			if (curPart.type == type)
				return i + 1;
		}
		return 0;
	}

	/**
	 * 
	 * @param index
	 *            1-based index of the part to start scanning after it.
	 * @param type
	 *            The type of the searched part.
	 * @return Zero if not found, or the 1-based index if found.
	 */
	public int getPartIndexAfter(int index, HtmlPartType type)
	{
		for (int i = index; i < parts.size(); i++)
		{
			HtmlPart curPart = parts.get(i);
			if (curPart.type == type)
				return i + 1;
		}
		return 0;
	}

	/**
	 * @param oParsedHtml
	 *            The other parsed HTML to try to match to this.
	 * @param minAnchorTextLen
	 *            The minimal length of a text or HTML tag to be considered as an anchor.
	 * @param maxOffset
	 *            The maximal forward offset in number of parts that we will consider for anchor.
	 * @return List of offsets to anchors in the other list of parts, containing number of parts for strong anchors and
	 *         nulls for empty, short text/HTML or text that do not fully match. Number of elements in the returned list
	 *         would always be exactly the number of parts in this parsed HTML.
	 */
	public LinkedList<Integer> compareGetPartsOffsets(ParsedHtml oParsedHtml, int minAnchorTextLen, int maxOffset)
	{
		LinkedList<Integer> result = new LinkedList<Integer>();

		// Index of the other, taking offset into account already
		int oIndex = 0;
		boolean isStrongSync = false;
		// Parts of the other, for code readability
		LinkedList<HtmlPart> oParts = oParsedHtml.parts;

		for (int index = 0; index < parts.size(); index++)
		{
			// If the other is shorter or exhausted, then there is nothing to say about the match
			if (oIndex >= oParts.size())
			{
				result.add(null);
				continue;
			}

			HtmlPart curPart = parts.get(index);

			// Do not use empty text or short text/HTML as an anchor
			if (curPart.type == HtmlPartType.TEXT_EMPTY || curPart.text.length() < minAnchorTextLen)
			{
				// If still in sync because of former strong anchor, then try to continue
				if (isStrongSync)
				{
					HtmlPart oCurPart = oParts.get(oIndex);
					if (curPart.equals(oCurPart))
					{
						result.add(oIndex - index);
						oIndex++;
						continue;
					}
				}

				isStrongSync = false;
				result.add(null);
				continue;
			}

			// We now have a part with significant text or HTML tag that we want to match

			// Get the other part
			int oMaxIndex = Math.min(oIndex + maxOffset, oParts.size() - 1);
			boolean match = false;
			for (int i = oIndex; i <= oMaxIndex; i++)
			{
				HtmlPart oCurPart = oParts.get(i);
				// On exact match
				if (curPart.equals(oCurPart))
				{
					result.add(i - index);
					isStrongSync = true;
					oIndex = i + 1;
					match = true;
					break;
				}
			}

			// If nothing match then do not move the other index
			if (!match)
			{
				result.add(null);
				isStrongSync = false;
			}
		}

		return result;
	}

	public int getHtmlTagCount(String tagName)
	{
		int result = 0;

		for (HtmlPart curPart : parts)
		{
			if (curPart.type != HtmlPartType.HTML_ELEMENT)
				continue;

			String curTagName = HtmlUtils.getHtmlTagName(curPart.text);
			if (curTagName == null)
				continue;

			if (curTagName.equals(tagName))
				result++;
		}

		return result;
	}

	/**
	 * Get the HTML body, original or processed, according to state of the internal replication that exists only if
	 * processing methods were called.
	 * 
	 * @return HTML body, original or processed, according to state of the internal replication.
	 * @see ParsedHtml#dupAnonymizeCharacters(boolean)
	 */
	public String getBody()
	{
		// Initialize the buffer with approximate size of result, so save time
		StringBuffer result = new StringBuffer(length);

		if (dupRemovedImagesAltText || dupRemovedImagesSource)
		{
			result.append("<style type=\"text/css\">\n");
			result.append("img{ " + (dupRemovedImagesAltText ? "color: red; " : "")
					+ (dupRemovedImagesSource ? "border:1px dotted red; " : "") + "}");
			result.append("</style>\n");
		}

		// Use the right part list - original or processed
		for (HtmlPart curPart : dupParts == null ? parts : dupParts)
		{
			if (curPart.text.isEmpty())
				continue;
			result.append(curPart.text);
		}

		return result.toString();
	}

	@Deprecated
	public String getBody(boolean removeImagesSource, boolean removeImagesAltText, boolean removeImagesTitle,
			boolean anonymizeLetters, boolean anonymizeDigits)
	{
		// Initialize the buffer with approximate size of result, so save time
		StringBuffer result = new StringBuffer(length);

		boolean processImages = removeImagesAltText || removeImagesSource || removeImagesTitle;
		boolean processText = anonymizeDigits || anonymizeLetters;

		//
		// Style for removed parts
		//
		if (removeImagesAltText || removeImagesSource)
		{
			result.append("<style type=\"text/css\">\n");
			result.append("img{ " + (removeImagesAltText ? "color: red; " : "")
					+ (removeImagesSource ? "border:1px dotted red; " : "") + "}");
			result.append("</style>\n");
		}

		//
		// Build the HTML while procesing
		//
		for (HtmlPart curPart : dupParts == null ? parts : dupParts)
		{
			if (curPart.text.isEmpty())
				continue;

			// Process image HTML elements
			if (processImages && curPart.type == HtmlPartType.HTML_ELEMENT)
			{
				String tagName = HtmlUtils.getHtmlTagName(curPart.text);
				if (tagName != null && tagName.equals("img"))
				{
					String processedText = curPart.text;
					if (removeImagesSource)
						processedText = processedText.replaceFirst("[ \\n]src[ \\n]*=[ \\n]*\"[^\"]+\"", " ");
					if (removeImagesAltText)
						processedText = processedText.replaceFirst("[ \\n]alt[ \\n]*=[ \\n]*\"[^\"]+\"",
								" alt=\"(alt)\"");
					if (removeImagesAltText)
						processedText = processedText.replaceFirst("[ \\n]title[ \\n]*=[ \\n]*\"[^\"]+\"",
								" title=\"(title)\"");
					result.append(processedText);
					continue;
				}
			} else if (processText && curPart.type == HtmlPartType.TEXT_REAL)
			{
				result.append(anonymizeCharacters(curPart.text, true));
				continue;
			}

			result.append(curPart.text);
		}

		return result.toString();
	}

	public void dupAnonymizeCharacters(boolean anonymizeDigits)
	{
		// Must call it before accessing the dup
		verifyPartsDup();

		for (HtmlPart curPart : dupParts)
		{
			if (curPart.type != HtmlPartType.TEXT_REAL)
				continue;

			curPart.text = anonymizeCharacters(curPart.text, anonymizeDigits);
		}
	}

	public void dupAnonymizeImages(boolean removeImagesSource, boolean removeImagesAltText, boolean removeImagesTitle)
	{
		if (!removeImagesAltText && !removeImagesSource && !removeImagesTitle)
			return;

		dupRemovedImagesAltText = dupRemovedImagesAltText || removeImagesAltText;
		dupRemovedImagesSource = dupRemovedImagesSource || removeImagesSource;

		// Must call it before accessing the dup
		verifyPartsDup();

		for (HtmlPart curPart : dupParts)
		{
			if (curPart.type != HtmlPartType.HTML_ELEMENT)
				continue;

			String tagName = HtmlUtils.getHtmlTagName(curPart.text);
			if (tagName == null || !tagName.equals("img"))
				continue;

			if (removeImagesSource)
				curPart.text = curPart.text.replaceFirst("[ \\n]src[ \\n]*=[ \\n]*\"[^\"]+\"", " ");
			if (removeImagesAltText)
				curPart.text = curPart.text.replaceFirst("[ \\n]alt[ \\n]*=[ \\n]*\"[^\"]+\"", " alt=\"(alt)\"");
			if (removeImagesAltText)
				curPart.text = curPart.text.replaceFirst("[ \\n]title[ \\n]*=[ \\n]*\"[^\"]+\"", " title=\"(title)\"");
		}
	}

	private static String anonymizeCharacters(String text, boolean anonymizeDigits)
	{
		// TODO handle all the special symbols like "&lt;" etc.
		String result = text.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("[A-Z]", "N")
				.replaceAll("[a-z]", "a");
		return anonymizeDigits ? result.replaceAll("[0-9]", "1") : result;
	}
}