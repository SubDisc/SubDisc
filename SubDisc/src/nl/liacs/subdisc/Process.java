package nl.liacs.subdisc;

import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.Column.*;
import nl.liacs.subdisc.gui.*;

public class Process
{
	// leave at false in svn head
	private static final boolean CAUC_LIGHT = false;
	private static boolean CAUC_HEAVY = false;
	private static final boolean CAUC_HEAVY_CONVEX = false; // select subgroups on convex hull if true, select top-1 if false
	static final boolean ROC_BEAM_TEST = false;

	public static SubgroupDiscovery runSubgroupDiscovery(Table theTable, int theFold, BitSet theBitSet, SearchParameters theSearchParameters, boolean showWindows, int theNrThreads, JFrame theMainWindow)
	{
		TargetType aTargetType = theSearchParameters.getTargetConcept().getTargetType();

		if (!TargetType.isImplemented(aTargetType))
			return null;

		SubgroupDiscovery aSubgroupDiscovery = null;

		switch (aTargetType)
		{
			case SINGLE_NOMINAL :
			{
				//recompute itsPositiveCount, as we may be dealing with cross-validation here, and hence a smaller number
				TargetConcept aTargetConcept = theSearchParameters.getTargetConcept();
				String aTargetValue = aTargetConcept.getTargetValue();
				int itsPositiveCount = aTargetConcept.getPrimaryTarget().countValues(aTargetValue);

				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, itsPositiveCount, theMainWindow);
				break;
			}
			case SINGLE_NUMERIC :
			{
				// new runCAUC() receives result after SD.mine()
				// not fully implemented yet
				if (CAUC_HEAVY)
				{
					caucHeavy(theTable, theFold, theBitSet, theSearchParameters, showWindows, theNrThreads);
					return null;
				}
				else if (SubgroupDiscovery.TEMPORARY_CODE)
				{
					temporaryCode(theTable, theSearchParameters);
					return null;
				}
				else
				{
					//recompute this number, as we may be dealing with cross-validation here, and hence a different value
					float itsTargetAverage = theSearchParameters.getTargetConcept().getPrimaryTarget().getAverage();
					Log.logCommandLine("average: " + itsTargetAverage);
					aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, itsTargetAverage, theMainWindow);
				}
				break;
			}
			case MULTI_NUMERIC :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theTable, theMainWindow, theSearchParameters);
				break;
			}
			case MULTI_LABEL :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, theMainWindow);
				break;
			}
			case DOUBLE_REGRESSION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, true, theMainWindow);
				break;
			}
			case DOUBLE_CORRELATION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, false, theMainWindow);
				break;
			}
			case SCAPE :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theMainWindow, theSearchParameters, theTable);
				break;
			}
			case LABEL_RANKING :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theMainWindow, theTable);
				break;
			}
			default :
			{
				throw new AssertionError(String.format("%s: %s '%s' not implemented",
									Process.class.getName(),
									TargetType.class.getName(),
									aTargetType));
			}
		}

		long aBegin = System.currentTimeMillis();
		aSubgroupDiscovery.mine(System.currentTimeMillis(), theNrThreads);
		// if 2nd argument to above mine() is < 0, you effectively run:
		//aSubgroupDiscovery.mine(System.currentTimeMillis());

		long anEnd = System.currentTimeMillis();
		float aMaxTime = theSearchParameters.getMaximumTime();

		if (aMaxTime > 0.0f && (anEnd > (aBegin + (aMaxTime * 60_000f))))
		{
			String aMessage = "Mining process ended prematurely due to time limit.";
			if (showWindows)
				JOptionPane.showMessageDialog(null,
								aMessage,
								"Time Limit",
								JOptionPane.INFORMATION_MESSAGE);
			else
				Log.logCommandLine(aMessage);
		}

		// called by SubgroupDiscovery.mine(), pure time of mining task, not any
		// sorting
		// NOTE aborted prematurely message not comes after this message
