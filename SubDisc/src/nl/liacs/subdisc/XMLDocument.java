/**
 * @author marvin
 * XML class for all XML file creation and parsing in SubDisc.
 * This class is uninstantiable and returns proper Documents.
 */
package nl.liacs.subdisc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class XMLDocument
{
	private XMLDocument() {}	// uninstantiable

	public static enum XMLType
	{
		AUTORUN, MRML, PATTERNSET;

		private final String qualifiedName = toString().toLowerCase();
		private final String publicId = toString().toLowerCase();
		private final String systemId = toString().toLowerCase() + ".dtd";
		private final String nameSpaceURI = null;
	}

	public static Document buildDocument(XMLType theXMLType)
	{
		Document aDocument = null;

		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(XMLErrorHandler.THE_ONLY_INSTANCE);
			DOMImplementation aDOMImplementation = builder.getDOMImplementation();
			aDocument =
				aDOMImplementation.createDocument(theXMLType.nameSpaceURI,
													theXMLType.qualifiedName,
													aDOMImplementation.createDocumentType(theXMLType.qualifiedName,
																							theXMLType.publicId,
																							theXMLType.systemId));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		//	new ErrorDialog(e, ErrorDialog.parsingError);
		}

		return aDocument;
	}

	// TODO fix mrml errors because of re-including (use getID?)
	// then refer to buildDoument code to avoid duplication
	public static Document parseXMLFile(File theXMLFile) 
	{
		Document aDocument = null;

		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(true);
			factory.setIgnoringElementContentWhitespace(true);
			factory.setIgnoringComments(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(XMLErrorHandler.THE_ONLY_INSTANCE);
			builder.setEntityResolver(new DTDResolver());
			aDocument = builder.parse(theXMLFile);
		}
		catch (Exception e)
		{
//			new ErrorDialog(e, ErrorDialog.parsingError);
		}

		return aDocument;
	}

	public static void saveDocument(Document theXMLDoc, File theOutputFile)
	{
		FileOutputStream aFileOutputStream = null;

		try
		{
			aFileOutputStream = new FileOutputStream(theOutputFile);
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty("doctype-system", theXMLDoc.getDoctype().getSystemId());
			t.setOutputProperty("indent", "yes");
			t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
			t.transform(new DOMSource(theXMLDoc), new StreamResult(aFileOutputStream));
		}
		catch (Exception e)
		{
//			new ErrorDialog(e, ErrorDialog.writeError);
		}
		finally
		{
			try
			{
				if (aFileOutputStream != null)
				{
					aFileOutputStream.flush();
					aFileOutputStream.close();
				}
			}
			catch (IOException e)
			{
//				new ErrorDialog(e, ErrorDialog.fileOutputStreamError);
			}
		}
	}

}
