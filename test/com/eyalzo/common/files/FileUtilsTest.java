package com.eyalzo.common.files;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class FileUtilsTest
{
	private void testGetCurrentDirFile1(String curDir, String fileName)
	{
		String result = FileUtils.getCurrentDirFile(fileName);
		assertEquals(curDir + File.separator + fileName, result);
	}

	@Test
	public void testGetCurrentDirFile()
	{
		// This call returns the current dir
		String curDir = FileUtils.getCurrentDirFile(null);
		assertNotNull(curDir);
		System.out.println("Current dir: " + curDir);

		testGetCurrentDirFile1(curDir, "a");
		testGetCurrentDirFile1(curDir, "a1");
		testGetCurrentDirFile1(curDir, "a1.com");
		testGetCurrentDirFile1(curDir, "a" + File.separator + "a1.com");
		testGetCurrentDirFile1(curDir, "a" + File.separator + "b" + File.separator + "a1.com");
	}
}
