package tools;

import java.net.*;
import java.nio.charset.Charset;
import java.io.*;

import main.CSV2SKOS;

import org.jdom2.Element;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches for terms in Gemet (http://www.eionet.europa.eu/gemet/)
 * see http://svn.eionet.europa.eu/projects/Zope/wiki/GEMETWebServiceAPI
 * 
 * based on: http://answers.oreilly.com/topic/257-how-to-parse-json-in-java/
 * 
 * @author Christian Rüh
 * @date 23.08.2012
 * 
 */

public class GemetFinder {
	static Logger log;
	
    public static void main(String[] args) throws Exception {
    	log = LoggerFactory.getLogger(GemetFinder.class);
    	
    	String searchTerm = "beach";
    	String lang = "en";
    	String domain = "http://www.eionet.europa.eu/gemet/";
    	String thesaurus_uri = "http://www.eionet.europa.eu/gemet/concept/";
    	String getMethod = "getConceptsMatchingKeyword";
    	int search_mode = 1;
    	String termURI = findTermReturn1stURI(getMethod, searchTerm, lang, domain, thesaurus_uri, search_mode);
	  	System.out.println(termURI);
    }

	private static String getURL(String getMethod, String searchTerm, String lang, String domain, String thesaurus_uri, int search_mode) {
		String searchURL = "";
		if (searchTerm!=null && lang!=null && thesaurus_uri!=null) {
			if (!searchTerm.equals("") && !lang.equals("") && !thesaurus_uri.equals("") && search_mode>=0 && search_mode<=4) {
				//Example: "http://www.eionet.europa.eu/gemet/getConceptsMatchingKeyword?thesaurus_uri=http://www.eionet.europa.eu/gemet/concept/&language=de&search_mode=1&keyword=Strand";
				searchURL = domain
							+getMethod
							+"?thesaurus_uri="+thesaurus_uri
							+"&language="+lang
							+"&search_mode="+search_mode
							+"&keyword="+searchTerm.replace(" ", "+").replace("\n", "+").replace("\r", "+");
//				System.out.println(searchURL);
				return searchURL;
			}
			else return null;
		}
		else return null;
	}

	private static String readAll(BufferedReader buffReader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int i;
        while ((i = buffReader.read()) != -1) {
        	stringBuilder.append((char) i);
        }
        return stringBuilder.toString();
      }

    private static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream inputStream = new URL(url).openStream();
        try {
          BufferedReader buffReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
          String jsonText = readAll(buffReader);
          JSONArray json = new JSONArray(jsonText);
          return json;
        } finally {
        inputStream.close();
        }
      }
    
    public static Element getCloseMatchElem(String getMethod, String searchTerm, String lang, String domain, String thesaurus_uri, int search_mode) throws IOException, JSONException {
    	Element closeMatch;
//    	System.out.println("Searching: "+searchTerm);
    	String gemetResult = findTermReturn1stURI(getMethod, searchTerm, lang, domain, thesaurus_uri, search_mode);
    	
		if (gemetResult!=null){
//			System.out.println("gemetResult: "+gemetResult);
			closeMatch = new Element("closeMatch",CSV2SKOS.nsSKOS);
			closeMatch.setAttribute("resource",gemetResult,CSV2SKOS.nsRDF);
			return closeMatch;
			}
    	else return null;
    }  
    
	public static String findTermReturn1stURI(String getMethod, String searchTerm, String lang, String domain, String thesaurus_uri, int search_mode) throws IOException, JSONException {
		String searchURL = getURL(getMethod, searchTerm, lang, domain, thesaurus_uri, search_mode);
		String resultURI = "";
		JSONArray json;
		
		if (searchURL!=null) {
			json = readJsonFromUrl(searchURL);
			if (json.length()>0) resultURI = (String) json.getJSONObject(0).get("uri");
			else resultURI = null;
		}
		else resultURI = null;
		
	  	return resultURI;
    	
	}
	
	public static String[] findTermReturnURIs(String getMethod, String searchTerm, String lang, String domain, String thesaurus_uri, int search_mode) throws IOException, JSONException {
		String searchURL = getURL(getMethod, searchTerm, lang, domain, thesaurus_uri, search_mode);
		String[] resultURIs = new String[100];
		
		JSONArray json = readJsonFromUrl(searchURL);
			
		if (json.length()>0) for (int i=0;i<json.length();i++) resultURIs[i] = (String) json.getJSONObject(i).get("uri");
		else resultURIs = null;
    	
	  	return resultURIs;
	}
}