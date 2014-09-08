/**
 * Copyright 2012 Eyal Zohar. All rights reserved.
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
package com.eyalzo.common.webgui;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * How to use, the global version:
 * <p>
 * <ol>
 * <li>Initialize the table with {@link ParamTable#ParamTable()}.
 * <li>Define the parameters with multiple calls to
 * {@link #addParam(String, String, double, double, double, double, boolean, String)} or similar.
 * <li>Each time you wish to use the table, do the following routine:
 * <ol>
 * <li>Call {@link #initWithBaseLinkAndValues(WebContext)}, that automatically performs {@link #disableAll()}.
 * <li>Read and enable each parameter with {@link #getValueAndEnable(String)}.
 * </ol>
 * </ol>
 * 
 * @author Eyal Zohar
 */
public class ParamTable
{
	private String								baseLink;
	private LinkedHashMap<String, ParamItem>	params			= new LinkedHashMap<String, ParamTable.ParamItem>();
	/**
	 * Stand-by values, in case it is needed to read values before the parameters are fully defined.
	 */
	private HashMap<String, Double>				valuesStandBy	= new HashMap<String, Double>();

	private static class ParamItem
	{
		/**
		 * Name in URL query.
		 */
		final String	name;
		/**
		 * Appears in table as a short description.
		 */
		final String	description;
		boolean			percents;
		double			value;
		double			minValue;
		double			maxValue;
		double			step;
		/**
		 * Long description, to appear as tool-tip pop-up in table.
		 */
		final String	longDescription;
		/**
		 * True if to show in output and/or include in URLs.
		 */
		boolean			enabled	= true;

		public ParamItem(String name, String description, boolean percents, double defaultValue, double minValue,
				double maxValue, double step, String longDescription)
		{
			this.name = name;
			this.description = description;
			this.percents = percents;
			this.minValue = minValue;
			// Make sure max is not lower than min
			this.maxValue = Math.max(maxValue, minValue);
			// Make sure value is between boundaries
			this.value = Math.max(Math.min(defaultValue, this.maxValue), this.minValue);
			this.step = step;
			this.longDescription = longDescription;
		}

		public static String normalizeName(String name)
		{
			if (name == null)
				return null;
			return name.trim().toLowerCase();
		}

