package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.event.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.ConditionListBuilder.ConditionList;

public class ConditionWindow extends JDialog implements ActionListener, ChangeListener
{
	private static final long serialVersionUID = 1L;

	JComboBox<String> itsComboBoxAttribute;
	JComboBox<String> itsComboBoxOperator;
	JTextField 	  itsTextFieldValue;
	JComboBox<String> itsComboBoxValue;
	JPanel     	  itsSpecificsLabels;

	ArrayList<Column> itsColumnList;
	Condition    	  itsCondition = null;
	private static final String ATTRIBUTE_BOX = "attribute box";
	private static final String OPERATOR_BOX = "operator box";

	// TODO should be tied to parent window
	public ConditionWindow(JFrame theParent, ArrayList<Column> theColumnList)
	{
		super(theParent, "Specify Condition", true);
		itsColumnList = theColumnList;
		initComponents();

		setIconImage(MiningWindow.ICON);
		setLocation(200, 200);
		pack();
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
		aPanelLabels.setLayout(new GridLayout(3, 1));
		JLabel aLabelAttribute = new JLabel("attribute");
		aPanelLabels.add(aLabelAttribute);
		JLabel aLabelOperator = new JLabel("operator");
		aPanelLabels.add(aLabelOperator);
		JLabel aLabelValue = new JLabel("value");
		aPanelLabels.add(aLabelValue);

		// Specifics Panel
		itsSpecificsLabels = new JPanel();
		itsSpecificsLabels.setLayout(new GridLayout(3, 1));

		//attribute
		itsComboBoxAttribute = GUI.buildComboBox(new String[0], ATTRIBUTE_BOX, this);
		itsSpecificsLabels.add(itsComboBoxAttribute);
		for (Column aColumn : itsColumnList)
			itsComboBoxAttribute.addItem(aColumn.getName());

		//operator
		itsComboBoxOperator = GUI.buildComboBox(new String[0], OPERATOR_BOX, this);
		itsSpecificsLabels.add(itsComboBoxOperator);
		Column aColumn = itsColumnList.get(0);

		//value (numeric)
		itsTextFieldValue = GUI.buildTextField("0");
		itsSpecificsLabels.add(itsTextFieldValue);
		//value (nominal)
		itsComboBoxValue = GUI.buildComboBox(new String[0], OPERATOR_BOX, this);

		updateOperatorValue();

		aNorthPanel.add(aPanelLabels, BorderLayout.WEST);
		aNorthPanel.add(itsSpecificsLabels, BorderLayout.EAST);
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

	//set the operator and values according to the currently selected column
	private void updateOperatorValue()
	{
		itsComboBoxOperator.removeAllItems();
		itsComboBoxValue.removeAllItems();		//set operator

		//find selected Column
		Column aColumn = itsColumnList.get(0);
		String anAttribute = (String) itsComboBoxAttribute.getSelectedItem();
		for (Column aC : itsColumnList)
			if (aC.getName().equals(anAttribute))
				aColumn = aC;
		for (Operator anOperator : Operator.getOperators(aColumn.getType()))
			if (anOperator.isSimple())
				itsComboBoxOperator.addItem(anOperator.toString());
		if (aColumn.getType() == AttributeType.NOMINAL)
		{
			for (String aValue : aColumn.getDomain())
				itsComboBoxValue.addItem(aValue);
			if (itsTextFieldValue.getParent() == itsSpecificsLabels) {
				itsTextFieldValue.setVisible(false);
				itsSpecificsLabels.remove(itsTextFieldValue);
				itsSpecificsLabels.add(itsComboBoxValue);
				itsComboBoxValue.setVisible(true);
			}
		}
		else
		{
			if (itsComboBoxValue.getParent() == itsSpecificsLabels) {
				itsComboBoxValue.setVisible(false);
				itsSpecificsLabels.remove(itsComboBoxValue);
				itsSpecificsLabels.add(itsTextFieldValue);
				itsTextFieldValue.setVisible(true);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();

		if ("delete".equals(anEvent))
		{
			itsCondition = null; //let parent window know 'Delete' was selected
			dispose();
		}
		else if (anEvent.equals("select"))
		{
			Column aColumn = itsColumnList.get(0);
			String anAttribute = (String) itsComboBoxAttribute.getSelectedItem();
			for (Column aC : itsColumnList)
				if (aC.getName().equals(anAttribute))
					aColumn = aC;
			Operator anOperator = Operator.fromString((String) itsComboBoxOperator.getSelectedItem());
			ConditionBase aCB = new ConditionBase(aColumn, anOperator);
			if (aColumn.getType() == AttributeType.NUMERIC)
			{
				float aValue = Float.parseFloat(itsTextFieldValue.getText());
				aColumn.buildSorted(new BitSet()); //provide empty BitSet as target. Irrelevant at the moment
				int i = aColumn.getSortedIndex(aValue); //look up sort index
				System.out.println("float: " + aValue + ", index: " + i);
				itsCondition = new Condition(aCB, aValue, i); //store condition for parent window
			}
			else
				itsCondition = new Condition(aCB, (String) itsComboBoxValue.getSelectedItem());
			System.out.println(itsCondition.toString());

			dispose();
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
				{
					for (String aValue : aColumn.getDomain())
						itsComboBoxValue.addItem(aValue); 
					if (itsTextFieldValue.getParent() == itsSpecificsLabels) {
						itsTextFieldValue.setVisible(false);
						itsSpecificsLabels.remove(itsTextFieldValue);
						itsSpecificsLabels.add(itsComboBoxValue);
						itsComboBoxValue.setVisible(true);
					}
				}
				else
				{
					if (itsComboBoxValue.getParent() == itsSpecificsLabels) {
						itsComboBoxValue.setVisible(false);
						itsSpecificsLabels.remove(itsComboBoxValue);
						itsSpecificsLabels.add(itsTextFieldValue);
						itsTextFieldValue.setVisible(true);
					}
				}

			}

		}
	}

	@Override
	public void stateChanged(ChangeEvent theEvent)
	{
		//TODO
	}

	public Condition getResult()
	{
		return itsCondition;
	}
}