//		echoMiningEnd(anEnd - aBegin, aSubgroupDiscovery.getNumberOfSubgroups());

		// FIXME MM for now hack this into p-value column
		aSubgroupDiscovery.getResult().markAlternativeDescriptions();

		if (showWindows)
		{
			/*
			 * Subgroup members need to be revived
			 * if after this experiment the Table used for this
			 * experiment is altered, it may be impossible to
			 * (correctly) re-create the Subgroup members from the
			 * Subgroup conditions
			 * (for example, when the AttributeType of a
			 * Condition.Column is altered, or the missing value for
			 * a Column is altered)
			 * reviving need not be done for other settings
			 * as none will need access to (unmodified) members
			 * when no window is shown, there is no danger of a
			 * an altered Table (through the GUI at least)
			 */
			for (Subgroup s : aSubgroupDiscovery.getResult())
				s.reviveMembers();
			new ResultWindow(theTable, aSubgroupDiscovery, theFold, theBitSet);
		}

		if (CAUC_LIGHT)
			caucLight(aSubgroupDiscovery, theBitSet);

/*		// temporary bonus results for CAUC experimentation
		SubgroupSet aSDResult = aSubgroupDiscovery.getResult();
		boolean aCommandlinelogState = Log.COMMANDLINELOG;
		Log.COMMANDLINELOG = false;
		SubgroupSet aSubgroupSetWithEntropy = aSDResult.getPatternTeam(theTable, aSDResult.size());

		Log.COMMANDLINELOG = aCommandlinelogState;
		Log.logCommandLine("======================================================");
		Log.logCommandLine("Simple Subgroup Set Size  : " + aSubgroupSetWithEntropy.size());
		Log.logCommandLine("Joint Entropy             : " + aSubgroupSetWithEntropy.getJointEntropy());
		Log.logCommandLine("Entropy / Set Size        : " + aSubgroupSetWithEntropy.getJointEntropy()/aSubgroupSetWithEntropy.size());
		Log.logCommandLine("Subgroups : ");
		for (Subgroup s : aSubgroupSetWithEntropy)
			Log.logCommandLine("    "+s.getConditions().toString());
*/		// end temp

		// FIXME MM temporary, force AUC + debug print for ROC classes
		if (ROC_BEAM_TEST && aTargetType == TargetType.SINGLE_NOMINAL)
			aSubgroupDiscovery.getResult().getROCList();

		return aSubgroupDiscovery;
	}

	private static void caucLight(SubgroupDiscovery theSubgroupDiscovery, BitSet theBitSet)
	{
		assert theSubgroupDiscovery.getSearchParameters().getTargetConcept().getTargetType() == TargetType.SINGLE_NUMERIC;

		final Column aTarget = theSubgroupDiscovery.getSearchParameters().getTargetConcept().getPrimaryTarget();
		final SubgroupSet aSet = theSubgroupDiscovery.getResult();
		final BitSet aMembers = membersCheck(theBitSet, aTarget.size());
		final float[] aDomain = aTarget.getUniqueNumericDomain(aMembers);

		// last index is whole dataset
		List<List<Double>> statistics = new ArrayList<List<Double>>(aDomain.length-1);
		for (int i = 0, j = aDomain.length-1; i < j; ++i)
		{
			BitSet aCAUCSet = (BitSet) aMembers.clone();
			caucMembers(aTarget, aDomain[i], aCAUCSet);

			// hack to use binary target for numeric target
			aSet.setBinaryTarget(aCAUCSet);

			statistics.add(compileStatistics(aDomain[i],
							aCAUCSet.cardinality(),
							aSet));
		}
		// dump results
		caucWrite("caucLight", aTarget, statistics);
	}

	private static void caucHeavy(Table theTable, int theFold, BitSet theBitSet, SearchParameters theSearchParameters, boolean showWindows, int theNrThreads)
	{
		// set to false, so normal SD is run
		CAUC_HEAVY = false;
		// use 'showWindows = false' below to avoid numerous windows
		final boolean showWindowsSetting = showWindows;

		final Column aBackup = theSearchParameters.getTargetConcept().getPrimaryTarget().copy();
		final BitSet aMembers = membersCheck(theBitSet, aBackup.size());
		final float[] aDomain = aBackup.getUniqueNumericDomain(aMembers);

		final List<Column> aColumns = theTable.getColumns();
		final String aName = aBackup.getName();
		final String aShort = aBackup.getShort();
		final int anIndex = aBackup.getIndex();
		final int aNrRows = aBackup.getIndex();

		// column will be binary instead of numeric
		final TargetConcept tc = theSearchParameters.getTargetConcept();
		tc.setTargetType(TargetType.SINGLE_NOMINAL.GUI_TEXT);
		tc.setTargetValue("1");
		// set an alternative quality measure
		final QM backupQM = theSearchParameters.getQualityMeasure();
		// XXX WRACC is used, but there is no motivation for this choice
		final QM altQM = QM.WRACC;
		theSearchParameters.setQualityMeasure(altQM);
		// set an alternative quality measure minimum
		final float backupMM = theSearchParameters.getQualityMeasureMinimum();
		//theSearchParameters.setQualityMeasureMinimum(Float.parseFloat(altQM.MEASURE_DEFAULT));
		// XXX WOUTER uses 0.01 to compare to old results
		// QualityMeasure.getMeasureMinimum(WRACC) changed from 0.01 to 0.02 in QM in r1282 (no mention in log)
		// WRACC.MEASURE_DEFAULT changed from 0.01 to 0.02 in QM in r1569 (synch of both implementations)
		theSearchParameters.setQualityMeasureMinimum(0.01f);

		Comparator<Subgroup> cmp = new SubgroupConditionListComparator();
		SubgroupSet aHeavySubgroupSet = new SubgroupSet(cmp);

		// last index is whole dataset
		for (int i = 0, j = aDomain.length-1; i < j; ++i)
		{
			BitSet aCAUCSet = (BitSet) aMembers.clone();
			caucMembers(aBackup, aDomain[i], aCAUCSet);

			// create temporary Column
			Column aColumn = new Column(aName,
							aShort,
							AttributeType.BINARY,
							anIndex,
							aNrRows);

			// set Column members
			for (int k = 0, m = aBackup.size(); k < m; ++k)
				aColumn.add(aCAUCSet.get(k));

			// use Column in Table
			aColumns.set(anIndex, aColumn);

			// set the new column as primary target
			theSearchParameters.getTargetConcept().setPrimaryTarget(aColumn);

			// run SD
			boolean aCommandlinelogState = Log.COMMANDLINELOG;
			Log.COMMANDLINELOG = false;
			SubgroupDiscovery sd =
				runSubgroupDiscovery(theTable, theFold, aMembers, theSearchParameters, false, theNrThreads, null);
			Log.COMMANDLINELOG = aCommandlinelogState;

			// For seeing the intermediate ROC curves, uncomment the next line
			//new ROCCurveWindow(sd.getResult(), theSearchParameters, sd.getQualityMeasure());

			Log.logCommandLine("Threshold value : " + aDomain[i]);

			if (CAUC_HEAVY_CONVEX)
			{
				// this seems pointless, but the ROC curve needs to be computed to prevent the next line from NullPointerError'ing
				ROCCurve aROCCurve = new ROCCurve(sd.getResult(), theSearchParameters, sd.getQualityMeasure());

				SubgroupSet aROCSubgroups = sd.getResult().getROCListSubgroupSet();

				// force update(), should have been in .getROCListSubgroupSet()
				aROCSubgroups.size();

				//Log.logCommandLine("ROC subgroups : " + aSize);
				for (Subgroup s : aROCSubgroups)
					Log.logCommandLine("    " + s.getConditions().toString());

				//select convex hull subgroups from the resulting subgroup set
				aHeavySubgroupSet.addAll(aROCSubgroups);

				// compile statistics
//				statistics.add(compileStatistics(aDomain[i],
//								aCAUCSet.cardinality(),
//								sd.getResult()));
			}
			else
			{
				SubgroupSet aResult = sd.getResult();
				int aSize = aResult.size();
				if (aSize>0)
				{
					Subgroup aTopOneSubgroup = aResult.first();
					Log.logCommandLine("Subgroup : ");
					Log.logCommandLine("    " + aTopOneSubgroup.getConditions().toString());
					aHeavySubgroupSet.add(aTopOneSubgroup);
				}
			}
		}

		// dump results
//		caucWrite("caucHeavy", aBackup, statistics);

		Log.logCommandLine("======================================================");
		Log.logCommandLine("Diverse Subgroup Set Size : " + aHeavySubgroupSet.size());
		Log.logCommandLine("Subgroups : ");
		for (Subgroup s : aHeavySubgroupSet)
			Log.logCommandLine("    "+s.getConditions().toString());
		boolean aCommandlinelogState = Log.COMMANDLINELOG;
		Log.COMMANDLINELOG = false;
		SubgroupSet aSubgroupSetWithEntropy = aHeavySubgroupSet.getPatternTeam(theTable, aHeavySubgroupSet.size());
		Log.COMMANDLINELOG = aCommandlinelogState;
//		Log.logCommandLine("Joint Entropy             : " + aHeavySubgroupSet.getJointEntropy());
//		Log.logCommandLine("Entropy / Set Size        : " + aHeavySubgroupSet.getJointEntropy()/aHeavySubgroupSet.size());
		Log.logCommandLine("Joint Entropy             : " + aSubgroupSetWithEntropy.getJointEntropy());
		Log.logCommandLine("Entropy / Set Size        : " + aSubgroupSetWithEntropy.getJointEntropy()/aHeavySubgroupSet.size());

		// restore original Column
		aColumns.set(anIndex, aBackup);
		tc.setPrimaryTarget(aBackup);
		tc.setTargetType(TargetType.SINGLE_NUMERIC.GUI_TEXT);
		// restore original SearchParameters
		theSearchParameters.setQualityMeasure(backupQM);
		theSearchParameters.setQualityMeasureMinimum(backupMM);

		// back to start
		CAUC_HEAVY = true;
		showWindows = showWindowsSetting;
	}

	private static BitSet membersCheck(BitSet theBitSet, int theSize)
	{
		if (theBitSet != null)
			return theBitSet;
		else
		{
			BitSet aMembers = new BitSet(theSize);
			aMembers.set(0, theSize);
			return aMembers;
		}
	}

	// simple updating of members bitset is possible if threshold test loop
	// is backwards, starting at before-last threshold, ending with first
	private static void caucMembers(Column theColumn, float theThreshold, BitSet theBitSet)
	{
		for (int k = theBitSet.nextSetBit(0); k >= 0; k = theBitSet.nextSetBit(k + 1))
			if (theColumn.getFloat(k) > theThreshold)
				theBitSet.clear(k);
	}

	public static void echoMiningStart()
	{
		Log.logCommandLine("Mining process started");
	}

	public static void echoMiningEnd(long theMilliSeconds, int theNumberOfSubgroups)
	{
		long minutes = theMilliSeconds / 60_000l;
		float seconds = (theMilliSeconds % 60_000l) / 1_000.0f;

		String aString = String.format(Locale.US,
			"Mining process finished in %d minute%s and %3$.3f seconds.%n",
			minutes, (minutes == 1 ? "" : "s"),
			seconds);

		if (theNumberOfSubgroups == 0)
			aString += "   No subgroups found that match the search criterion.\n";
		else if (theNumberOfSubgroups == 1)
			aString += "   1 subgroup found.\n";
		else
			aString += "   " + theNumberOfSubgroups + " subgroups found.\n";
		Log.logCommandLine(aString);
	}

	private static List<Double> compileStatistics(float theThreshold, int theNrMembers, SubgroupSet theSubgroupSet)
	{
		// [threshold, n, AUC, fpr_1, tpr_1, ..., fpr_h, tpr_h]
		List<Double> stats = new ArrayList<Double>();
		stats.add(Double.valueOf(theThreshold));
		stats.add(Double.valueOf(theNrMembers));
		stats.add(theSubgroupSet.getROCList().getAreaUnderCurve());
		stats.add(0.0);
		stats.add(0.0);
		for (Object[] oa : theSubgroupSet.getROCListSubgroups())
		{
			stats.add((Double) oa[1]);
			stats.add((Double) oa[2]);
		}
		stats.add(1.0);
		stats.add(1.0);
		return stats;
	}

	// to std.out or file
	private static void caucWrite(String theCaller, Column theTarget, List<List<Double>> theStatistics)
	{
		// write or print to std.out
		System.out.println("#" + theCaller);
		System.out.println("#" + theTarget.getName());
		System.out.println("#threshold,n,AUC,frp_1,tpr_1,...,fpr_h,tpr_h");
		for (List<Double> l : theStatistics)
		{
			String aTemp = l.toString().replaceAll(", ", ",");
			System.out.println(aTemp);
		}
		new CAUCWindow(theTarget, theStatistics);
	}

	private static final void temporaryCode(Table theTable, SearchParameters theSearchParameters)
	{
		TargetConcept aTargetConcept = theSearchParameters.getTargetConcept();
		assert aTargetConcept.getTargetType() == TargetType.SINGLE_NUMERIC;

		Column aTarget = aTargetConcept.getPrimaryTarget();

		// compute h using Silverman
		float h = ProbabilityDensityFunction_ND.h_silverman(1, theTable.getNrRows());
		// FIXME h still needs to be multiplied by the std_dev, use Vec
		float aMax = aTarget.getMax();
		float aMin = aTarget.getMin();
		float aRange = aMax - aMin;
		int aNrBins = (int) Math.ceil(aRange / h);

		// XXX 4 is just a magic number, nrSplits is 1 less than nrBins
		int aMaxNrSplitPoints = (4 * aNrBins) - 1;
		System.out.println("\nrange / h = bins ( ceil(bins) )");
		System.out.format("%f / %f = %f ( %d )%n", aRange, h, aRange/h, aNrBins);

		long aTime = System.nanoTime();
		String aPath = String.format("%s_%d", theTable.getName(), aTime);

		// XXX aTargetAverage could be a dummy value
		float aTargetAverage = aTarget.getAverage();

//		aMaxNrSplitPoints = 75;
		// run everything for Equal Height and Equal Width
		for (int i = 1; i <= aMaxNrSplitPoints; ++i)
		{
			temporaryCodeHelper(theTable, theSearchParameters, aTargetAverage, i, false, aPath);
			temporaryCodeHelper(theTable, theSearchParameters, aTargetAverage, i, true, aPath);
		}

		// also run the original code once
		SubgroupDiscovery.TEMPORARY_CODE = false;
		SubgroupDiscovery aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, aTargetAverage, null);
		aSubgroupDiscovery.mine(System.currentTimeMillis(), 1);

		String aFileName = String.format("%s_%s.txt", aPath, "ORIG");
		System.out.println("\nwriting: " + aFileName);

		XMLAutoRun.save(aSubgroupDiscovery.getResult(), aFileName, theSearchParameters.getTargetConcept().getTargetType());

		SubgroupDiscovery.TEMPORARY_CODE = true;
		System.out.println("Done");
	}

	private static final void temporaryCodeHelper(Table theTable, SearchParameters theSearchParameters, float theTargetAverage, int theNrSplitPoints, boolean useEqualWidth, String thePath)
	{
		SubgroupDiscovery.TEMPORARY_CODE = true;
		SubgroupDiscovery.TEMPORARY_CODE_NR_SPLIT_POINTS = theNrSplitPoints;
		SubgroupDiscovery.TEMPORARY_CODE_USE_EQUAL_WIDTH = useEqualWidth;

		String aDiscretisationType = useEqualWidth ? "EW" : "EH";
		System.out.format("%nrunning: %s %d%n", aDiscretisationType, theNrSplitPoints);

		SubgroupDiscovery aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, theTargetAverage, null);
		aSubgroupDiscovery.mine(System.currentTimeMillis(), 1);

		String aFileName = String.format("%s_%s_%05d.txt", thePath, aDiscretisationType, theNrSplitPoints);
		System.out.println("\nwriting: " + aFileName);

		XMLAutoRun.save(aSubgroupDiscovery.getResult(), aFileName, theSearchParameters.getTargetConcept().getTargetType());
	}
}
