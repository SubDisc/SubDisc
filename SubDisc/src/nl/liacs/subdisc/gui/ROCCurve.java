package nl.liacs.subdisc.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.JPanel;

import nl.liacs.subdisc.ROCList;
import nl.liacs.subdisc.SearchParameters;
import nl.liacs.subdisc.Subgroup;
import nl.liacs.subdisc.SubgroupROCPoint;
import nl.liacs.subdisc.SubgroupSet;

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
		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(3);
		itsAreaUnderCurve = aFormatter.format(aROCList.getAreaUnderCurve());
		for(SubgroupROCPoint p : aROCList)
			System.out.println(p);

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
			itsPoints.add(new Arc2D.Float(p.getFPR(), -p.getTPR(), 0.0F, 0.0F, -180.0F, 180.0F, Arc2D.OPEN));

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
			itsLines.moveTo(i*0.1F, 0.0F);
			itsLines.lineTo(i*0.1F, 0.01F);
			itsLines.moveTo(0.0F, i*-0.1F);
			itsLines.lineTo(-0.01F, i*-0.1F);
		}
	}

	public ROCCurve(Vector<Point2D> theCurve)
	{
		super();
		setBackground(Color.white);

		itsCurve = new GeneralPath();
		itsCurve.moveTo(0, 0);
		ListIterator<Point2D> anIterator = theCurve.listIterator();
		while (anIterator.hasNext())
		{
			Point2D aPoint = anIterator.next();
			float anX = (float)aPoint.getX();
			float aY = (float)aPoint.getY();
			itsCurve.lineTo(anX, -aY);
		}
		itsCurve.lineTo(1, -1);

		itsLines = new GeneralPath();
		itsLines.moveTo(0, 0);
		itsLines.lineTo(0, -1);
		itsLines.lineTo(1, -1);
		itsLines.lineTo(1, 0);
		itsLines.lineTo(0, 0);
		for(int i=0; i<11; i++)
		{
			itsLines.moveTo(i*0.1F, 0.0F);
			itsLines.lineTo(i*0.1F, 0.01F);
			itsLines.moveTo(0.0F, i*-0.1F);
			itsLines.lineTo(-0.01F, i*-0.1F);
		}
	}

	public String getAreaUnderCurve() { return itsAreaUnderCurve; }

	public void paintComponent(Graphics theGraphic)
	{
		int aWidth = getWidth();
		int aHeight = getHeight();
		float aSize = Math.min(aWidth, aHeight)*0.85F;

		super.paintComponent(theGraphic);
		Graphics2D aGraphic = (Graphics2D)theGraphic;
		aGraphic.scale(aSize, aSize);
		aGraphic.translate(0.15, 1.05);
		aGraphic.setStroke(new BasicStroke(3.0F/aSize));

		if (itsPoints != null)
		{
			for(Arc2D aPoint : itsPoints)
				aGraphic.draw(aPoint);
		}

		aGraphic.setStroke(new BasicStroke(2.0F/aSize));
		aGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
									RenderingHints.VALUE_ANTIALIAS_ON);
		aGraphic.draw(itsCurve);
		aGraphic.setStroke(new BasicStroke(1.0F/aSize));
		aGraphic.draw(itsLines);

		Font aFont = new Font("SansSerif", Font.PLAIN, 11);
		Font aNewFont = aFont.deriveFont(11.0F/aSize);
		aGraphic.setFont(aNewFont);
		aGraphic.drawString("(0,0)", -0.05F, 0.04F);
		aGraphic.drawString("FPR", 0.44F, 0.08F);
		aGraphic.drawString("TPR", -0.1F, -0.44F);

		aGraphic.drawString(Integer.toString(itsMin), itsXMin, -0.03F);
		aGraphic.drawString(Integer.toString(itsMax), itsXMax, -0.03F);

		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(1);
		for(int i=1; i<11; i++)
		{
			aGraphic.drawString(aFormatter.format(i*0.1F), i*0.1F, 0.04F);
			aGraphic.drawString(aFormatter.format(i*0.1F), -0.07F, i*-0.1F);
		}
	}
}
