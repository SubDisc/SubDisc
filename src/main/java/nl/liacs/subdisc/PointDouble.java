package nl.liacs.subdisc;

/**
 * Like Point2D.Double, but location can not be changed after creation.
 * Additionally, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
 * are not allowed.
 */
// Could be achieved by overwriting setLocation(), but this is more direct.
public class PointDouble
{
	public final double x;
	public final double y;

	public PointDouble(double x, double y)
	{
		if (!isValid(x) || !isValid(y))
			throw new IllegalArgumentException("NaN and (+/-)Infinity are not allowed");

		this.x = x;
		this.y = y;
	}

	private static final boolean isValid(double theNumber)
	{
		return !Double.isNaN(theNumber) && !Double.isInfinite(theNumber);
	}

	public void print()
	{
		Log.logCommandLine(this.toString());
	}

	@Override
	public String toString()
	{
		return "PointDouble["+x+", "+y+"]";
	}
}
