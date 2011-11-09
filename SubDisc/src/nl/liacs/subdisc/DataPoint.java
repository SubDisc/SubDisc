/**
 * TODO This class does exactly the same as the Point2D.Double class. What is
 * the use of this class?
 */

package nl.liacs.subdisc;

public class DataPoint
{
	private final float itsX;
	private final float itsY;

	public DataPoint(float theX, float theY)
	{
		itsX = theX;
		itsY = theY;
	}

	public float getX()
	{
		return itsX;
	}

	public float getY()
	{
		return itsY;
	}

	@Override
	public boolean equals(Object theOtherObject)
	{
		DataPoint aDataPoint = (DataPoint) theOtherObject;
		return (aDataPoint.getX()==itsX && aDataPoint.getY() == itsY);
	}

}
