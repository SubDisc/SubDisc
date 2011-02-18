package nl.liacs.subdisc.gui;

import java.awt.event.*;
import java.text.*;

import javax.swing.*;

public class RandomQualitiesWindow extends JDialog implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public static final String RANDOM_SUBGROUPS = "Random subgroups";
	public static final String RANDOM_CONDITIONS = "Random condition";
	public static final String DEFAULT_NR = "1000";

	private ButtonGroup itsMethods;
	private JTextField itsAmountField;
	private String[] itsSettings;

	public RandomQualitiesWindow(String theSetting)
	{
		super.setModalityType(DEFAULT_MODALITY_TYPE);
		itsSettings = new String[] { theSetting, null };
		initComponents();
		setTitle("Which method?");
		setLocation(100, 100);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
	}

	private void initComponents()
	{
		itsMethods = new ButtonGroup();

		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		JPanel aRadioButtonPanel = new JPanel();
		aRadioButtonPanel.setLayout(new BoxLayout(aRadioButtonPanel, BoxLayout.PAGE_AXIS));
		aRadioButtonPanel.add(GUI.buildLabel("By which method should the\nrandom qualities be computed?", null));

		JRadioButton aRadioButton = new JRadioButton(RANDOM_SUBGROUPS);
		aRadioButton.setActionCommand(RANDOM_SUBGROUPS);
		aRadioButton.setSelected(true);
		aRadioButtonPanel.add(aRadioButton);
		itsMethods.add(aRadioButton);
		aRadioButton = new JRadioButton(RANDOM_CONDITIONS);
		aRadioButton.setActionCommand(RANDOM_CONDITIONS);
		aRadioButtonPanel.add(aRadioButton);
		itsMethods.add(aRadioButton);
		getContentPane().add(aRadioButtonPanel);

		JPanel aNumberPanel = new JPanel();
		aNumberPanel.add(GUI.buildLabel("Amount", itsAmountField));
		aNumberPanel.add(itsAmountField = GUI.buildTextField(DEFAULT_NR));
		getContentPane().add(aNumberPanel);

		JPanel aButtonPanel = new JPanel();
		aButtonPanel.add(GUI.buildButton("OK", 'O', "ok", this));
		aButtonPanel.add(GUI.buildButton("Cancel", 'C', "cancel", this));
		getContentPane().add(aButtonPanel);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String aCommand = theEvent.getActionCommand();
		if ("ok".equals(aCommand))
		{
			NumberFormat aFormat = NumberFormat.getNumberInstance();
			try
			{
				if (aFormat.parse(itsAmountField.getText()).intValue() <= 1)
					showErrorDialog("Amount must be 'x > 1'.");
				else
				{
					itsSettings[0] = itsMethods.getSelection().getActionCommand();
					itsSettings[1] = itsAmountField.getText();
					dispose();
				}
			}
			catch (ParseException e)
			{
				showErrorDialog(e.getMessage());
			}
		}
		else if ("cancel".equals(aCommand))
			dispose();
	}

	private void showErrorDialog(String theMessage)
	{
		JOptionPane.showMessageDialog(this, theMessage,"Invalid input", JOptionPane.ERROR_MESSAGE);
	}

	public String[] getSettings()
	{
		return itsSettings;
	}
}
