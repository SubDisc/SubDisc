package nl.liacs.subdisc.gui;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.net.URISyntaxException;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;

public class SubDisc
{
	/*
	 * External jars required for correct execution.
	 * KNIME related jars are not required, when used as a KNIME plugin,
	 * KNIME loads is own jfreechart, jcommon and KNIME related jars.
	 */
	/*
	 * jfreechart, jcommon licence:
	 * Like JFreeChart, JCommon is also licensed under the terms of the GNU
	 * Lesser General Public Licence.
	 */
	/* 
	 * Jama licence:
	 * Copyright Notice This software is a cooperative product of
	 * The MathWorks and the National Institute of Standards and Technology
	 * (NIST) which has been released to the public domain.
	 * Neither The MathWorks nor NIST assumes any responsibility whatsoever
	 * for its use by other parties, and makes no guarantees, expressed or
	 * implied, about its quality, reliability, or any other characteristic.
	 */
	private static final String[] JARS = {
		// for drawing
		"jfreechart-1.0.17.jar",
		"jcommon-1.0.21.jar",
		// for propensity score, Rob
		"weka.jar",
		// for Cook's distance only
		"Jama-1.0.3.jar",
		// for KNIME
//		"knime-core.jar",
//		"org.eclipse.osgi_3.6.1.R36x_v20100806.jar",
//		"knime-base.jar",
//		"org.eclipse.core.runtime_3.6.0.v20100505.jar",
//		"org.knime.core.util_4.1.1.0034734.jar",
	};

	private static String itsJarRevision;

	/*
	 * There is a difference between the command line options:
	 *   -Djava.awt.headless=true; and
	 *   showWindows.
	 *
	 * The first is a java Toolkit option, and suppresses all GUI elements.
	 *
	 * The second is a SubDisc option, and works only in a GraphicsEnvironment.
	 * When an experiment is run from XML, the default is to write the result to
	 * a file, and shutdown the program.
	 * When showWindows is set to a 'true' value, the result is written to file,
	 * but the application does not shutdown.
	 * Instead, a ResultWindow is shown.
	 * This allows for an immediate inspection of the result, and easy access to
	 * additional functionality, like Browse, Pattern Team, and ROC.
	 */
	public static void main(String[] args)
	{
		// Skipping libs check. Using Maven for dependency management.
		// checkLibs();

		try 
		{
			String aJarPath = SubDisc.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			//System.out.println("JAR Path : " + aJarPath);
			String aJarName = aJarPath.substring(aJarPath.lastIndexOf("/") + 1);
			int aLastDot = aJarName.lastIndexOf(".");
			int aRevision = aLastDot-1;
			while (Character.isDigit(aJarName.charAt(aRevision)) && aRevision > 0)
				aRevision--;
			itsJarRevision = aJarName.substring(aRevision + 1, aLastDot);
			
			System.out.println("JAR revision number: " + itsJarRevision);
		}
		catch (URISyntaxException e) 
		{
			e.printStackTrace();
		}

		if (!GraphicsEnvironment.isHeadless() && (SplashScreen.getSplashScreen() != null))
		{
			// else assume it is an XML-autorun experiment and close immediately
			if (args.length == 0)
			{
				try { Thread.sleep(3000); }
				catch (InterruptedException e) {};
			}

			SplashScreen.getSplashScreen().close();
		}

		if (XMLAutoRun.autoRunSetting(args))
			return;

		// apparently starting with a file loader is problematic on OSX
		// TODO check: the EDT code has changed, this might not be true anymore
		if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
		{
			new MiningWindow();
		}
		else
		{
			FileHandler aLoader = new FileHandler(Action.OPEN_FILE);
			Table aTable = aLoader.getTable();
			SearchParameters aSearchParameters = aLoader.getSearchParameters();

			if (aTable == null)
				new MiningWindow();
			else if (aSearchParameters == null)
				new MiningWindow(aTable);
			else
				new MiningWindow(aTable, aSearchParameters); // XML
		}
	}

	// may move to a separate class
	private static void checkLibs() {
/*
		// spaces in paths may give problems, leave code in just in case
		String path = SubDisc.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath;
		try {
			decodedPath = URLDecoder.decode(path, "UTF-8");
			path = new File(path).getParentFile().getPath();
		} catch (UnsupportedEncodingException e) {}
*/
		File dir = new File(new File("").getAbsolutePath());
		File libs = null;
		for (File f : dir.listFiles()) {
			if ("libs".equals(f.getName())) {
				libs = f;
				break;
			}
		}
		System.out.println("Starting SubDisc from:");
		System.out.println("\t" + dir.getAbsolutePath());
		System.out.println("Looking for /libs/ directory...");
		System.out.format("/libs/ directory %sfound:%n", libs == null ? "not " : "");
		System.out.println("\t" + (libs == null ? "" : libs.getAbsolutePath()));

		if (libs != null) {
			System.out.format("Looking for required jars (%d)...%n",
						JARS.length);
			checkJars(libs);
		} else {
			System.out.println("Most drawing functionality will not work.");
		}
	}

	private static void checkJars(File libsDir) {
		final String[] files = libsDir.list();
		List<String> notFound = new ArrayList<String>();
		OUTER: for (String jar : JARS) {
				for (String file : files) {
					if (jar.equals(file)) {
						System.out.format("\tFound: '%s'.%n", jar);
						continue OUTER;
					}
				}
				notFound.add(jar);
			}

		if (!notFound.isEmpty())
			tryHarder(notFound, files);
	}

	/*
	 * If another version is found it might work.
	 * Newer version may have removed deprecated methods, older version may
	 * not have implemented some of the required methods.
	 * 
	 * TODO subdisc.mf's Class-Path attribute defines the required jars,
	 * other libraries will not be loaded automatically, could be done here.
	 */
	private static void tryHarder(List<String> jars, String[] files) {
		OUTER: for (String jar : jars) {
				String base = jar.substring(0, jar.indexOf("-"));
				for (String file : files) {
					if (file.startsWith(base)) {
						System.out.format("\tFound: '%s', ('%s' expected).%n",
									file,
									jar);
						continue OUTER;
					}
				}
				System.out.format("\t'%s' not found, some functions will not work.%n", jar);
			}
	}

	String getRevisionNumber() { return itsJarRevision; }
}
