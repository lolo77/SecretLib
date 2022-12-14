package com.secretlib.util;

import java.util.Calendar;

/**
 * 
 * @author Florent FRADET
 *
 */
public class Log
{

	public final static int TRACE = 0;
	public final static int DEBUG = 1;
	public final static int INFO = 2;
	public final static int WARN = 3;
	public final static int ERROR = 4;
	public final static int CRITICAL = 5;

	private static final String TABLABEL[] =
	{ "TRACE", "DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL" };
	private static final String TAB = "  ";

	private static int _level = ERROR; // Niveau de log par défaut
	String _className = "[null]";

	int _recursCount = 0;
	private final int RECURS_MAX = 40;

	public Log()
	{

	}


	public Log(Class<?> theClass)
	{
		_className = theClass.getSimpleName();
	}


	public Log(Class<?> theClass, int level)
	{
		this(theClass);
		_level = level;
	}

	public static int getLevel() {
		return _level;
	}

	public static void setLevel(int level) {
		_level = level;
	}

	private void out(int level, String msg)
	{
		level = (level < TRACE) ? TRACE : (level > CRITICAL) ? CRITICAL : level;
		if (_level <= level)
		{
			long t = System.currentTimeMillis() % 1000;
			int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			int m = Calendar.getInstance().get(Calendar.MINUTE);
			int s = Calendar.getInstance().get(Calendar.SECOND);

			String sp = "";
			for (int i = 0; i < Math.min(_recursCount, RECURS_MAX); i++)
			{
				sp += TAB;
			}

			String sLog = String.format("%02d",h) + ":" + String.format("%02d",m) + ":" + String.format("%02d",s) + "," + String.format("%03d",t) + "|" + String.format("%-8s", TABLABEL[level]) + "|"
					+ String.format("%-16s", _className) + "|" + sp + msg;

			if (level >= ERROR)
			{
				System.err.println(sLog);
			} else
			{
				System.out.println(sLog);
			}
		}
	}


	public boolean isTrace()
	{
		return (_level <= TRACE);
	}


	public boolean isDebug()
	{
		return (_level <= DEBUG);
	}


	public boolean isInfo()
	{
		return (_level <= INFO);
	}


	public boolean isWarn()
	{
		return (_level <= WARN);
	}


	public boolean isErr()
	{
		return (_level <= ERROR);
	}


	public boolean isCrit()
	{
		return (_level <= CRITICAL);
	}


	public void begin(String funcName)
	{
		_recursCount++;
		trace("Entered " + funcName);
	}


	public void end(String funcName)
	{
		trace("Exiting " + funcName);
		if (_recursCount > 0)
		{
			_recursCount--;
		}
	}


	public void trace(String msg)
	{
		out(TRACE, msg);
	}


	public void info(String msg)
	{
		out(INFO, msg);
	}


	public void debug(String msg)
	{
		out(DEBUG, msg);
	}


	public void warn(String msg)
	{
		out(WARN, msg);
	}


	public void error(String msg)
	{
		out(ERROR, msg);
	}


	public void crit(String msg)
	{
		out(CRITICAL, msg);
	}

}
