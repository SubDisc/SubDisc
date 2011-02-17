package nl.liacs.subdisc.gui;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class SecondaryTargetsWindow extends BasicJListWindow implements ActionListener//, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private Object[] itsOriginalSelection;

	public SecondaryTargetsWindow(JList theJList)
	{
		super(theJList);

		if (theJList == null)
			constructorWarning("SecondaryTargetsWindow", true);
		else if (itsJList.getModel().getSize() == 0)
			constructorWarning("SecondaryTargetsWindow", false);
		else
		{
			itsOriginalSelection = itsJList.getSelectedValues();
			addAdditionalComponents();
			display("Secondary Targets");
		}
	}

	private void addAdditionalComponents()
	{
		// TODO add alpha, beta, post processing count
	}

	// constructor guarantees itsJListSize > 0
	@Override
	protected void disposeCancel()
	{
		itsJList.clearSelection();

		// Columns might be disabled/re-typed since last time (order is same)
		// could be faster
		int aCurrentSize = itsJList.getModel().getSize();
		ArrayList<Object> aCurrentList = new ArrayList<Object>(aCurrentSize);

		for (int i = 0; i < aCurrentSize; i++)
			aCurrentList.add(itsJList.getModel().getElementAt(i));

		
		int aLastSize = itsOriginalSelection.length;

		for (int i = 0, j = -1; i < aLastSize && aCurrentSize > 0; i++)
		{
			if ((j = aCurrentList.indexOf(itsOriginalSelection[i])) != -1)
			{
				itsJList.addSelectionInterval(j, j);
				--aCurrentSize;
			}
		}

		dispose();
	}
}
