package nl.liacs.subdisc.gui;

import javax.swing.*;

public class MultiNumericTargetsWindow extends BasicJListWindow// implements ActionListener//, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private int[] itsOriginalSelection;
	public MultiNumericTargetsWindow(JList theJList)
	{
		super(theJList);

		if (theJList == null)
			constructorWarning("MultiNumericTargetsWindow", true);
		else if (itsJList.getModel().getSize() == 0)
			constructorWarning("MultiNumericTargetsWindow", false);
		else
		{
			itsOriginalSelection = itsJList.getSelectedIndices();
			display("Multi-numeric targets");
		}
	}

	@Override
	protected void disposeOk()
	{
		// FIXME MM maybe 1 is a valid setting (SINGLE_NUMERIC)
		if (itsJList.getSelectedIndices().length <= 1)
		{
			JOptionPane.showMessageDialog(
					this,
					"At least 2 Columns must be selected.",
					"Invalid input",
					JOptionPane.ERROR_MESSAGE);
		}
		else
			dispose();
	}

	// constructor guarantees itsJListSize > 0
	@Override
	protected void disposeCancel()
	{
		itsJList.clearSelection();

		for (int i : itsOriginalSelection)
			itsJList.addSelectionInterval(i, i);

		dispose();
	}
}
