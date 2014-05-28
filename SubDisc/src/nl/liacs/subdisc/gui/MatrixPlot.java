package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.geom.*;
import java.text.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import nl.liacs.subdisc.*;

public class MatrixPlot extends JPanel
{
	private static final long serialVersionUID = 1L;
	private GeneralPath itsLines;
	private LabelRankingMatrix itsLRM;
	private String itsTitle;

	public MatrixPlot(LabelRankingMatrix theLRM, String theTitle)
	{
		super();
		setBackground(Color.white);

		itsLRM = theLRM;
		itsTitle = theTitle;
		itsLines = new GeneralPath();
		itsLines.moveTo(0, 0);
		itsLines.lineTo(0, -1);
		itsLines.lineTo(1, -1);
		itsLines.lineTo(1, 0);
		itsLines.lineTo(0, 0);
		for(int i=0; i<itsLRM.getSize()+1; i++)
		{
			itsLines.moveTo(i/(float)itsLRM.getSize(), 0.0f);
			itsLines.lineTo(i/(float)itsLRM.getSize(), 0.01f);
			itsLines.moveTo(0.0f, -i/(float)itsLRM.getSize());
			itsLines.lineTo(-0.01f, -i/(float)itsLRM.getSize());
		}
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

		//matrix colors
		int aResolution = itsLRM.getSize();
		for (int i=0; i<aResolution; i++)
		{
			float aY = i/(float)itsLRM.getSize()-1;
			for (int j=0; j<aResolution; j++)
			{
				float anX = j/(float)aResolution;
				
				int aValue = (int) (255*itsLRM.get(i,j));
				boolean isNegative = (aValue<0);
				if (isNegative)
					aValue = -aValue;
				aValue = Math.min(aValue, 255);
				aValue = Math.max(aValue, 0);
				if (isNegative)
					aGraphic.setColor(new Color(255, 255-aValue, 255-aValue)); //red
				else
					aGraphic.setColor(new Color(255-aValue, 255, 255-aValue)); //green
				aGraphic.fill(new Rectangle2D.Double(anX, aY, 1/(float)aResolution, 1/(float)aResolution));
			}
		}

		aGraphic.setColor(Color.black);
		aGraphic.setStroke(new BasicStroke(1.0f/aSize));
		aGraphic.draw(itsLines);

		Font aFont = new Font("SansSerif", Font.PLAIN, 11);
		Font aNewFont = aFont.deriveFont(11.0f/aSize);
		aGraphic.setFont(aNewFont);
		for(int i=0; i<itsLRM.getSize(); i++)
		{
			aGraphic.drawString(LabelRanking.getLetter(i), (i+0.5f)/itsLRM.getSize(), 0.04f);
			aGraphic.drawString(LabelRanking.getLetter(itsLRM.getSize()-i-1), -0.07f, -(i+0.5f)/itsLRM.getSize());
		}
		aGraphic.drawString(itsTitle, 0.4f, -1.04f);
	}
}
