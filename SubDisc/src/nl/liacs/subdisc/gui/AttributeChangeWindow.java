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
import nl.liacs.subdisc.FileHandler.Action;

public class AttributeChangeWindow extends JFrame
{
	private static final long serialVersionUID = 1L;
	private JPanel jPanelNorth;
	private JButton jButtonPressMe;
	private JScrollPane itsScrollPane;

	public static void main(String[] args)
	{
		AttributeChangeWindow a = new AttributeChangeWindow();

		new FileHandler(Action.OPEN_FILE);
		JTable table = new JTable(new AttributeTableModel(FileHandler.itsTable));
		table.setPreferredScrollableViewportSize(new Dimension(1024, 800));
		table.setFillsViewportHeight(true);

		table.getColumnModel().getColumn(0).setPreferredWidth(10);
		table.getColumnModel().getColumn(1).setPreferredWidth(50);
		table.getColumnModel().getColumn(2).setPreferredWidth(50);
		table.getColumnModel().getColumn(3).setPreferredWidth(50);

		//Create the scroll pane and add the table to it.
		JScrollPane aScrollPane = new JScrollPane(table);

		a.initComponents(aScrollPane);
		a.setupTypeColumn(table, table.getColumnModel().getColumn(2));	// NOTE (2) is dependent on TableTableModel
		a.setTitle("Attribute types for " + FileHandler.itsTable.itsName);
		a.setVisible(true);
	}
	private void initComponents(JScrollPane theScrollPane)
	{
		jPanelNorth = new JPanel(new GridLayout(2, 1));
		JPanel aSubgroupPanel = new JPanel();
		JPanel aSubgroupSetPanel = new JPanel();

		itsScrollPane = new JScrollPane(theScrollPane);
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
		aSubgroupPanel.add(jButtonPressMe);

		jPanelNorth.add(aSubgroupPanel);
		jPanelNorth.add(aSubgroupSetPanel);
		getContentPane().add(jPanelNorth, BorderLayout.NORTH);
		getContentPane().add(itsScrollPane, BorderLayout.CENTER);
		
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

		comboBox.addItem("numeric");
		comboBox.addItem("nominal");
		comboBox.addItem("ordinal");
		comboBox.addItem("binary");

		theTypeColumn.setCellEditor(new DefaultCellEditor(comboBox));
	}
}
