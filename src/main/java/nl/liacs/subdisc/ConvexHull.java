package nl.liacs.subdisc;

/*
 * Class containing for maintaining and constructing convex hulls in 2D.
 * A hull is split into an upper and lower part for convenience.
 * Sorted by x coordinate.
 */
public final class ConvexHull
{
	private HullPoint [][] itsHullPoints;

	private ConvexHull()
	{
		itsHullPoints = new HullPoint[2][];
	}

	/**
	 * Constructs a single point hull, using {@link HullPoint}.
	 * 
	 * @see HullPoint
	 */
	public ConvexHull(int theX, int theY, float theLabel1, float theLabel2)
	{
		this();

		for (int aSide = 0; aSide < 2; aSide++)
		{
			itsHullPoints[aSide] = new HullPoint[1];
			itsHullPoints[aSide][0] = new HullPoint(theX, theY, theLabel1, theLabel2);
		}
	}

	public int getSize(int theSide)
	{
		return itsHullPoints[theSide].length;
	}

	public HullPoint getPoint(int theSide, int theIndex)
	{
		return itsHullPoints[theSide][theIndex];
	}

	/* assumes points on upper and lower hull are already sorted by x
	 * coordinate hence linear time complexity
	 */
	private void grahamScanSorted()
	{
		for (int aSide = 0; aSide < 2; aSide++)
		{
			int aLen = itsHullPoints[aSide].length;

			if (aLen < 3)
				continue;

			long aSign = (aSide == 0) ? 1L : -1L;

			int aPruneCnt = 0;
			int[] aNextList = new int[aLen];
			int[] aPrevList = new int[aLen];
			for (int i = 0; i < aLen; i++)
			{
				aNextList[i] = i + 1;
				aPrevList[i] = i - 1;
			}

			int aCurr = 0;
			while (aNextList[aCurr] < aLen && aNextList[aNextList[aCurr]] < aLen )
			{
				// FIXME MM
				// use long instead of float
				// HullPoint itsX and itsY are int
				// a conversion to float will lose precision
				// when int > 2^24 (16 million)
				// this can happen for large datasets where a
				// single attribute value has more than 16M
				// occurrences (positive/ negative count)
				// a long can be used safely without overflow
				// in the calculation below
				// as the biggest difference between two ints
				// can only be -2^31 for 0 and Integer.MIN_VALUE
				// and (-2^31) * (-2^31) > Long.MIN_VALUE
				// similar reasoning holds for Long.MAX_VALUE
				// double would not suffice,as it has only 52
				// significant bits, it can represent exactly
				// 2^53 ints
				// NOTE that Integer.MAX_VALUE is the largest
				// possible positive/ negative count, as all
				// Column data is in a single array (with a 
				// MAX_SIZE = 2^31 as defined by the JLS)
				int aX1 = itsHullPoints[aSide][aCurr].itsX;
				int aY1 = itsHullPoints[aSide][aCurr].itsY;
				int aX2 = itsHullPoints[aSide][aNextList[aCurr]].itsX;
				int aY2 = itsHullPoints[aSide][aNextList[aCurr]].itsY;
				int aX3 = itsHullPoints[aSide][aNextList[aNextList[aCurr]]].itsX;
				int aY3 = itsHullPoints[aSide][aNextList[aNextList[aCurr]]].itsY;

				// conversion to long protects against overflow
				long aY21 = aY2-aY1;
				long aX32 = aX3-aX2;
				//
				long aY32 = aY3-aY2;
				long aX21 = aX2-aX1;

				if ((aSign * aY21 * aX32) > (aSign * aY32 * aX21)) //convex, go to next point
				{
					aCurr = aNextList[aCurr];
				}
				else // not convex, remove middle point, go to previous point
				{
					aPrevList[aNextList[aNextList[aCurr]]] = aCurr;
					aNextList[aCurr] = aNextList[aNextList[aCurr]];
					aPruneCnt++;
					if (aCurr > 0)
						aCurr = aPrevList[aCurr];
				}
			}

			// put convexhullpoints in a new list
			HullPoint[] aNewHullPoints = new HullPoint[aLen - aPruneCnt];
			aCurr = 0;
			for (int i = 0; i < aNewHullPoints.length; i++)
			{
				aNewHullPoints[i] = itsHullPoints[aSide][aCurr];
				aCurr = aNextList[aCurr];
			}
			itsHullPoints[aSide] = aNewHullPoints;
		}
	}

	/*
	 * assumes this.x < theOther.x, i.e., no overlap between the hulls
	 * hence linear time complexity
	 */
	public ConvexHull concatenate(ConvexHull theOther)
	{
		ConvexHull aResult = new ConvexHull();

		for (int aSide = 0; aSide < 2; aSide++)
		{
			int aLen1 = itsHullPoints[aSide].length;
			int aLen2 = theOther.itsHullPoints[aSide].length;
			aResult.itsHullPoints[aSide] = new HullPoint[aLen1 + aLen2];
			for (int i = 0; i < aLen1; i++)
				aResult.itsHullPoints[aSide][i] = itsHullPoints[aSide][i];
			for (int i = 0; i < aLen2; i++)
				aResult.itsHullPoints[aSide][aLen1+i] = theOther.itsHullPoints[aSide][i];
		}

		aResult.grahamScanSorted();

		return aResult;
	}

//	/* 
//	 * Compute the Minkowski difference of two convex polygons.
//	 * Again, linear time complexity.
//	 */
//	public ConvexHull minkowskiDifference(ConvexHull theOther)
//	{
//		return minkowskiDifference(theOther, true);
//	}

