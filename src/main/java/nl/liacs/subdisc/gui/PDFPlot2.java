package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

public class PDFPlot2 extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

	private final GeneralPath itsLines;
	private final float[][] itsPDF;
	private final int itsXSize;
	private final int itsYSize;
	private final String itsTitle;
	private final String itsXAxis;
	private final String itsYAxis;

	// data is plotted as is, no normalisation is performed.
	public PDFPlot2(float[][] thePDF, double[] theStats, double[][] theLimits, String theTitle, String theXAxis, String theYAxis)
	{
		super();
		setBackground(Color.WHITE);

		itsPDF = thePDF;
		itsXSize = thePDF.length;
		itsYSize = thePDF[0].length;
		itsTitle = theTitle;
		itsXAxis = theXAxis;
		itsYAxis = theYAxis;

		itsLines = new GeneralPath();
		itsLines.moveTo(0, 0);
		itsLines.lineTo(0, -1);
		itsLines.lineTo(1, -1);
		itsLines.lineTo(1, 0);
		itsLines.lineTo(0, 0);
		for(int i=0; i<itsXSize+1; i++)
		{
			itsLines.moveTo(i/(float)itsXSize, 0.0f);
			itsLines.lineTo(i/(float)itsXSize, 0.01f);
		}
		for(int i=0; i<itsYSize+1; i++)
		{
			itsLines.moveTo(0.0f, -i/(float)itsYSize);
			itsLines.lineTo(-0.01f, -i/(float)itsYSize);
		}

		// hard coded for now, see ProbabilityDensityFunction_ND
		//lastDXDY = new double[] { x_min, x_max, x_n, y_min, y_max, y_n, dx, dy };
		double aGridXMin = theStats[0];
		double aGridXMax = theStats[1];
		double aGridYMin = theStats[3];
		double aGridYMax = theStats[4];

		// [ d1[min,max], d2[min,max], ... , dd[min,max] ]
		double aDataXMin = theLimits[0][0];
		double aDataXMax = theLimits[0][1];
		double aDataYMin = theLimits[1][0];
		double aDataYMax = theLimits[1][1];

		//bounding box
		double aScaleX = aGridXMax - aGridXMin;
		double aScaleY = aGridYMax - aGridYMin;
		itsLines.moveTo((aDataXMin-aGridXMin)/aScaleX, (aGridYMin-aDataYMin)/aScaleY);
		itsLines.lineTo((aDataXMax-aGridXMin)/aScaleX, (aGridYMin-aDataYMin)/aScaleY);
		itsLines.moveTo((aDataXMin-aGridXMin)/aScaleX, (aGridYMin-aDataYMax)/aScaleY);
		itsLines.lineTo((aDataXMax-aGridXMin)/aScaleX, (aGridYMin-aDataYMax)/aScaleY);

		itsLines.moveTo((aDataXMin-aGridXMin)/aScaleX, (aGridYMin-aDataYMin)/aScaleY);
		itsLines.lineTo((aDataXMin-aGridXMin)/aScaleX, (aGridYMin-aDataYMax)/aScaleY);
		itsLines.moveTo((aDataXMax-aGridXMin)/aScaleX, (aGridYMin-aDataYMin)/aScaleY);
		itsLines.lineTo((aDataXMax-aGridXMin)/aScaleX, (aGridYMin-aDataYMax)/aScaleY);
	}

	@Override
	public void paintComponent(Graphics theGraphic)
	{
		int aWidth = getWidth();
		int aHeight = getHeight();
		float aSize = Math.min(aWidth, aHeight)*0.85f;

		super.paintComponent(theGraphic);
		Graphics2D aGraphic = (Graphics2D) theGraphic;
		aGraphic.scale(aSize, aSize);
		aGraphic.translate(0.15, 1.1);
		aGraphic.setStroke(new BasicStroke(3.0f/aSize));

		double aSizeXd = itsXSize;
		double aSizeYd = itsYSize;
		double aWidthX = 1.0/itsXSize;
		double aWidthY = 1.0/itsYSize;
		for (int i = 0; i < itsXSize; ++i)
		{
			double anX = i/aSizeXd;
			for (int j = itsYSize-1; j>=0; --j)
			{
				double aY = (j+1.0)/aSizeYd;
				int aValue = (int) Math.rint(255.0*(itsPDF[i][j]));
				assert (aValue >= -255 && aValue <= 255);
				if (aValue < 0)
					aGraphic.setColor(new Color(255, aValue+255, aValue+255)); //red
				else
					aGraphic.setColor(new Color(255-aValue, 255, 255-aValue)); //green
				// grayscale
				//aGraphic.setColor(new Color(255-aValue, 255-aValue, 255-aValue));
				aGraphic.fill(new Rectangle2D.Double(anX, -aY, aWidthX, aWidthY));
			}
		}

		aGraphic.setColor(Color.BLACK);
		aGraphic.setStroke(new BasicStroke(1.0f/aSize));
		aGraphic.draw(itsLines);

		Font aNewFont = DEFAULT_FONT.deriveFont(11.0f/aSize);
		aGraphic.setFont(aNewFont);
		aGraphic.drawString(itsTitle, 0.5f, -1.02f);
		aGraphic.drawString(itsXAxis, 0.4f, 0.06f);
		aGraphic.drawString(itsYAxis, 0.01f, -0.52f);
	}
}
