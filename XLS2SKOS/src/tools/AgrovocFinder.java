package tools;

import java.net.URL;

import main.CSV2SKOS;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.jdom2.Element;

//source: http://www.torsten-horn.de/techdocs/java-soap-axis.htm#WebService-Client-mit-WSDL2Java s. UniversellerClient
//logging stuff, see http://logback.qos.ch/manual/configuration.html

public class AgrovocFinder {
	public static String wsEndpoint = "http://agrovoc.mimos.my/ACSWWebservice/services/ACSWWebService";
	public static String wsNamespace = "http://www.fao.org/webservices/AgrovocWS";
	public static String wsMethod = "getConceptByKeyword";
	
	public static String ontologyName = "Agrovoc";
	//TXT | URI-TXT | SKOS
	public static String format = "URI-TXT";
//	public static String format = "SKOS";
	
	public static void main(String[] args) throws Exception {
//		Log log = LogFactory.getLog("AgrovocFinder");
		
		String searchTerm = "beach";
		//Contains | Exact Match | Starts With | Ends With | Exact Word
		String searchMode = "Starts With";
		//All | de | en | fr ...
		String lang = "en";
		
		String[] params = new String[5];
		params[0] = ontologyName;
		params[1] = searchTerm;
		params[2] = format;
		params[3] = searchMode;
		params[4] = lang;
		
		System.out.println(getElemByKeword(searchTerm, lang, searchMode).getAttributeValue("resource", CSV2SKOS.nsRDF));
		
//    	String resultURL = getSearchTermURL(searchTerm);
//    	String[] labelAndDefinition = getLabelAndDefinition(resultURL);
//    	if (labelAndDefinition!=null) System.out.println(labelAndDefinition[0]);
    }
	
	public static Element getElemByKeword(String searchTerm, String lang, String searchMode) throws Exception {
		String[] params = new String[5];
		params[0] = ontologyName;
		params[1] = searchTerm;
		params[2] = format;
		params[3] = searchMode;
		params[4] = lang;
		
		Service service = new Service();
	    Call call = (Call) service.createCall();
	    call.setTargetEndpointAddress(new URL(wsEndpoint));
	    call.setOperationName( new javax.xml.namespace.QName(wsNamespace, wsMethod) );
	    String ret = (String) call.invoke(params);
	    
	    Element match;
	    
	    if (format.equals("URI-TXT") && ret.indexOf("URI")!=-1) {
//	    	System.out.println(ret.substring(ret.indexOf("URI")+5, ret.indexOf("Label")-1));
	    	match = new Element("closeMatch",CSV2SKOS.nsSKOS);
			match.setAttribute("resource",ret.substring(ret.indexOf("URI")+5, ret.indexOf("Label")-1),CSV2SKOS.nsRDF);
	    	return match;
	    }
	    else return null;
	}
	
	
}
