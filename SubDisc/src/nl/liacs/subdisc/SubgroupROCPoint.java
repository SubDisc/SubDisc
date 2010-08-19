package nl.liacs.subdisc;

import java.awt.geom.Point2D;

public class SubgroupROCPoint extends Point2D.Float
{
	private static final long serialVersionUID = 1L;
	public final int ID;

	/**
	 * Create a SubgroupROCPoint for a Subgroup. This is nothing more than a
	 * Point2D.Float with theSubgroup.getID() as its identifier,
	 * theSubgroup.getFalsePositiveRate() for Point2D.x and
	 * theSubgroup.getTruePositiveRate() for Point2D.y.
	 * @param theSubgroup
	 */
	public SubgroupROCPoint(Subgroup theSubgroup)
	{
		ID = theSubgroup.getID();
		super.x = theSubgroup.getFalsePositiveRate();
		super.y = theSubgroup.getTruePositiveRate();
	}

	/**
	 * Convenience methods, more intuitive way to get
	 * the FalsePositiveRate and TruePositiveRate then
	 * subgroupROCPoint.x or subgroupROCPoint.getX() and
	 * subgroupROCPoint.y or subgroupROCPoint.getY() resp.
	 * @return FPD = x, TPR = y
	 */
	public float getFPR() { return super.x; }
	public float getTPR() { return super.y; }

	/**
	 * Do no allow the X/Y coordinates to be changed.
	 * TODO use final in constructor.
	 */
	@Override
	public void setLocation(double x, double y) {}

	@Override
	public String toString()
	{
		return "Subgroup " + ID + " (FPR " + getFPR() + ", TPR "+ getTPR() + ")";
	}

}
