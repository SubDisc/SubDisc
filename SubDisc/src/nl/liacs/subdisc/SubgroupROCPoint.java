package nl.liacs.subdisc;

import java.awt.geom.Point2D;

/**
 * A SubgroupROCPoint of a Subgroup is nothing more than a Point2D.&nbsp;Float
 * with this subgroups' getID() as identifier, getFalsePositiveRate() for
 * Point2D&nbsp;.x and getTruePositiveRate() for Point2D&nbsp;.y.
 * @param theSubgroup the Subgroup for which to create the SubgroupROCPoint.
 */
public class SubgroupROCPoint extends Point2D.Float
{
	private static final long serialVersionUID = 1L;
	public final int ID;

	/**
	 * Create a SubgroupROCPoint for the Subgroup passed in as parameter.
	 * @param theSubgroup the Subgroup for which to create the SubgroupROCPoint.
	 */
	public SubgroupROCPoint(Subgroup theSubgroup)
	{
		ID = theSubgroup.getID();
		super.x = theSubgroup.getFalsePositiveRate();
		super.y = theSubgroup.getTruePositiveRate();
	}

	/**
	 * Convenience method, more intuitive way to get the FalsePositiveRate then
	 * subgroupROCPoint&nbsp;.x or subgroupROCPoint&nbsp;.getX().
	 * @return x, better known as FalsePositiveRate.
	 */
	public float getFPR() { return super.x; }

	/**
	 * Convenience methods, more intuitive way to get the TruePositiveRate then
	 * subgroupROCPoint&nbsp;.y or subgroupROCPoint&nbsp;.getY().
	 * @return y, better known as TruePositiveRate.
	 */
	public float getTPR() { return super.y; }

	// TODO use final in constructor.
	/**
	 * SubgroupROCPoint overrides the setLocation method of Point2D, because we 
	 * do no allow the X/Y coordinates to be changed.
	 */
	@Override
	public void setLocation(double x, double y) {}

	/**
	 * Override Objects toString() method to to get detailed information about
	 * this SubgroupROCPoint.
	 * @return a String representation of this SubgroupROCPoint.
	 */
	@Override
	public String toString()
	{
		return "Subgroup " + ID + " (FPR " + getFPR() + ", TPR "+ getTPR() + ")";
	}

}
