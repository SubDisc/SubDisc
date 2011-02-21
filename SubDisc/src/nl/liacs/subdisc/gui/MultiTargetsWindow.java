package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class MultiTargetsWindow extends BasicJListWindow implements ActionListener//, ListSelectionListener
{
	private static final long serialVersionUID = 1L;
	//alpha = '\u03B1' '&#x3b1' beta =  '\u03B2' '&#x3b2' 
	private static final String ERROR =
		"Fields with invalid input: %d.\nAlpha and Beta must be '0 <= x <= 1'.\nNumber of repetitions must be 'x > 1'.";

	private Object[] itsOriginalSelection;
	private SearchParameters itsSearchParameters;
	private JTextField itsAlphaField;
	private JTextField itsBetaField;
	private JComboBox itsPostProcessBox =
		GUI.buildComboBox(new Object[] { "No","Subgroups", "Conditions" });
	private JTextField itsNrRepetitionsField =
		GUI.buildTextField(RandomQualitiesWindow.DEFAULT_NR_REPETITIONS);

	public MultiTargetsWindow(JList theJList, SearchParameters theSearchParameters)
	{
		super(theJList);

		if (theJList == null || theSearchParameters == null)
			constructorWarning("MultiTargetsWindow", true);
		else if (itsJList.getModel().getSize() == 0)
			constructorWarning("MultiTargetsWindow", false);
		else
		{
			itsOriginalSelection = itsJList.getSelectedValues();
			itsSearchParameters = theSearchParameters;
			addAdditionalComponents();
			display("Secondary Targets");
		}
	}

	private void addAdditionalComponents()
	{
		JPanel aPanel = new JPanel();
		aPanel.setBorder(GUI.buildBorder("Settings"));

		aPanel.setLayout(new GridLayout(4, 2));
		aPanel.add(GUI.buildLabel("Alpha", itsAlphaField));
		aPanel.add(itsAlphaField = GUI.buildTextField(String.valueOf(itsSearchParameters.getAlpha())));
		aPanel.add(GUI.buildLabel("Beta", itsBetaField));
		aPanel.add(itsBetaField = GUI.buildTextField(String.valueOf(itsSearchParameters.getBeta())));
		aPanel.add(GUI.buildLabel("Post process", itsPostProcessBox));
		aPanel.add(itsPostProcessBox);
		aPanel.add(GUI.buildLabel("Number of repetitions", itsNrRepetitionsField));
		aPanel.add(itsNrRepetitionsField);

		getContentPane().add(aPanel, BorderLayout.NORTH);
	}

	private void showErrorDialog(String theMessage)
	{
		JOptionPane.showMessageDialog(this,
						theMessage,
						"Invalid input",
						JOptionPane.ERROR_MESSAGE);
	}

	@Override
	protected void disposeOk()
	{
		NumberFormat aFormat = NumberFormat.getNumberInstance(Locale.US);
		try
		{
			byte aNrInvalid = 0;

			float anAlpha = aFormat.parse(itsAlphaField.getText()).floatValue();
			if (anAlpha < 0.0f || anAlpha > 1.0f)
			{
				itsAlphaField.selectAll();
				++aNrInvalid;
			}
			else
				itsSearchParameters.setAlpha(anAlpha);

			float aBeta = aFormat.parse(itsBetaField.getText()).floatValue();
			if (aBeta < 0.0f || aBeta > 1.0f)
			{
				itsBetaField.selectAll();
				++aNrInvalid;
			}
			else
				itsSearchParameters.setBeta(aBeta);

			int aNrRepetitions = aFormat.parse(itsNrRepetitionsField.getText()).intValue();
			if (aNrRepetitions <= 1) // (amount > itsSearchParameters.getMaximumSubgroups())
			{
				itsNrRepetitionsField.selectAll();
				++aNrInvalid;
			}
			else
			{
//				itsSearchParameters.setPostProcessingType(itsPostProcessBox.getSelectedItem());
//				itsSearchParameters.setPostProcessing(anAmount);
			}

			if (aNrInvalid == 0)
				dispose();
			else
				showErrorDialog(String.format(ERROR, aNrInvalid));
		}
		catch (ParseException e)
		{
			showErrorDialog(e.getMessage());
		}
	}

	// constructor guarantees itsJListSize > 0
	@Override
	protected void disposeCancel()
	{
		itsJList.clearSelection();

		// Columns might be disabled/re-typed since last time (order is same)
		// could be faster
		int aCurrentSize = itsJList.getModel().getSize();
		List<Object> aCurrentList = new ArrayList<Object>(aCurrentSize);

		for (int i = 0; i < aCurrentSize; ++i)
			aCurrentList.add(itsJList.getModel().getElementAt(i));

		int aLastSize = itsOriginalSelection.length;

		for (int i = 0, j = -1; i < aLastSize && aCurrentSize > 0; i++)
		{
			if ((j = aCurrentList.indexOf(itsOriginalSelection[i])) != -1)
			{
				itsJList.setSelectedIndex(j);
				--aCurrentSize;
			}
		}

		dispose();
	}
}
