package com.eyalzo.common.webgui;

import static org.junit.Assert.*;

import org.junit.Test;

public class ParamTableTest
{
	ParamTable	paramTable	= new ParamTable("http://site.com/path/page.html");

	@Test
	public void testAddParam()
	{
		paramTable.addParam("param1desc", "param1", 0, 100, 0, 1, false, null);
		assertEquals(1, paramTable.getParamsCount());
		paramTable.addParam("param1desc2", "param1", 0, 100, 0, 1, false, null);
		assertEquals(1, paramTable.getParamsCount());
		paramTable.addParam("param2desc", "param2", 0, 100, 0, 1, false, null);
		assertEquals(2, paramTable.getParamsCount());
	}

	@Test
	public void testGetTableAsHtml()
	{
		testAddParam();
		String result = paramTable.getTableAsHtml();
		assertTrue(result != null && result.length() > 0);
		System.out.println(result);
	}

	@Test
	public void testGetParamsAsUrlWithBaseLink()
	{
		testAddParam();
		String result = paramTable.getParamsAsUrlWithBaseLink(null, 0);
		assertEquals(paramTable.getBaseLink() + "?param1=0&param2=0", result);
	}

	@Test
	public void testGetParamsAsQueryString()
	{
		testAddParam();
		String result = paramTable.getParamsAsQueryString(null, 0);
		assertEquals("param1=0&param2=0", result);
		result = paramTable.getParamsAsQueryString("param1", 1);
		assertEquals("param1=1&param2=0", result);
		result = paramTable.getParamsAsQueryString("param2", 2.2);
		assertEquals("param1=0&param2=2.2", result);
		result = paramTable.getParamsAsQueryString("param2", 2.333333);
		assertEquals("param1=0&param2=2.333333", result);
	}
}
