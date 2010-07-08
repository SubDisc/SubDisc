/**
 * @author marvin
 * DTDResolver to determine which DTD to use.
 * TODO check if systemId is valid (using XMLType), else return null
 * TODO does not reach catch if systemId is null
 */

package nl.liacs.subdisc;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class DTDResolver implements EntityResolver
{
	public InputSource resolveEntity(String publicId, String systemId)
	{
		return new InputSource(this.getClass().getResourceAsStream(systemId.substring(systemId.lastIndexOf("/")).toLowerCase()));
	}
}