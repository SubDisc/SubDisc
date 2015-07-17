package nl.liacs.subdisc;

import java.sql.*;
import java.util.*;

import javax.swing.JOptionPane;

public class DatabaseConnection
{
	private static final int DEFAULT_DB = 1;
	private static final int MYSQL = 1;

	private Connection itsConnection = null;
	private DatabaseMetaData itsMetaData;
	private String itsDriver = "";
	private String itsURL = "";
	private String itsHost = "localhost";
	private String itsDatabase = "";
	private String itsUser = "";
	private String itsPassword = "";

	public DatabaseConnection(String theDriver, String theURL, String theHost, String theDatabase, String theUser, String thePassword)
	{
		itsDriver = theDriver;
		itsURL = theURL;
		itsHost = theHost;
		itsDatabase = theDatabase;
		itsUser = theUser;
		itsPassword = thePassword;
		System.out.println("Connecting using user '" + itsUser + "'");
	}

	public void openConnection() throws Exception
	{
		Class.forName(itsDriver);
		if (itsURL.equals(""))
		{
			itsConnection = DriverManager.getConnection("jdbc:mysql://" + itsHost + "/" + itsDatabase,
				itsUser, itsPassword);
		}
		else
		{
			itsConnection = DriverManager.getConnection(itsURL, itsUser, itsPassword);
		}
		itsMetaData = itsConnection.getMetaData();
		System.out.println(itsMetaData.getDatabaseProductName() + " " + itsDatabase + "  connection open");
	}

	public void closeConnection()
	{
		try {
			System.out.println(itsMetaData.getDatabaseProductName() + " " + itsDatabase + "  connection lost");
			if (!itsConnection.isClosed())
				itsConnection.close();
			itsConnection = null;
		} catch (java.sql.SQLException e)
		{
			JOptionPane.showMessageDialog(null, "Could not close connection!", "alert", JOptionPane.ERROR_MESSAGE);
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(400, 400);
			aWindow.setVisible(true);
		}
    	}

	public ResultSet executeStatement(String theQuery, boolean isSimple, boolean printSQL)
	{
		ResultSet aResult = null;
		Statement aStatement;

		//createStatement
		try
		{
			if (isSimple)
				aStatement = itsConnection.createStatement();
			else
				aStatement = itsConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		}
		catch (java.sql.SQLException e1)
		{
			System.out.println("Connection timed out during createStatement. Re-establishing.");
			try
			{
				openConnection();
				if (isSimple)
					aStatement = itsConnection.createStatement();
				else
					aStatement = itsConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			}
			catch (Exception e2)
			{
				System.out.println("Reconnect failed.");
				return null;
			}
		}

		//executeQuery
		try
		{
			if (printSQL)
				System.out.println(theQuery);
			aResult = aStatement.executeQuery(theQuery);
			return aResult;
		}
		catch (java.sql.SQLException e3)
		{
			if (e3.getMessage().contains("onnection"))
				System.out.println("Connection timed out. Re-establishing.");
			try
			{
				openConnection();
				aStatement = itsConnection.createStatement();
				aResult = aStatement.executeQuery(theQuery);
				return aResult;
			}
			catch (Exception e4)
			{
				if (e3.getMessage().contains("onnection"))
					System.out.println("Reconnect failed.");
				JOptionPane.showMessageDialog(null, "Could not execute statement!", "alert", JOptionPane.ERROR_MESSAGE);
				ErrorWindow aWindow = new ErrorWindow(e4);
				aWindow.setLocation(400, 400);
				aWindow.setVisible(true);
				return null;
			}
		}
	}

	public boolean executeUpdate(String theQuery)
	{
		try
		{
			Statement aStatement = itsConnection.createStatement();
			System.out.println(theQuery);
			aStatement.executeUpdate(theQuery);
			return true;
		} catch (java.sql.SQLException e)
		{
			JOptionPane.showMessageDialog(null, "Could not execute statement!", "alert", JOptionPane.ERROR_MESSAGE);
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(400, 400);
			aWindow.setVisible(true);
			return false;
		}
	}

	public boolean execute(String theQuery, boolean isPrintQuery)
	{
		try {
			Statement aStatement = itsConnection.createStatement();
			if (isPrintQuery)
				System.out.println(theQuery);
			aStatement.execute(theQuery);
			return true;
		} catch (java.sql.SQLException e)
		{
			JOptionPane.showMessageDialog(null, "Could not execute statement!", "alert", JOptionPane.ERROR_MESSAGE);
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(400, 400);
			aWindow.setVisible(true);
			return false;
		}
	}

	public String getCreateTableQuery(String theTableName, String theSelectClause, String theRemainderClause)
	{
		if (getDatabaseProduct() == MYSQL)
		{
			String aQuery = "CREATE TABLE " + theTableName + " AS SELECT " + theSelectClause + " " + theRemainderClause + ";";
			return aQuery;
		}

		//DEFAULT
		String aQuery = "SELECT " + theSelectClause + " INTO " + theTableName + " " + theRemainderClause + ";";
		return aQuery;
	}

	public int getDatabaseProduct()
	{
		try
		{
			String aDBType = itsMetaData.getDatabaseProductName();
			System.out.println("DB = \'" + aDBType + "\'");

			if (aDBType.equalsIgnoreCase("mysql"))
				return MYSQL;

			//else
			return DEFAULT_DB;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Could not get database product information!", "alert",
				JOptionPane.ERROR_MESSAGE);
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(400, 400);
			aWindow.setVisible(true);
			return DEFAULT_DB;
		}
	}

	public boolean supportsCreateIndex()
	{
		return true;
	}

	public DatabaseMetaData getMetaData()
	{
		try
		{
			return itsConnection.getMetaData();
		}
		catch (java.sql.SQLException e)
		{
			JOptionPane.showMessageDialog(null, "Could not get database metadata!", "alert",
				JOptionPane.ERROR_MESSAGE);
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(400, 400);
			aWindow.setVisible(true);
			return null;
		}
	}

	public ArrayList<String> getTableViewNames() throws SQLException
	{
		ArrayList<String> aTables = new ArrayList<String>();
		ResultSet aResultSet = itsConnection.getMetaData().getTables(null, null, null, new String[]{"TABLE", "VIEW"});

		while (aResultSet.next())
		{
			aTables.add(aResultSet.getString(3));
			//System.out.println("Table: " + aResultSet.getString(3));
		}
		return aTables;
	}

	public boolean checkTableName(String theTableName)
	{
		try
		{
			ArrayList<String> existingTables = new ArrayList<String>();
			ResultSet aResultSet = itsMetaData.getTables(null, null, null, new String[]{"TABLE", "VIEW"});

			while (aResultSet.next())
				existingTables.add(aResultSet.getString(3));

			if (!theTableName.equals(""))
			{
				if (existingTables.contains(theTableName))
					return false;
				else
					return true;
			}
			return true;
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(null, "Table name already exists!", "alert",
				JOptionPane.ERROR_MESSAGE);
			ErrorWindow aWindow = new ErrorWindow(ex);
			aWindow.setLocation(400, 400);
			aWindow.setVisible(true);
			return false;
		}
	}

	public void setDriver(String theDriver) { itsDriver = theDriver; }
	public void setURL(String theURL) { itsURL = theURL; }
	public void setHost(String theHost) { itsHost = theHost; }
	public void setDatabase(String theDatabase) { itsDatabase = theDatabase; }
	public void setUser(String theUser) { itsUser = theUser; }
	public void setPassword(String thePassword) { itsPassword = thePassword; }
}
