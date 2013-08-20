package nl.liacs.subdisc;

/**
 * Like Point2D.Float, but location can not be changed after creation.
 */
// Could be achieved by overwriting setLocation(), but this is more direct.
public class PointFloat
{
	public final float x;
	public final float y;

	public PointFloat(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	public PointFloat(PointFloat theOther)
	{
		this(theOther.x, theOther.y);
	}

	@Override
	public String toString()
	{
		return "PointFloat["+x+", "+y+"]";
	}

	public void print()
	{
		Log.logCommandLine(this.toString());
	}
}