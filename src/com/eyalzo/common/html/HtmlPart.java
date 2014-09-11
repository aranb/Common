package com.eyalzo.common.html;

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
	 * Get text or HTML tag converted to HTML displayable string, where special symbols like &lt; are converted to their
	 * safe form (as &amp;lt;). Also converts the newlines to visible two characters "\n".
	 * 
	 * @return Display safe HTML representation of the text.
	 */
	public String getTextAsHtmlDisplayable()
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;")
				.replace("\n", "\\n");
	}

	/**
	 * Get text or HTML tag converted to HTML displayable string, where special symbols like &lt; are converted to their
	 * safe form (as &amp;lt;). Also converts the newlines to visible two characters "\n".
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
