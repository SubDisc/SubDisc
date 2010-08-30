package nl.liacs.subdisc.gui;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import nl.liacs.subdisc.DAGView;
import nl.liacs.subdisc.Log;
import nl.liacs.subdisc.SearchParameters;
import nl.liacs.subdisc.Subgroup;
import nl.liacs.subdisc.SubgroupSet;
import nl.liacs.subdisc.TargetConcept.TargetType;

public class ResultWindow extends JFrame
{
	private static final long serialVersionUID = 1L;
//	public static final Image ICON = Toolkit.getDefaultToolkit().getImage(ResultWindow.class.getResource("/Safarii.gif"));

	private SearchParameters itsSearchParameters;
	private ResultTableModel itsResultTableModel;
	private JTable itsSubgroupTable;
	private SubgroupSet itsSubgroupSet;
	private DAGView itsDAGView; //layout of the graph on the whole database

	public ResultWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters, DAGView theDAGView)
	{

		itsSubgroupSet = theSubgroupSet;
		itsSearchParameters = theSearchParameters;
		itsDAGView = theDAGView;
		itsResultTableModel = new ResultTableModel(itsSubgroupSet, itsSearchParameters);
		itsSubgroupTable = new JTable(itsResultTableModel);
		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);

		initComponents ();
//		setIconImage(ICON);
		initialise();

		if (itsSubgroupSet.isEmpty())
			setTitle("No patterns found that match the set criterion");
		else
			setTitle(itsSubgroupSet.size() + " patterns found");
	}

	public void initialise()
	{
		itsSubgroupTable.getColumnModel().getColumn(0).setPreferredWidth(15);
		itsSubgroupTable.getColumnModel().getColumn(1).setPreferredWidth(15);
		itsSubgroupTable.getColumnModel().getColumn(2).setPreferredWidth(20);
		itsSubgroupTable.getColumnModel().getColumn(3).setPreferredWidth(80);
		itsSubgroupTable.getColumnModel().getColumn(4).setPreferredWidth(600);

		itsScrollPane.add(itsSubgroupTable);
		itsScrollPane.setViewportView(itsSubgroupTable);

		itsSubgroupTable.setRowSelectionAllowed(true);
		itsSubgroupTable.setColumnSelectionAllowed(false);
		itsSubgroupTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		itsSubgroupTable.addKeyListener(new java.awt.event.KeyListener()
		{
			public void keyPressed(java.awt.event.KeyEvent key)
			{
				if (key.getKeyCode() == KeyEvent.VK_ENTER)
					if (itsSubgroupTable.getRowCount() > 0)
						jButtonShowDAGActionPerformed();
				if (key.getKeyCode() == KeyEvent.VK_DELETE)
					if(itsSubgroupTable.getRowCount() > 0)
						jButtonDeleteSubgroupsActionPerformed();
			}

			public void keyReleased(java.awt.event.KeyEvent key) {	}

			public void keyTyped(java.awt.event.KeyEvent key) {	}
		});

		pack ();
	}

	private void initComponents()
	{
		jPanelSouth = new JPanel(new GridLayout(2, 1));
		JPanel aSubgroupPanel = new JPanel();
		JPanel aSubgroupSetPanel = new JPanel();

		itsScrollPane = new javax.swing.JScrollPane();
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				exitForm();
			}
		});

		jButtonShowDAG = initButton("Show DAG", 'S');
		jButtonShowDAG.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonShowDAGActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonShowDAG);

		jButtonROC = initButton("ROC", 'R');
		jButtonROC.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonROCActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonROC);
		jButtonROC.setEnabled(itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);

		jButtonDeleteSubgroups = initButton("Delete Pattern", 'D');
		jButtonDeleteSubgroups.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonDeleteSubgroupsActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonDeleteSubgroups);

		jButtonPostprocess = initButton("Postprocess results", 'P');
		jButtonPostprocess.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonPostprocessActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonPostprocess);
		jButtonPostprocess.setEnabled(itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL);

		//possibly disable buttons
		if (itsSubgroupSet != null) //Subgroup set
		{
			if (itsSubgroupSet.isEmpty())
			{
				jButtonShowDAG.setEnabled(false);
				jButtonROC.setEnabled(false);
				jButtonDeleteSubgroups.setEnabled(false);
				jButtonPostprocess.setEnabled(false);
			}
		}

			


		//close button
		jButtonCloseWindow = initButton("Close", 'C');
		jButtonCloseWindow.addActionListener(new java.awt.event.ActionListener() {
		 	public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonCloseWindowActionPerformed();
		  	}
		});

		aSubgroupSetPanel.add(jButtonCloseWindow);

		jPanelSouth.add(aSubgroupPanel);
		jPanelSouth.add(aSubgroupSetPanel);
		getContentPane().add(jPanelSouth, java.awt.BorderLayout.SOUTH);
		getContentPane().add(itsScrollPane, java.awt.BorderLayout.CENTER);
	}

	private JButton initButton(String theName, int theMnemonic)
	{
		JButton aButton = new javax.swing.JButton();
		aButton.setPreferredSize(new java.awt.Dimension(110, 25));
		aButton.setBorder(new javax.swing.border.BevelBorder(0));
		aButton.setMinimumSize(new java.awt.Dimension(82, 25));
		aButton.setMaximumSize(new java.awt.Dimension(110, 25));
		aButton.setFont(new java.awt.Font ("Dialog", 1, 11));
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic);
		return aButton;
	}

	private void jButtonShowDAGActionPerformed()
	{
		int[] aSelectionIndex = itsSubgroupTable.getSelectedRows();

		if (aSelectionIndex.length>0)
		{
			int i = 0;
			while (i < aSelectionIndex.length)
			{
				Log.logCommandLine("subgroup " + (aSelectionIndex[i]+1));
				Iterator<Subgroup> anIterator = itsSubgroupSet.iterator();
				int aCount = 0;
				while (anIterator.hasNext())
				{
					Subgroup aSubgroup = anIterator.next();
					if (aCount == aSelectionIndex[i])
					{
						ModelWindow aWindow = new ModelWindow(aSubgroup.getDAG(), 1200, 900);
						aWindow.setLocation(0, 0);
						aWindow.setSize(1200, 900);
						aWindow.setVisible(true);
						aWindow.setTitle("Bayesian net induced from subgroup " + (aSelectionIndex[i]+1));
					}
					aCount++;
				}

				i++;
			}
		}
	}

	private void jButtonROCActionPerformed()
	{
		new ROCCurveWindow(itsSubgroupSet, itsSearchParameters);
	}

	private void jButtonDeleteSubgroupsActionPerformed()
	{
		if (itsSubgroupSet.isEmpty())
			return;
		Iterator<Subgroup> anIterator = itsSubgroupSet.iterator();

		int[] aSelectionIndex = itsSubgroupTable.getSelectedRows();

		for (int i = 0; i < aSelectionIndex.length; i++)
		{
			for (int j = 0; j <= aSelectionIndex[i]; j++)
				anIterator.next();

			anIterator.remove();

			for (int j = i + 1; j< aSelectionIndex.length; j++)
				if (aSelectionIndex[j] > aSelectionIndex[i] )
					aSelectionIndex[j] -= 1;
		}
		itsSubgroupTable.repaint();

		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);
		else
		{
			jButtonDeleteSubgroups.setEnabled(false);
		}
	}
	
	private void jButtonPostprocessActionPerformed()
	{
		Log.logCommandLine("Postprocessing! (to be implemented)");
	}

	private void jButtonCloseWindowActionPerformed() { dispose(); }
	private void exitForm() {	dispose(); }

	private javax.swing.JPanel jPanelSouth;
	private javax.swing.JButton jButtonShowDAG;
	private javax.swing.JButton jButtonDeleteSubgroups;
	private javax.swing.JButton jButtonPostprocess;
	private javax.swing.JButton jButtonROC;
	private javax.swing.JButton jButtonCloseWindow;
	private javax.swing.JScrollPane itsScrollPane;
}
