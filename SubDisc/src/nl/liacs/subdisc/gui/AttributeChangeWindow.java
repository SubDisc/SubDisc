package nl.liacs.subdisc.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.BitSet;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import nl.liacs.subdisc.FileHandler;
import nl.liacs.subdisc.Table;
import nl.liacs.subdisc.Attribute.AttributeType;
import nl.liacs.subdisc.gui.AttributeTableModel.ColumnHeader;
import nl.liacs.subdisc.gui.AttributeTableModel.Selection;

public class AttributeChangeWindow extends JFrame implements ActionListener, ItemListener
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private JTable jTable;
	private JCheckBox selectAll = GUI.buildCheckBox("Select all", this);
	private JCheckBox selectAllNumeric = GUI.buildCheckBox("Select all numeric", this);
	private JCheckBox selectAllNominal = GUI.buildCheckBox("Select all nominal", this);
	private JCheckBox selectAllOrdinal = GUI.buildCheckBox("Select all ordinal", this);
	private JCheckBox selectAllBinary = GUI.buildCheckBox("Select all binary", this);
	private JCheckBox invertSelection = GUI.buildCheckBox("Invert selection", this);	// TODO needs rethinking

	public AttributeChangeWindow(Table theTable)
	{
		itsTable = theTable;
		initJTable(itsTable);
		initComponents();
		setupTypeColumn(jTable, jTable.getColumnModel().getColumn(ColumnHeader.TYPE.columnNr));
//		setupCheckBoxColumn(jTable.getColumnModel().getColumn(Column.SELECT.columnNr));
		setTitle("Attribute types for: " + FileHandler.itsTable.itsName);
		setVisible(true);
	}

	private void initJTable(Table theTable)
	{
		jTable = new JTable(AttributeTableModel.THE_ONLY_INSTANCE.setup(theTable));
/*
		{
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
				Component returnComp = super.prepareRenderer(renderer, row, column);
				if (!returnComp.getBackground().equals(getSelectionBackground())){
					returnComp .setBackground(row % 2 == 0 ? new Color(240,240,240) : Color.WHITE);
				}
				return returnComp;
			};
		};
*/
		jTable.setDefaultRenderer(String.class, new CustomRenderer());
//		jTable = new JTable(new AttributeTableModel(theTable));
		jTable.setPreferredScrollableViewportSize(new Dimension(1024, 800));
		jTable.setFillsViewportHeight(true);

		jTable.getColumnModel().getColumn(0).setPreferredWidth(10);
		jTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		jTable.getColumnModel().getColumn(2).setPreferredWidth(50);
	}

	private void initComponents()
	{
		JPanel jPanelSouth = new JPanel(new GridLayout(1, 4));
		JPanel aCheckBoxPanel = new JPanel();
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

		// selection checkboxes
		aCheckBoxPanel.setLayout(new BoxLayout(aCheckBoxPanel, BoxLayout.Y_AXIS));

		aCheckBoxPanel.add(selectAll);
		aCheckBoxPanel.add(selectAllNumeric);
		aCheckBoxPanel.add(selectAllNominal);
		aCheckBoxPanel.add(selectAllOrdinal);
		aCheckBoxPanel.add(selectAllBinary);
		aCheckBoxPanel.add(invertSelection);
		jPanelSouth.add(aCheckBoxPanel);

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

		ButtonGroup newType = new ButtonGroup();
		for(AttributeType at : AttributeType.values())
		{
			aRadioButtonPanel.add(new JRadioButton(at.name().toLowerCase()));
		}
		for(Component rb : aRadioButtonPanel.getComponents())
		{
			newType.add((AbstractButton) rb);
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

		JTextField aNewMissingValue = new JTextField("?");	// TODO reset based on selected attributes' type
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
//		System.out.println(theEvent.getActionCommand());
		BitSet bs = AttributeTableModel.getSelectedAttributes();
		for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1))
		{
//			System.out.println(itsTable.getColumn(i).getName());
			if("disable".equals(theEvent.getActionCommand()))
			{
				itsTable.getColumn(i).setIsEnabled(false);
			}
			else if("enable".equals(theEvent.getActionCommand()))
			{
				itsTable.getColumn(i).setIsEnabled(true);
			}
			else if("type".equals(theEvent.getActionCommand()))
			{
				
			}
			else if("missing".equals(theEvent.getActionCommand()))
			{
				
			}
		}
		itsTable.update();
	}

	@Override
	public void itemStateChanged(ItemEvent theEvent)
	{
		boolean selected = (theEvent.getStateChange() == ItemEvent.SELECTED);
		Object o = theEvent.getItemSelectable();
		if(o == selectAll)
		{
			AttributeTableModel.setSelectedAttributes(Selection.ALL);	// TODO will change
		}
		if(o == selectAllNumeric)
		{
			AttributeTableModel.selectAllType(AttributeType.NUMERIC, selected);
		}
		if(o == selectAllNominal)
		{
			AttributeTableModel.selectAllType(AttributeType.NOMINAL, selected);
		}
		if(o == selectAllOrdinal)
		{
			AttributeTableModel.selectAllType(AttributeType.ORDINAL, selected);
		}
		if(o == selectAllBinary)
		{
			AttributeTableModel.selectAllType(AttributeType.BINARY, selected);
		}
		if(o == invertSelection)
		{
			AttributeTableModel.setSelectedAttributes(Selection.INVERT);	// TODO will change
		}
	}

	private void exitForm() { dispose(); }

	public void setupTypeColumn(JTable table, TableColumn theTypeColumn)
	{
		JComboBox comboBox = new JComboBox();

		for(AttributeType at : AttributeType.values())
			comboBox.addItem(at.toString().toLowerCase());

		theTypeColumn.setCellEditor(new DefaultCellEditor(comboBox));
	}

	public void setupCheckBoxColumn(TableColumn theCheckBoxColumn)
	{
		theCheckBoxColumn.setCellRenderer(new CheckBoxRenderer());
	}

	// will be put in separate classes
	public class CustomRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			c.setBackground(Color.RED);
			return c;
		}
	}

	// TODO needs work
	public class CheckBoxRenderer extends JLabel implements TableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Color newColor = (Boolean)value ? Color.GREEN : Color.RED;
			setBackground(newColor);

			return this;
		}
	}


}
