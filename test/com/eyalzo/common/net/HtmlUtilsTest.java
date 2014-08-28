package com.eyalzo.common.net;

import static org.junit.Assert.*;

import org.junit.Test;

public class HtmlUtilsTest
{
	@Test
	public void testGetHtmlTextSpacesPrefixLen()
	{
		int result;

		result = HtmlUtils.getHtmlTextSpacesPrefixLen(null);
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen("");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen("a");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen(" a");
		assertEquals(1, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen("a ");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen(" a ");
		assertEquals(1, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen("\n a");
		assertEquals(2, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen(" \ta");
		assertEquals(2, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen(" \t \n ");
		assertEquals(5, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen(" \t&nbsp;\n ");
		assertEquals(10, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen(" \t&nbsp;");
		assertEquals(8, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen("&nbsp; abc &nbsp;");
		assertEquals(7, result);

		result = HtmlUtils.getHtmlTextSpacesPrefixLen("&nbSp;");
		assertEquals(0, result);
	}

	@Test
	public void testGetHtmlTextSpacesSuffixLen()
	{
		int result;

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(null);
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen("");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen("a");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(" a");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen("a ");
		assertEquals(1, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(" a ");
		assertEquals(1, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen("\n a");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen("\n a \n\t");
		assertEquals(3, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(" \ta");
		assertEquals(0, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(" \t \n ");
		assertEquals(5, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(" \t&nbsp;\n ");
		assertEquals(10, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(" \t&nbsp;");
		assertEquals(8, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen(" &nbsp;");
		assertEquals(7, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen("&nbsp; abc &nbsp;");
		assertEquals(7, result);

		result = HtmlUtils.getHtmlTextSpacesSuffixLen("&nbSp;");
		assertEquals(0, result);
	}
}
