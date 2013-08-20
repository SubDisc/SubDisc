package nl.liacs.subdisc;

/**
 * A SubgroupROCPoint of a {@link Subgroup} is nothing more than a 
 * {@link PointDouble} with that Subgroups' {@link Subgroup#getID() getID()} as
 * identifier, {@link Subgroup#getFalsePositiveRate() getFalsePositiveRate()}
 * for {@link PointDouble.x x} and {@link Subgroup#getTruePositiveRate()} for
 * {@link PointDouble.y y}.
 */
/*
 * FIXME MM
 * ID may not be stable throughout the lifetime of a Subgroup
 * this would lead to a mismatch between Subgroup.getID() and ID
 */
public class SubgroupROCPoint extends PointDouble
{
	public final int ID;
	private final Subgroup itsSubgroup;

	/**
	 * Creates a SubgroupROCPoint for the {@link Subgroup} passed in as
	 * parameter.
	 * 
	 * @param theSubgroup the Subgroup for which to create the
	 * SubgroupROCPoint.
	 */
	public SubgroupROCPoint(Subgroup theSubgroup)
	{
		super(theSubgroup.getFalsePositiveRate(), theSubgroup.getTruePositiveRate());
		itsSubgroup = theSubgroup;
		ID = itsSubgroup.getID();
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

	/**
	 * Overrides <code>Object</code>s' <code>toString()</code> method to to
	 * get detailed information about this SubgroupROCPoint.
	 * 
	 * @return a <code>String</code> representation of this
	 * SubgroupROCPoint.
	 */
	@Override
	public String toString()
	{
		return "Subgroup " + ID + " (FPR " + getFPR() + ", TPR "+ getTPR() + ")";
	}
}
