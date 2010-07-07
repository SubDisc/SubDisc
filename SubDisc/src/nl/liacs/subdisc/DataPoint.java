package nl.liacs.subdisc;

public class DataPoint
{
	private double itsX;
	private double itsY;
	
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
		return (aDataPoint.getX()==getX() && aDataPoint.getY() == getY());
	}

}
