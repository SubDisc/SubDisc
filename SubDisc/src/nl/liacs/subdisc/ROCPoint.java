package nl.liacs.subdisc;

/**
 * Like Point2D.Double, but location can not be changed after creation.
 * Additionally, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
 * are not allowed, and both x and y must lie between [0-1].
 */
// Could be achieved by overwriting setLocation(), but this is more direct.
public class ROCPoint extends PointDouble
{
	public ROCPoint(double x, double y) {
		super(x, y);
		if (!isValid(x) || !isValid(y))
			throw new IllegalArgumentException("Coordinates must lie in interval [0-1] in both dimensions");
	}

	// -0.0 is not allowed
	private static final boolean isValid(double theNumber)
	{
		return (theNumber >= 0.0 && theNumber <= 1.0);
	}

	/**
	 * Convenience method, more intuitive way to get the FalsePositiveRate
	 * then subgroupROCPoint&nbsp;.x or subgroupROCPoint&nbsp;.getX().
	 * 
	 * @return x, better known as FalsePositiveRate.
	 */
	public double getFPR() { return super.x; }

	/**
	 * Convenience method, more intuitive way to get the TruePositiveRate
	 * then subgroupROCPoint&nbsp;.y or subgroupROCPoint&nbsp;.getY().
	 * 
	 * @return y, better known as TruePositiveRate.
	 */
	public double getTPR() { return super.y; }

	public void print()
	{
		Log.logCommandLine(this.toString());
	}

	// copy of Point2D source code.
	// Returns the hashcode for this ROCPoint.
	public int hashCode()
	{
		long bits = java.lang.Double.doubleToLongBits(x);
		bits ^= java.lang.Double.doubleToLongBits(y) * 31;
		return (((int) bits) ^ ((int) (bits >> 32)));
	}

	// copy of Point2D source code.
	// Determines whether or not two points are equal.
	// Two instances of ROCPoint are equal if the values of their x and y
	// member fields, representing their position in the coordinate space,
	// are the same.
	// Parameters:
	//    obj an object to be compared with this ROCPoint
	// Returns:
	//    true if the object to be compared is an instance of ROCPoint and
	// has the same x and y values; false otherwise.
	public boolean equals(Object object)
	{
		if (object instanceof ROCPoint)
		{
			ROCPoint r = (ROCPoint) object;
			return (this.x == r.x) && (this.y == r.y);
		}

		return super.equals(object);
	}

	@Override
	public String toString()
	{
		return "ROCPoint["+x+", "+y+"]";
	}
}