package nl.liacs.subdisc;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLErrorHandler implements ErrorHandler
{
	/** 
	 * For now only use one errorHandler, may change.
	 */
	public static final XMLErrorHandler THE_ONLY_INSTANCE = new XMLErrorHandler();

	@Override
	public void error(SAXParseException e) throws SAXException
	{
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException
	{
	}

	@Override
	public void warning(SAXParseException e) throws SAXException
	{
	}

}
