package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.event.*;

import nl.liacs.subdisc.*;

public class ConditionWindow extends JFrame implements ActionListener, ChangeListener
{
	private static final long serialVersionUID = 1L;

	JComboBox<String> itsComboBoxAttribute;
	JComboBox<String> itsComboBoxOperator;
	JTextField 	  itsTextFieldValue;
	JComboBox<String> itsComboBoxValue;

	ArrayList<Column> itsColumnList;
	private static final String ATTRIBUTE_BOX = "attribute box";
	private static final String OPERATOR_BOX = "operator box";

	// TODO should be tied to parent window
	public ConditionWindow(ArrayList<Column> theColumnList)
	{
		itsColumnList = theColumnList;
		initComponents();

		setTitle("Specify Condition");
		setIconImage(MiningWindow.ICON);
		setLocation(200, 200);
		setSize(new Dimension(300, 200));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private void initComponents()
	{
		setLayout(new BorderLayout());

		JPanel aNorthPanel = new JPanel();
		aNorthPanel.setLayout(new BorderLayout());
		aNorthPanel.setBorder(GUI.buildBorder("Subset condition"));

		// Labels Panel
		JPanel aPanelLabels = new JPanel();
		aPanelLabels.setLayout(new GridLayout(4, 1));
		JLabel aLabelAttribute = new JLabel("attribute");
		aPanelLabels.add(aLabelAttribute);
		JLabel aLabelOperator = new JLabel("operator");
		aPanelLabels.add(aLabelOperator);
		JLabel aLabelValue = new JLabel("value");
		aPanelLabels.add(aLabelValue);

		// Specifics Panel
		JPanel aSpecificsLabels = new JPanel();
		aSpecificsLabels.setLayout(new GridLayout(4, 1));

		//attribute
		itsComboBoxAttribute = GUI.buildComboBox(new String[0], ATTRIBUTE_BOX, this);
		aSpecificsLabels.add(itsComboBoxAttribute);
		for (Column aColumn : itsColumnList)
			itsComboBoxAttribute.addItem(aColumn.getName());

		//operator
		itsComboBoxOperator = GUI.buildComboBox(new String[0], OPERATOR_BOX, this);
		aSpecificsLabels.add(itsComboBoxOperator);
		Column aColumn = itsColumnList.get(0);
		for (Operator anOperator : Operator.getOperators(aColumn.getType()))
			if (anOperator.isSimple())
				itsComboBoxOperator.addItem(anOperator.toString());

		//value (numeric)
		itsTextFieldValue = GUI.buildTextField("0");
		aSpecificsLabels.add(itsTextFieldValue);
		//value (nominal)
		itsComboBoxValue = GUI.buildComboBox(new String[0], OPERATOR_BOX, this);
		aSpecificsLabels.add(itsComboBoxValue);
		for (String aValue : aColumn.getDomain())
			itsComboBoxValue.addItem(aValue); 

		aNorthPanel.add(aPanelLabels, BorderLayout.WEST);
		aNorthPanel.add(aSpecificsLabels, BorderLayout.EAST);
		add(aNorthPanel, BorderLayout.NORTH);

		//buttons
		JPanel aPanelButtons = new JPanel();
		aPanelButtons.setMinimumSize(new Dimension(0, 30));
		JButton aSelect = GUI.buildButton("Select", 'S', "select", this);
		aPanelButtons.add(aSelect, BorderLayout.SOUTH);
		JButton aDelete = GUI.buildButton("Delete", 'D', "delete", this);
		aPanelButtons.add(aDelete, BorderLayout.SOUTH);
		add(aPanelButtons, BorderLayout.SOUTH);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();

		if ("delete".equals(anEvent))
			dispose();			//TODO
		else if (anEvent.equals("select"))
		{
			Column aColumn = itsColumnList.get(0);
			String anAttribute = (String) itsComboBoxAttribute.getSelectedItem();
			for (Column aC : itsColumnList)
				if (aC.getName().equals(anAttribute))
					aColumn = aC;
			Operator anOperator = Operator.fromString((String) itsComboBoxOperator.getSelectedItem());
			ConditionBase aCB = new ConditionBase(aColumn, anOperator);
			Condition aCondition;
			if (aColumn.getType() == AttributeType.NUMERIC)
				aCondition = new Condition(aCB, Float.parseFloat(itsTextFieldValue.getText()), 0);
			else
				aCondition = new Condition(aCB, (String) itsComboBoxValue.getSelectedItem());
			System.out.println(aCondition.toString());

			dispose();			//TODO
		}
		if (anEvent.equals(ATTRIBUTE_BOX))
		{
			if (itsComboBoxOperator != null) //has it been properly initialised?
			{
				itsComboBoxOperator.removeAllItems();
				itsComboBoxValue.removeAllItems();
				Column aColumn = itsColumnList.get(0);
				String anAttribute = (String) itsComboBoxAttribute.getSelectedItem();
				for (Column aC : itsColumnList)
					if (aC.getName().equals(anAttribute))
						aColumn = aC;
				for (Operator anOperator : Operator.getOperators(aColumn.getType()))
					if (anOperator.isSimple())
						itsComboBoxOperator.addItem(anOperator.toString());
				if (aColumn.getType() == AttributeType.NOMINAL)
					for (String aValue : aColumn.getDomain())
						itsComboBoxValue.addItem(aValue); 

			}

		}
	}

	@Override
	public void stateChanged(ChangeEvent theEvent)
	{
		//TODO
	}
}
