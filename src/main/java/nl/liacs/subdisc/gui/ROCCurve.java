package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.geom.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class ROCCurve extends JPanel
{
	private static final long serialVersionUID = 1L;

	private GeneralPath itsCurve;
	private GeneralPath itsLines;
	private List<Arc2D> itsPoints;
	private String itsAreaUnderCurve;
	private float itsSize;
	private float itsXMin, itsXMax, itsYMin, itsYMax;
	private float itsXStart, itsYStart, itsXEnd, itsYEnd;
	private int itsMin, itsMax;
	private int itsMinSupport;
	private float itsTotalTargetCoverage;
	private QualityMeasure itsQualityMeasure;

	public ROCCurve(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters, QualityMeasure theQualityMeasure)
	{
		super();
		setBackground(Color.white);

		ROCList aROCList = theSubgroupSet.getROCList();

		if (aROCList == null)
			return;

		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(3);
		itsAreaUnderCurve = aFormatter.format(aROCList.getAreaUnderCurve());

		List<SubgroupROCPoint> aPoints = new ArrayList<SubgroupROCPoint>(theSubgroupSet.size());
		for(Subgroup aSubgroup: theSubgroupSet)
		{
			SubgroupROCPoint aPoint = new SubgroupROCPoint(aSubgroup);
			aPoints.add(aPoint);
		}

		itsCurve = new GeneralPath();
		itsCurve.moveTo(0, 0);
		for(SubgroupROCPoint p : aROCList)
			itsCurve.lineTo(p.getFPR(), -p.getTPR());
		itsCurve.lineTo(1, -1);

		itsPoints = new ArrayList<Arc2D>(aPoints.size());
		for(SubgroupROCPoint p : aPoints)
			itsPoints.add(new Arc2D.Double(p.getFPR(), -p.getTPR(), 0.0, 0.0, -180.0, 180.0, Arc2D.OPEN));

		int aTotalCoverage = theSubgroupSet.getTotalCoverage();
		itsTotalTargetCoverage = theSubgroupSet.getTotalTargetCoverage();
		int aMinCoverage = theSearchParameters.getMinimumCoverage();
		int aMaxCoverage = (int) (aTotalCoverage * theSearchParameters.getMaximumCoverageFraction());
		float aFalseCoverage = aTotalCoverage - itsTotalTargetCoverage;
		itsMin = aMinCoverage;
		itsMax = aMaxCoverage;

		itsQualityMeasure = theQualityMeasure;

		//compute start and end points of min/max coverage lines
		itsXMin = aMinCoverage/aFalseCoverage;
		itsYMin = aMinCoverage/itsTotalTargetCoverage;
		itsXMax = aMaxCoverage/aFalseCoverage;
		itsYMax = aMaxCoverage/itsTotalTargetCoverage;
		itsXStart = (itsYMax-1f)*(itsXMax/itsYMax);
		itsYStart = 1f;
		if (itsYMax < 1f) // crosses left boundary, rather than top boundary
		{
			itsXStart = 0f;
			itsYStart = itsYMax;
		}
		itsXEnd = 1f;
		itsYEnd = itsYMax-(itsYMax/itsXMax);
		if (itsXMax < 1f) // crosses bottom boundary, rather than right boundary
		{
			itsXEnd = itsXMax;
			itsYEnd = 0f;
		}

		itsLines = new GeneralPath();
		itsLines.moveTo(itsXMin, 0);								//min cov
		itsLines.lineTo(0, -itsYMin);								//min cov
		itsLines.moveTo(itsXStart, -itsYStart);							//max cov
		itsLines.lineTo(itsXEnd, -itsYEnd);							//max cov
		//min support
		itsMinSupport = theSearchParameters.getMinimumSupport();	
		if (itsMinSupport > 0)									
		{
			itsLines.moveTo(0, -itsMinSupport/itsTotalTargetCoverage);
			itsLines.lineTo(1, -itsMinSupport/itsTotalTargetCoverage);
		}
		itsLines.moveTo(0, 0);
		itsLines.lineTo(0, -1);
		itsLines.lineTo(1, -1);
		itsLines.lineTo(1, 0);
		itsLines.lineTo(0, 0);
		for(int i=0; i<11; i++)
		{
			itsLines.moveTo(i*0.1f, 0.0f);
			itsLines.lineTo(i*0.1f, 0.01f);
			itsLines.moveTo(0.0f, i*-0.1f);
			itsLines.lineTo(-0.01f, i*-0.1f);
		}
	}

	public String getAreaUnderCurve() { return itsAreaUnderCurve; }

	@Override
	public void paintComponent(Graphics theGraphic)
	{
		int aWidth = getWidth();
		int aHeight = getHeight();
		itsSize = Math.max(260f, Math.min(aWidth, aHeight)*0.85f);
		AffineTransform aTransform = new AffineTransform();
		aTransform.translate(0.15*itsSize, 1.05*itsSize);
		aTransform.scale(itsSize, itsSize);

		super.paintComponent(theGraphic);
		Graphics2D aGraphic = (Graphics2D)theGraphic;
		aGraphic.setStroke(new BasicStroke(3.0f));

		//isometrics
		int N = itsQualityMeasure.getNrRecords();
		int p = itsQualityMeasure.getNrPositives();
		int aResolution = 400;
		float aMax = (float) Math.max(itsQualityMeasure.calculate(p, p), itsQualityMeasure.calculate(p, N));
		aMax = (float) Math.max(aMax, itsQualityMeasure.calculate(0, N-p));
		for (int i=0; i<aResolution; i++)
		{
			float anX = i/(float)aResolution;
			float aNegatives = anX*(N-p); //this can be fractional, even though the counts are always integer, picture looks nicer this way
			for (int j=0; j<aResolution; j++)
			{
				float aY = -(j+1)/(float)aResolution;
				float aPositives = -aY*p; //this can be fractional, even though the counts are always integer
// FIXME MM
// casting from fractional float to int might not yield best pictures
// calculate(x, y) requires (x >= 0) and (y > 0), this needs to be guaranteed
				//int aValue = (int) (255 * itsQualityMeasure.calculate((int)aPositives, (int)(aNegatives+aPositives)) / aMax);
				int aValue = (int) (255 * itsQualityMeasure.calculate((int)aPositives, Math.max(1, (int)(aNegatives+aPositives))) / aMax);
				boolean isNegative = (aValue<0);
				if (isNegative)
					aValue = -aValue;
				aValue = Math.min(aValue, 255);
				aValue = Math.max(aValue, 0);
				aValue = 15*(Math.round(aValue/15));
				if (isNegative)
					aGraphic.setColor(new Color(255, 255-aValue, 255-aValue)); //red
				else
					aGraphic.setColor(new Color(255-aValue, 255, 255-aValue)); //green
				aGraphic.fill(new Rectangle2D.Double((anX+0.15)*itsSize, (aY+1.05)*itsSize, itsSize/aResolution, itsSize/aResolution));
			}
		}

		aGraphic.setColor(Color.black);

		if (itsPoints != null)
			for(Arc2D aPoint : itsPoints)
				aGraphic.draw(aTransform.createTransformedShape(aPoint));

		aGraphic.setStroke(new BasicStroke(2.0f));
		aGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		aGraphic.draw(aTransform.createTransformedShape(itsCurve));
		aGraphic.setStroke(new BasicStroke(1.0f));
		aGraphic.draw(aTransform.createTransformedShape(itsLines));

		Font aFont = new Font("SansSerif", Font.PLAIN, 11);
		Font aNewFont = aFont.deriveFont(11.0f);
		aGraphic.setFont(aNewFont);
		aGraphic.drawString("(0,0)", (-0.05f+0.15f)*itsSize, (0.04f+1.05f)*itsSize);
		aGraphic.drawString("FPR", (0.44f+0.15f)*itsSize, (0.08f+1.05f)*itsSize);
		aGraphic.drawString("TPR", (-0.1f+0.15f)*itsSize, (-0.44f+1.05f)*itsSize);

		//scales
		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(1);
		for(int i=1; i<11; i++)
		{
			aGraphic.drawString(aFormatter.format(i*0.1f), (i*0.1f+0.15f)*itsSize, (0.04f+1.05f)*itsSize);
			aGraphic.drawString(aFormatter.format(i*0.1f), (-0.07f+0.15f)*itsSize, (i*-0.1f+1.05f)*itsSize);
		}

		//qualities
		aFormatter.setMaximumFractionDigits(4);
		aFont = new Font("SansSerif", Font.PLAIN, 10);
		aNewFont = aFont.deriveFont(10.0f);
		aGraphic.setFont(aNewFont);
		aGraphic.drawString(aFormatter.format(itsQualityMeasure.getROCHeaven()), (0.02f+0.15f)*itsSize, (-0.96f+1.05f)*itsSize);
		aGraphic.drawString(aFormatter.format(itsQualityMeasure.getROCHell()), (0.9f+0.15f)*itsSize, (-0.02f+1.05f)*itsSize);

		//min and max support
		aGraphic.drawString(Integer.toString(itsMin), (itsXMin+0.15f)*itsSize, (-0.03f+1.05f)*itsSize);
		aGraphic.drawString(Integer.toString(itsMax), (itsXEnd+0.01f+0.15f)*itsSize, (-Math.max(itsYEnd, 0.03f)+1.05f)*itsSize);
		//min support
		if (itsMinSupport > 0)
			aGraphic.drawString(Integer.toString(itsMinSupport), (1+0.16f)*itsSize, (-itsMinSupport/(float)itsTotalTargetCoverage +1.05f)*itsSize);

	}
}
