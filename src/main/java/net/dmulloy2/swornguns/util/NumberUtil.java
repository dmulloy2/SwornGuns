package net.dmulloy2.swornguns.util;

/**
 * Util for managing Numbers
 * 
 * @author dmulloy2
 */

public class NumberUtil
{
	private NumberUtil() { }

	public static int toInt(Object object)
	{
		if (object instanceof Number)
		{
			return ((Number) object).intValue();
		}

		try
		{
			return Integer.valueOf(object.toString());
		}
		catch (Exception e)
		{
		}

		return -1;
	}

	public static boolean isInt(Object object)
	{
		return toInt(object) != -1;
	}

	public static float toFloat(Object object)
	{
		if (object instanceof Number)
		{
			return ((Number) object).floatValue();
		}

		try
		{
			return Float.valueOf(object.toString());
		}
		catch (Exception e)
		{
		}

		return -1;
	}

	public static boolean isFloat(Object object)
	{
		return toFloat(object) != -1;
	}

	public static double toDouble(Object object)
	{
		if (object instanceof Number)
		{
			return ((Number) object).doubleValue();
		}

		try
		{
			return Double.valueOf(object.toString());
		}
		catch (Exception e)
		{
		}

		return -1;
	}

	public static boolean isDouble(Object object)
	{
		return toDouble(object) != -1;
	}

	public static long toLong(Object object)
	{
		if (object instanceof Number)
		{
			return ((Number) object).longValue();
		}

		try
		{
			return Long.valueOf(object.toString());
		}
		catch (Exception e)
		{
		}

		return -1;
	}

	public static boolean isLong(Object object)
	{
		return toLong(object) != -1;
	}

	public static short toShort(Object object)
	{
		if (object instanceof Number)
		{
			return ((Number) object).shortValue();
		}

		try
		{
			return Short.valueOf(object.toString());
		}
		catch (Exception e)
		{
		}

		return -1;
	}

	public static boolean isShort(Object object)
	{
		return toShort(object) != -1;
	}

	public static byte toByte(Object object)
	{
		if (object instanceof Number)
		{
			return ((Number) object).byteValue();
		}

		try
		{
			return Byte.valueOf(object.toString());
		}
		catch (Exception e)
		{
		}

		return -1;
	}

	public static boolean isByte(Object object)
	{
		return toByte(object) != -1;
	}
}