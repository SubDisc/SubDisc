package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class PDFPlot extends JPanel
{
	private static final long serialVersionUID = 1L;
	private GeneralPath itsLines;
	private ProbabilityDensityFunction2_2D itsPDF;
	private String itsTitle;

	public PDFPlot(ProbabilityDensityFunction2_2D thePDF, String theTitle)
	{
		super();
		setBackground(Color.white);

		itsPDF = thePDF;
		itsTitle = theTitle;
		itsLines = new GeneralPath();
		itsLines.moveTo(0, 0);
		itsLines.lineTo(0, -1);
		itsLines.lineTo(1, -1);
		itsLines.lineTo(1, 0);
		itsLines.lineTo(0, 0);
		for(int i=0; i<itsPDF.getSizeX()+1; i++)
		{
			itsLines.moveTo(i/(float)itsPDF.getSizeX(), 0.0f);
			itsLines.lineTo(i/(float)itsPDF.getSizeX(), 0.01f);
		}
		for(int i=0; i<itsPDF.getSizeY()+1; i++)
		{
			itsLines.moveTo(0.0f, -i/(float)itsPDF.getSizeY());
			itsLines.lineTo(-0.01f, -i/(float)itsPDF.getSizeY());
		}
		
		//bounding box
		double aScaleX = itsPDF.getHighX() - itsPDF.getLowX();
		double aScaleY = itsPDF.getHighY() - itsPDF.getLowY();
		itsLines.moveTo(itsPDF.getMinX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMinY())/aScaleY);
		itsLines.lineTo(itsPDF.getMaxX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMinY())/aScaleY);
		itsLines.moveTo(itsPDF.getMinX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMaxY())/aScaleY);
		itsLines.lineTo(itsPDF.getMaxX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMaxY())/aScaleY);

		itsLines.moveTo(itsPDF.getMinX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMinY())/aScaleY);
		itsLines.lineTo(itsPDF.getMinX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMaxY())/aScaleY);
		itsLines.moveTo(itsPDF.getMaxX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMinY())/aScaleY);
		itsLines.lineTo(itsPDF.getMaxX()/aScaleX, (itsPDF.getHighY()-itsPDF.getMaxY())/aScaleY);
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

		//densities
		double aMax = itsPDF.getMaxDensity();
		for (int i=0; i<itsPDF.getSizeY(); i++)
		{
			float aY = (itsPDF.getSizeY()-i)/(float)itsPDF.getSizeY()-1;
			for (int j=0; j<itsPDF.getSizeX(); j++)
			{
				float anX = j/(float)itsPDF.getSizeX();

				int aValue = (int) (255*(itsPDF.get(j,i)/aMax));
				aValue = Math.min(aValue, 255);
				aValue = Math.max(aValue, 0);
					aGraphic.setColor(new Color(255-aValue, 255-aValue, 255-aValue)); //green
				aGraphic.fill(new Rectangle2D.Double(anX, aY, 1/(float)itsPDF.getSizeX(), 1/(float)itsPDF.getSizeY()));
			}
		}

		aGraphic.setColor(Color.black);
		aGraphic.setStroke(new BasicStroke(1.0f/aSize));
		aGraphic.draw(itsLines);
		
//		Log.logCommandLine("X: (" + itsPDF.getMinX() + ", " + itsPDF.getMaxX() + ")" + " (" + itsPDF.getLowX() + ", " + itsPDF.getHighX() + ")");
//		Log.logCommandLine("Y: (" + itsPDF.getMinY() + ", " + itsPDF.getMaxY() + ")" + " (" + itsPDF.getLowY() + ", " + itsPDF.getHighY() + ")");

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
