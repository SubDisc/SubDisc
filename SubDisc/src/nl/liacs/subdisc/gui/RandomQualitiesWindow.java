package nl.liacs.subdisc.gui;

import java.awt.event.*;
import java.text.*;

import javax.swing.*;

public class RandomQualitiesWindow extends JDialog implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public static final String RANDOM_SUBGROUPS = "Random subgroups";
	public static final String RANDOM_CONDITIONS = "Random condition";
	public static final String DEFAULT_NR_REPETITIONS = "1000";

	private ButtonGroup itsMethods;
	private JTextField itsNrRepetitionsField;
	private String[] itsSettings;

	public RandomQualitiesWindow(String theSetting)
	{
		super.setModalityType(DEFAULT_MODALITY_TYPE);
		itsSettings = new String[] { theSetting, null };
		initComponents();
		setTitle("Which method?");
		setIconImage(MiningWindow.ICON);
		setLocation(100, 100);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
	}

	private void initComponents()
	{
		itsMethods = new ButtonGroup();

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		JPanel aMasterPanel = new JPanel();
		aMasterPanel.setLayout(new BoxLayout(aMasterPanel, BoxLayout.Y_AXIS));
		aMasterPanel.setBorder(GUI.buildBorder("Method to compute random qualities"));

		JPanel aRadioButtonPanel = new JPanel();
		aRadioButtonPanel.setLayout(new BoxLayout(aRadioButtonPanel, BoxLayout.Y_AXIS));

		JRadioButton aRadioButton = new JRadioButton(RANDOM_SUBGROUPS);
		aRadioButton.setActionCommand(RANDOM_SUBGROUPS);
		aRadioButtonPanel.add(aRadioButton);
		itsMethods.add(aRadioButton);

		aRadioButton = new JRadioButton(RANDOM_CONDITIONS);
		aRadioButton.setActionCommand(RANDOM_CONDITIONS);
		aRadioButtonPanel.add(aRadioButton);
		itsMethods.add(aRadioButton);
		aMasterPanel.add(aRadioButtonPanel);

		JPanel aNumberPanel = new JPanel();
		aNumberPanel.add(GUI.buildLabel("Number of repetitions", itsNrRepetitionsField));
		aNumberPanel.add(Box.createHorizontalStrut(50));
		aNumberPanel.add(itsNrRepetitionsField = GUI.buildTextField(DEFAULT_NR_REPETITIONS));
		aNumberPanel.setAlignmentX(LEFT_ALIGNMENT);
		aMasterPanel.add(aNumberPanel);

		getContentPane().add(aMasterPanel);

		final JPanel aButtonPanel = new JPanel();
		aButtonPanel.add(GUI.buildButton("OK", 'O', "ok", this));
		aButtonPanel.add(GUI.buildButton("Cancel", 'C', "cancel", this));
		aButtonPanel.setAlignmentX(LEFT_ALIGNMENT);

		getContentPane().add(aButtonPanel);

		// select appropriate radio button, focus itsNrRepetitionsField
		if (RANDOM_CONDITIONS.equals(itsSettings[0]))
			((JRadioButton) aRadioButtonPanel.getComponent(1)).setSelected(true);
		else
			((JRadioButton) aRadioButtonPanel.getComponent(0)).setSelected(true);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				aButtonPanel.getComponent(0).requestFocusInWindow();
			}
		});
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
				if (aFormat.parse(itsNrRepetitionsField.getText()).intValue() <= 1)
					showErrorDialog("Number of repetitions must be > 1.");
				else
				{
					itsSettings[0] = itsMethods.getSelection().getActionCommand();
					itsSettings[1] = itsNrRepetitionsField.getText();
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
