package nl.liacs.subdisc;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

// TODO this class needs revision
public class MShape extends JComponent implements MouseMotionListener, MouseListener, ActionListener, Serializable
{
	private static final long serialVersionUID = 1L;

//	Color itsColor = Color.green;
//	boolean setShadow = true;
//	protected JLabel itsComponent = null;
	private boolean dragging = false;
	private Point dragpoint = new Point();

	/** Creates new Shape */
	public MShape(String label)
	{
		super();
		addMouseMotionListener(this);
		addMouseListener(this);
//		itsComponent = new JLabel(label);
		//add(itsComponent);
		//itsComponent.addActionListener(this);
		//this.addActionListener(this);
	}

	public Point getConnectPoint()
	{
		Rectangle r = this.getBounds();
		Point p = new Point((int)(r.x + (0.5 * r.width)), (int)(r.y + (0.5 * r.height)));
		return p;
	}

	boolean mouseOnMe(MouseEvent e)
	{
		Rectangle r = this.getBounds();
		if (e.getX() > r.x && r.x + r.width > e.getX())
			if ((e.getY() > r.y && r.y + r.height > e.getY()))
				return true;
		return false;
	}

	@Override
	public Dimension getPreferredSize()
	{
		Rectangle r = this.getBounds();
		return new Dimension(r.width, r.height);
	}

	/** The minimum size of the Shape. */
	@Override
	public Dimension getMinimumSize()
	{
		Rectangle r = this.getBounds();
		return new Dimension(r.width, r.height);
	}

	@Override
	public void mouseDragged(java.awt.event.MouseEvent mouseEvent)
	{
		if (mouseOnMe(mouseEvent) || dragging)
		{
			if (dragging)
			{
				Rectangle r = this.getBounds();
				setBounds(mouseEvent.getX() - dragpoint.x, mouseEvent.getY() - dragpoint.y, r.width, r.height);
			}
			else
			{
				Rectangle r = this.getBounds();
				dragpoint = new Point(mouseEvent.getX() - r.x, mouseEvent.getY() - r.y);
				dragging = true;
			}
		}
	}

	@Override
	public void mouseReleased(java.awt.event.MouseEvent mouseEvent)
	{
		if (mouseOnMe(mouseEvent))
			dragging = false;
	}

	@Override public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {}
	@Override public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {}
	@Override public void mouseExited(java.awt.event.MouseEvent mouseEvent) {}
	@Override public void mouseMoved(java.awt.event.MouseEvent mouseEvent) {}
	@Override public void mousePressed(java.awt.event.MouseEvent mouseEvent) {}

	@Override public void actionPerformed(java.awt.event.ActionEvent actionEvent) {}
}
