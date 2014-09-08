package com.eyalzo.common.cli;

import static org.junit.Assert.*;

import org.junit.Test;

public class CommandLineTest
{

	@Test
	public void testIsLegalOptionName()
	{
		//
		// One letter, legal
		//
		assertEquals(true, CommandLine.isLegalOptionName("-a"));
		assertEquals(true, CommandLine.isLegalOptionName("-b"));
		assertEquals(true, CommandLine.isLegalOptionName("-z"));
		assertEquals(true, CommandLine.isLegalOptionName("-A"));
		assertEquals(true, CommandLine.isLegalOptionName("-B"));
		assertEquals(true, CommandLine.isLegalOptionName("-Z"));

		//
		// One letter, illegal
		//
		assertEquals(false, CommandLine.isLegalOptionName(" -a"));
		assertEquals(false, CommandLine.isLegalOptionName("-a "));
		assertEquals(false, CommandLine.isLegalOptionName("-aa"));
		assertEquals(false, CommandLine.isLegalOptionName("-a1"));
		assertEquals(false, CommandLine.isLegalOptionName("-1a"));
		assertEquals(false, CommandLine.isLegalOptionName("-1"));
		assertEquals(false, CommandLine.isLegalOptionName("-("));
		assertEquals(false, CommandLine.isLegalOptionName("-+"));

		//
		// Multiple letters, legal
		//
		assertEquals(true, CommandLine.isLegalOptionName("--a"));
		assertEquals(true, CommandLine.isLegalOptionName("--ab"));
		assertEquals(true, CommandLine.isLegalOptionName("--abc"));
		assertEquals(true, CommandLine.isLegalOptionName("--abcd"));

		assertEquals(true, CommandLine.isLegalOptionName("--a-a"));
		assertEquals(true, CommandLine.isLegalOptionName("--a-a-a"));
		assertEquals(true, CommandLine.isLegalOptionName("--a-a-a-a"));

		assertEquals(true, CommandLine.isLegalOptionName("--aa-a"));
		assertEquals(true, CommandLine.isLegalOptionName("--a-aa"));
		assertEquals(true, CommandLine.isLegalOptionName("--aa-aa"));

		//
		// Multiple letters, illegal
		//
		assertEquals(false, CommandLine.isLegalOptionName(" --a-"));
		assertEquals(false, CommandLine.isLegalOptionName("--a- "));
		assertEquals(false, CommandLine.isLegalOptionName("--a-"));
		assertEquals(false, CommandLine.isLegalOptionName("--ab1"));
		assertEquals(false, CommandLine.isLegalOptionName("--ab2c"));
		assertEquals(false, CommandLine.isLegalOptionName("--a bcd"));

		assertEquals(false, CommandLine.isLegalOptionName("--a--a"));
		assertEquals(false, CommandLine.isLegalOptionName("--a-a2-a"));
		assertEquals(false, CommandLine.isLegalOptionName("--a-a-aZ-a"));

		assertEquals(false, CommandLine.isLegalOptionName("--Zaa-a"));
		assertEquals(false, CommandLine.isLegalOptionName("--a2-aa"));
		assertEquals(false, CommandLine.isLegalOptionName("--aa+-aa"));
	}
}
