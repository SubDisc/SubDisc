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
	private ArrayList<Arc2D.Float> itsPoints;
	private String itsAreaUnderCurve;
	private float itsXMin, itsXMax, itsYMin, itsYMax;
	private int itsMin, itsMax;

	public ROCCurve(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters)
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
		for(Subgroup s : theSubgroupSet)
			aPoints.add(new SubgroupROCPoint(s));

		itsCurve = new GeneralPath();
		itsCurve.moveTo(0, 0);
		for(SubgroupROCPoint p : aROCList)
			itsCurve.lineTo(p.getFPR(), -p.getTPR());
		itsCurve.lineTo(1, -1);

		itsPoints = new ArrayList<Arc2D.Float>(aPoints.size());
		for(SubgroupROCPoint p : aPoints)
			itsPoints.add(new Arc2D.Float(p.getFPR(), -p.getTPR(), 0.0f, 0.0f, -180.0f, 180.0f, Arc2D.OPEN));

		int aTotalCoverage = theSubgroupSet.getTotalCoverage();
		float aTotalTargetCoverage = theSubgroupSet.getTotalTargetCoverage();
		int aMinCoverage = theSearchParameters.getMinimumCoverage();
		int aMaxCoverage = aTotalCoverage * (int)theSearchParameters.getMaximumCoverage();
		float aFalseCoverage = aTotalCoverage - aTotalTargetCoverage;

		itsXMin = aMinCoverage/aFalseCoverage;
		itsXMax = aMaxCoverage/aFalseCoverage;
		itsYMin = aMinCoverage/aTotalTargetCoverage;
		itsYMax = aMaxCoverage/aTotalTargetCoverage;
		itsMin = aMinCoverage;
		itsMax = aMaxCoverage;

		itsLines = new GeneralPath();
		itsLines.moveTo(itsXMin, 0);
		itsLines.lineTo(0, -itsYMin);
		itsLines.moveTo(itsXMax, 0);
		itsLines.lineTo(0, -itsYMax);
		itsLines.moveTo(0, 0);
		itsLines.lineTo(0, -1);
		itsLines.lineTo(1, -1);
		itsLines.lineTo(1, 0);
		itsLines.lineTo(0, 0);
		//PathIterator p = itsLines.getPathIterator(new AffineTransform());
		for(int i=0; i<11; i++)
		{
			itsLines.moveTo(i*0.1f, 0.0f);
			itsLines.lineTo(i*0.1f, 0.01f);
			itsLines.moveTo(0.0f, i*-0.1f);
			itsLines.lineTo(-0.01f, i*-0.1f);
		}
	}

	public String getAreaUnderCurve() { return itsAreaUnderCurve; }

	public void paintComponent(Graphics theGraphic)
	{
		int aWidth = getWidth();
		int aHeight = getHeight();
		float aSize = Math.min(aWidth, aHeight)*0.85f;

		super.paintComponent(theGraphic);
		Graphics2D aGraphic = (Graphics2D)theGraphic;
		aGraphic.scale(aSize, aSize);
		aGraphic.translate(0.15, 1.05);
		aGraphic.setStroke(new BasicStroke(3.0f/aSize));

		if (itsPoints != null)
		{
			for(Arc2D aPoint : itsPoints)
				aGraphic.draw(aPoint);
		}

		aGraphic.setStroke(new BasicStroke(2.0f/aSize));
		aGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
									RenderingHints.VALUE_ANTIALIAS_ON);
		aGraphic.draw(itsCurve);
		aGraphic.setStroke(new BasicStroke(1.0f/aSize));
		aGraphic.draw(itsLines);

		Font aFont = new Font("SansSerif", Font.PLAIN, 11);
		Font aNewFont = aFont.deriveFont(11.0f/aSize);
		aGraphic.setFont(aNewFont);
		aGraphic.drawString("(0,0)", -0.05f, 0.04f);
		aGraphic.drawString("FPR", 0.44f, 0.08f);
		aGraphic.drawString("TPR", -0.1f, -0.44f);

		aGraphic.drawString(Integer.toString(itsMin), itsXMin, -0.03f);
		aGraphic.drawString(Integer.toString(itsMax), itsXMax, -0.03f);

		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(1);
		for(int i=1; i<11; i++)
		{
			aGraphic.drawString(aFormatter.format(i*0.1f), i*0.1f, 0.04f);
			aGraphic.drawString(aFormatter.format(i*0.1f), -0.07f, i*-0.1f);
		}
	}
}
