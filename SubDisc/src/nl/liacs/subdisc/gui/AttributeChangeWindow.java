/**
 * TODO if changes are made also update other opened windows, eg. BrowseWindow
 */
package nl.liacs.subdisc.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import nl.liacs.subdisc.Column;
import nl.liacs.subdisc.Table;
import nl.liacs.subdisc.Attribute.AttributeType;

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
		setTitle("Attribute types for: " + itsTable.itsName);
		setVisible(true);
	}

	private void initJTable(Table theTable)
	{
		jTable = new JTable(AttributeTableModel.THE_ONLY_INSTANCE.setup(theTable));
		jTable.setPreferredScrollableViewportSize(new Dimension(1024, 800));
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
		aSelectionPanel.setLayout(new GridLayout(3, 2));

		aSelectionPanel.add(GUI.buildButton("Select All", 'A', "all", this));
		// TODO could use generic loop over all AttributeTypes
		aSelectionPanel.add(GUI.buildButton("Select All Numeric", 'N', "numeric", this));
		aSelectionPanel.add(GUI.buildButton("Select All Nominal", 'L', "nominal", this));
		aSelectionPanel.add(GUI.buildButton("Select All Ordinal", 'O', "ordinal", this));
		aSelectionPanel.add(GUI.buildButton("Select All Binary", 'B', "binary", this));
		aSelectionPanel.add(GUI.buildButton("Invert Selection", 'I', "invert", this));
//		aSelectionPanel.add(GUI.buildButton("Clear Selection", 'X', "clear", this));
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
		jPanelSouth.add(aDisablePanel);

		// change type TODO buttons may need names
		aRadioButtonPanel.setLayout(new BoxLayout(aRadioButtonPanel, BoxLayout.Y_AXIS));

		for(AttributeType at : AttributeType.values())
		{
			aRadioButtonPanel.add(new JRadioButton(at.name().toLowerCase()));
		}
		for(Component rb : aRadioButtonPanel.getComponents())
		{
			aNewType.add((AbstractButton) rb);
		}
		JLabel aNewTypeLabel = new JLabel(" New type:");
		aNewTypeLabel.setFont(GUI.DEFAULT_BUTTON_FONT);
		aRadioButtonPanel.add(aNewTypeLabel, 0);

		aChangeTypePanel.add(aRadioButtonPanel);
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
		for(AttributeType at : AttributeType.values())
		{
			if(at.name().equalsIgnoreCase(theCommand))
			{
				selectType(at);
				return;
			}
		}

		if("all".equals(theCommand))
		{
			jTable.selectAll();
		}
		else if("invert".equals(theCommand))
		{
			for(int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
			{
				if(jTable.isRowSelected(i))
					jTable.removeRowSelectionInterval(i, i);
				else
					jTable.addRowSelectionInterval(i, i);
			}
		}
		else
		{
			for(int i : jTable.getSelectedRows())
			{
				if("disable".equals(theCommand))
				{
					itsTable.getColumn(i).setIsEnabled(false);
				}
				else if("enable".equals(theCommand))
				{
					itsTable.getColumn(i).setIsEnabled(true);
				}
				else if("type".equals(theCommand))
				{
					setType();
				}
				else if("missing".equals(theCommand))
				{
					setMissing(i);
				}
			}
			itsTable.update();
			jTable.repaint();
		}
		// TODO TEST ONLY
		for(Column c : itsTable.getColumns())
		{
//			System.out.println(c.getName() + " " + c.getIsEnabled());
		}
	}

	private void selectType(AttributeType theType)
	{
		for(int i =0, j = itsTable.getColumns().size(); i < j; ++i)
			if(itsTable.getColumn(i).getType() == theType)
				jTable.addRowSelectionInterval(i, i);
	}

	// TODO
	private void setType()
	{
		
	}

	private void setMissing(int theColumnIndex)
	{
		String aNewValue = aNewMissingValue.getText();
		Column c = itsTable.getColumn(theColumnIndex);
		if(c.isValidValue(aNewValue))
			c.setNewMissingValue(aNewValue);
		else
			;	// TODO WARNING!!!
	}

	private void exitForm() { dispose(); }
}
