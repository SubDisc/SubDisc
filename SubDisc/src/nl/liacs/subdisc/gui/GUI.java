/**
 * Convenience class to build GUI elements.
 * Any class calling these functions should implement ActionListener,
 *  if ActionListeners have to be set.
 * Also they need to be passed as (the last) parameter.
 * Typical usage: aPanel.add(GUI.getButton("Name", 'X', "KeyPressed", this));
 */

package nl.liacs.subdisc.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

public class GUI
{
	// TODO remove DEFAULT_
	public static final Font DEFAULT_TEXT_FONT = new Font ("Dialog", 0, 10);
	public static final Font DEFAULT_BUTTON_FONT = new Font ("Dialog", 1, 11);
	public static final Dimension DEFAULT_TEXT_FIELD_DIMENSION = new Dimension (86, 22);
	public static final Dimension DEFAULT_WINDOW_DIMENSION = new Dimension (1024, 700);

	public enum Event
	{
		DISABLE_ATTRIBUTE,
		ENABLE_ATTRIBUTE,
		CHANGE_ATTRIBUTE_TYPE,
		CHANGE_MISSING;
	}

	private GUI() {}; // ininstantiable class

	public static JButton buildButton(String theName, int theMnemonic, String theActionCommand, ActionListener theClass)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(new Dimension(110, 25));
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(new Dimension(82, 25));
		aButton.setMaximumSize(new Dimension(110, 25));
		aButton.setFont(DEFAULT_BUTTON_FONT);
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic);
		aButton.setActionCommand(theActionCommand);
		aButton.addActionListener(theClass);
		return aButton;
	}

	public static JButton buildButton(String theName, String theActionCommand, ActionListener theClass)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(new Dimension(110, 25));
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(new Dimension(82, 25));
		aButton.setMaximumSize(new Dimension(110, 25));
		aButton.setFont(DEFAULT_BUTTON_FONT);
		aButton.setText(theName);
		aButton.setActionCommand(theActionCommand);
		aButton.addActionListener(theClass);
		return aButton;
	}
	
	public static JCheckBox buildCheckBox(String theName, ItemListener theClass)
	{
		JCheckBox aCheckBox = new JCheckBox(theName);
//		aCheckBox.setMnemonic(theMnemonic);
		aCheckBox.addItemListener(theClass);
		return aCheckBox;
	}

	public static JLabel buildLabel(String theName)
	{
		JLabel aJLable = new JLabel(theName);
		aJLable.setFont(DEFAULT_TEXT_FONT);
		return aJLable;
	}

}