	public ConvexHull minkowskiDifference(ConvexHull theOther, boolean thePruneDegenerate)
	{
		ConvexHull aResult = new ConvexHull();

		for (int aSide = 0 ; aSide < 2; aSide++)
		{
			double aSign = (aSide==0) ? 1.0 : -1.0;

			int aLen1 = itsHullPoints[aSide].length;
			int aLen2 = theOther.itsHullPoints[1-aSide].length;
			HullPoint[] aHull = new HullPoint[aLen1 + aLen2];
			int aHullSize = 0;

			int i = 0;
			int j = aLen2 - 1;
			double aSlope1, aSlope2;
			while (i < aLen1 - 1 || j > 0)
			{
				if (i == aLen1 - 1)
					aSlope1 = aSign * Double.NEGATIVE_INFINITY; // dummy for last
				else
				{
					//aSlope1 = (itsHullPoints[aSide][i+1].itsY - itsHullPoints[aSide][i].itsY) / (itsHullPoints[aSide][i+1].itsX - itsHullPoints[aSide][i].itsX + itsHullPoints[aSide][i+1].itsY - itsHullPoints[aSide][i].itsY);
					// safe conversion of int to double
					double aYdiff = (itsHullPoints[aSide][i+1].itsY - itsHullPoints[aSide][i].itsY);
					double aXdiff = (itsHullPoints[aSide][i+1].itsX - itsHullPoints[aSide][i].itsX);
					// sum does not lose precision as
					// arguments are actually ints
					aSlope1 = aYdiff / (aXdiff + aYdiff);
				}
				if (j == 0)
					aSlope2 = aSign * Double.NEGATIVE_INFINITY; // dummy for last
				else
				{
//					aSlope2 = (theOther.itsHullPoints[1-aSide][j-1].itsY - theOther.itsHullPoints[1-aSide][j].itsY) / (theOther.itsHullPoints[1-aSide][j-1].itsX - theOther.itsHullPoints[1-aSide][j].itsX + theOther.itsHullPoints[1-aSide][j-1].itsY - theOther.itsHullPoints[1-aSide][j].itsY);
					// see comments above
					double aYdiff = theOther.itsHullPoints[1-aSide][j-1].itsY - theOther.itsHullPoints[1-aSide][j].itsY;
					double aXdiff = theOther.itsHullPoints[1-aSide][j-1].itsX - theOther.itsHullPoints[1-aSide][j].itsX;
					aSlope2 = aYdiff / (aXdiff + aYdiff);
				}

				if (aSign * aSlope1 >= aSign * aSlope2)
				{
					aHull[aHullSize] = new HullPoint(itsHullPoints[aSide][i]);
					aHull[aHullSize].itsLabel2 = aSide;
					aHullSize++;
					i++;
				}
				if (aSign * aSlope1 <= aSign * aSlope2)
				{
					aHull[aHullSize] = new HullPoint(theOther.itsHullPoints[1-aSide][j]);
					aHull[aHullSize].itsLabel2 = 1 - aSide;
					aHullSize++;
					j--;
				}
			}
			aHull[aHullSize] = new HullPoint(itsHullPoints[aSide][i]);
			aHull[aHullSize].itsLabel2 = aSide;
			aHullSize++;
			aHull[aHullSize] = new HullPoint(theOther.itsHullPoints[1-aSide][j]);
			aHull[aHullSize].itsLabel2 = 1 - aSide;
			aHullSize++;

			// build final hull
			HullPoint[] aNewHull = new HullPoint[aHullSize];

			for (int k = 0; k < aHullSize; k++)
			{
				int aVertex = k;
				int aNextVertex = (aVertex + 1) % aHullSize;
				while (aHull[aVertex].itsLabel2 == aHull[aNextVertex].itsLabel2)
					aNextVertex = (aNextVertex + 1) % aHullSize;
				if (aHull[aNextVertex].itsLabel1 >= aHull[aVertex].itsLabel1)
				{
					int tmp=aNextVertex; aNextVertex=aVertex; aVertex=tmp;
				}
				// set its own label and the other's label; for intervals these are the rhs and lhs end points, resp.
				aNewHull[k] = new HullPoint(aHull[aVertex].itsX - aHull[aNextVertex].itsX, aHull[aVertex].itsY - aHull[aNextVertex].itsY, aHull[aVertex].itsLabel1, aHull[aNextVertex].itsLabel1);
			}

			aResult.itsHullPoints[aSide] = aNewHull;
		}

		if (thePruneDegenerate)
			aResult.grahamScanSorted();

		return aResult;
	}

	/**
	 * Simple class for 2D points, having 2 labels, used in
	 * {@link ConvexHull}.
	 * 
	 * @see ConvexHull
	 */
	// static as it need no reference to (members of) the enclosing class
	static final class HullPoint
	{
		public final int itsX;
		public final int itsY;
		public final float itsLabel1;
		// not final, (re)set by ConvexHull, so it is private
		// external classes will have to use getLabel2() to obtain value
		// but can not change it
		private float itsLabel2;

		private HullPoint(int theX, int theY, float theLabel1, float theLabel2)
		{
			itsX = theX;
			itsY = theY;
			itsLabel1 = theLabel1;
			itsLabel2 = theLabel2;
		}

		private HullPoint(HullPoint theOther)
		{
			this(theOther.itsX, theOther.itsY, theOther.itsLabel1, theOther.itsLabel2);
		}

		/**
		 * External classes can only obtain the value for the second
		 * label of this {@link HullPoint} by calling this method, they
		 * can not change its value.
		 * 
		 * @return the value of the second label for this
		 * {@link HulPoint}.
		 */
		public float getLabel2()
		{
			return itsLabel2;
		}

		public void print()
		{
			Log.logCommandLine("HullPoint (" + itsX + "," + itsY + ") " + itsLabel1 + ", " + itsLabel2);
		}
	}
}
