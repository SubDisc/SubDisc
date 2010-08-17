/**
 * TODO use ArrayList, not Vector
 */
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
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.JPanel;

public class ROCCurve extends JPanel
{
	private static final long serialVersionUID = 1L;

	GeneralPath itsCurve;
	GeneralPath itsLines;
	Vector<Arc2D> itsPoints;
	float itsXMin, itsXMax;
	int itsMin, itsMax;

	public ROCCurve(Vector<Point2D> theCurve, Vector<Point2D> thePoints,
					float theXMin, float theYMin, int theMin, float theXMax, float theYMax, int theMax)
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

		itsPoints = new Vector<Arc2D>();
		anIterator = thePoints.listIterator();
		while (anIterator.hasNext())
		{
			Point2D aPoint = anIterator.next();
			float anX = (float)aPoint.getX();
			float aY = (float)aPoint.getY();
			Arc2D anArc = new Arc2D.Float(anX, -aY, 0.0F, 0.0F, -180.0F, 180.0F, Arc2D.OPEN);
			itsPoints.add(anArc);
		}

		itsXMin = theXMin;
		itsXMax = theXMax;
		itsMin = theMin;
		itsMax = theMax;

		itsLines = new GeneralPath();
		itsLines.moveTo(theXMin, 0);
		itsLines.lineTo(0, -theYMin);
		itsLines.moveTo(theXMax, 0);
		itsLines.lineTo(0, -theYMax);
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
			ListIterator<Arc2D> anIterator = itsPoints.listIterator();
			while (anIterator.hasNext())
			{
				Arc2D aPoint = anIterator.next();
				aGraphic.draw(aPoint);
			}
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
