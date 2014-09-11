package com.eyalzo.common.html;

/**
 * @author Eyal Zohar
 */
public enum HtmlPartType
{
	/**
	 * Everything that starts with < and ends with >.
	 */
	HTML_ELEMENT, /**
	 * Not HTML, but has only white spaces, including "&nbsp;" parts (6 characters).
	 */
	TEXT_EMPTY, /**
	 * Has something meaningful, possibly just a comma or dot, but still something that may be worth
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
