/*
 * TODO if changes are made also update other opened windows, eg. BrowseWindow
 */
package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.Attribute.*;

public class AttributeChangeWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private JTable jTable;
	private ButtonGroup aNewType = new ButtonGroup();
	private JTextField aNewMissingValue = new JTextField("?");

	public AttributeChangeWindow(Table theTable)
	{
		itsTable = theTable;
		initJTable(itsTable);
		initComponents();
		setTitle("Attribute types for: " + itsTable.getTableName());
		setVisible(true);
	}

	private void initJTable(Table theTable)
	{
		jTable = new JTable(new AttributeTableModel(theTable));
		jTable.setPreferredScrollableViewportSize(new Dimension(GUI.DEFAULT_WINDOW_DIMENSION));
		jTable.setFillsViewportHeight(true);
/*
		jTable.getColumnModel().getColumn(0).setPreferredWidth(50);
		jTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		jTable.getColumnModel().getColumn(2).setPreferredWidth(50);
*/
	}

	private void initComponents()
	{
		JPanel jPanelSouth = new JPanel(new GridLayout(1, 4));
		JPanel aSelectionPanel = new JPanel();
		JPanel aDisablePanel = new JPanel();
		JPanel aRadioButtonPanel = new JPanel();
		JPanel aChangeTypePanel = new JPanel();
		JPanel aSetMissingPanel = new JPanel();

		JScrollPane jScrollPane = new JScrollPane(jTable);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exitForm();
			}
		});

		// selection buttons
		aSelectionPanel.setLayout(new BoxLayout(aSelectionPanel, BoxLayout.Y_AXIS));

		aSelectionPanel.add(GUI.buildButton("Select All", 'A', "all", this));
		// TODO could use generic loop over all AttributeTypes
		aSelectionPanel.add(GUI.buildButton("Select All Numeric", 'N', "numeric", this));
		aSelectionPanel.add(GUI.buildButton("Select All Nominal", 'L', "nominal", this));
		aSelectionPanel.add(GUI.buildButton("Select All Ordinal", 'O', "ordinal", this));
		aSelectionPanel.add(GUI.buildButton("Select All Binary", 'B', "binary", this));
		aSelectionPanel.add(GUI.buildButton("Invert Selection", 'I', "invert", this));
		aSelectionPanel.add(GUI.buildButton("Clear Selection", 'X', "clear", this));
		jPanelSouth.add(aSelectionPanel);

		// enable / disable
		JLabel aDisableLable = new JLabel("Disable/Enable Selected:");
		aDisableLable.setFont(GUI.DEFAULT_BUTTON_FONT);
		aDisablePanel.add(aDisableLable);
		aDisablePanel.setLayout(new BoxLayout(aDisablePanel, BoxLayout.Y_AXIS));
		aDisablePanel.add(Box.createVerticalGlue());
		aDisablePanel.add(GUI.buildButton("Disable Selected", 'D', "disable", this));
		aDisablePanel.add(Box.createVerticalGlue());
		aDisablePanel.add(GUI.buildButton("Enable Selected", 'E', "enable", this));
		aDisablePanel.add(Box.createVerticalGlue());
		aDisablePanel.add(GUI.buildButton("Toggle Selection", 'T', "toggle", this));
		aDisablePanel.add(Box.createVerticalGlue());
		jPanelSouth.add(aDisablePanel);

		// change type TODO buttons may need names
		aChangeTypePanel.setLayout(new BoxLayout(aChangeTypePanel, BoxLayout.Y_AXIS));
		JLabel aNewTypeLabel = new JLabel("New type:");
		aNewTypeLabel.setFont(GUI.DEFAULT_BUTTON_FONT);
		aChangeTypePanel.add(aNewTypeLabel);
		aChangeTypePanel.add(Box.createVerticalGlue());

		aRadioButtonPanel.setLayout(new BoxLayout(aRadioButtonPanel, BoxLayout.Y_AXIS));

		for(AttributeType at : AttributeType.values())
		{
			JRadioButton aRadioButton = new JRadioButton(at.name().toLowerCase());
			aRadioButton.setActionCommand(at.name());
			aRadioButtonPanel.add(aRadioButton);
		}

		for(Component rb : aRadioButtonPanel.getComponents())
			aNewType.add((AbstractButton) rb);

		aChangeTypePanel.add(aRadioButtonPanel);
		aChangeTypePanel.add(Box.createVerticalGlue());
		aChangeTypePanel.add(GUI.buildButton("Change Type", 'C', "type", this));
		jPanelSouth.add(aChangeTypePanel);

		// set missing
		aSetMissingPanel.setLayout(new BoxLayout(aSetMissingPanel, BoxLayout.Y_AXIS));
		aSetMissingPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		JLabel aSetMissingLabel = new JLabel("Set value for missing elements:");
		aSetMissingLabel.setFont(GUI.DEFAULT_BUTTON_FONT);
		aSetMissingPanel.add(aSetMissingLabel);
		aSetMissingPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		aNewMissingValue.setMaximumSize(new Dimension(220, 60));
		aSetMissingPanel.add(aNewMissingValue);
		aSetMissingPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		aSetMissingPanel.add(GUI.buildButton("Change Missing", 'M', "missing", this));
		jPanelSouth.add(aSetMissingPanel);

		getContentPane().add(jScrollPane, BorderLayout.CENTER);
		getContentPane().add(jPanelSouth, BorderLayout.SOUTH);

		pack();
	}

	// TODO use GUI.Event enums 
	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String theCommand = theEvent.getActionCommand();
		// generic loop prevents hard coding all AttributeTypes
		for (AttributeType at : AttributeType.values())
		{
			if (at.name().equalsIgnoreCase(theCommand))
			{
				for (int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
					if (itsTable.getColumn(i).getType() == at)
						jTable.addRowSelectionInterval(i, i);
				return;
			}
		}

		if ("all".equals(theCommand))
			jTable.selectAll();
		else if ("invert".equals(theCommand))
		{
			for (int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
			{
				if (jTable.isRowSelected(i))
					jTable.removeRowSelectionInterval(i, i);
				else
					jTable.addRowSelectionInterval(i, i);
			}
		}
		else
		{
			if ("disable".equals(theCommand) || "enable".equals(theCommand))
			{
				boolean enable = "enable".equals(theCommand);
				for (int i : jTable.getSelectedRows())
					itsTable.getColumn(i).setIsEnabled(enable);
			}
			else if ("toggle".equals(theCommand))
			{
				Column aColumn = null;
				for (int i : jTable.getSelectedRows())
				{
					aColumn = itsTable.getColumn(i);
					aColumn.setIsEnabled(!aColumn.getIsEnabled());
				}
			}
			else if ("type".equals(theCommand))
			{
				String aType = aNewType.getSelection().getActionCommand();
				for (int i : jTable.getSelectedRows())
					itsTable.getColumn(i).setType(aType);
			}
			else if ("missing".equals(theCommand))
				for (int i : jTable.getSelectedRows())
						setMissing(i);

			itsTable.update();
			jTable.repaint();
		}
	}
/*
	private void selectType(AttributeType theType)
	{
		for (int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
			if (itsTable.getColumn(i).getType() == theType)
				jTable.addRowSelectionInterval(i, i);
	}
*/
	/*
	 * TODO the isValidValue() will be checked in setMissingValue() and that
	 * method will throw a warning.
	 */
	private void setMissing(int theColumnIndex)
	{
		String aNewValue = aNewMissingValue.getText();
		Column c = itsTable.getColumn(theColumnIndex);
		if (c.isValidValue(aNewValue))
			c.setNewMissingValue(aNewValue);
		else
			;	// TODO throw warning
	}

	private void exitForm() { dispose(); }
}
