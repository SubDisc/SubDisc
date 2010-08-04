package nl.liacs.subdisc.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import nl.liacs.subdisc.FileHandler;
import nl.liacs.subdisc.Table;
import nl.liacs.subdisc.Attribute.AttributeType;
import nl.liacs.subdisc.gui.AttributeTableModel.Column;

public class AttributeChangeWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private JTable jTable;

	public AttributeChangeWindow(Table theTable)
	{
		initJTable(theTable);
		initComponents();
		setupTypeColumn(jTable, jTable.getColumnModel().getColumn(Column.TYPE.columnNr));
//		setupCheckBoxColumn(jTable.getColumnModel().getColumn(Column.SELECT.columnNr));
		setTitle("Attribute types for: " + FileHandler.itsTable.itsName);
		setVisible(true);
	}

	private void initJTable(Table theTable)
	{
		jTable = new JTable(new AttributeTableModel(theTable));
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
		JPanel jPanelMain = new JPanel(new GridLayout(2, 0));
		JPanel aButtonPanel = new JPanel();

		JScrollPane jScrollPane = new JScrollPane(jTable);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exitForm();
			}
		});

		aButtonPanel.add(GUI.getButton("Disable Selected", 'D', "disable", this));
		aButtonPanel.add(GUI.getButton("Enable Selected", 'E', "enable", this));
		aButtonPanel.add(GUI.getButton("Change Type", 'C', "type", this));
		aButtonPanel.add(GUI.getButton("Change Missing", 'M', "missing", this));

		jPanelMain.add(aButtonPanel);
		getContentPane().add(jScrollPane, BorderLayout.CENTER);
		getContentPane().add(jPanelMain, BorderLayout.SOUTH);

		pack();
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		System.out.println(theEvent.getActionCommand());
		if("disable".equals(theEvent.getActionCommand()))
		{
			
		}
		else if("enable".equals(theEvent.getActionCommand()))
		{
			
		}
		else if("type".equals(theEvent.getActionCommand()))
		{
			
		}
		else if("missing".equals(theEvent.getActionCommand()))
		{
			
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
