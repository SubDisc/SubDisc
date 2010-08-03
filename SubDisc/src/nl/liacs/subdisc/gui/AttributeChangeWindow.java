package nl.liacs.subdisc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.table.TableColumn;

import nl.liacs.subdisc.FileHandler;
import nl.liacs.subdisc.Table;
import nl.liacs.subdisc.Attribute.AttributeType;
import nl.liacs.subdisc.gui.AttributeTableModel.Column;

public class AttributeChangeWindow extends JFrame
{
	private static final long serialVersionUID = 1L;
	private JPanel jPanelMain;
	private JButton jButtonPressMe;
	private JScrollPane jScrollPane;
	private JTable jTable;

	public AttributeChangeWindow(Table theTable)
	{
		initJTable(theTable);
		initComponents();
		setupTypeColumn(jTable, jTable.getColumnModel().getColumn(Column.TYPE.columnNr));
		setTitle("Attribute types for: " + FileHandler.itsTable.itsName);
		setVisible(true);
	}

	private void initJTable(Table theTable)
	{
		jTable = new JTable(new AttributeTableModel(theTable));
		jTable.setPreferredScrollableViewportSize(new Dimension(1024, 800));
		jTable.setFillsViewportHeight(true);

		jTable.getColumnModel().getColumn(0).setPreferredWidth(10);
		jTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		jTable.getColumnModel().getColumn(2).setPreferredWidth(50);
	}

	private void initComponents()
	{
		jPanelMain = new JPanel(new GridLayout(2, 0));
		JPanel aButtonPanel = new JPanel();

		jScrollPane = new JScrollPane(jTable);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exitForm();
			}
		});

		jButtonPressMe = initButton("Press Me", 'P');
		jButtonPressMe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				System.out.println("You Pressed Button...");
			}
		});
		aButtonPanel.add(jButtonPressMe);

		jPanelMain.add(aButtonPanel);
		getContentPane().add(jScrollPane, BorderLayout.CENTER);
		getContentPane().add(jPanelMain, BorderLayout.SOUTH);
	
		pack();
	}

	private JButton initButton(String theName, int theMnemonic)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(new Dimension(110, 25));
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(new Dimension(82, 25));
		aButton.setMaximumSize(new Dimension(110, 25));
		aButton.setFont(new Font ("Dialog", 1, 11));
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic);
		return aButton;
	}

	private void exitForm() { dispose(); }

	public void setupTypeColumn(JTable table, TableColumn theTypeColumn)
	{
		JComboBox comboBox = new JComboBox();

		for(AttributeType at : AttributeType.values())
			comboBox.addItem(at.toString().toLowerCase());

		theTypeColumn.setCellEditor(new DefaultCellEditor(comboBox));
	}
}
