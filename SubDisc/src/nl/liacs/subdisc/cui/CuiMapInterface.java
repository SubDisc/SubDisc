package nl.liacs.subdisc.cui;

import java.util.*;

public interface CuiMapInterface
{
	// TODO for now
	final static String CUI_DIR = "/home/marvin/SubDisc/CUI/";
	final static String ENTREZ2CUI_PATH = "/home/marvin/SubDisc/CUI/entrez2cui.txt";
	final static String GO2CUI_PATH = "/home/marvin/SubDisc/CUI/go2cui.txt";
//	final static String ENSEMBL2CUI_PATH = "/home/marvin/SubDisc/CUI/ensembl2cui.txt";

	final static int NR_ENTREZ_CUI = 64870;
	final static int NR_GO_CUI = 15879;
//	final static int NR_ENSEMBL_CUI= 0;
	
	public Map<String, ? extends Object> getMap();
}