		/**
		 * @param value
		 *            The given value.
		 * @return The value as string, without trailing zeros and/or a redundant period.
		 */
		public static String trimValue(double value)
		{
			String result = Double.toString(((long) (value * 1000)) / 1000.0);
			while (result.endsWith("0"))
				result = result.substring(0, result.length() - 1);
			return result.endsWith(".") ? result.substring(0, result.length() - 1) : result;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	public ParamTable()
	{
	}

	public ParamTable(String baseLink)
	{
		this.baseLink = baseLink;
	}

	/**
	 * Initialize the base-link and read all parameters values. It does not define parameters, but instead it sets
	 * values of already defined parameters, and saves values for the others (stand-by, until defined).
	 */
	public ParamTable(WebContext context)
	{
		this(context.command);
		initWithBaseLinkAndValues(context);
	}

	/**
	 * Initialize the base-link, disable all the parameters, and read all parameters values. It does not define
	 * parameters, but instead it sets values of already defined parameters, and saves values for the others (stand-by,
	 * until defined).
	 */
	public void initWithBaseLinkAndValues(WebContext context)
	{
		this.baseLink = context.getUrl();

		disableAll();

		for (Entry<String, String> entry : context.getParams().entrySet())
		{
			// Param name
			String paramName = ParamItem.normalizeName(entry.getKey());
			if (paramName.isEmpty())
				continue;
			// Try to get a double value
			String paramValueStr = entry.getValue();
			if (paramValueStr == null || paramValueStr.isEmpty())
				continue;
			double paramValue = 0;
			try
			{
				paramValue = Double.parseDouble(paramValueStr);
			} catch (NumberFormatException e)
			{
				continue;
			}

			ParamItem paramItem = params.get(paramName);
			if (paramItem != null)
			{
				// Make sure value is between boundaries
				paramItem.value = Math.max(Math.min(paramValue, paramItem.maxValue), paramItem.minValue);
			} else
			{
				valuesStandBy.put(paramName, paramValue);
			}
		}
	}

	public void setValue(String paramName, double value)
	{
		if (paramName == null || paramName.isEmpty())
			return;

		ParamItem paramItem = params.get(paramName);
		if (paramItem == null)
			return;

		// Make sure value is between boundaries
		paramItem.value = Math.max(Math.min(value, paramItem.maxValue), paramItem.minValue);
	}

	/**
	 * @return Value of parameter, which can be a previously initiated value from a URL, or the default given here.
	 */
	public double addParamPercents(String description, String name, double defaultValue, double step,
			String longDescription)
	{
		return addParam(description, name, 0.0, 100.0, defaultValue, step, true, longDescription);
	}

	/**
	 * @return Value of parameter, which can be a previously initiated value from a URL, or the default given here.
	 */
	public double addParam(String description, String name, double minValue, double maxValue, double defaultValue,
			double step, boolean percents, String longDescription)
	{
		if (name == null)
			return defaultValue;
		name = ParamItem.normalizeName(name);
		if (name.isEmpty())
			return defaultValue;

		ParamItem paramItem = new ParamItem(name, description, percents, defaultValue, minValue, maxValue, step,
				longDescription);
		// Check for a value that was set before the parameter was fully defined
		Double stanbyValue = this.valuesStandBy.get(name);
		if (stanbyValue != null)
			paramItem.value = stanbyValue;
		// Add to the list
		synchronized (params)
		{
			params.put(name, paramItem);
		}

		return paramItem.value;
	}

	public String getTableAsHtml()
	{
		if (params.isEmpty())
			return null;

		StringBuffer buffer = new StringBuffer();

		buffer.append("<table rules='rows' style='font-size:8pt; border-color: #FFFFFF; border-style:solid; border-width:0px;' width='%100'>");
		synchronized (params)
		{
			for (ParamItem curParam : params.values())
			{
				// Skip disabled parameters
				if (!curParam.enabled)
					continue;

				buffer.append("   <tr bgcolor=\"lightgrey\">");
				// Param
				buffer.append("<td title=\"");
				buffer.append(curParam.longDescription == null || curParam.longDescription.isEmpty() ? curParam.name
						: curParam.name + " - " + curParam.longDescription.replace("\"", "&quot;"));
				buffer.append("\"><b>");
				buffer.append(curParam.description.replace("<", "&lt;").replace(">", "&gt;"));
				buffer.append(":</b></td>");
				// Lower
				buffer.append("<td>");
				if (curParam.value <= curParam.minValue)
					buffer.append("&nbsp;");
				else
				{
					double minValue = Math.max(curParam.minValue, round(curParam.value - curParam.step));
					buffer.append("&darr; " + getHtmlLink(curParam, minValue) + "&nbsp;");
				}
				buffer.append("</td>");
				// Value
				buffer.append("<td align=center><b>");
				buffer.append(ParamItem.trimValue(curParam.value));
				if (curParam.percents)
					buffer.append("%");
				buffer.append("</b></td>");
				// Higher
				buffer.append("<td align=right>");
				if (curParam.value >= curParam.maxValue)
					buffer.append("&nbsp;");
				else
				{
					double maxValue = Math.min(curParam.maxValue, round(curParam.value + curParam.step));
					buffer.append("&nbsp;" + getHtmlLink(curParam, maxValue) + " &uarr;");
				}
				buffer.append("</td>");
				// Row end
				buffer.append("</tr>\n");
			}
		}
		buffer.append("</table>\n");

		return buffer.toString();
	}

	private double round(double number)
	{
		return Math.round(number * 1000) / 1000.0;
	}

	private String getHtmlLink(ParamItem paramItem, double paramValue)
	{
		return "<a href=\"" + getParamsAsUrlWithBaseLink(paramItem.name, paramValue) + "\">"
				+ ParamItem.trimValue(paramValue) + (paramItem.percents ? "%" : "") + "</a>";
	}

	/**
	 * Generate the parameters and their values as a full URL made of base-link and query string with separating &,
	 * while (optionally) using an alternative value for one of the parameters.
	 * 
	 * @param paramName
	 *            Optional. Name of parameter to override its value.
	 * @param paramValue
	 *            Optional. Overriding value for the given parameter.
	 * @return Base-link and a query string made of list of parameters and their values, starting with the first added
	 *         parameter and then a separating & between each couple.
	 */
	public String getParamsAsUrlWithBaseLink(String paramName, double paramValue)
	{
		return getParamsAsUrl(this.baseLink, paramName, paramValue);
	}

	/**
	 * Generate the parameters and their values as a full URL made of base-link and query string with separating &,
	 * while (optionally) using an alternative value for one of the parameters.
	 * 
	 * @param paramName
	 *            Optional. Name of parameter to override its value.
	 * @param paramValue
	 *            Optional. Overriding value for the given parameter.
	 * @return Base-link and a query string made of list of parameters and their values, starting with the first added
	 *         parameter and then a separating & between each couple.
	 */
	public String getParamsAsUrl(String baseLink, String paramName, double paramValue)
	{
		String queryString = getParamsAsQueryString(paramName, paramValue);
		if (queryString.isEmpty())
			return baseLink;

		if (baseLink == null || baseLink.isEmpty())
			return queryString;

		baseLink = getLinkWithoutEnabledParams(baseLink);

		// If the base-link already has a query string
		if (baseLink.contains("?"))
			return baseLink + "&" + queryString;

		// If the base-link does not have a query string
		return baseLink + "?" + queryString;
	}

	public String getParamsAsUrl(String baseLink)
	{
		return getParamsAsUrl(baseLink, null, 0);
	}

	/**
	 * Generate the parameters and their values as a query string with separating &, while (optionally) using an
	 * alternative value for one of the parameters.
	 * 
	 * @param paramName
	 *            Optional. Name of parameter to override its value.
	 * @param paramValue
	 *            Optional. Overriding value for the given parameter.
	 * @return List of parameters and their values, starting with the first added parameter and then a separating &
	 *         between each couple.
	 */
	public String getParamsAsQueryString(String paramName, double paramValue)
	{
		paramName = ParamItem.normalizeName(paramName);
		StringBuffer buffer = new StringBuffer();
		boolean replaced = false;
		synchronized (params)
		{
			if (params.isEmpty())
				return "";
			for (ParamItem curParam : params.values())
			{
				// Skip disabled parameters
				if (!curParam.enabled)
					continue;

				if (buffer.length() > 0)
					buffer.append("&");
				buffer.append(curParam.name);
				buffer.append("=");
				if (curParam.name.equals(paramName))
				{
					if (!replaced)
						buffer.append(ParamItem.trimValue(paramValue));
					replaced = true;
				} else
					buffer.append(ParamItem.trimValue(curParam.value));
			}
		}

		// Handle the case that the parameter is not even in the list
		if (!replaced && paramName != null && !paramName.isEmpty())
		{
			if (buffer.length() > 0)
				buffer.append("&");
			buffer.append(paramName);
			buffer.append("=");
			buffer.append(ParamItem.trimValue(paramValue));
		}
		return buffer.toString();
	}

	public String getParamsAsQueryString()
	{
		return getParamsAsQueryString(null, 0);
	}

	/**
	 * @return Total number of fully defined parameters, even if disabled.
	 */
	public int getParamsCount()
	{
		synchronized (params)
		{
			return params.size();
		}
	}

	public String getBaseLink()
	{
		return this.baseLink;
	}

	/**
	 * @param link
	 *            The given link.
	 * @return The given link after all the enabled parameters and their values were removed from it.
	 */
	public String getLinkWithoutEnabledParams(String link)
	{
		if (link == null)
			return null;

		int queryIndex = link.indexOf('?');
		if (queryIndex == -1)
			return link;

		// Start with the link
		String result = link.substring(0, queryIndex + 1);
		String[] split = link.substring(queryIndex + 1).split("[&]");
		for (String curKeyVal : split)
		{
			String[] keyVal = curKeyVal.split("[=]");
			if (keyVal.length == 2)
			{
				String paramName = ParamItem.normalizeName(keyVal[0]);
				ParamItem paramItem = params.get(paramName);
				if (paramItem != null && paramItem.enabled)
					continue;
			}
			if (result.length() != queryIndex + 1)
				result += "&";
			result += curKeyVal;
		}
		return result;
	}

	public void printHtml(WebContext context)
	{
		context.appendString(getTableAsHtml());
	}

	/**
	 * @param paramName
	 *            Name of parameter.
	 * @return Null if the parameter was not found, or parameter record if found.
	 */
	private ParamItem getParam(String paramName)
	{
		paramName = ParamItem.normalizeName(paramName);
		if (paramName == null || paramName.isEmpty())
			return null;

		ParamItem paramItem = null;
		synchronized (params)
		{
			paramItem = params.get(paramName);
		}

		return paramItem;
	}

	/**
	 * @param paramName
	 *            Name of parameter.
	 * @return Empty if the parameter was not found, or parameter's description as displayed in table if found.
	 */
	public String getParamDescription(String paramName)
	{
		ParamItem paramItem = getParam(paramName);
		if (paramItem == null)
			return "";

		return paramItem.description;
	}

	/**
	 * @param paramName
	 *            Name of parameter.
	 * @return Zero if the parameter was not found, or current value if found (then, it also enables it).
	 */
	public double getValueAndEnable(String paramName)
	{
		ParamItem paramItem = getParam(paramName);
		if (paramItem == null)
			return 0;

		paramItem.enabled = true;
		return paramItem.value;
	}

	/**
	 * @param paramName
	 *            Name of parameter.
	 * @return Next value according to step and given current value, or zero if overflow or parameter not found.
	 */
	public double getNextValue(String paramName, double currentValue)
	{
		ParamItem paramItem = getParam(paramName);
		if (paramItem == null)
			return 0;

		double nextValue = currentValue + paramItem.step;
		if (nextValue <= paramItem.maxValue)
			return nextValue;

		if (currentValue < paramItem.maxValue)
			return paramItem.maxValue;

		return 0;
	}

	/**
	 * @param paramName
	 *            Name of parameter.
	 * @return Previous value according to step and given current value, or less than the minimum if reached the minimum
	 *         or parameter not found.
	 */
	public double getPrevValue(String paramName, double currentValue)
	{
		ParamItem paramItem = getParam(paramName);
		if (paramItem == null)
			return 0;

		double nextValue = currentValue - paramItem.step;
		if (nextValue >= paramItem.minValue)
			return nextValue;

		if (currentValue > paramItem.minValue)
			return paramItem.minValue;

		return paramItem.minValue - 1;
	}

	/**
	 * @param paramName
	 *            Name of parameter.
	 * @return Minimum value, or zero if parameter not found.
	 */
	public double getMinValue(String paramName)
	{
		ParamItem paramItem = getParam(paramName);
		if (paramItem == null)
			return 0;

		return paramItem.minValue;
	}

	public void enable(String paramName)
	{
		paramName = ParamItem.normalizeName(paramName);
		if (paramName == null || paramName.isEmpty())
			return;

		ParamItem paramItem = null;
		synchronized (params)
		{
			paramItem = params.get(paramName);
		}
		if (paramItem == null)
			return;

		paramItem.enabled = true;
	}

	public void disableAll()
	{
		synchronized (params)
		{
			for (ParamItem curParam : params.values())
			{
				curParam.enabled = false;
			}
		}

	}
}
