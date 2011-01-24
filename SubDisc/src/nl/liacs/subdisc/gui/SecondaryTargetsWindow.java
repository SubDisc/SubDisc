package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class SecondaryTargetsWindow extends JDialog implements ActionListener//, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private final JList itsJList;
	private int itsJListSize = 0;	// not safe, modality only blocks other WINDOWS
	private Object[] itsLastSelection;
	private JLabel itsFeedBackLabel = new JLabel();

	public SecondaryTargetsWindow(JList theJList)
	{
		super.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		itsJList = theJList;

		if (itsJList == null)
		{
			// TODO ErrorLog
			Log.logCommandLine(
				"SecondaryTargetsWindow Constructor: parameter can not be 'null'.");
			return;
		}
		else if ((itsJListSize = itsJList.getModel().getSize()) == 0)
		{
			// TODO ErrorLog
			Log.logCommandLine(
			"SecondaryTargetsWindow Constructor: the list can not be empty.");
			return;
		}
		else
		{
//			itsJListSize = itsJList.getModel().getSize();
			itsLastSelection = itsJList.getSelectedValues();
			initComponents(itsJList);
			setTitle("Secondary Targets");
			setLocation(100, 100);
			setSize(GUI.WINDOW_DEFAULT_SIZE);	// TODO
//			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			pack();
			setVisible(true);
		}
	}

	private void initComponents(JList theJList)
	{
		getContentPane().add(new JScrollPane(theJList), BorderLayout.CENTER);

		JPanel aButtonPanel = new JPanel();
		aButtonPanel.setLayout(new BoxLayout(aButtonPanel, BoxLayout.Y_AXIS));

		// selection buttons
		JPanel aSelectPanel = new JPanel();
		aSelectPanel.add(GUI.buildButton("Select All", 'A', "all", this));
		aSelectPanel.add(GUI.buildButton("Select None", 'N', "none", this));
		aSelectPanel.add(GUI.buildButton("Invert Selection", 'I', "invert", this));
		aButtonPanel.add(aSelectPanel);

		// confirm and cancel buttons
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("OK", 'O', "ok", this));
		aClosePanel.add(GUI.buildButton("Cancel", 'C', "close", this));
		aButtonPanel.add(aClosePanel);

		// feedback label
		Box aFeedBackBox = Box.createHorizontalBox();
		itsFeedBackLabel.setText(getFeedBackText());
		itsFeedBackLabel.setFont(GUI.DEFAULT_TEXT_FONT);
		aFeedBackBox.add(itsFeedBackLabel);
		aFeedBackBox.add(Box.createHorizontalGlue());
		aButtonPanel.add(aFeedBackBox);

		getContentPane().add(aButtonPanel, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e) { disposeUndoChanges(); }
		});

	}

	private String getFeedBackText()
	{
		return String.format(" %d of %d selected", itsJList.getSelectedIndices().length, itsJListSize);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anAction = theEvent.getActionCommand();

		if ("all".equals(anAction))
			itsJList.setSelectionInterval(0, itsJListSize - 1);
		else if ("none".equals(anAction))
			itsJList.clearSelection();
		else if ("invert".equals(anAction))
		{
			// TODO use isSelected()
			int[] aSelection = itsJList.getSelectedIndices();
			itsJList.clearSelection();

			int i = 0;
			for (int j = 0, k = aSelection.length; j < k; i++)
			{
				if (i == aSelection[j])
					++j;
				else
					itsJList.addSelectionInterval(i, i);
			}
			if (i < itsJListSize)
				itsJList.addSelectionInterval(i, itsJListSize - 1);
		}
		else if ("ok".equals(anAction))
		{
			itsLastSelection = itsJList.getSelectedValues();
			dispose();
		}
		else if ("close".equals(anAction))
			disposeUndoChanges();

		itsFeedBackLabel.setText(getFeedBackText());
	}

	// constructor guarantees itsJListSize > 0
	private void disposeUndoChanges()
	{
		itsJList.clearSelection();

		// Columns might be disabled/re-typed since last time (order is same)
		// could be faster
		int aCurrentSize = itsJList.getModel().getSize();
		ArrayList<Object> aCurrentList = new ArrayList<Object>(aCurrentSize);

		for (int i = 0; i < aCurrentSize; i++)
			aCurrentList.add(itsJList.getModel().getElementAt(i));

		int aLastSize = itsLastSelection.length;

		for (int i = 0, j = -1; i < aLastSize && aCurrentSize > 0; i++)
		{
			if ((j = aCurrentList.indexOf(itsLastSelection[i])) != -1)
			{
				itsJList.addSelectionInterval(j, j);
				--aCurrentSize;
			}
		}

		dispose();
	}

	// TODO update feedBackLabel on mouseSelections
/*
	@Override
	public void valueChanged(ListSelectionEvent theEvent)
	{
//			compute selected targets and update TargetConcept
//			int[] aSelection = jListSecondaryTargets.getSelectedIndices();
//			ArrayList<Attribute> aList = new ArrayList<Attribute>(aSelection.length);
//			for (int anIndex : aSelection)
//				aList.add(itsTable.getAttribute(itsTable.getBinaryIndex(anIndex)));
		int aNrBinary = itsJList.getSelectedIndices().length;
		ArrayList<Attribute> aList = new ArrayList<Attribute>(aNrBinary);
		for (Column c : itsTable.getColumns())
		{
			if (c.getAttribute().isBinaryType())
			{
				aList.add(c.getAttribute());
				if (--aNrBinary == 0)
					break;
			}
		}

		itsTargetConcept.setMultiTargets(aList);

		//update GUI
		initTargetInfo();
	}
*/
}
