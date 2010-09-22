package nl.liacs.subdisc.gui;

import java.io.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.cui.*;

public class CuiDomainChooser extends JFrame
{

	private static final long serialVersionUID = 1L;

	private List<File> itsAvailableDomains;
	private final Map<String, Integer> itsCui2LineNrMap;

	public CuiDomainChooser()
	{
		super("CUI Domain Chooser");
		initComponents();
		setLocation(100, 100);
		setSize(GUI.DEFAULT_WINDOW_DIMENSION);	// TODO
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
		itsCui2LineNrMap = null;
	}

	private void initComponents()
	{
		JPanel aRadioButtonPanel = new JPanel();
		JPanel jPanelSouth = new JPanel();
		JRadioButton aRadioButton;

		File aCuiDir = new File(CuiMapInterface.CUI_DIR);

		if (aCuiDir == null || !aCuiDir.exists())
		{
			ErrorLog.log(aCuiDir, new FileNotFoundException(""));
			aRadioButtonPanel.add(new JLabel("No Domain Files Found"));
		}
		else
		{
			// there are about 30 CUI-Domain files
			itsAvailableDomains = new ArrayList<File>(30);
			for (File f : aCuiDir.listFiles())
				if (FileType.getFileType(f) == FileType.CUI)
					itsAvailableDomains.add(f);

			for (File f : itsAvailableDomains)
			{
				String aCleanName = cleanFileName(f);
				aRadioButton = new JRadioButton(aCleanName);
				aRadioButton.setActionCommand(aCleanName);	// f.getAbsolutePath
				aRadioButtonPanel.add(aRadioButton);
			}
		}

		getContentPane().add(aRadioButtonPanel);
	}

	private String cleanFileName(File theFile)
	{
		return FileType.removeExtension(theFile).substring(5).replace("_", " ");
	}

	/**
	 * Returns the <code>Map<String, String></code> for this CuiDomainChooser.
	 * 
	 * @return the <code>Map<String, Integer></code> for this CuiDomainChooser,
	 * or <code>null</code> if there is none.
	 */
	public Map<String, Integer> getMap()
	{
		if (itsCui2LineNrMap == null)
			return null;
		else
			return Collections.unmodifiableMap(itsCui2LineNrMap);
	}
}
