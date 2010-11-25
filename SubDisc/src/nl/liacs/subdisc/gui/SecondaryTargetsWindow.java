package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import nl.liacs.subdisc.*;

public class SecondaryTargetsWindow extends JFrame implements ActionListener//, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private final JList itsJList;
	private int itsJListSize = 0;
	private JLabel itsFeedBackLabel = new JLabel();

	public SecondaryTargetsWindow(JList theJList)
	{
		itsJList = theJList;

		if (itsJList == null)
		{
			Log.logCommandLine(
				"SecondaryTargetsWindow Constructor: parameter can not be 'null'.");
			return;
		}
		else
		{
			itsJListSize = itsJList.getModel().getSize();
			initComponents(itsJList);
			setTitle("Secondary Targets");
			setLocation(100, 100);
			setSize(GUI.WINDOW_DEFAULT_SIZE);	// TODO
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
		aClosePanel.add(GUI.buildButton("Use Selected", 'U', "use", this));
		aClosePanel.add(GUI.buildButton("Cancel", 'C', "close", this));
		aButtonPanel.add(aClosePanel);

		// feedback label
		Box aFeedBackBox = Box.createHorizontalBox();
//		aFeedBackBox.add(GUI.buildLabel("Last Action: "));
		itsFeedBackLabel.setText(getFeedBackText());
		itsFeedBackLabel.setFont(GUI.DEFAULT_TEXT_FONT);
		aFeedBackBox.add(itsFeedBackLabel);
		aFeedBackBox.add(Box.createHorizontalGlue());
		aButtonPanel.add(aFeedBackBox);

		getContentPane().add(aButtonPanel, BorderLayout.SOUTH);
	}

	// TODO for size? not needs with 3 buttons panel
	/*
	 * Based on Swing tutorial TableRenderDemo.java.
	 * This method picks column sizes, based on column heads only.
	 * Could use JTable tables' itsTable for sizes instead (1 less parameter).
	 * TODO Put in SwingWorker background thread.
	 */
	private void initColumnSizes(Table theDataTable, JTable theJTable)
	{
		int aHeaderWidth = 0;

		TableCellRenderer aRenderer = theJTable.getTableHeader().getDefaultRenderer();

		for (int i = 0, j = theJTable.getColumnModel().getColumnCount(); i < j; i++)
		{
			// 91 is width of "(999 distinct)"
			aHeaderWidth = Math.max(aRenderer.getTableCellRendererComponent(
									null, theDataTable.getAttribute(i).getName(),
									false, false, 0, 0).getPreferredSize().width,
									91);

			theJTable.getColumnModel().getColumn(i).setPreferredWidth(aHeaderWidth);
		}
	}

	private String getFeedBackText()
	{
		return String.format(" %d of %d selected", itsJList.getSelectedIndices().length, itsJListSize);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		if ("all".equals(theEvent.getActionCommand()))
			itsJList.setSelectionInterval(0, itsJListSize - 1);
		else if ("none".equals(theEvent.getActionCommand()))
			itsJList.clearSelection();
		else if ("invert".equals(theEvent.getActionCommand()))
		{
			// not efficient, but easy
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
		else if ("use".equals(theEvent.getActionCommand()))
			dispose();
		else if ("close".equals(theEvent.getActionCommand()))
			dispose();

		itsFeedBackLabel.setText(getFeedBackText());
	}
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
