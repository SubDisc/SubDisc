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
	private String itsXAxis, itsYAxis;

	public PDFPlot(ProbabilityDensityFunction2_2D thePDF, String theTitle, String theXAxis, String theYAxis)
	{
		super();
		setBackground(Color.white);

		itsPDF = thePDF;
		itsTitle = theTitle;
		itsXAxis = theXAxis;
		itsYAxis = theYAxis;
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
		itsLines.moveTo((itsPDF.getMinX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMinY())/aScaleY);
		itsLines.lineTo((itsPDF.getMaxX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMinY())/aScaleY);
		itsLines.moveTo((itsPDF.getMinX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMaxY())/aScaleY);
		itsLines.lineTo((itsPDF.getMaxX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMaxY())/aScaleY);

		itsLines.moveTo((itsPDF.getMinX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMinY())/aScaleY);
		itsLines.lineTo((itsPDF.getMinX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMaxY())/aScaleY);
		itsLines.moveTo((itsPDF.getMaxX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMinY())/aScaleY);
		itsLines.lineTo((itsPDF.getMaxX()-itsPDF.getLowX())/aScaleX, (itsPDF.getLowY()-itsPDF.getMaxY())/aScaleY);
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

		Font aFont = new Font("SansSerif", Font.PLAIN, 12);
		Font aNewFont = aFont.deriveFont(11.0f/aSize);
		aGraphic.setFont(aNewFont);
		aGraphic.drawString(itsXAxis, 0.4f, 0.06f);
		aGraphic.drawString(itsYAxis, 0.01f, -0.52f);
		aGraphic.drawString(itsTitle, 0.5f, -1.02f);
	}
}
