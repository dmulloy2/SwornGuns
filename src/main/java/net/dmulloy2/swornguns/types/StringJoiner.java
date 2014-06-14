/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.types;

/**
 * StringJoiner is used to construct a sequence of characters separated by a
 * delimiter
 * 
 * @author dmulloy2
 */

public class StringJoiner
{
	private String glue;
	private StringBuilder builder;

	/**
	 * Constructs a new StringJoiner with a given delimiter
	 * 
	 * @param glue
	 *        - Delimiter
	 */
	public StringJoiner(String glue)
	{
		this.glue = glue;
		this.builder = new StringBuilder();
	}

	/**
	 * Appends a given {@link String} to this StringJoiner
	 * 
	 * @param string
	 *        - String to append
	 * @return This, for chaining
	 */
	public final StringJoiner append(final String string)
	{
		if (! string.isEmpty())
			builder.append(string + glue);
		return this;
	}

	/**
	 * Appends an {@link Iterable} set of {@link String}s
	 * 
	 * @param strings
	 *        - Strings to append
	 * @return This, for chaining
	 */
	public final StringJoiner appendAll(final Iterable<String> strings)
	{
		for (String string : strings)
		{
			append(string);
		}

		return this;
	}

	/**
	 * Appends a given array of {@link String}s to this StringJoiner
	 * 
	 * @param strings
	 *        - Strings to append
	 * @return This, for chaining
	 */
	public final StringJoiner appendAll(final String... strings)
	{
		for (String string : strings)
		{
			append(string);
		}

		return this;
	}

	/**
	 * Resets this {@link StringJoiner}'s string value
	 * 
	 * @return This, for chaining
	 */
	public final StringJoiner newString()
	{
		this.builder = new StringBuilder();
		return this;
	}

	/**
	 * Sets a new delimiter for this {@link StringJoiner} while keeping the
	 * string value.
	 * 
	 * @param glue
	 *        - New delimiter
	 * @return This, for chaining
	 */
	public final StringJoiner setGlue(final String glue)
	{
		this.glue = glue;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String toString()
	{
		if (builder.lastIndexOf(glue) >= 0)
		{
			builder.delete(builder.lastIndexOf(glue), builder.length());
		}

		return builder.toString();
	}
}