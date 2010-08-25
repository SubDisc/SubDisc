/**
 * TODO This class does exactly the same as the Point2D.Double class. What is
 * the use of this class?
 */

package nl.liacs.subdisc;

public class DataPoint
{
	private final double itsX;
	private final double itsY;
	
	public DataPoint(double theX, double theY)
	{
		itsX = theX;
		itsY = theY;
	}

	public double getX()
	{
		return itsX;		
	}

	public double getY()
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
