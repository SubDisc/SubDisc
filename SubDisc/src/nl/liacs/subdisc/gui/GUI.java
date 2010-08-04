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

import javax.swing.JButton;
import javax.swing.border.BevelBorder;

public class GUI
{
	public enum Event
	{
		DISABLE_ATTRIBUTE,
		ENABLE_ATTRIBUTE,
		CHANGE_ATTRIBUTE_TYPE,
		CHANGE_MISSING;
	}

	private GUI() {}; // ininstantiable class

	public static JButton getButton(String theName, int theMnemonic, String theActionCommand, ActionListener theClass)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(new Dimension(110, 25));
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(new Dimension(82, 25));
		aButton.setMaximumSize(new Dimension(110, 25));
		aButton.setFont(new Font ("Dialog", 1, 11));
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic);
		aButton.setActionCommand(theActionCommand);
		aButton.addActionListener(theClass);
		return aButton;
	}
}
