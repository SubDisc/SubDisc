package nl.liacs.subdisc;

import java.awt.*;
import java.awt.event.*;

public class VisualNode extends MShape
{
	private static final long serialVersionUID = 1L;
	private static final int FONT_SIZE = 11;

	private final String itsTarget;
	private int itsXPos;
	private int itsYPos;
	private Color itsFillColor = Color.orange;

	public VisualNode(int theX, int theY, String theTarget)
	{
		super("");
		itsTarget = theTarget;
		itsXPos = theX;
		itsYPos = theY;
		calcBounds();
	}

	private void calcBounds()
	{
		int aStringIDLength = itsTarget.length();
		int aWidth = (aStringIDLength + 1) * 7;
		int aHeight = 2 * (FONT_SIZE);
		setBounds(itsXPos, itsYPos, aWidth, aHeight);
	}

	public void shift(int theX, int theY)
	{
		itsXPos += theX;
		itsYPos += theY;
		calcBounds();
	}

	@Override
	public void setLocation(int theX, int theY)
	{
		itsXPos = theX;
		itsYPos = theY;
		calcBounds();
	}

	@Override
	public void paint(java.awt.Graphics g)
	{
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Rectangle r = getBounds();

		//shadow
		g2.setColor(Color.black);
		g2.setStroke(new BasicStroke(1.0f));
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g2.fillRect(r.x + 3, r.y + 3, r.width, r.height);

		//fill
		g2.setColor(itsFillColor);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
		g2.fillRect(r.x, r.y, r.width, r.height);

		//outline
		g2.setColor(Color.black);
		g2.setStroke(new BasicStroke(1.0f));
		g2.drawLine(r.x, r.y, r.x, r.y + r.height);
		g2.drawLine(r.x, r.y, r.x + r.width, r.y);
		g2.drawLine(r.x, r.y + r.height, r.x + r.width, r.y + r.height);
		g2.drawLine(r.x + r.width, r.y, r.x + r.width, r.y + r.height);

		g2.setColor(Color.black);
		g2.setFont(g.getFont().deriveFont(Font.PLAIN, FONT_SIZE));
		g2.drawString(itsTarget, r.x + 5, r.y + 4 + FONT_SIZE);
//		g2.drawString(itsCov, r.x + 5, r.y + 4 + (2 * itsFontSize));
//		g2.drawString("Acc.: " + itsAcc, r.x + 5, r.y + 5 + (3 * itsFontSize));
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (mouseOnMe(e) && (e.getClickCount() > 1))
			return; // this does nothing
	}

	/* OBSOLETE ***************************************************************/

	@Deprecated
	public void setColor(boolean inPatternTeam)
	{
		if (inPatternTeam)
			itsFillColor = new Color(140, 140, 255);
		else
			itsFillColor = Color.orange;
		repaint();
	}
}
