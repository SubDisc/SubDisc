/**
 * Convenience class to build GUI elements.
 * Any class calling these functions should implement ActionListener,
 *  if ActionListeners have to be set.
 * Also they need to be passed as (the last) parameter.
 * Typical usage: aPanel.add(GUI.getButton("Name", 'X', "KeyPressed", this));
 */

package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

public class GUI
{
	public static final Color RED = new Color(255, 150, 166);
	public static final Dimension TEXT_FIELD_DEFAULT_SIZE = new Dimension(86, 22);
	public static final Dimension WINDOW_DEFAULT_SIZE = new Dimension(1000, 600);
	public static final Dimension ROC_WINDOW_DEFAULT_SIZE = new Dimension(600, 600);
	public static final Dimension MATRIX_WINDOW_DEFAULT_SIZE = new Dimension(900, 400);
	// button
	public static final Dimension BUTTON_DEFAULT_SIZE = new Dimension(160, 25);
	public static final Dimension BUTTON_MEDIUM_SIZE = new Dimension(100, 25);

	public enum Event
	{
		DISABLE_ATTRIBUTE,
		ENABLE_ATTRIBUTE,
		CHANGE_ATTRIBUTE_TYPE,
		CHANGE_MISSING;
	}

	private GUI() {}; // uninstantiable class

	public static JButton buildButton(String theName, String theActionCommand, ActionListener theClass)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(BUTTON_DEFAULT_SIZE);
		aButton.setText(theName);
		aButton.setActionCommand(theActionCommand);
		aButton.addActionListener(theClass);
		return aButton;
	}

	public static JButton buildButton(String theName, int theMnemonic, String theActionCommand, ActionListener theClass)
	{
		// use constructor without mnemonic
		JButton aButton = GUI.buildButton(theName, theActionCommand, theClass);
		aButton.setMnemonic(theMnemonic);
		return aButton;
	}

	public static JRadioButton buildRadioButton(String theName, String theActionCommand, ActionListener theClass)
	{
		JRadioButton aRadioButton = new JRadioButton();
		aRadioButton.setText(theName);
		aRadioButton.setActionCommand(theActionCommand);
		aRadioButton.addActionListener(theClass);
		return aRadioButton;
	}

	public static JCheckBox buildCheckBox(String theName, ItemListener theClass)
	{
		JCheckBox aCheckBox = new JCheckBox(theName);
		aCheckBox.addItemListener(theClass);
		return aCheckBox;
	}

	public static JCheckBox buildCheckBox(String theName, ItemListener theClass, boolean isSelected)
	{
		JCheckBox aCheckBox = new JCheckBox(theName, isSelected);
		aCheckBox.addItemListener(theClass);
		return aCheckBox;
	}

	public static JComboBox<String> buildComboBox(String[] theItems, String theActionCommand, ActionListener theClass)
	{
		JComboBox<String> aComboBox = new JComboBox<>();
		aComboBox.setPreferredSize(TEXT_FIELD_DEFAULT_SIZE);
		aComboBox.setMinimumSize(TEXT_FIELD_DEFAULT_SIZE);

		for (String s : theItems)
			aComboBox.addItem(s);

		aComboBox.setActionCommand(theActionCommand);
		aComboBox.addActionListener(theClass);
		return aComboBox;
	}

	public static JComboBox<String> buildComboBox(String[] theItems, ActionListener theClass)
	{
		JComboBox<String> aComboBox = new JComboBox<>();
		aComboBox.setPreferredSize(TEXT_FIELD_DEFAULT_SIZE);
		aComboBox.setMinimumSize(TEXT_FIELD_DEFAULT_SIZE);

		for (String s : theItems)
			aComboBox.addItem(s);

		aComboBox.addActionListener(theClass);
		return aComboBox;
	}

	public static JLabel buildLabel(String theName, Component theComponent)
	{
		JLabel aJLable = new JLabel(theName);
		aJLable.setLabelFor(theComponent);
		return aJLable;
	}

	public static JMenuItem buildMenuItem(String theText, int theMnemonic, KeyStroke theAccelerator, ActionListener theClass)
	{
		JMenuItem aMenuItem = new JMenuItem(theText, theMnemonic);
		aMenuItem.setAccelerator(theAccelerator);
		aMenuItem.addActionListener(theClass);
		return aMenuItem;
	}

	public static JTextField buildTextField(String theText)
	{
		JTextField aTextField = new JTextField();
		aTextField.setPreferredSize(TEXT_FIELD_DEFAULT_SIZE);
		aTextField.setMinimumSize(TEXT_FIELD_DEFAULT_SIZE);
		aTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		aTextField.setText(theText);
		return aTextField;
	}

	// no need for null check
	public static Border buildBorder(String theTitle)
	{
		return new TitledBorder(new EtchedBorder(), theTitle, 4, 2);
	}

	// on window opening focus on the specified JComponent
	// TODO to be used by all window classes
	public static void focusComponent(final JComponent theComponentToFocus, JFrame theFrame)
	{
		theFrame.addWindowListener(
			new WindowAdapter()
			{
				@Override
				public void windowOpened(WindowEvent e)
				{
					theComponentToFocus.requestFocusInWindow();
				}
			});
	}
}
