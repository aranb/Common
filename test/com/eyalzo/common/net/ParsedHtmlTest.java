package com.eyalzo.common.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.eyalzo.common.html.HtmlDiff;
import com.eyalzo.common.html.ParsedHtml;

public class ParsedHtmlTest
{
	private ParsedHtml	parsedHtml1;
	private ParsedHtml	parsedHtml2;

	@Before
	public void setUp() throws Exception
	{
		parsedHtml1 = new ParsedHtml("<a href=abc.com>My text<b>Bold</b></a>".getBytes());
		parsedHtml2 = new ParsedHtml("<a href=abc.com>My other text<b>Bold</b></a>".getBytes());
	}

	@Test
	public void testParsedHtml()
	{
		assertEquals(6, parsedHtml1.parts.size());
		assertEquals(6, parsedHtml2.parts.size());
	}

	@Test
	public void testCompareGetPartsOffsets()
	{
		HtmlDiff result;

		// No tolerance for resync
		result = new HtmlDiff(parsedHtml1, parsedHtml2, 0, 0);
		assertEquals(parsedHtml1.parts.size(), result.syncOffsets.size());
		assertEquals(0, (int) result.syncOffsets.get(0));
		assertNull(result.syncOffsets.get(1));
		assertNull(result.syncOffsets.get(2));
		assertNull(result.syncOffsets.get(3));
		assertNull(result.syncOffsets.get(4));
		assertNull(result.syncOffsets.get(5));

		assertEquals(1, result.textOffsets.size());
		assertEquals(0, (int) result.textOffsets.get(1));

		// With tolerance for resync
		result = new HtmlDiff(parsedHtml1, parsedHtml2, 0, 1);
		assertEquals(parsedHtml1.parts.size(), result.syncOffsets.size());
		assertEquals(0, (int) result.syncOffsets.get(0));
		assertNull(result.syncOffsets.get(1));
		// Now it managed to resync with the "<b>" element
		assertEquals(0, (int) result.syncOffsets.get(2));
		assertEquals(0, (int) result.syncOffsets.get(3));
		assertEquals(0, (int) result.syncOffsets.get(4));
		assertEquals(0, (int) result.syncOffsets.get(5));

		// With tolerance for resync, min text 3 so it catches the "<b>"
		result = new HtmlDiff(parsedHtml1, parsedHtml2, 3, 1);
		assertEquals(parsedHtml1.parts.size(), result.syncOffsets.size());
		assertEquals(0, (int) result.syncOffsets.get(0));
		assertNull(result.syncOffsets.get(1));
		// Now it managed to resync with the "<b>" element
		assertEquals(0, (int) result.syncOffsets.get(2));

		// With tolerance for resync, min text 4 so it does not catch the "<b>"
		result = new HtmlDiff(parsedHtml1, parsedHtml2, 4, 1);
		assertEquals(parsedHtml1.parts.size(), result.syncOffsets.size());
		assertEquals(0, (int) result.syncOffsets.get(0));
		assertNull(result.syncOffsets.get(1));
		// Now it does not catch the "<b>" element
		assertNull(result.syncOffsets.get(2));
		// And the allowed offset disables resync
		assertNull(result.syncOffsets.get(3));
		assertNull(result.syncOffsets.get(4));
		assertNull(result.syncOffsets.get(5));

		// With more tolerance for resync, min text 4 so it does not catch the "<b>"
		result = new HtmlDiff(parsedHtml1, parsedHtml2, 4, 2);
		assertEquals(parsedHtml1.parts.size(), result.syncOffsets.size());
		assertEquals(0, (int) result.syncOffsets.get(0));
		assertNull(result.syncOffsets.get(1));
		// Now it does not catch the "<b>" element
		assertNull(result.syncOffsets.get(2));
		// And the allowed offset now enables resync using the "Bold" and other 4 characters elements
		assertEquals(0, (int) result.syncOffsets.get(3));
		assertEquals(0, (int) result.syncOffsets.get(4));
		assertEquals(0, (int) result.syncOffsets.get(5));
	}
}
