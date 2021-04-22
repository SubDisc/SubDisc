package nl.liacs.subdisc.postprocess;

import java.util.*;

public final class StringAsIntegerComparator implements Comparator<String>
{
	public static final StringAsIntegerComparator INSTANCE;
	static { INSTANCE = new StringAsIntegerComparator(); }

	// uninstantiable
	private StringAsIntegerComparator(){};

	/** throws error on invalid input */
	@Override
	public int compare(String a, String b)
	{
		return Integer.parseInt(a) - Integer.parseInt(b);
	}
}
