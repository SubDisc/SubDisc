package nl.liacs.subdisc;

/**
 * Like PointFloat, but two labels can be set.
 */
// old HullPoint, may be replaced by PointDouble + label
class ConvexHullPoint extends PointFloat
{
	public final float itsLabel1;
	// not final, (re)set by ConvexHull, 'safe' as class is package-private
	public float itsLabel2;

	public ConvexHullPoint(float x, float y, float theLabel1, float theLabel2)
	{
		super(x, y);
		itsLabel1 = theLabel1;
		itsLabel2 = theLabel2;
	}

	public ConvexHullPoint(ConvexHullPoint theOther)
	{
		this(theOther.x, theOther.y, theOther.itsLabel1, theOther.itsLabel2);
	}

	@Override
	public String toString()
	{
		return String.format("ConvexHullPoint[%f, %f] %f, %f", x, y, itsLabel1, itsLabel2);
	}
}
