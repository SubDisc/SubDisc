package nl.liacs.subdisc;

public enum XMLNodeSearchParameter
{
	QUALITY_MEASURE
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return theSearchParameters.getQualityMeasureString();
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	QUALITY_MEASURE_MINIMUM
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getQualityMeasureMinimum());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	SEARCH_DEPTH
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getSearchDepth());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	MINIMUM_SEARCH_DEPTH
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getMinimumCoverage());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	MAXIMUM_SEARCH_DEPTH
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getMaximumCoverage());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	MAXIMUM_NR_SUBGROUPS
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getMaximumSubgroups());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	MAXIMUM_TIME
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getMaximumTime());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	SEARCH_STRATEGY
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return SearchParameters.getSearchStrategyName(theSearchParameters.getSearchStrategy());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	SEARCH_STRATEGY_WIDTH
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getSearchStrategyWidth());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	NUMERIC_STRATEGY
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return theSearchParameters.getNumericStrategy().toString();
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	NR_SPLITPOINTS
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getNrSplitPoints());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	ALPHA
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getAlpha());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	BETA
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getBeta());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	POST_PROCESSING_COUNT
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getPostProcessingCount());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	},
	MAXIMUM_POST_PROCESSING_COUNT
	{
		@Override
		public String getValueFromData(SearchParameters theSearchParameters)
		{
			return String.valueOf(theSearchParameters.getMaximumPostProcessingSubgroups());
		}

		@Override
		public void setValueFromFile(SearchParameters theSearchParameter, String theValue)
		{
		}
	};

	abstract String getValueFromData(SearchParameters theSearchParameters);
	abstract void setValueFromFile(SearchParameters theSearchParameter, String theValue);
}
