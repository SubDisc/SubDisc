package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

public class PDFPlot2 extends JPanel
{
	private static final long serialVersionUID = 1L;
	private final GeneralPath itsLines;
	private final float[][] itsPDF;
	private final int itsXSize;
	private final int itsYSize;
	private final float itsMaxDensity;
	private String itsTitle;

	public PDFPlot2(float[][] thePDF, String theTitle)
	{
		super();
		setBackground(Color.WHITE);

		itsPDF = thePDF;
		itsXSize = thePDF.length;
		itsYSize = thePDF[0].length;
		itsMaxDensity = getMaxDensity(thePDF);
		itsTitle = theTitle;
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
	}

	private static final float getMaxDensity(float[][] thePDF)
	{
		float aMax = -Float.MAX_VALUE;
		for (float[] row : thePDF)
			for (int j = 0; j < row.length; ++j)
				if (row[j] > aMax)
					aMax = row[j];
		return aMax;
	}

	@Override
	public void paintComponent(Graphics theGraphic)
	{
		int aWidth = getWidth();
		int aHeight = getHeight();
		float aSize = Math.min(aWidth, aHeight)*0.85f;

		super.paintComponent(theGraphic);
		Graphics2D aGraphic = (Graphics2D)theGraphic;
		aGraphic.scale(aSize, aSize);
		aGraphic.translate(0.15, 1.1);
		aGraphic.setStroke(new BasicStroke(3.0f/aSize));

		for (int i = 0; i < itsXSize; ++i)
		{
			float anX = i/(float)itsXSize;
			for (int j = itsYSize-1; j>=0; --j)
			{
				float aY = (j+1.0f)/(float)itsYSize;
				int aValue = (int) (255*(itsPDF[i][j]/itsMaxDensity));
				aValue = Math.min(aValue, 255);
				aValue = Math.max(aValue, 0);
				aGraphic.setColor(new Color(255-aValue, 255-aValue, 255-aValue));
				aGraphic.fill(new Rectangle2D.Double(anX, -aY, 1.0/itsXSize, 1.0/itsYSize));
			}
		}

		aGraphic.setColor(Color.BLACK);
		aGraphic.setStroke(new BasicStroke(1.0f/aSize));
		aGraphic.draw(itsLines);

		Font aFont = new Font("SansSerif", Font.PLAIN, 11);
		Font aNewFont = aFont.deriveFont(11.0f/aSize);
		aGraphic.setFont(aNewFont);
//		for(int i=0; i<itsPDF.getSizeX(); i++)
//			aGraphic.drawString(LabelRanking.getLetter(i), (i+0.5f)/itsPDF.getSizeX(), 0.04f);
//		for(int i=0; i<itsPDF.getSizeY(); i++)
//			aGraphic.drawString(LabelRanking.getLetter(itsPDF.getSizeY()-i-1), -0.07f, -(i+0.5f)/itsPDF.getSizeY());
		aGraphic.drawString(itsTitle, 0.4f, -1.04f);
	}
}
