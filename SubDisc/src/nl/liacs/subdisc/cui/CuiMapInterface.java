package nl.liacs.subdisc.cui;

import java.util.*;

public interface CuiMapInterface
{
	// TODO for now
	final static String CUI_DIR = "CUI/";
	final static String EXPRESSION_CUIS = "CUI/expression_cuis.txt";
	final static String ENTREZ2CUI = "CUI/entrez2cui.txt";
	final static String GO2CUI = "CUI/go2cui.txt";
//	final static String ENSEMBL2CUI = "CUI/ensembl2cui.txt";

	final static int NR_DOMAINS = 28;
	final static int NR_ENTREZ_CUI = 64870;
	final static int NR_GO_CUI = 15879;
//	final static int NR_ENSEMBL_CUI= 0;
	
	public Map<String, ? extends Object> getMap();
}
