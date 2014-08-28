/**
 * Copyright 2011 Eyal Zohar. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY EYAL ZOHAR ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * EYAL ZOHAR OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed or implied, of Eyal Zohar.
 */
package com.eyalzo.common.cli;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Manage and parse command line commands and options.
 * <p>
 * Options can be in any of the four legal formats:
 * <ol>
 * <li>-a: Single letter flag. Case sensitive. These multiple single letter options can be combined into a single
 * command line word: so `-ab' is equivalent to `-a -b'.
 * <li>--abc-def: Words flag. All lower-case. Can use hyphen for space.
 * <li>--abc-def=123: Words option. All lower-case. Can use hyphen for space. Value must follow an equal sign, without
 * spaces. Value may be optional, meaning that it can also function as a flag.
 * <li>-a 10: Single letter option. Value may be optional, meaning that it can also function as a flag.
 * </ol>
 * 
 * @author Eyal Zohar
 * 
 */
public class CommandLine
{
	/**
	 * Defined options that can be later read and used.
	 */
	private HashMap<String, DefinedOption>	definedOptions	= new HashMap<String, DefinedOption>();

	/**
	 * Ordered list of the values that do not have option name, meaning the words that do not start with "-" or "--" and
	 * do not belong to options.
	 */
	private LinkedList<String>				readValues		= new LinkedList<String>();
	/**
	 * All options, meaning the ones that start with "-" or "--", with or without values.
	 */
	private HashMap<String, String>			readOptions		= new HashMap<String, String>();
	/**
	 * Non-null on error.
	 */
	private String							errorMessage;

	/**
	 * In case of duplicates only the last appearance is saved.
	 * 
	 * @param args
	 *            Command line arguments from main().
	 * @return True if the parsing went well, or false if something went wrong like missing mandatory parameters,
	 *         illegal values, undefined option name, etc.
	 */
	public boolean parse(String[] args)
	{
		// Walk through the arguments
		for (int i = 0; i < args.length; i++)
		{
			String curArg = args[i];

			// Option in the format --abc-def=123
			if (curArg.startsWith("--") && curArg.contains("="))
			{
				// Need to split to 2 parts now in order to check the format of the option
				String[] split = curArg.split("=", 2);
				if (!isLegalOptionName(split[0]))
				{
					errorMessage = "Illegal option '" + split[0] + "' !";
					return false;
				}

				DefinedOption option = definedOptions.get(curArg);

				// Defined option?
				if (option == null)
				{
					errorMessage = "Undefined option '" + curArg + "' !";
					return false;
				}

				// The option name is legal so now need to save the value
				// Can assume that we have two parts exactly because of the conditions above
				readOptions.put(split[0], split[1]);
			}
			// Options, meaning option name and optionally a value
			else if (isLegalOptionName(curArg))
			{
				DefinedOption option = definedOptions.get(curArg);

				// Defined option?
				if (option == null)
				{
					errorMessage = "Undefined option '" + curArg + "' !";
					return false;
				}

				// TODO store value
			}
			// Values with no option name
			else
			{
				// Duplicates are allowed
				readValues.add(curArg);
			}
		}

		return true;
	}

	/**
	 * Get option's value as numeric.
	 * 
	 * @param optionName
	 *            Name of option, with the leading "-" or "--", case sensitive.
	 * @param defaultValue
	 *            Default value to use in case the option was not set or the value has an illegal format.
	 * @return The read value or the default in case it was missing or invalid.
	 */
	public long getOptionNum(String optionName, long defaultValue)
	{
		String strVal = readOptions.get(optionName);
		if (strVal == null)
			return defaultValue;

		long result;
		try
		{
			result = Long.parseLong(strVal);
		} catch (NumberFormatException e)
		{
			return defaultValue;
		}

		return result;
	}

	/**
	 * @return False if option name is illegal or option already exists.
	 */
	public boolean addOptionString(String optionName, String description, boolean isMandatory, String defaultValue)
	{
		// Is it legal?
		if (!isLegalOptionName(optionName))
			return false;

		// Already defined?
		if (definedOptions.containsKey(optionName))
			return false;

		OptionString option = new OptionString(description, isMandatory, defaultValue);
		definedOptions.put(optionName, option);

		return true;
	}

	/**
	 * @return False if option name is illegal or option already exists.
	 */
	public boolean addOptionBool(String optionName, String description)
	{
		// Is it legal?
		if (!isLegalOptionName(optionName))
			return false;

		// Already defined?
		if (definedOptions.containsKey(optionName))
			return false;

		DefinedOption option = new DefinedOption(description);
		definedOptions.put(optionName, option);

		return true;
	}

	/**
	 * @return False if option name is illegal or option already exists.
	 */
	public boolean addOptionNum(String optionName, String description, boolean isMandatory, long defaultVal,
			long minVal, long maxVal)
	{
		// Is it legal?
		if (!isLegalOptionName(optionName))
			return false;

		// Already defined?
		if (definedOptions.containsKey(optionName))
			return false;

		// Ranges
		if (maxVal < minVal || defaultVal < minVal || defaultVal > maxVal)
			return false;

		DefinedOptionNum option = new DefinedOptionNum(description, isMandatory, defaultVal, minVal, maxVal);
		definedOptions.put(optionName, option);

		return true;
	}

	/**
	 * 
	 * @param optionname
	 *            Name of option, with the leading "-" or "--".
	 * @return True if the option's name seems legal.
	 */
	static boolean isLegalOptionName(String optionName)
	{
		if (optionName == null)
			return false;

		// Check for the one-letter options
		if (optionName.matches("\\-[a-zA-Z]"))
			return true;

		// The "--" options
		return optionName.matches("\\-\\-[a-z]+(\\-[a-z]+)*");
	}

	/**
	 * 
	 * @return The latest error message when returning from {@link #parse(String[])}.
	 */
	public String getErrorMessage()
	{
		return errorMessage;
	}

	public String getUsageHelp()
	{
		StringBuffer buffer = new StringBuffer(100 + 100 * definedOptions.size());

		// Handle (optional) error
		if (errorMessage != null && !"".equals(errorMessage))
		{
			buffer.append("Error: ");
			buffer.append(errorMessage);
			buffer.append("\r\n");
		}

		// Add options
		List<String> sortedDefinedOptions = getDefinedOptionsNamesSorted();
		for (String curOptionName : sortedDefinedOptions)
		{
			buffer.append(String.format("%20s\r\n", curOptionName));
		}

		return buffer.toString();
	}

	/**
	 * @return Sorted list of all the defined options names, case insensitive and ignores the leading hyphens.
	 */
	public LinkedList<String> getDefinedOptionsNamesSorted()
	{
		LinkedList<String> result = new LinkedList<String>(definedOptions.keySet());

		Collections.sort(result, new Comparator<String>()
		{
			/**
			 * @param optionName1
			 * @param optionName2
			 * @return Comparison between the two option names.
			 */
			@Override
			public int compare(String optionName1, String optionName2)
			{
				// Remove the leading "-" or "--"
				optionName1 = optionName1.substring(optionName1.startsWith("--") ? 2 : 1).toLowerCase();
				optionName2 = optionName2.substring(optionName2.startsWith("--") ? 2 : 1).toLowerCase();

				return optionName1.compareTo(optionName2);
			}
		});

		return result;
	}
}
