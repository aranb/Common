/**
 * Copyright 2013 Eyal Zohar. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY EYAL ZOHAR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL EYAL ZOHAR OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Eyal Zohar.
 */
package com.eyalzo.common.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.eyalzo.common.misc.StringUtils;
import com.eyalzo.common.webgui.DisplayTable;

/**
 * Read file made of a single header line and then "key=value" lines, where
 * valid keys always start with an alpha (lower or upper). In addition, it may
 * have section headers in INI file style, meaning "[section]" format.
 */
public abstract class KeyValueFileReader
{
	/**
	 * Header line to be written as the first line in every file, and is also
	 * checked when reading.
	 */
	protected final String	headerLine;
	/**
	 * Absolute or relative file name.
	 */
	protected String		fileName;
	/**
	 * Time stamp of the last version read from disk.
	 */
	private long			fileTimeStamp;
	/**
	 * True only if the file exists and is not empty.
	 */
	protected boolean		valid;
	protected String		separator	= "=";

	//
	// Statistics - valid only when reading is complete
	//
	private int				statLines;
	private int				statKeyValueLines;
	private int				statKeyValueLinesSuccess;
	private int				statSectionLines;
	private int				statCommentLines;
	private long			statLastReadTime;
	private long			statLastCheckTime;
	private int				statCheckCount;
	private long			statProcessingTimeMillis;

	/**
	 * Constructs a new instance, reading the list of ranges from the file given
	 * in the path
	 * 
	 * @param fileName
	 *            Relative or absolute path.
	 * @param Header
	 *            line to be searched in the file's first line, as a preliminary
	 *            test before the other lines are read.
	 */
	public KeyValueFileReader(String fileName, String headerLine)
	{
		// Replace newlines with spaces and remove surrounding spaces.
		if (headerLine == null)
			this.headerLine = null;
		else
			this.headerLine = headerLine.trim().replace('\n', '_');
		this.fileName = fileName;
	}

