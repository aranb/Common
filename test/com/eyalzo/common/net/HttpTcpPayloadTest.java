package com.eyalzo.common.net;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpTcpPayloadTest
{
	@Test
	public void testHtmlWithoutUrls()
	{
		testHtmlWithoutUrls("<html></html>", "<html></html>", 0, true);

		testHtmlWithoutUrls("<html><img src=\"picture\"></html>", "<html><img src=\"\"></html>", 0, true);
		testHtmlWithoutUrls("<html><img src=\"picture\"></html>", "<html><img src=\"picture\"></html>", 0, false);

		testHtmlWithoutUrls("<html><img src=\"picture1\"><img src=\"picture2\"><img src=\"picture3\"></html>",
				"<html><img src=\"\"><img src=\"\"><img src=\"\"></html>", 0, true);
		testHtmlWithoutUrls("<html><img src=\"picture1\"><img src=\"picture2\"><img src=\"picture3\"></html>",
				"<html><img src=\"picture1\"><img src=\"picture2\"><img src=\"picture3\"></html>", 0, false);

		testHtmlWithoutUrls(
				"<html><img src=\"picture1\"><a href=\"http://mysite.com/path\"><img src=\"picture2\"><img src=\"picture3\"></html>",
				"<html><img src=\"\"><a href=\"\"><img src=\"\"><img src=\"\"></html>", 0, true);
		testHtmlWithoutUrls(
				"<html><img src=\"picture1\"><a href=\"http://mysite.com/path\"><img src=\"picture2\"><img src=\"picture3\"></html>",
				"<html><img src=\"picture1\"><a href=\"\"><img src=\"picture2\"><img src=\"picture3\"></html>", 0, false);

		testHtmlWithoutUrls(
				"<html><img src=\"picture1\"><a href=\"http://mysite.com/path\"><img src=\"picture2\"><img src=\"picture3\"></html>",
				"<a href=\"\"><img src=\"\"><img src=\"\"></html>", 26, true);
		testHtmlWithoutUrls(
				"<img alt=\"\" border=\"0\" height=\"1\"src=\"http://www.ups.com/img/1.gif\" width=\"576\"><table cellpadding=\"10\" cellspacing=\"0\" border=\"0\" width=\"100%\">",
				"<img alt=\"\" border=\"0\" height=\"1\"src=\"\" width=\"576\"><table cellpadding=\"10\" cellspacing=\"0\" border=\"0\" width=\"100%\">",
				0, true);
	}

	public void testHtmlWithoutUrls(String inputStr, String expectedStr, int startOffset, boolean removeImages)
	{
		byte[] inputBytes = inputStr.getBytes();
		byte[] output = HttpTcpPayload.htmlWithoutUrls(inputBytes, startOffset, inputBytes.length, removeImages);
		String outputStr = new String(output);
		System.out.print("\nInput:    \"");
		System.out.print(inputStr);
		System.out.print("\"\nExpected: \"");
		System.out.print(expectedStr);
		System.out.print("\"\nOutput:   \"");
		System.out.print(outputStr);
		System.out.print("\"\n");
		assertEquals(expectedStr, outputStr);
	}
}
