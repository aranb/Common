package com.eyalzo.common.net;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.Test;

public class HttpUtilsTest
{
	@Test
	public void testDownloadUrlToFileStringStringIntIntLogger()
	{
		Logger log = Logger.getAnonymousLogger();
		boolean success = HttpUtils.downloadUrlToFile("http://www.microsoft.com", "c:\\temp\\1.html", log);
		// assertTrue(success);
	}

	@Test
	public void testGetHtmlWithoutUrls()
	{
		String html1 = "Hello World!";
		String html1expect = html1.toString();
		String html2 = "Hello World!<img src=\"linktosomepic\">This is a picture";
		String html2expect = html2.replace("linktosomepic", "");

		String result = HttpUtils.getHtmlWithoutUrls(html1);
		assertEquals(html1expect, result);
		result = HttpUtils.getHtmlWithoutUrls(html2);
		assertEquals(html2expect, result);
	}
}
