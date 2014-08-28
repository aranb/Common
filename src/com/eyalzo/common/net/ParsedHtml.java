package com.eyalzo.common.net;

import java.util.LinkedList;

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

		/**
		 * Get text or HTML tag converted to HTML displayable string, where special symbols like &lt; are converted to
		 * their safe form (as &amp;lt;). Also converts the newlines to visiable two characters "\n".
		 * 
		 * @return Display safe HTML representation of the text.
		 */
		String getTextAsHtmlDisplayable()
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
	}

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

	//
	// Text parts
	//
	/**
	 * Significant text parts, all started as {@link HtmlPartType#TEXT_REAL} and were aggregated if possible, and then
	 * filtered if they ended up being too short or having only symbols.
	 */
	private LinkedList<String>			textProcessedParts;

	public ParsedHtml(byte[] bodyBytes)
	{
		parts = new LinkedList<ParsedHtml.HtmlPart>();

		// Offset and state
		boolean stateHtml = false;
		int startOffset = 0;
		for (int offset = 0; offset < bodyBytes.length; offset++)
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

		// Update stats
		updateStats();
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

		textProcessedParts = new LinkedList<String>();

		// Try to merge
		String curText = null;
		for (HtmlPart curPart : parts)
		{
			if (curPart.type == HtmlPartType.TEXT_REAL)
			{
				if (curText == null)
					curText = curPart.text.replace("&nbsp;", " ").trim();
				else
				{
					// Add a delimiter, that is needed because of the trims
					curText += " ";
					curText += curPart.text.replace("&nbsp;", " ").trim();
				}
				continue;
			}

			if (curPart.type == HtmlPartType.HTML_ELEMENT)
			{
				String tagName = HtmlUtils.getHtmlTagName(curPart.text);
				if (tagName != null && curText != null)
				{
					// System.out.println(tagName);
					if (tagName.equals("br") || tagName.equals("table") || tagName.equals("tr") || tagName.equals("td")
							|| tagName.equals("p"))
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
	}

	/**
	 * @return Significant text parts, all started as {@link HtmlPartType#TEXT_REAL} and were aggregated if possible,
	 *         and then filtered if they ended up being too short or having only symbols.
	 */
	public LinkedList<String> getTextProcessedParts()
	{
		generateTextProcessedParts();
		return textProcessedParts;
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
	 * @return The complete stats of parts found when parsed the HTML, after some initial processing of style and
	 *         script.
	 * @see #getStatHtmlElementCount()
	 */
	public MapCounter<HtmlPartType> getStats()
	{
		return statPartsCount;
	}

	public DisplayTable webGuiTextProcessed()
	{
		generateTextProcessedParts();

		DisplayTable table = new DisplayTable();

		table.addCol("#", "Sequence of merged text part", true);
		table.addCol("Currency?", "Mark '+' if looks like curreny (USD)", false);
		table.addCol("Text", "The text itself");

		int i = 0;
		for (String curText : textProcessedParts)
		{
			table.addRow(null);
			i++;
			table.addCell(i);
			table.addCell(StringUtils.isTextCurrency(curText) ? "+" : null);
			table.addCell(curText, null, false, true);
		}

		return table;
	}
}
