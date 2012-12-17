package tools;

import java.net.*;
import java.util.Iterator;
import java.util.List;
import java.io.*;

import main.CSV2SKOS;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

/**
 * Searches for terms in the umthes (Umwelt-Thesaurus of the UBA)
 * 
 * @author Christian Rüh
 * @date 17.08.2012
 * 
 */

public class UmthesFinder {
	
    public static void main(String[] args) throws Exception {
    	String searchTerm = "abfall";
    	String resultURL = getSearchTermURL(searchTerm);
    	String[] labelAndDefinition = getLabelAndDefinition(resultURL);
    	if (labelAndDefinition!=null) System.out.println(labelAndDefinition[0]);
    }

    public static Element getCloseMatchElem(String searchTerm) {
//    	System.out.println("Request with searchTerm " + searchTerm + " coming in.");
    	Element closeMatch;
    	String umthesResult = UmthesFinder.getSearchTermURL(searchTerm.replace(" ", "+").replace("\n", "+").replace("\r", "+"));
		if (umthesResult!=null){
			closeMatch = new Element("closeMatch",CSV2SKOS.nsSKOS);
			closeMatch.setAttribute("resource",umthesResult,CSV2SKOS.nsRDF);
			return closeMatch;
			}
    	else return null;
    }
    
	public static String getSearchTermURL(String searchTerm) {
		if (searchTerm.equals("")) return null;
		else {
			String resultURI = "";
	    	String nsUBA = "http://data.uba.de/";
	    	String ubaURLStart = "http://data.uba.de/umt/search.rdf?c=&for=all&l%5B%5D=de&page=1&q=";
	    	String ubaURLEnd = "&qt=exact&t=labeling-skosxl-base";
	//    	String searchTerm = "strand";
	        URL documentUrl = null;
			try {
				documentUrl = new URL(ubaURLStart+searchTerm+ubaURLEnd);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
			// ---- Read XML file ----
		    SAXBuilder builder = new SAXBuilder();
		    InputStream inputStream = null;
			try {
				inputStream = documentUrl.openStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    Document doc = null;
			try {
				doc = builder.build(inputStream);
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		    List<Element> children = doc.getRootElement().getChildren();
		    //check if there are children
		    if (children.size() > 0)
			{
		    	//iterate through the children
				Iterator<Element> iter = children.iterator();
				while(iter.hasNext())
				{
					Element current = (Element)iter.next();
					Namespace nsSDC = current.getNamespace("sdc");
	//				if (nsSDC!=null) System.out.println(nsSDC.getURI());
					
					Namespace nsRDF = current.getNamespace("rdf");
	//				if (nsRDF!=null) System.out.println(nsRDF.getURI());
				    Element child = current.getChild("link", nsSDC);
				    if (child!=null) {
				    	resultURI = child.getAttributeValue("resource", nsRDF);
				    	//Nach dem 1. Ergebnis aufhören
				    	break;
				    }
	//			    System.out.println(resultURI);
				}
			}
		    //There is a bug in umthes that everything points to http://localhost:8080/ instead of the correct umthes namespace
		    String result = resultURI.replaceAll("http://localhost:8080/", nsUBA);
		    if (!result.equals("")) return result;
		    else return null;
		}
    	
	}

	public static String[] getLabelAndDefinition(String resultURI) {
	    if (resultURI!=null) {
	    	System.out.println(resultURI.concat(".rdf"));
	    
		    URL resultURL = null;
			try {
				resultURL = new URL(resultURI.concat(".rdf"));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    SAXBuilder resultBuilder = new SAXBuilder();
		    InputStream resultInputStream = null;
			try {
				resultInputStream = resultURL.openStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    Document resultDoc = null;
			try {
				SAXBuilder builder = new SAXBuilder();
				resultDoc = builder.build(resultInputStream);
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		    String prefLabel = "";
		    String definition = "";
		    
		    List<Element> resultChildren = resultDoc.getRootElement().getChildren();
		    //check if there are children
		    if (resultChildren.size() > 0)
			{
		    	//iterate through the children
				Iterator<Element> iter = resultChildren.iterator();
				while(iter.hasNext())
				{
					Element current = (Element)iter.next();
					Namespace nsSKOS = current.getNamespace("skos");
	//				if (nsSDC!=null) System.out.println(nsSDC.getURI());
					
					Namespace nsRDF = current.getNamespace("rdf");
	//				if (nsRDF!=null) System.out.println(nsRDF.getURI());
				    Element childPrefLabel = current.getChild("prefLabel", nsSKOS);
				    if (childPrefLabel!=null) prefLabel = childPrefLabel.getValue();
	//			    System.out.println(prefLabel);
				    
				    Element childDefinition = current.getChild("definition", nsSKOS);
				    if (childDefinition!=null) definition = childDefinition.getValue();
	//			    System.out.println(definition);
				}
			}
		    String[] resultArray = new String[2];
		    resultArray[0] = prefLabel;
		    resultArray[1] = definition;
		    return resultArray;
		    }
	    else {
//	    	System.out.println("Flasche leer");
	    	return null;
	    }
	}
}