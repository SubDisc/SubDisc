package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.swing.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.cui.*;

public class CuiDomainChooser extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private CountDownLatch itsDoneSignal;
	private List<File> itsAvailableDomains;
	private File itsDomainFile;
	private ButtonGroup itsDomainButtons = new ButtonGroup();

	public CuiDomainChooser(CountDownLatch aDoneSignal)
	{
		super("CUI Domain Chooser");
		super.setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		itsDoneSignal = aDoneSignal;
		initComponents();
		setLocation(100, 100);
//		setSize(GUI.DEFAULT_WINDOW_DIMENSION);	// TODO
//		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
	}

	private void initComponents()
	{
		JPanel aRadioButtonPanel = new JPanel();
		JPanel aButtonPanel = new JPanel();
		JRadioButton aRadioButton;

		File aCuiDir = new File(CuiMapInterface.CUI_DIR);

		if (aCuiDir == null || !aCuiDir.exists())
		{
			ErrorLog.log(aCuiDir, new FileNotFoundException());
			aRadioButtonPanel.add(new JLabel("No Domain Files Found"));
		}
		else
		{
			itsAvailableDomains =
								new ArrayList<File>(CuiMapInterface.NR_DOMAINS);
			for (File f : aCuiDir.listFiles())
				if (f.getName().startsWith(CuiMapInterface.DOMAIN_FILE_PREFIX))
					itsAvailableDomains.add(f);
			Collections.sort(itsAvailableDomains);

			aRadioButtonPanel.setLayout(new BoxLayout(aRadioButtonPanel,
														BoxLayout.PAGE_AXIS));

			for (File f : itsAvailableDomains)
			{
				aRadioButton = new JRadioButton(cleanFileName(f));
				aRadioButton.setActionCommand(f.getAbsolutePath());
				aRadioButtonPanel.add(aRadioButton);
			}

			for (Component c : aRadioButtonPanel.getComponents())
				itsDomainButtons.add((AbstractButton) c);

			if (itsAvailableDomains.size() > 0)
				((JRadioButton) aRadioButtonPanel.getComponent(0))
				.setSelected(true);

			aButtonPanel.add(
				GUI.buildButton("Use Domain", KeyEvent.VK_U, "domain", this));
			aButtonPanel.add(
				GUI.buildButton("Cancel", KeyEvent.VK_C, "cancel", this));
		}
		getContentPane().add(aRadioButtonPanel);
		getContentPane().add(aButtonPanel);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e) { CountDownAndDispose(); }
		});
	}

	private String cleanFileName(File theFile)
	{
		return FileType.removeExtension(theFile).substring(5).replace("_", " ");
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String aCommand = theEvent.getActionCommand();

		if ("domain".equals(aCommand))
		{
			itsDomainFile =
				new File(itsDomainButtons.getSelection().getActionCommand());
			CountDownAndDispose();
		}
		else if ("cancel".equals(aCommand))
			CountDownAndDispose();
	}

	private void CountDownAndDispose()
	{
		itsDoneSignal.countDown();
		dispose();
	}

	public File getFile()
	{
		return itsDomainFile;
	}

}