	/**
	 * Read the file into the data structure, assuming that it is already
	 * sorted.
	 */
	protected boolean readFile()
	{
		statLastReadTime = System.currentTimeMillis();
		// Remember when started so we can calculate processing time
		long startTime = statLastCheckTime;

		String curSectionName = "";
		boolean tempValid = true;
		valid = false;

		// Reset stats
		statCommentLines = statKeyValueLines = statKeyValueLinesSuccess = statLines = statSectionLines = 0;

		BufferedReader in = null;

		try
		{
			// Get prepared to read line by line
			in = new BufferedReader(new FileReader(fileName));

			//
			// Check header line
			//
			String readHedaer;
			if (this.headerLine != null)
			{
				readHedaer = in.readLine();
				if (readHedaer == null || !readHedaer.equals(this.headerLine))
					return false;
			}

			// A chance to reset/clear lists before filling them again
			beforeProcessing();

			//
			// Read the rest of the file
			//
			String curReadLine;
			while ((curReadLine = in.readLine()) != null)
			{
				statLines++;

				curReadLine = curReadLine.trim();

				// Skip comments
				if (curReadLine.startsWith("#") || curReadLine.startsWith("'") || curReadLine.startsWith("/"))
				{
					statCommentLines++;
					continue;
				}

				// Look for section headers
				if (curReadLine.startsWith("[") && curReadLine.endsWith("]"))
				{
					statSectionLines++;
					// We are in section header
					curSectionName = curReadLine.equals("[]") ? "" : curReadLine.substring(1, curReadLine.length() - 1)
							.trim();
					continue;
				}

				//
				// Separate key and value (the value may have a = inside)
				//
				int equalsIndex = curReadLine.indexOf(separator);
				if (equalsIndex <= 0)
					continue;

				statKeyValueLines++;

				String key = curReadLine.substring(0, equalsIndex).trim();
				String value = curReadLine.substring(equalsIndex + 1).trim();

				// Call the line processing method
				boolean success = processKeyValue(key, value, curSectionName, statLines);
				if (success)
					statKeyValueLinesSuccess++;
			}

			// Remember the file time stamp
			File file = new File(fileName);
			fileTimeStamp = file.lastModified();
		} catch (FileNotFoundException f)
		{
			tempValid = false;
		} catch (Exception e)
		{
			tempValid = false;
		} finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				} catch (IOException e)
				{
					tempValid = false;
				}
			}
		}

		valid = tempValid;
		statProcessingTimeMillis = System.currentTimeMillis() - startTime;

		return valid;
	}

	private boolean hasAnUpdatedFile()
	{
		statLastCheckTime = System.currentTimeMillis();
		statCheckCount++;

		File file = new File(fileName);
		long timeStamp = file.lastModified();
		return (timeStamp != fileTimeStamp);
	}

	/**
	 * @return True if the file was found to be different then the last read,
	 *         and it was also read successfully. In this case it returns after
	 *         it called {@link #processKeyValue(String, String, String, int)}.
	 */
	public boolean updateListsIfNeeded()
	{
		if (hasAnUpdatedFile())
			return readFile();

		return false;
	}

	/**
	 * Override this to process each line separately.
	 * 
	 * @param key
	 *            Key name, which is the left part, before the first "=" sign.
	 *            Already trimmed.
	 * @param value
	 *            Value, which is the right part, after the first "=" sign.
	 *            Already trimmed.
	 * @param section
	 *            Optional section name, as was found in the last "[section]"
	 *            line. May be empty but never null. Trimmed and without the []
	 *            signs.
	 * @param serial
	 *            1-based serial number of the line, according to the newlines
	 *            found.
	 * @return Some kind of an indication to success.
	 */
	protected abstract boolean processKeyValue(String key, String value, String section, int serial);

	/**
	 * Called when a new processing cycle starts, before the first call to
	 * {@link #processKeyValue(String, String, String, int)}.
	 */
	protected abstract void beforeProcessing();

	public boolean getValid()
	{
		return valid;
	}

	public DisplayTable webGuiDetails()
	{
		DisplayTable table = new DisplayTable();

		table.addField("File", "'" + fileName + "'", "The given file name, absolute or relative");
		table.addField("Full path", "'" + getFullPath() + "'",
				"The full path, as interpreted according to the current dir");
		table.addField("Header line", headerLine, "Header line to be found in file's first line");
		table.addField("Last modified", fileTimeStamp <= 0 ? "" : StringUtils.createDateFormat(fileTimeStamp),
				"File last modified time, as found last time tried to read the file");
		table.addField("Last read", statLastReadTime <= 0 ? "" : StringUtils.createDateFormat(statLastReadTime),
				"Last time the file was read after change");
		table.addField("Last check", statLastCheckTime <= 0 ? "" : StringUtils.createDateFormat(statLastCheckTime),
				"Last time the file was checked for change");
		table.addField("Check count", statCheckCount, "Number of times the file was checked for a change");
		table.addField(null, null, null);
		table.addField("Lines", statLines, "How many lines in file, excluding the header");
		table.addField("Key-value lines", statKeyValueLines, "Lines containing key=value");
		table.addField("Key-value lines, processed", statKeyValueLinesSuccess,
				"Lines containing key=value, processed successfully");
		table.addField("Comment lines", statCommentLines, "Comment lines, those starting with any of #,/");
		table.addField("Processing time", statProcessingTimeMillis <= 0 ? "" : statProcessingTimeMillis + " mSec",
				"Time it took to read and process the file in mSec");
		table.addField("Sections", statSectionLines, "Section lines in the format [section]");

		return table;
	}

	public String getFullPath()
	{
		if (fileName == null || fileName.isEmpty())
			return "";

		File file = new File(fileName);

		return file.getAbsolutePath();
	}

	/**
	 * @return Number of lines in the file, without the header line.
	 */
	public int size()
	{
		return statLines;
	}

	/**
	 * @return Number of lines processed successfully.
	 */
	public int getLinesSuccessCount()
	{
		return statKeyValueLinesSuccess;
	}
}
