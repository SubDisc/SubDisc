/*
 * TODO if changes are made also update other opened windows, eg. BrowseWindow
 */
package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.Attribute.*;

public class AttributeChangeWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private JTable jTable;
	private ButtonGroup aNewType = new ButtonGroup();
	private JTextField aNewMissingValue =
		new JTextField(AttributeType.NOMINAL.DEFAULT_MISSING_VALUE);
	private JLabel itsFeedBackLabel = new JLabel();
	private MiningWindow itsMiningWindow;

	public AttributeChangeWindow(MiningWindow theMiningWindow, Table theTable)
	{
		if (theTable == null || theMiningWindow == null)
		{
			Log.logCommandLine("To create a Data Editor Window parameters can not be null.");
			return;
		}
		else
		{
			itsMiningWindow = theMiningWindow;
			itsTable = theTable;
			initJTable(itsTable);
			initComponents();
			setTitle("Attribute types for: " + itsTable.getTableName());
			setLocation(100, 100);
			setSize(GUI.DEFAULT_WINDOW_DIMENSION);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setVisible(true);
		}
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

//		jPanelSouth.add(Box.createHorizontalGlue());

		// selection buttons
		aSelectionPanel.setLayout(new BoxLayout(aSelectionPanel, BoxLayout.Y_AXIS));

		aSelectionPanel.add(GUI.buildButton("Select All", 'A', "all", this));
		// TODO could use generic loop over all AttributeTypes
		aSelectionPanel.add(GUI.buildButton("Select All Nominal", 'L', AttributeType.NOMINAL.toString(), this));
		aSelectionPanel.add(GUI.buildButton("Select All Numeric", 'N', AttributeType.NUMERIC.toString(), this));
		aSelectionPanel.add(GUI.buildButton("Select All Ordinal", 'O', AttributeType.ORDINAL.toString(), this));
		aSelectionPanel.add(GUI.buildButton("Select All Binary", 'B', AttributeType.BINARY.toString(), this));
		aSelectionPanel.add(GUI.buildButton("Invert Selection", 'I', "invert", this));
		aSelectionPanel.add(GUI.buildButton("Clear Selection", 'X', "clear", this));
		jPanelSouth.add(aSelectionPanel);

		// change type
		aChangeTypePanel.setLayout(new BoxLayout(aChangeTypePanel, BoxLayout.Y_AXIS));
		JLabel aNewTypeLabel = new JLabel("New type:");
		aNewTypeLabel.setFont(GUI.DEFAULT_BUTTON_FONT);
		aChangeTypePanel.add(aNewTypeLabel);
		aChangeTypePanel.add(Box.createVerticalGlue());

		aRadioButtonPanel.setLayout(new BoxLayout(aRadioButtonPanel, BoxLayout.Y_AXIS));

		for(AttributeType at : AttributeType.values())
		{
			String aType = at.toString();
			JRadioButton aRadioButton = new JRadioButton(aType.toLowerCase());
			aRadioButton.setActionCommand(aType);	// UPPERCASE
			aRadioButtonPanel.add(aRadioButton);
		}

		for(Component rb : aRadioButtonPanel.getComponents())
			aNewType.add((AbstractButton) rb);

		aChangeTypePanel.add(aRadioButtonPanel);
		aChangeTypePanel.add(Box.createVerticalGlue());
		aChangeTypePanel.add(GUI.buildButton("Change Type", 'C', "type", this));
		jPanelSouth.add(aChangeTypePanel);

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
		aDisablePanel.add(GUI.buildButton("Toggle Selected", 'T', "toggle", this));
		aDisablePanel.add(Box.createVerticalGlue());
		jPanelSouth.add(aDisablePanel);

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

//		jPanelSouth.add(Box.createHorizontalGlue());

		itsFeedBackLabel.setText("Attributes loaded for " + itsTable.getTableName());

		getContentPane().add(jScrollPane, BorderLayout.CENTER);
		getContentPane().add(jPanelSouth, BorderLayout.SOUTH);
//		getContentPane().add(itsFeedBackLabel);

		pack();
	}

	// TODO use GUI.Event enums
	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String aCommand = theEvent.getActionCommand();
		// generic loop prevents hard coding all AttributeTypes
		for (AttributeType at : AttributeType.values())
		{
			if (at.toString().equals(aCommand))
			{
				for (int i = 0, j = itsTable.getColumns().size(); i < j; i++)
					if (itsTable.getColumn(i).getType() == at)
						jTable.addRowSelectionInterval(i, i);
				return;
			}
		}

		if ("all".equals(aCommand))
			jTable.selectAll();
		else if ("invert".equals(aCommand))
		{
			for (int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
			{
				if (jTable.isRowSelected(i))
					jTable.removeRowSelectionInterval(i, i);
				else
					jTable.addRowSelectionInterval(i, i);
			}
		}
		else if ("clear".equals(aCommand))
			jTable.clearSelection();
		else
		{
			if ("disable".equals(aCommand) || "enable".equals(aCommand))
			{
				boolean enable = "enable".equals(aCommand);
				for (int i : jTable.getSelectedRows())
					itsTable.getColumn(i).setIsEnabled(enable);
			}
			else if ("toggle".equals(aCommand))
			{
				Column aColumn = null;
				for (int i : jTable.getSelectedRows())
				{
					aColumn = itsTable.getColumn(i);
					aColumn.setIsEnabled(!aColumn.getIsEnabled());
				}
			}
			else if ("type".equals(aCommand))
			{
				String aType = aNewType.getSelection().getActionCommand();
				for (int i : jTable.getSelectedRows())
					itsTable.getColumn(i).setType(aType);
					// TODO show messageDialog asking to treat first value as
					// 'true' or 'false' (see Column.toBinary())
					// TODO failed to change type warning
			}
			else if ("missing".equals(aCommand))
			{
				String aNewValue = aNewMissingValue.getText();
				ArrayList<Integer> aWrongType = new ArrayList<Integer>(jTable.getSelectedRows().length);
				for (int i : jTable.getSelectedRows())
					if (!itsTable.getColumn(i).setNewMissingValue(aNewValue))
						aWrongType.add(i);

				if (aWrongType.size() > 0)
				{
					jTable.getSelectionModel().clearSelection();
					for (int i : aWrongType)
						jTable.addRowSelectionInterval(i, i);

					String anIndicator;
					if (aWrongType.size() == 1)
					{
						Column aColumn = itsTable.getColumn(aWrongType.get(0));
						anIndicator = String.format("attribute '%s', which is of type '%s'.%n",
										aColumn.getName(),
										aColumn.getAttribute().getTypeName());
					}
					else
						anIndicator = "some attributes. They are of an incompatible type.\n See selection.";
					JOptionPane.showMessageDialog(null,
										String.format(
											"'%s' is not a valid value for %s",
											aNewValue,
											anIndicator),
										"alert",
										JOptionPane.ERROR_MESSAGE);
				}
			}
			jTable.repaint();
			itsMiningWindow.update();
		}
	}

	private void exitForm() { dispose(); }
}

