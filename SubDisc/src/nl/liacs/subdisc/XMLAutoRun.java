package nl.liacs.subdisc;

import nl.liacs.subdisc.XMLDocument.XMLType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XMLAutoRun
{
	private Document itsDocument;

	public XMLAutoRun(SearchParameters theSearchParameters, Table theTable)
	{
		if(theSearchParameters == null || theTable == null)
			return;

		itsDocument = XMLDocument.buildDocument(XMLType.AUTORUN);
		itsDocument.getLastChild().appendChild(buildExperimentElement(theSearchParameters, theTable));
	}

	private Node buildExperimentElement(SearchParameters theSearchParameters, Table theTable)
	{
		Node anExperimentNode = itsDocument.createElement("experiment");
//		Node anExperimentElement = ((Document)theAutoRunNode).createElement("experiment");

		for(XMLNode x : XMLNode.values())
			x.createNode(anExperimentNode, theSearchParameters, theTable);


		return anExperimentNode;
	}
/*
	public String[] getAllSearchParameters()
	{
		String[] aSearchParameterArray = new String[28];
		TargetConcept aTargetConcept = getTargetConcept();

		aSearchParameterArray[0] = "TargetConcept: ";
		aSearchParameterArray[1] = String.valueOf(aTargetConcept.getNrTargetAttributes());
		aSearchParameterArray[2] = aTargetConcept.getTargetType().name();

		Attribute aPrimaryTarget = aTargetConcept.getPrimaryTarget();
		aSearchParameterArray[3] = String.valueOf(aPrimaryTarget.getIndex());
		aSearchParameterArray[4] = aPrimaryTarget.getName();
		aSearchParameterArray[5] = aPrimaryTarget.getShort();
		aSearchParameterArray[6] = aPrimaryTarget.getType().name();

		aSearchParameterArray[7] = aTargetConcept.getTargetValue();

		Attribute aSecondaryTarget = aTargetConcept.getSecondaryTarget();	// TODO
//		aSearchParameterArray[8] = String.valueOf(aSecondaryTarget.getIndex());
//		aSearchParameterArray[9] = aSecondaryTarget.getName();
//		aSearchParameterArray[10] = aSecondaryTarget.getShort();
//		aSearchParameterArray[11] = aSecondaryTarget.getType().name();

//		aTargetConcept.getSecondaryTargets(); 
		
		aSearchParameterArray[13] = getQualityMeasureString();
		aSearchParameterArray[14] = String.valueOf(getQualityMeasureMinimum());

		aSearchParameterArray[15] = String.valueOf(getSearchDepth());
		aSearchParameterArray[16] = String.valueOf(getMinimumCoverage());
		aSearchParameterArray[17] = String.valueOf(getMaximumCoverage());
		aSearchParameterArray[18] = String.valueOf(getMaximumSubgroups());
		aSearchParameterArray[19] = String.valueOf(getMaximumTime());

		aSearchParameterArray[20] = getSearchStrategyName(getSearchStrategy());
		aSearchParameterArray[21] = String.valueOf(getSearchStrategyWidth());
		aSearchParameterArray[22] = getNumericStrategy().name();

		aSearchParameterArray[23] = String.valueOf(getNrSplitPoints());
		aSearchParameterArray[24] = String.valueOf(getAlpha());
		aSearchParameterArray[25] = String.valueOf(getBeta());
		aSearchParameterArray[26] = String.valueOf(getPostProcessingCount());
		aSearchParameterArray[27] = String.valueOf(getMaximumPostProcessingSubgroups());

		for(String s : aSearchParameterArray)
			System.out.println(s);
		return aSearchParameterArray;
	}
*/
	/*
	 * 		String[] aSearchParameterArray = new String[28];
		TargetConcept aTargetConcept = getTargetConcept();

		aSearchParameterArray[0] = "TargetConcept: ";
		aSearchParameterArray[1] = String.valueOf(aTargetConcept.getNrTargetAttributes());
		aSearchParameterArray[2] = aTargetConcept.getTargetType().name();

		Attribute aPrimaryTarget = aTargetConcept.getPrimaryTarget();
		aSearchParameterArray[3] = String.valueOf(aPrimaryTarget.getIndex());
		aSearchParameterArray[4] = aPrimaryTarget.getName();
		aSearchParameterArray[5] = aPrimaryTarget.getShort();
		aSearchParameterArray[6] = aPrimaryTarget.getType().name();
	 */
	private enum XMLNode
	{
		TARGET_CONCEPT
		{
			@Override
			public String getValueFromData(theSearchParameters, theTable)
			{
				return "";
			}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		NR_TARGET_ATTRIBUTES
		{
			@Override
			public String getValueFromData()
			{
				return String.valueOf(itsSearchParameters. TargetConcept.getNrTargetAttributes());
			}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		TARGET_TYPE_NAME
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		PRIMARY_TARGET
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		PRIMARY_TARGET_INDEX
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		PRIMARY_TARGET_NAME
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		PRIMARY_TARGET_SHORT
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		PRIMARY_TARGET_TYPE
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		TARGET_VALUE
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SECONDARY_TARGET
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SECONDARY_TARGET_INDEX
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SECONDARY_TARGET_NAME
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SECONDARY_TARGET_SHORT
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SECONDARY_TARGET_TYPE
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SECONDARY_TARGETS
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		QUALITY_MEASURE
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		QUALITY_MEASURE_MINIMUM
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SEARCH_DEPTH
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		MINIMUM_SEARCH_DEPTH
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		MAXIMUM_SEARCH_DEPTH
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		MAXIMUM_NR_SUBGROUPS
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		MAXIMUM_TIME
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SEACRH_STRATEGY
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		SEARCH_STRATEGY_WIDTH
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		NUMERIC_STRATEGY
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		NR_SPLITPOINTS
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		ALPHA
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		BETA
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		POST_PROCESSING_COUNT
		{
			@Override
			public String getValueFromData() {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		},
		MAXIMUM_POST_PROCESSING_COUNT
		{
			@Override
			public String getValueFromData(SearchParameters theSearchParameter, String theValue) {}

			@Override
			public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
			{
			}
		};

		abstract String getValueFromData(SearchParameters theSearchParameters, Table theTable);
		abstract void setValueFromFile(SearchParameters theSearchParameter, String theValue);

		public void createNode(Node theExperimentNode, SearchParameters theSearchParameters, Table theTable)
		{
			theExperimentNode.appendChild(theExperimentNode.getOwnerDocument().createElement(toString())).setTextContent(getValueFromData(theSearchParameters, theTable));
		}
	}	
}
