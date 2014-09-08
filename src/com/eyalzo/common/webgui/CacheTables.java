package com.eyalzo.common.webgui;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Cache {@link DisplayTable} in web-gui handlers (typically extend
 * {@link HttpHandler}.
 * 
 * @author Eyal Zohar
 */
public class CacheTables
{
	private final long						ttlMillis;
	private static final long				DEFAULT_TTL_MILLIS	= 5 * 60 * 1000;
	private HashMap<String, DisplayTable>	cachedTables		= new HashMap<String, DisplayTable>();

	public CacheTables(long ttlMillis)
	{
		if (ttlMillis <= 0)
			this.ttlMillis = DEFAULT_TTL_MILLIS;
		else
			this.ttlMillis = ttlMillis;
	}

	/**
	 * Remove all expired tables.
	 */
	public void cleanup()
	{
		synchronized (cachedTables)
		{
			for (Iterator<DisplayTable> it = cachedTables.values().iterator(); it.hasNext();)
			{
				DisplayTable table = it.next();
				if (table.getCreateTime() < System.currentTimeMillis() - ttlMillis)
					it.remove();
			}
		}
	}

	/**
	 * Look for valid table that was recently saved under a name with
	 * parameters. For name that is only the command use
	 * {@link #getCachedTable(WebContext)}.
	 * 
	 * @return Table if saved under the same command and still valid (by time),
	 *         or null otherwise.
	 */
	public DisplayTable getCachedTable(String name, WebContext context)
	{
		synchronized (cachedTables)
		{
			DisplayTable table = cachedTables.get(name);
			if (table == null)
				return null;

			// If expired or the user asked specifically to refresh
			if (table.getCreateTime() < System.currentTimeMillis() - ttlMillis
					|| context.getParamAsBool("refreshtable", false) == true)
			{
				cachedTables.remove(name);
				return null;
			}

			return table;
		}
	}

	/**
	 * Look for valid table that was recently saved under the command (URL up to
	 * the ? sign).
	 * 
	 * @return Table if saved under the same command and still valid (by time),
	 *         or null otherwise.
	 */
	public DisplayTable getCachedTableByCommand(WebContext context)
	{
		return getCachedTable(context.command, context);
	}

	/**
	 * Look for valid table that was recently saved under the command (URL up to
	 * the ? sign).
	 * 
	 * @return Table if saved under the same command and still valid (by time),
	 *         or null otherwise.
	 */
	public DisplayTable getCachedTableByCommandAndParam(String paramName, WebContext context)
	{
		String paramValue = context.getParamAsString(paramName, "");
		return getCachedTable(context.command + "?" + paramName + "=" + paramValue, context);
	}

	public void addCachedTableByCommandAndParam(String paramName, WebContext context, DisplayTable table)
	{
		String paramValue = context.getParamAsString(paramName, "");
		addCachedTable(context.command + "?" + paramName + "=" + paramValue, table);
	}

	/**
	 * @param name
	 *            The name of the table, which is typically the command and/or a
	 *            parameter and/or its value.
	 * @param table
	 *            The filled table to be saved here.
	 */
	public void addCachedTable(String name, DisplayTable table)
	{
		if (name == null || name.isEmpty())
			return;

		synchronized (cachedTables)
		{
			cachedTables.put(name, table);
		}
	}
}
