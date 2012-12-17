package main;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.jdom2.*;
import org.jdom2.output.*;
import org.json.JSONException;

//Alles für den CSV Import
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JProgressBar;

import com.csvreader.CsvReader;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.turtle.TurtleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.AgrovocFinder;
import tools.GemetFinder;
import tools.UmthesFinder;

/**
 * Converts a CSV/XLS file into a SKOS RDF/XML file
 * <br>
 * primarily for usage in iQvoc (thus the conversion to RDF/N-Triples at the end)
 * <p>
 * Inspiredy by: http://rivuli-development.com/further-reading/sesame-cookbook/parsing-and-writing-rdf-with-rio/
 * <br>
 * to avoid journeys like these: http://topquadrantblog.blogspot.de/2010/12/how-to-convert-spreadsheet-to-skos.html
 * 
 * @version 07.08.2012: new functionality: looking for every term in umthes and if it exists there setting a closematch for that term
 * @version 17.08.2012: complete redesign to connect with the GUI
 * @version 21.08.202: major changes due to the introduction of hierarchy levels
 * 
 * @author Christian Rüh
 * @date 17.08.2012
 * 
 */

public class CSV2SKOS {
	//GEMET settings
	private static String gemet_domain = "http://www.eionet.europa.eu/gemet/";
	private static String gemet_thesaurus_uri = "http://www.eionet.europa.eu/gemet/concept/";
	private static String gemet_getMethod = "getConceptsMatchingKeyword";
	
//	static String thesaurusURI = "nokis_wortliste_wSources2.csv";
//	static String thesaurusURI = "nokis_wortliste5.csv";
//	static String thesaurusURI = "nokis_short.csv";
//	static String thesaurusURI = "kueste.csv";
//	static String thesaurusURI = "lanis_Wirbellose.csv";
	static String thesaurusURI = "KueBi_Schlagworte.csv";
	
	static String rdfFileName = "file.rdf";
	static String rdfFileNameNew = "app_test_file.rdf";
	static String ntFileName = "file.nt";
	static String ttlFileName = "file.ttl";
	static String rdfFileNameCollection = "collection.rdf";
	static String ntFileNameCollection = "collection.nt";
	static String encoding="ISO-8859-1";
	
	static String baseURI = "http://139.30.111.16:3000/";
	static String nsNOKIS = "nokis/";
	static String URINOKIS = baseURI+nsNOKIS;
	static String topTerm = "NOKIS";
	static String subTopTerm = "TestSub";
//	static String topLabel = "NOKIS Wortliste";
	static String topLabel_de = "NOKIS";
	static String topLabel_en = "NOKIS";
	static String collectionName = "NOKIS Collection";
	static String collectionDefinition = "Schlagwörter, die zur Beschreibung von marinen Daten in NOKIS Metadaten verwendet werden.";
	
	public static Namespace nsRDF = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	public static Namespace nsSKOS = Namespace.getNamespace("skos", "http://www.w3.org/2004/02/skos/core#");
	static Namespace nsShema = Namespace.getNamespace("schema", baseURI+"schema#");
	static Namespace nsMrt = Namespace.getNamespace("mrt", baseURI+"MareThesScheme#");
	//TODO übersetzen
	/* Für xml:lang bräuchte man eigentlich den namespace "xml", den mann so definieren könnte:
	Namespace nsp = Namespace.getNamespace("xml","http://www.w3.org/XML/1998/namespace");
	produziert Fehlermeldung "The name "xml" is not legal for JDOM/XML Namespace prefixs: Namespace prefixes cannot begin with "xml" in any combination of case."
	Lösung: "org.jdom2.Namespace.XML_NAMESPACE" als Namespace
	*/
	
	private JProgressBar progressBar;
	
	private static Logger log;
		
	public static void main(String[] args) throws Exception {	
		log = LoggerFactory.getLogger(CSV2SKOS.class);
		
		//create the root and superordinate concept/element etc.
		Document doc = setupDocument(baseURI, topTerm, topLabel_de, topLabel_en);
		
		String[][] dataSmall = {
			    {"begriff_de", "skos:prefLabel", "German", null, "true"},
			    {"begriff_en", "skos:prefLabel", "English", null, "false"},
			};
		
		String[][] data = {
			    {"begriff_de", "skos:prefLabel", "German", null, "true"},
			    {"begriff_en", "skos:prefLabel", "English", null, "false"},
			    {"definition_de", "skos:definition", "German", null, "false"},
			    {"definition_en", "skos:definition", "English", null, "false"},
			};
		
		String[][] dataWSources = {
			    {"begriff_de", "skos:prefLabel", "German", null, "true"},
			    {"begriff_en", "skos:prefLabel", "English", null, "false"},
			    {"definition_de", "skos:definition", "German", null, "false"},
			    {"definition_en", "skos:definition", "English", null, "false"},
			    {"source1", "skos:editorialNote", "German", null, "false"},
			    {"source2", "skos:editorialNote", "English", null, "false"},
			    {"source3", "skos:editorialNote", "German", null, "false"},
			    {"source4", "skos:editorialNote", "English", null, "false"},
			};
		
		String[][] dataLanis = {
			    {"Hierarchieebene 3", "skos:prefLabel", "German", "1", "true"},
			    {"Hierarchieebene 4", "skos:prefLabel", "German", "2", "true"},
			    {"Hierarchieebene 5", "skos:prefLabel", "German", "3", "true"},
			    {"ID Fachschlagwort", "", "", "", "false"},
			    {"ID des Oberbegriffes", "", "", "", "false"},
			    {"Typ (Ordnungsdeskriptor)", "", "", "", "false"},
			    {"Synonym 1", "skos:altLabel", "German", null, "false"},
			    {"Synonym 2", "skos:altLabel", "German", "", "false"},
			    {"Englisches Synonym 1", "skos:altLabel", "English", null, "false"},
			    {"Englisches Synonym 2", "skos:altLabel", "English", null, "false"},
			    {"Englisches Synonym 3", "skos:altLabel", "English", null, "false"},
			    {"Englisches Synonym 4", "skos:altLabel", "English", null, "false"},
			};
		
		String[][] dataKueBiSL = {
			    {"Schlagwort", "skos:prefLabel", "German", null, "true", "false"},
			    {"Kategorie", "skos:prefLabel", "German", null, "false", "true"},
			    {"Nutzer-Hinweis", "skos:definition", "German", null, "false", "false"},
			    {"Vergleich-Pos.", "skos:editorialNote", "German", null, "false", "false"},
			};
		
		JProgressBar progressBarXY = new JProgressBar();
		//Document doc, String namespace, String[][] tableArray, int rows, String thesaurusPath
		Boolean umthes = false;
//		Boolean umthes = true;
//		Boolean gemet = true;
		Boolean gemet = false;
		int gemet_search_mode = 1;
		
//		Boolean agrovoc = true;
		Boolean agrovoc = false;
		String agrovoc_search_mode = "Contains";
		//kueste.csv
//		doc = fillDocument(doc, baseURI, topTerm, dataSmall, 2, thesaurusURI, ';', progressBarXY, umthes, gemet, gemet_search_mode);
		//nokis_wortliste5.csv
//		doc = fillDocument(doc, baseURI, topTerm, data, 4, thesaurusURI, ';', progressBarXY, umthes, gemet, gemet_search_mode, agrovoc, agrovoc_search_mode);
//		doc = fillDocument2(doc, baseURI, topTerm, subTopTerm, data, 4, thesaurusURI, ';', progressBarXY, umthes, gemet, gemet_search_mode, agrovoc, agrovoc_search_mode);
		//nokis_wortliste_wSources2.csv
//		doc = fillDocument(doc, baseURI, dataWSources, 8, thesaurusURI);
		//Lanis_wirbellose.csv
//		doc = fillDocument(doc, baseURI, topTerm, dataLanis, 12, thesaurusURI, ';', progressBarXY, umthes, gemet, gemet_search_mode);
//		
		//KueBiSL
		doc = fillDocument2(doc, baseURI, topTerm, "", dataKueBiSL, 4, thesaurusURI, ';', progressBarXY, umthes, gemet, gemet_search_mode, agrovoc, agrovoc_search_mode, "German");
		
		//addSemantics(Document doc, String namespace, String topTerm, boolean startOnly, JProgressBar progressBar)
//		doc = addSemantics(doc, baseURI, topTerm, true, progressBarXY);
		
		doc = addRelatedTerms(doc, baseURI, topTerm, progressBarXY);
//		
//		Document docCollection = buildCollection(doc);
// 
		//saveRDF(Document doc, String namespace, String rdfFile, String formatRDF) formatRDF = NTriples | Turtle | RDFXML
//		saveRDF(doc, baseURI, ntFileName, "NTriples");
		saveRDF(doc, baseURI, rdfFileNameNew, "RDFXML");
//		saveRDF(docCollection, rdfFileNameCollection);
//			
//		//Convert the RDF file to N-Triples format
//		convertRDF2NT(rdfFileName, ntFileName);
//		convertRDF2NT(rdfFileNameCollection, ntFileNameCollection);

		
		
//        String[] headers = CSV2SKOS.getHeaders("I:\\_dev\\SKOS\\Schlagwortlisten\\nokis_wortliste_wSources4.csv");
//        String[] headers = CSV2SKOS.getHeaders(thesaurusURI,';');
//        if (headers!=null){
//        	for (String header : headers) {
//        		System.out.println(header);
//        	}
//        }
//        else System.out.println("Error while reading the headers");
		
//		String skosCorrespondent = "skos:prefLabel";
//		String skosElement = skosCorrespondent.substring(skosCorrespondent.indexOf(":")+1, skosCorrespondent.length());
//		System.out.println(skosCorrespondent.indexOf(":"));
//		System.out.println(skosCorrespondent.length()-1);
//		System.out.println(skosElement);
	}

	
	/*
	 * Test Area Begin
	 */   
    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        } 
    }
    
    final PropertyChangeListener listener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            if ("progress".equals(evt.getPropertyName())) {
            	progressBar.setValue((Integer)evt.getNewValue());
            }
        }
    };

	/*
	 * Test Area End
	 */   
    
    /**
     * Build a collection instead of a superordinate concept
     * <p>
     * This feature does not seem to work in iQvoc yet
     */
	private static Document buildCollection(Document doc) {
		//define root element rdf:RDF
		Element rdf = new Element("RDF",nsRDF);
		rdf.addNamespaceDeclaration(nsRDF);
		rdf.addNamespaceDeclaration(nsSKOS);
		rdf.addNamespaceDeclaration(nsShema);
		rdf.addNamespaceDeclaration(nsMrt);
		
		//create XML document with root element
		Document docColl = new Document(rdf);
		
		Element collection = new Element("Collection",nsSKOS);
		collection.setAttribute("about",URINOKIS+"#"+collectionName.replace(" ", "_"),nsRDF);
//		collection.setAttribute("about",baseURI+"collections/"+collectionName.replace(" ", "_"),nsRDF);
		
		
		Element prefLabel = new Element("prefLabel",nsSKOS);
		prefLabel.setAttribute("lang","de",org.jdom2.Namespace.XML_NAMESPACE);
		prefLabel.setText(collectionName);
		collection.addContent(prefLabel);
		
		Element definition = new Element("definition",nsSKOS);
		definition.setAttribute("lang","de",org.jdom2.Namespace.XML_NAMESPACE);
		definition.setText(collectionDefinition);
		collection.addContent(definition);
		
		//Get all children
		List<Element> children = doc.getRootElement().getChildren();
		//check if there are children
		if (children.size() > 0)
			{
				//iterate through the children
				Iterator<Element> iter = children.iterator();
				while(iter.hasNext())
				{
				    Element current = (Element)iter.next();
				    
				    String currURI = current.getAttributeValue("about", nsRDF);
				    if (currURI!=null) {
				    	Element member = new Element("member",nsSKOS);
				    	member.setAttribute("resource",currURI,nsRDF);
				    	collection.addContent(member);
				    }
				}
			}
		docColl.getRootElement().addContent(collection);
		return docColl;
	}
	


	/**
	 * Solving the "s." and "s.a." issue
	 * @param doc the document where skos:related elements shall be added
	 * @param namespace the namespace used in the URI of the concepts
	 * @param topTerm the superordinate concept's name which is also used in the URI of each concept
	 * @param progressBar a JProgressBar to show the progress in {@link gui.CSV2SKOSApp}
	 * @return a JDOM {@link Document} with (hopefully) many skos:related elements added
	 */
	public static Document addRelatedTerms(Document doc, String namespace, String topTerm, JProgressBar progressBar) {
		String removeRelated = "";
		Element relatedElem;
		int i = 1;
		//Get all children
		List<Element> children = doc.getRootElement().getChildren();
		int childrenNum = children.size();
		//check if there are children
		if (children.size() > 0)
			{
				//iterate through the children
				Iterator<Element> iter = children.iterator();
				while(iter.hasNext())
				{
				    Element current = (Element)iter.next();
				    
				    //BEGIN: Solving the "s." and "s.a." issue
				    String relatedTerm = "";
				    String firstRelatedTerm = "";
				    String secondRelatedTerm = "";
				    Element child = current.getChild("prefLabel", nsSKOS);
				    if (child!=null && child.getValue().contains("s.")){
				    	if (child!=null && child.getValue().contains("a.")){
				    		//Only the term itself (from 0 to 'a.' 3 chars backwards), i.e. w/o "s." or "s.a." which gives a nicer display
				    		removeRelated = child.getValue().substring(0,child.getValue().indexOf("a.")-3);
				    		//The related term(s) come after the "s.a."
					    	relatedTerm = child.getValue().substring(child.getValue().indexOf("a.")+3,child.getValue().length());
					    	//There may be more than one related term mentioned after "s." or "s.a."
					    	if (child.getValue().indexOf(";")>=0) {
					    		firstRelatedTerm = relatedTerm.substring(0,relatedTerm.indexOf(";"));
					    		secondRelatedTerm = relatedTerm.substring(relatedTerm.indexOf(";")+2, relatedTerm.length());
					    		//Look the related term up in the Document and only add a relation if it's present in the Document
					    		if (findConceptWithURI(doc, namespace+"#"+firstRelatedTerm+"_"+topTerm)!=null) {
					    			relatedElem = new Element("related",nsSKOS);
						    		relatedElem.setAttribute("resource",namespace+"#"+firstRelatedTerm+"_"+topTerm,nsRDF);
						    		current.addContent(relatedElem);
					    		}
					    		if (findConceptWithURI(doc, namespace+"#"+secondRelatedTerm+"_"+topTerm)!=null) {
					    			relatedElem = new Element("related",nsSKOS);
						    		relatedElem.setAttribute("resource",namespace+"#"+secondRelatedTerm+"_"+topTerm,nsRDF);
						    		current.addContent(relatedElem);
					    		}
					    	}
					    	else {
					    		if (findConceptWithURI(doc, namespace+"#"+relatedTerm+"_"+topTerm)!=null) {
					    			relatedElem = new Element("related",nsSKOS);
						    		relatedElem.setAttribute("resource",namespace+"#"+relatedTerm+"_"+topTerm,nsRDF);
						    		current.addContent(relatedElem);
					    		}
					    	}
					    	//Replace the prefLabel by the term w/o "s." or "s.a."
					    	child.setText(removeRelated);
					    }
				    	
				    	else {
				    		//Only the beginning of the term (from 0 to 's.')
							removeRelated =  child.getValue().substring(0, child.getValue().indexOf("s."));
				    		relatedTerm = child.getValue().substring(child.getValue().indexOf("s.")+3,child.getValue().length());
//				    		System.out.println(relatedTerm);
					    	if (child.getValue().indexOf(";")>=0) {
//					    		System.out.println("Found an ';' in "+relatedTerm);
					    		firstRelatedTerm = relatedTerm.substring(0,relatedTerm.indexOf(";"));
//					    		System.out.println(firstRelatedTerm);
					    		secondRelatedTerm = relatedTerm.substring(relatedTerm.indexOf(";")+2, relatedTerm.length());
//					    		System.out.println(secondRelatedTerm);
					    		if (findConceptWithURI(doc, namespace+"#"+firstRelatedTerm+"_"+topTerm)!=null) {
					    			relatedElem = new Element("related",nsSKOS);
						    		relatedElem.setAttribute("resource",namespace+"#"+firstRelatedTerm+"_"+topTerm,nsRDF);
						    		current.addContent(relatedElem);
					    		}
					    		if (findConceptWithURI(doc, namespace+"#"+secondRelatedTerm+"_"+topTerm)!=null) {
					    			relatedElem = new Element("related",nsSKOS);
						    		relatedElem.setAttribute("resource",namespace+"#"+secondRelatedTerm+"_"+topTerm,nsRDF);
						    		current.addContent(relatedElem);
					    		}
					    	}
					    	else {
					    		if (findConceptWithURI(doc, namespace+"#"+relatedTerm+"_"+topTerm)!=null) {
					    			relatedElem = new Element("related",nsSKOS);
						    		relatedElem.setAttribute("resource",namespace+"#"+relatedTerm+"_"+topTerm,nsRDF);
						    		current.addContent(relatedElem);
					    		}
					    	}
					    	child.setText(removeRelated);
				    	}
				    }
				i++;
				int progress = (i * 100) / childrenNum;
				progressBar.setValue(progress);
				}
			}
		return doc;
	}
	
	
	/**
	 * If a concept has (a) broader concept(s) (e.g. beach scarp has beach as broader concept) this is added to the concept as skos:broader.
	 * <br>
	 * Also, the superordinate concept is deleted so that the narrower concept (e.g. beach scrap) is display directly under the broader concept (here: beach) in the tree view
	 * @param doc the Document to be altered
	 * @param namespace the namespace used in the URI of the concepts
	 * @param topTerm the superordinate concept's name which is also used in the URI of each concept
	 * TODO: Do the search with search modes like GEMET (http://svn.eionet.europa.eu/projects/Zope/wiki/GEMETWebServiceAPI)
	 * @param startOnly If true a suffix regex (SQL syntax: term LIKE 'accident%') is used for the search, and if false a prefix/suffix combined regex (SQL syntax: term LIKE '%accident%') is used for the search
	 * @param progressBar a {@link JProgressBar} to show the progress in {@link gui.CSV2SKOSApp}
	 * @return a JDOM {@link Document} with (hopefully) many skos:broader elements added to some concepts
	 */
	public static Document addSemantics(Document doc, String namespace, String topTerm, boolean startOnly, JProgressBar progressBar) {
		int i = 1;
		//Get all children
		List<Element> children = doc.getRootElement().getChildren();
		int childrenNum = children.size();
		//check if there are children
		if (children.size() > 0) {
			//iterate through the children
			Iterator<Element> iter = children.iterator();
			while(iter.hasNext()) {
				//Get the current concept/element
			    Element current = (Element)iter.next();
			    			    
			    //Get the URI of the current concept
			    String conceptURI = "";
			    if (current.getAttribute("about", nsRDF)!=null) conceptURI = current.getAttributeValue("about", nsRDF);
			    
				//Check if there is a broader term for the term handled at the moment
				List<Element> broaderElems = checkForBroaderTerms(doc, conceptURI, current, namespace, topTerm, startOnly);
				
				if (!broaderElems.isEmpty()) {
					//If there is/are broader term/s first delete the existing one (so that it shows directly under the broader term in the tree)
					current.removeChild("broader", nsSKOS);
					//Add the broader term/s
					for (Element broaderElem : broaderElems) current.addContent(broaderElem);
				}
				i++;
				//Update the progressBar
				int progress = (i * 100) / childrenNum;
				progressBar.setValue(progress);
			}
		}
		return doc;
	}
	
	/**
	 * Gets the table headers of a given CSV file with a given delimter
	 * @param thesaurusPath The Path of the CSV file
	 * @param delimiter The delimiter used in the CSV file
	 * @return A String array with all the headers
	 */
	public static String[] getHeaders(String thesaurusPath, char delimiter) {
		//initialize the String array
		String[] headers = new String[100];
		//initialize CSVReader to import the CSV file and set ";" as delimiter
		CsvReader csvReader = null;
		try {
			csvReader = new CsvReader(thesaurusPath, delimiter);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Cannot open CSV file");
			return null;
		}
		try {
			csvReader.readHeaders();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Cannot read headers");
			return null;
		}
		
		try {
			headers = csvReader.getHeaders();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Cannot get headers");
			return null;
		}

		csvReader.close();
		
		return headers;
	}

	/**
	 * Counts the number of lines in any given text-based file.
	 * @param filePath The path to the text file
	 * @return The number of lines in the file as {@link Integer}.
	 * @throws IOException 
	 */
	public static int count(String filePath) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filePath));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        while ((readChars = is.read(c)) != -1) {
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n')
	                    ++count;
	            }
	        }
	        return count;
	    } finally {
	        is.close();
	    }
	}

	/**
	 * Fills the JDOM {@link Document} with the information found in the CSV file.
	 * @param doc the Document to be filled
	 * @param namespace the namespace used in the URI of the concepts
	 * @param topTerm the superordinate concept's name which is also used in the URI of each concept
	 * @param tableArray The logic put into the GUI.<br>Defines which header of the CSV file serves which SKOS purpose.<br>tableArray[x][0] is the header.<br>tableArray[x][1] is the SKOS correspondent for that header/column in the CSV file.<br>tableArray[x][2] is the language the information of this column is written in.<br>tableArray[x][3] gives the hierarchy level for the column.<br>tableArray[x][4] defines if this column is used to build the URI of the concept.
	 * @param rows number of rows in the String[][] tableArray
	 * @param thesaurusPath the path to the CSV file (the word list or thesaurus)
	 * @param progressBar a {@link JProgressBar} to show the progress in {@link gui.CSV2SKOSApp}
	 * @param umthes decides whether to search Umthes and skos:colseMatches or not
	 * @param gemet decides whether to search GEMET and skos:colseMatches or not
	 * @param gemet_search_mode sets the search mode for the GEMET search (exact, suffix regex etc.)
	 * @return a JDOM {@link Document} (hopefully) filled with many concepts
	 * @throws Exception 
	 */
	public static Document fillDocument(Document doc, String namespace, String topTerm, String[][] tableArray, int rows, String thesaurusPath, char delimiter, JProgressBar progressBar, Boolean umthes, Boolean gemet, int gemet_search_mode, Boolean agrovoc, String agrovoc_search_mode) throws Exception {
		//Used for files that contain hierarchies
		String[] hierarchies = new String[100];
		String broaderURI = "";
		Element broaderElem = null;
		Boolean hierarchyTop = true;
		
		//Initialize the to be build concept
		Element concept = null;
		//Initialize a list of elements that make up the content of each concept
		List<Element> newElems = new ArrayList<Element>();
		//Initialize the matches
		Element matchElemUmthes = null;
		Element matchElemGemet = null;
		Element matchElemAgrovoc = null;
		
		//Initialize the number of rows because calling count() might throw an exception
		int numRows = 0;
		try {
			numRows = count(thesaurusPath);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		//initialize CSVReader to import the CSV file and use the given delimiter
		CsvReader csvReader = null;
		try {
			csvReader = new CsvReader(thesaurusPath, delimiter);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Cannot open CSV file");
			return null;
		}
		
		//TODO: find out if this is really needed
		try {
			csvReader.readHeaders();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Cannot read headers");
			return null;
		}
		
		//loop line for line through the CSV file and put all the elements into the document in SKOS format
		//z is not really needed, just for debugging purposes
		int z=1;

		try {
			while (csvReader.readRecord()) {
				//this goes rowwise throught the CSV file
				//initialize the URI String the new concept is going to get
				String URI4NewConcept=null;
				
				//Appearance:
				//header - skos:correspondent - language - hierarchyLevel - elemAsUri?
				//Go row by row through the tableArray
				for (int i=0; i<rows; i++){
					//Get all the import info from the tableArray
					String header = tableArray[i][0];
					String skosCorrespondent = tableArray[i][1];
					String languageLong = tableArray[i][2];
					String language = convertLanguage(languageLong);
					String hierarchyLevel = tableArray[i][3];
					String elemAsUri = tableArray[i][4];
					
					//Get the term in the column "header" in the current row of the CSV file (a single cell in fact)
					//Remove non-breaking spaces (?) and insert nothing for the them instead
					String elemTerm = csvReader.get(header).trim().replaceAll("[\\s\\u00A0]+$", "");
					//Build a new element for the upcoming concept with the outcommings of the tableArray and the cell content
					Element newElem = buildElem(elemTerm, skosCorrespondent, language);
					
					//Make the searches (if wanted by the user) which return new element which will be added to the newElems list which hold every info for the concept
					if (umthes==true) if (skosCorrespondent.equals("skos:prefLabel")) matchElemUmthes = UmthesFinder.getCloseMatchElem(elemTerm);
					if (matchElemUmthes!=null) {
						newElems.add(matchElemUmthes);
						//Set the matchElemGemet to null in case there is another result in another language
						//just newElems.add(matchElemGemet) would add the existing one first and then the new one
						matchElemUmthes = null;
					}
					//GemetFinder.getCloseMatchElem(String getMethod, String searchTerm, String lang, String domain, String thesaurus_uri, int search_mode)
					if (gemet==true) if (skosCorrespondent.equals("skos:prefLabel")) matchElemGemet = GemetFinder.getCloseMatchElem(gemet_getMethod, elemTerm, language, gemet_domain, gemet_thesaurus_uri, gemet_search_mode);
					if (matchElemGemet!=null) {
						newElems.add(matchElemGemet);
						//Set the matchElemGemet to null in case there is another result in another language
						//just newElems.add(matchElemGemet) would add the existing one first and then the new one
						matchElemGemet = null;
					}
					//AgrovocFinder.getURIByKeword(String searchTerm, String lang, String searchMode)
					if (agrovoc==true) if (skosCorrespondent.equals("skos:prefLabel")) matchElemAgrovoc = AgrovocFinder.getElemByKeword(elemTerm, language, agrovoc_search_mode);
					if (matchElemAgrovoc!=null) {
						newElems.add(matchElemAgrovoc);
						//Set the matchElemGemet to null in case there is another result in another language
						//just newElems.add(matchElemGemet) would add the existing one first and then the new one
						matchElemAgrovoc = null;
					}

					//Add the new element to the Element list
					if (newElem!=null) {
						newElems.add(newElem);
						
						//The term for the URI must not be empty
						if (elemAsUri.equals("true") && !elemTerm.equals("")) {
							//Also there is the "s." and "s.a. issue
							String removeRelated = "";
							if (csvReader.get(header).contains("s.")){
						    	if (elemTerm.contains("a.")){
						    		//Only the beginning of the term (from 0 to 'a.' 3 chars backwards)
						    		removeRelated = elemTerm.substring(0,elemTerm.indexOf("a.")-3);
						    		URI4NewConcept = namespace+"#"+removeRelated.trim().replaceAll("[\\s\\u00A0]+$", "").replaceAll(" ", "_")+"_"+topTerm;
						    	}
								else {
									//Only the beginning of the term (from 0 to 's.')
									removeRelated = elemTerm.substring(0,elemTerm.indexOf("s."));
									URI4NewConcept = namespace+"#"+removeRelated.trim().replaceAll("[\\s\\u00A0]+$", "").replaceAll(" ", "_")+"_"+topTerm;
								}
							}
							//If there is no "s." and "s.a. issue
							//The topTerm at the end of the URI is added because iQvoc places every concepts under concepts/ with no regard to the namespace at all
							//resulting in conflicts when there already is a concept with a certain name
							//If there is a space in the term it is replaced by an underscore
						    else URI4NewConcept = namespace+"#"+csvReader.get(header).trim().replaceAll("[\\s\\u00A0]+$", "").replaceAll(" ", "_")+"_"+topTerm;
							
							//Now build the new concept
							concept = new Element("Concept",nsSKOS);
							concept.setAttribute("about",URI4NewConcept,nsRDF);
							if (hierarchyLevel!=null) {
								//If there are hierarchies involved set the hierarchies String array at position "hierarchyLevel" to "URI4NewConcept"
								if (!hierarchyLevel.equals("")) hierarchies[Integer.parseInt(hierarchyLevel)] = URI4NewConcept;
							}
						}
						
						if (hierarchyLevel!=null) {
							if (!hierarchyLevel.equals("")) {
								//If there is a level over the current hierarchyLevel
								if (hierarchies[Integer.parseInt(hierarchyLevel)-1]!=null) {
									//one level up is the broader term to be used which is stored in hierarchies at position "hierarchyLevel" minus 1
									broaderURI = hierarchies[Integer.parseInt(hierarchyLevel)-1];
											
									broaderElem = new Element("broader",nsSKOS);
									broaderElem.setAttribute("resource",broaderURI,nsRDF);
									//add the broader term (the term one position above in the hierarchy) to the newElems list
									newElems.add(broaderElem);
									//If it is not the topmost element in the hierarchy later on the topTerm (superordinate concept) should not be added to the concept as skos:broader
									hierarchyTop = false;
								}
							}
						}
					}
				}
				
				//If there is a pre-existing concept for the term right now handled any new information (found in the newElems list) will be added to it
				Element oldElem = findConceptWithURI(doc, URI4NewConcept);				
				if (oldElem!=null) {
					if (newElems.size() > 0) {
						//iterate through the children
						Iterator<Element> iter = newElems.iterator();
						while(iter.hasNext())
						{
						    Element current = (Element)iter.next();
						    //Go through the pre-existing element and look if the information is really new or if its already included
						    boolean isNew = false;
						    List<Element> oldChildren = oldElem.getChildren();
						    Iterator<Element> iter4Old = oldChildren.iterator();
						    while(iter4Old.hasNext()) {
						    	Element currentOld = (Element)iter4Old.next();
						    	//First check: If the value of the element is the same it is not new
						    	//But even if the value of the element is not the same it might be that a prefLabel was checked against a definition, thus we have to confirm it is the same SKOS element in the same language
						    	//Second check: Is the name (skos:prefLabel, skos:definition etc.) of the current element the same as the old element?
						    	//Third check: Is the attribute (most probably the language setting) of the current element the same as the old element?
						    	if (!currentOld.getValue().equals(current.getValue()) && currentOld.getName().equals(current.getName()) && currentOld.getAttributes().toString().equals(current.getAttributes().toString())){
						    		isNew = true;
						    		//Only one prefLabel in each language is allowed, so every additional label is set to altLabel
						    		if (current.getName().equals("prefLabel")) {
						    			current.setName("altLabel");
						    		}
						    	}
						    }
						    //TODO this looks unnecessarily complicated
						    if (isNew) oldElem.addContent(current);
						    //The Iterator would keep all the elements it had so far resulting in the problem that it tries to add elements which were already included
						    iter.remove();
						}
					}
			    }
				//If there is a pre-existing concept
				else {
					//... iterate through the new elements and add them to the concept
					if (newElems.size() > 0) {
						//iterate through the children
						Iterator<Element> iter = newElems.iterator();
						while(iter.hasNext()) {
						    Element current = (Element)iter.next();
						    concept.addContent(current);
						    //The Iterator would keep all the elements it had so far resulting in the problem that it tries to add elements which were already included
						    iter.remove();
						}
					if (hierarchyTop) {
						//Set the superordinate concept/element only if hierarchyTop is true (if there is no hierarchy this is true by default)
						Element topElem = new Element("broader",nsSKOS);
						topElem.setAttribute("resource",namespace+"#"+topTerm,nsRDF);
						concept.addContent(topElem);
						}
					//add the new concept to the Document
					doc.getRootElement().addContent(concept);
					}
			}
			z++;
			//Calculate the progress for the progressBar...
			int progress = (z * 100) /numRows - 1;
			//... and set its value
			progressBar.setValue(progress);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Cannot read records of the CSV file");
			return null;
		}
		//close the CSVReader inputStream
		csvReader.close();
		
		return doc;
	}

	public static Document fillDocument2(Document doc, String namespace, String topTerm, String subTopTerm, String[][] tableArray, int rows, String thesaurusPath, char delimiter, JProgressBar progressBar, Boolean umthes, Boolean gemet, int gemet_search_mode, Boolean agrovoc, String agrovoc_search_mode, String mainlang) throws Exception {
		String mainLangShort = convertLanguage(mainlang);
		Element subTopConcept = null;
		//subTopTerm issue
		if (subTopTerm!=null) if (!subTopTerm.equals("")) {
			subTopConcept = buildConcept(topTerm, subTopTerm, namespace, mainLangShort);
			doc.getRootElement().addContent(subTopConcept);
		}
		
		//Used for files that contain hierarchies
		String[] hierarchies = new String[100];
		String broaderURI = "";
		Element broaderElem = null;
		Boolean hierarchyTop = true;
		Boolean newCat = true;
		String categoryOld = "";
		Element catagoryElem = null;
		String categoryString = "";
		String categoryChkboxString = "";
		Boolean categoryBool = false;
		String catagoryURI = "";
		
		//Initialize the to be build concept
		Element concept = null;
		//Initialize a list of elements that make up the content of each concept
		List<Element> newElems = new ArrayList<Element>();
		//Initialize the matches
		Element matchElemUmthes = null;
		Element matchElemGemet = null;
		Element matchElemAgrovoc = null;
		
		//Initialize the number of rows because calling count() might throw an exception
		int numRows = 0;
		try {
			numRows = count(thesaurusPath);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		//initialize CSVReader to import the CSV file and use the given delimiter
		CsvReader csvReader = null;
		try {
			csvReader = new CsvReader(thesaurusPath, delimiter);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Cannot open CSV file");
			return null;
		}
		
		//TODO: find out if this is really needed
		try {
			csvReader.readHeaders();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Cannot read headers");
			return null;
		}
		
		//loop line for line through the CSV file and put all the elements into the document in SKOS format
		//z is not really needed, just for debugging purposes
		int z=1;

		try {
			while (csvReader.readRecord()) {
				//this goes rowwise throught the CSV file
				//initialize the URI String the new concept is going to get
				String URI4NewConcept=null;
				
				//Appearance:
				//header - skos:correspondent - language - hierarchyLevel - elemAsUri?
				//Go row by row through the tableArray
				for (int i=0; i<rows; i++){
					//Get all the import info from the tableArray
					String header = tableArray[i][0];
					String skosCorrespondent = tableArray[i][1];
					String languageLong = tableArray[i][2];
					String language = convertLanguage(languageLong);
					String hierarchyLevel = tableArray[i][3];
					String elemAsUri = tableArray[i][4];
					categoryChkboxString = tableArray[i][5];
					if (categoryChkboxString.equals("true")) {
						categoryString = csvReader.get(header).trim().replaceAll("[\\s\\u00A0]+$", "").replaceAll(" ", "_");
						categoryBool = true;
					}
//					else categoryString = "";
					
					//Check if category has changed
					if (!categoryOld.equals(categoryString) && !categoryString.equals("")) newCat=true;
					else newCat=false;
					categoryOld = categoryString;
					
					//Create a new concept for the category so that it can be used as broader for the following terms
					if (categoryChkboxString.equals("true") && newCat==true) {
						catagoryElem = buildConcept(topTerm, categoryString, namespace, mainLangShort);
						doc.getRootElement().addContent(catagoryElem);
						catagoryURI = namespace+"#"+categoryString+"_"+topTerm;
					}
					
					//Get the term in the column "header" in the current row of the CSV file (a single cell in fact)
					//Remove non-breaking spaces (?) and insert nothing for the them instead
					String elemTerm = csvReader.get(header).trim().replaceAll("[\\s\\u00A0]+$", "");
					//Build a new element for the upcoming concept with the outcommings of the tableArray and the cell content
					Element newElem = buildElem(elemTerm, skosCorrespondent, language);
					
					//Make the searches (if wanted by the user) which return new element which will be added to the newElems list which hold every info for the concept
					if (umthes==true) if (skosCorrespondent.equals("skos:prefLabel")) matchElemUmthes = UmthesFinder.getCloseMatchElem(elemTerm);
					if (matchElemUmthes!=null) {
						newElems.add(matchElemUmthes);
						//Set the matchElemGemet to null in case there is another result in another language
						//just newElems.add(matchElemGemet) would add the existing one first and then the new one
						matchElemUmthes = null;
					}
					//GemetFinder.getCloseMatchElem(String getMethod, String searchTerm, String lang, String domain, String thesaurus_uri, int search_mode)
					if (gemet==true) if (skosCorrespondent.equals("skos:prefLabel")) matchElemGemet = GemetFinder.getCloseMatchElem(gemet_getMethod, elemTerm, language, gemet_domain, gemet_thesaurus_uri, gemet_search_mode);
					if (matchElemGemet!=null) {
						newElems.add(matchElemGemet);
						//Set the matchElemGemet to null in case there is another result in another language
						//just newElems.add(matchElemGemet) would add the existing one first and then the new one
						matchElemGemet = null;
					}
					//AgrovocFinder.getURIByKeword(String searchTerm, String lang, String searchMode)
					if (agrovoc==true) if (skosCorrespondent.equals("skos:prefLabel")) matchElemAgrovoc = AgrovocFinder.getElemByKeword(elemTerm, language, agrovoc_search_mode);
					if (matchElemAgrovoc!=null) {
						newElems.add(matchElemAgrovoc);
						//Set the matchElemGemet to null in case there is another result in another language
						//just newElems.add(matchElemGemet) would add the existing one first and then the new one
						matchElemAgrovoc = null;
					}

					//Add the new element to the Element list
					if (newElem!=null) {
						newElems.add(newElem);
						
						//The term for the URI must not be empty
						if (elemAsUri.equals("true") && !elemTerm.equals("")) {
							//Also there is the "s." and "s.a. issue
							String removeRelated = "";
							if (csvReader.get(header).contains("s.")){
						    	if (elemTerm.contains("a.")){
						    		//Only the beginning of the term (from 0 to 'a.' 3 chars backwards)
						    		removeRelated = elemTerm.substring(0,elemTerm.indexOf("a.")-3);
						    		URI4NewConcept = namespace+"#"+removeRelated.trim().replaceAll("[\\s\\u00A0]+$", "").replaceAll(" ", "_")+"_"+topTerm;
						    	}
								else {
									//Only the beginning of the term (from 0 to 's.')
									removeRelated = elemTerm.substring(0,elemTerm.indexOf("s."));
									URI4NewConcept = namespace+"#"+removeRelated.trim().replaceAll("[\\s\\u00A0]+$", "").replaceAll(" ", "_")+"_"+topTerm;
								}
							}
							//If there is no "s." and "s.a. issue
							//The topTerm at the end of the URI is added because iQvoc places every concepts under concepts/ with no regard to the namespace at all
							//resulting in conflicts when there already is a concept with a certain name
							//If there is a space in the term it is replaced by an underscore
						    else URI4NewConcept = namespace+"#"+csvReader.get(header).trim().replaceAll("[\\s\\u00A0]+$", "").replaceAll(" ", "_")+"_"+topTerm;
							
							//Now build the new concept
							concept = new Element("Concept",nsSKOS);
							concept.setAttribute("about",URI4NewConcept,nsRDF);
							if (hierarchyLevel!=null) {
								//If there are hierarchies involved set the hierarchies String array at position "hierarchyLevel" to "URI4NewConcept"
								if (!hierarchyLevel.equals("")) hierarchies[Integer.parseInt(hierarchyLevel)] = URI4NewConcept;
							}
						}
						
						if (hierarchyLevel!=null) {
							if (!hierarchyLevel.equals("")) {
								//If there is a level over the current hierarchyLevel
								if (hierarchies[Integer.parseInt(hierarchyLevel)-1]!=null) {
									//one level up is the broader term to be used which is stored in hierarchies at position "hierarchyLevel" minus 1
									broaderURI = hierarchies[Integer.parseInt(hierarchyLevel)-1];
											
									broaderElem = new Element("broader",nsSKOS);
									broaderElem.setAttribute("resource",broaderURI,nsRDF);
									//add the broader term (the term one position above in the hierarchy) to the newElems list
									newElems.add(broaderElem);
									//If it is not the topmost element in the hierarchy later on the topTerm (superordinate concept) should not be added to the concept as skos:broader
									hierarchyTop = false;
								}
							}
						}
					}
				}
				
				//If there is a pre-existing concept for the term right now handled any new information (found in the newElems list) will be added to it
				Element oldElem = findConceptWithURI(doc, URI4NewConcept);				
				if (oldElem!=null) {
					if (newElems.size() > 0) {
						//iterate through the children
						Iterator<Element> iter = newElems.iterator();
						while(iter.hasNext())
						{
						    Element current = (Element)iter.next();
						    //Go through the pre-existing element and look if the information is really new or if its already included
						    boolean isNew = false;
						    List<Element> oldChildren = oldElem.getChildren();
						    Iterator<Element> iter4Old = oldChildren.iterator();
						    while(iter4Old.hasNext()) {
						    	Element currentOld = (Element)iter4Old.next();
						    	//First check: If the value of the element is the same it is not new
						    	//But even if the value of the element is not the same it might be that a prefLabel was checked against a definition, thus we have to confirm it is the same SKOS element in the same language
						    	//Second check: Is the name (skos:prefLabel, skos:definition etc.) of the current element the same as the old element?
						    	//Third check: Is the attribute (most probably the language setting) of the current element the same as the old element?
						    	if (!currentOld.getValue().equals(current.getValue()) && currentOld.getName().equals(current.getName()) && currentOld.getAttributes().toString().equals(current.getAttributes().toString())){
						    		isNew = true;
						    		//Only one prefLabel in each language is allowed, so every additional label is set to altLabel
						    		if (current.getName().equals("prefLabel")) {
						    			current.setName("altLabel");
						    		}
						    	}
						    }
						    //TODO this looks unnecessarily complicated
						    if (isNew) oldElem.addContent(current);
						    //The Iterator would keep all the elements it had so far resulting in the problem that it tries to add elements which were already included
						    iter.remove();
						}
					}
			    }
				//If there is a pre-existing concept
				else {
					//... iterate through the new elements and add them to the concept
					if (newElems.size() > 0) {
						//iterate through the children
						Iterator<Element> iter = newElems.iterator();
						while(iter.hasNext()) {
						    Element current = (Element)iter.next();
						    concept.addContent(current);
						    //The Iterator would keep all the elements it had so far resulting in the problem that it tries to add elements which were already included
						    iter.remove();
						}
					if (hierarchyTop) {
						//Set the superordinate concept/element only if hierarchyTop is true (if there is no hierarchy this is true by default)
						Element topElem = new Element("broader",nsSKOS);
						//If there is a subtopterm use this
						if (subTopTerm!=null) if (!subTopTerm.equals("")) topElem.setAttribute("resource",namespace+"#"+subTopTerm+"_"+topTerm,nsRDF);
						else if (categoryBool==true) topElem.setAttribute("resource",catagoryURI,nsRDF);
						//Otherwise set it to the topTerm
						else topElem.setAttribute("resource",namespace+"#"+topTerm,nsRDF);
						concept.addContent(topElem);
						}
					//add the new concept to the Document
					doc.getRootElement().addContent(concept);
					}
			}
			z++;
			//Calculate the progress for the progressBar...
			int progress = (z * 100) /numRows - 1;
			//... and set its value
			progressBar.setValue(progress);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Cannot read records of the CSV file");
			return null;
		}
		//close the CSVReader inputStream
		csvReader.close();
		
		return doc;
	}
	
	private static Element buildConcept(String topTerm, String elemTerm, String baseURI, String mainlang) {
		//create the superordinate concept/element
		Element descr = new Element("Description",nsRDF);
		descr.setAttribute("about",baseURI+"#"+elemTerm+"_"+topTerm,nsRDF);
		
		//set type of the superordinate concept/element to skos:Concept
		Element type = new Element("type",nsRDF);
		type.setAttribute("resource","http://www.w3.org/2004/02/skos/core#Concept",nsRDF);
		descr.addContent(type);
		
		Element newElem = new Element("prefLabel",nsSKOS);
		newElem.setAttribute("lang",mainlang,org.jdom2.Namespace.XML_NAMESPACE);
		newElem.setText(elemTerm);
		descr.addContent(newElem);
		
		Element broader = new Element("broader",nsSKOS);
		broader.setAttribute("resource",baseURI+"#"+topTerm,nsRDF);
		descr.addContent(broader);
		
		return descr;
	}
	
	/**
	 * Builds a new element for the upcoming concept with the outcommings of the tableArray and the cell content
	 * @param elemTerm term in the column "header" in the current row of the CSV file (a single cell in fact)
	 * @param skosCorrespondent the SKOS element to use (e.g. prefLabel, altLabel, definition etc.)
	 * @param language the language of the term
	 * @return an {@link Element} ready to be added by the concept
	 */
	private static Element buildElem(String elemTerm, String skosCorrespondent, String language) {
		//Check if there is content at all
		if (!elemTerm.equals("") && !skosCorrespondent.equals("")){
			//Remove the "skos:" in front of the SKOS element (eg "skos:prefLabel" becomes just "prefLabel"
			String skosElement = skosCorrespondent.substring(skosCorrespondent.indexOf(":")+1, skosCorrespondent.length());
			//TODO: check that only valid SKOS elements are used here
			Element newElem = new Element(skosElement,nsSKOS);
			newElem.setAttribute("lang",language,org.jdom2.Namespace.XML_NAMESPACE);
			newElem.setText(elemTerm);
			
			return newElem;
			}
		else return null;
	}
	
	/**
	 * A language converter, which converts a given full language term (like "German") to ISO 639-1 abbreviations
	 * @param languageLong the full language term (like "German")
	 * @return the ISO 639-1 abbreviation for the given language
	 */
	private static String convertLanguage(String languageLong) {
		String language = "";
		//Check if there is content at all
		if (!languageLong.equals("")){
			//TODO: more languages!
			if (languageLong.equals("German")) language = "de";
			else if (languageLong.equals("English")) language = "en";
			else if (languageLong.equals("French")) language = "fr";
			else return null;

			return language;
			}
		else return null;
	}

	/**
	 * Saves a JDOM {@link Document} as {@link RDF} in the given {@link RDFFormat}
	 * @param doc the JDOM {@link Document} to be saved as {@link RDF}
	 * @param namespace the namespace to resolve any relative URI references (if everything worked out as it should this could be "" or anything, too)
	 * @param rdfFilePath the path where the RDF file will be saved
	 * @param formatRDF The {@link RDFFormat} the file will be saved in
	 * @throws FileNotFoundException
	 */
	public static void saveRDF(Document doc, String namespace, String rdfFilePath, String formatRDF) throws FileNotFoundException {
		//initialize a RDFWriter
		RDFWriter rdfWriter = null;
		//initialize a XMLOutputter
		XMLOutputter xmlOutput = new XMLOutputter();
 		//display it nice
		Format format = Format.getPrettyFormat();
		//because of German language set encoding to ISO-8859-1
		//TODO: what about other languages? Why not UTF-8?
		format.setEncoding(encoding);
		xmlOutput.setFormat(format);
		//Output the JDOM Document as a String
		String xmlStr = xmlOutput.outputString(doc);

		//initialize a StringReader with the given String (Document) because that is what the RDFParser is able to parse
		StringReader strReader = new StringReader(xmlStr);
	
		//Set the right RDFWriter for the format specified
		if (formatRDF.equals("NTriples")) rdfWriter = new NTriplesWriter(new FileOutputStream (rdfFilePath));
		else if  (formatRDF.equals("Turtle")) rdfWriter = new TurtleWriter(new FileOutputStream (rdfFilePath));
		else if  (formatRDF.equals("RDFXML")) rdfWriter = new RDFXMLWriter(new FileOutputStream (rdfFilePath));
		
		//initialize a RDFParser
		RDFParser rdfParser = new RDFXMLParser();
		//and set the RDFWriter as its RDFHandler
		rdfParser.setRDFHandler(rdfWriter);
		try {
			//parse the String (Document) into the right format
			rdfParser.parse(strReader, namespace);
		} catch (RDFParseException | RDFHandlerException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println("RDF File "+ rdfFilePath + " saved");
	  }

	/**
	 * Sets up a new JDOM {@link Document} and fill it with the necessary namespaces and the superordinate concept
	 * @param baseURI the namespace used in the URI of the concepts
	 * @param topTerm the superordinate concept's name which is also used in the URI of each concept
	 * @param topLabel_de the superordinate concept's German label
	 * @param topLabel_en the superordinate concept's English label
	 * @return a JDOM {@link Document} set up with the basic RDF stuff
	 */
	public static Document setupDocument(String baseURI, String topTerm, String topLabel_de, String topLabel_en) {
		//define root element rdf:RDF
		Element rdf = new Element("RDF",nsRDF);
		//add the RDF and SKOS namespaces to it
		rdf.addNamespaceDeclaration(nsRDF);
		rdf.addNamespaceDeclaration(nsSKOS);
		//TODO: Hand these namespaces over from the GUI
		rdf.addNamespaceDeclaration(nsShema);
		rdf.addNamespaceDeclaration(nsMrt);
		
		//create XML document with root element
		Document doc = new Document(rdf);
						 
		//create the superordinate concept/element
		Element descr = new Element("Description",nsRDF);
		descr.setAttribute("about",baseURI+"#"+topTerm,nsRDF);
		
		//TODO trying to define the superordinate concept/element as top term (for the database), but does not work
		Element top = new Element("topConceptOf",nsSKOS);
		top.setAttribute("resource",baseURI+"schema",nsRDF);
		
		//set type of the superordinate concept/element to skos:Concept
		Element type = new Element("type",nsRDF);
		type.setAttribute("resource","http://www.w3.org/2004/02/skos/core#Concept",nsRDF);
		
		//set German prefLabel of the superordinate concept/element
		if (!topLabel_de.equals("")) {
			Element prefLabelDE = new Element("prefLabel",nsSKOS);
			prefLabelDE.setAttribute("lang","de",org.jdom2.Namespace.XML_NAMESPACE);
			prefLabelDE.setText(topLabel_de);
			descr.addContent(prefLabelDE);
		}
		
		//set English prefLabel of the superordinate concept/element
		if (!topLabel_en.equals("")) {
			Element prefLabelEN = new Element("prefLabel",nsSKOS);
			prefLabelEN.setAttribute("lang","en",org.jdom2.Namespace.XML_NAMESPACE);
			prefLabelEN.setText(topLabel_en);
			descr.addContent(prefLabelEN);
		}
		
		//Add all the subelements to the Description element
		descr.addContent(top);
		descr.addContent(type);
		
		//add the superordinate concept/element to the document
		doc.getRootElement().addContent(descr);
		
		return doc;
	}

	
	private static Element addSources(String[] sources, Element oldElem){
		List<Element> sourceElements = new ArrayList<Element>();
		for (int i=0;i<sources.length;i++) {
			if (!sources[i].equals("")) {
				Element source = new Element("editorialNote",nsSKOS);
				source.setAttribute("lang","de",org.jdom2.Namespace.XML_NAMESPACE);
				source.setText(sources[i]);
				oldElem.addContent(source);
			}
		}
		if (sourceElements.isEmpty()) return oldElem;
		else return oldElem;
	}
	
	
	
	private static Document addTopTerm(Document doc) {
		//Get all children
		List<Element> children = doc.getRootElement().getChildren();
		//check if there are children
		if (children.size() > 0)
			{
				//iterate through the children
				Iterator<Element> iter = children.iterator();
				while(iter.hasNext())
				{
				    Element current = (Element)iter.next();
					//Set the superordinate concept/element
					Element broader = new Element("broader",nsSKOS);
					//TODO: Broader term fester definieren
					broader.setAttribute("resource",URINOKIS+"#"+topTerm,nsRDF);
					current.addContent(broader);
				}
			}
		return doc;
	}

	/*
	 * This method could be used (if finished) to find all relations between concepts looking into every definition etc.
	 * Running this would be time consuming
	 * Importing all possible realtions to iQvoc would be even more time consuming
	 */
	private static Element checkForRelations(Document doc, String[] termElems, String uRI4NewConcept) {
		Element related = null;
		int compare = 0;
		
		//Get all children
		List<Element> children = doc.getRootElement().getChildren();
		//check if there are children
		if (children.size() > 0)
			{
				//iterate through the children
				Iterator<Element> iter = children.iterator();
				while(iter.hasNext())
				{
				    Element current = (Element)iter.next();
				    //Return the XPath 1.0 string value of this element, which is the complete, ordered content of all text node descendants of this element (i.e. the text that's left after all references are resolved and all other markup is stripped out.)
//				    String currTerm = current.getValue();
				    String currTerm = "";
				    Element child = current.getChild("prefLabel", nsSKOS);
				    if (child!=null) currTerm = child.getValue();
				    System.out.println(currTerm);
				    //Check if any of the gives terms in the String array "termElems" matches something in the XPath 1.0 string value of the current element
				    for ( String termElem : termElems ){
				    	compare = termElem.toLowerCase().indexOf(currTerm.toLowerCase());
				    	//If there is a match return a skos:related element...
				    	if (compare!=-1) {
//				    		System.out.println("Erfolg!");
				    		//... but only if such an element is not pre existing
				    		//check if child has attributes and that it has no "skos:related" element and makes sure that it does not create a relation to itself
						    if (current.hasAttributes()==true){
//						    	System.out.println(current.getName());
						    	if (!current.getAttributeValue("about", nsRDF).equals(uRI4NewConcept)){
						    		related = new Element("broader",nsSKOS);
									related.setAttribute("resource",current.getAttributeValue("about", nsRDF),nsRDF);
									return related;
						    	}
						    }
				    	}
				    	//If there is no match return a "null" element
						else {
//							System.out.println("KEIN Erfolg!");
						}
				    }
				}
			}
		return related;
	}
	
	/**
	 * 
	 * TODO: Do the search with search modes like GEMET (http://svn.eionet.europa.eu/projects/Zope/wiki/GEMETWebServiceAPI)
	 * @param doc
	 * @param uRI4NewConcept
	 * @param oldElem
	 * @param namespace
	 * @param topTerm
	 * @param startOnly
	 * @return
	 */
	private static List<Element> checkForBroaderTerms(Document doc, String uRI4NewConcept, Element oldElem, String namespace, String topTerm, boolean startOnly) {
//		Element broader = null;
//		int compare = -1;
//		int compare2 = -1;
		List<Element> broaderElems = new ArrayList<Element>();
		
		boolean compareAny = false;
		boolean compare = false;
		boolean compare2 = false;
		int i=0;
		String termToCheckAgainst = "";
		Element checkElem = oldElem.getChild("prefLabel", nsSKOS);
		if (checkElem!=null) termToCheckAgainst = checkElem.getValue();
	    String termToCheckAgainst_oL = termToCheckAgainst.replace(" ", "_");
	    String termToCheckAgainstURI = namespace+"#"+termToCheckAgainst_oL+"_"+topTerm;
//	    System.out.println("Begriff ist " + termToCheckAgainst);
		
	    if (!termToCheckAgainst.equals(topTerm)) {
	    	//Get all children
			List<Element> children = doc.getRootElement().getChildren();
			//check if there are children
			if (children.size() > 0)
				{
					//iterate through the children
					Iterator<Element> iter = children.iterator();
					while(iter.hasNext())
					{
						i++;
					    Element currElem = (Element)iter.next();
					    //Return the XPath 1.0 string value of this element, which is the complete, ordered content of all text node descendants of this element (i.e. the text that's left after all references are resolved and all other markup is stripped out.)
	//				    String currTerm = current.getValue();
					    
					    //Set the term to check against to the prefLabel of the concept
					    String currTerm = "";
	//				    
					    Element child = currElem.getChild("prefLabel", nsSKOS);
					    if (child!=null) currTerm = child.getValue();
					    String currTerm_oL = currTerm.replace(" ", "_");
					    String currTermURI = namespace+"#"+currTerm_oL+"_"+topTerm;
					    
					    //Checks if the currTerm appears in any of the given terms
				    	String termURI = namespace+"#"+currTerm+"_"+topTerm;
				    	//Check if current term starts with the termToCheckAgainst
				    	boolean compareStart = termToCheckAgainst.toLowerCase().matches(currTerm.toLowerCase()+ ".*");
				    	//Check if current term has the termToCheckAgainst someplace in it - this should be used with caution!
				    	if (!startOnly)	compareAny = termToCheckAgainst.toLowerCase().matches(".*" + currTerm.toLowerCase()+ ".*");
	//			    	System.out.println("Vergleiche " + termToCheckAgainst + " mit " + currTerm + " Ergebnis ist: " + compareStart + " und " + compareAny);
				    	if (compareStart) {
//					    		System.out.println("Erfolg bei Start "+termToCheckAgainst+" mit "+currTerm);
	//				    		System.out.println("Erfolg bei Start "+termToCheckAgainstURI+" mit "+termURI);
				    		if ((!currElem.getAttributeValue("about", nsRDF).equals(namespace+"#"+topTerm)) && (!currTermURI.equals(termToCheckAgainstURI))){
					    		Element broader = new Element("broader",nsSKOS);
					    		broader.setAttribute("resource",namespace+"#"+currTerm_oL+"_"+topTerm,nsRDF);
					    		broaderElems.add(broader);
//								return broader;
						    }
				    	}
				    	else if (compareAny) {
//					    		System.out.println("Erfolg bei Any "+termToCheckAgainst+" mit "+currTerm);
				    		if ((!currElem.getAttributeValue("about", nsRDF).equals(namespace+"#"+topTerm)) && (!termURI.equals(termToCheckAgainstURI))){
				    			Element broader = new Element("broader",nsSKOS);
					    		broader.setAttribute("resource",namespace+"#"+currTerm_oL+"_"+topTerm,nsRDF);
					    		broaderElems.add(broader);
//								return broader;
						    }
				    	}
	//				    	if (compare || compare2) {
	//				    		System.out.println("Erfolg!");
				    		//... but only if such an element is not pre existing
				    		//check if child has attributes and that it has no "skos:broader" element and makes sure that it does not create a broader relation to itself
	//						    if (current.hasAttributes()==true){
	//						    	String preExtBroader ="";
						    	//if there is a pre-existing concept to which new info shall be added it has to be checked wether this old concept already has the broader term we are just going to add
						    	//First off we are extracting the URI of the broader term if such exists
	//						    	if ((oldElem!=null) && (oldElem.getChild("broader", nsSKOS)!=null  && !preExtBroader.equals(current.getAttributeValue("about", nsRDF))))	preExtBroader = oldElem.getChild("broader", nsSKOS).getAttributeValue("resource", nsRDF);
						    	//Secondly we check if the broader term URI we are going to add is the same as old broader term URI
	//					    	if ((!current.getAttributeValue("about", nsRDF).equals(URINOKIS+"#"+topTerm)) && (!termURI.equals(currTermURI))){
	//					    		broader = new Element("narrower",nsSKOS);
	//					    		broader.setAttribute("resource",URINOKIS+"#"+currTerm_oL,nsRDF);
	//								return broader;
	//						    }
	//				    	}
					}
				}
		    }
			
		return broaderElems;
	}
	
	private static Element findConceptWithURI(Document doc, String searchURI) {
		//Get all children
		List<Element> children = doc.getRootElement().getChildren();
		//check if there are children
		if (children.size() > 0)
			{
				//iterate through the children
				Iterator<Element> iter = children.iterator();
				while(iter.hasNext())
				{
				    Element current = (Element)iter.next();
				    //check if child has attributes and that the attribute "about" is not null
				    if ((current.hasAttributes()==true && current.getAttributeValue("about", nsRDF)!=null)){
	//			    	System.out.println(current.getAttributeValue("about", nsRDF));
				    	//check if the "about" attribute matches the search URI which means that there is already a concept for that term
					    if (current.getAttributeValue("about", nsRDF).equals(searchURI)){
	//				    	System.out.println("Treffer! "+searchURI);
					    	return current;
					    }
			    }
			  }
			}
		return null;
	}

	public static void convertRDF2NT(String rdfFile, String ntFile, String namespace) throws IOException {
//		RDFWriter rdfWriter;
		try {
			//Import the RDF xml file
			InputStream inputStream = new FileInputStream(rdfFile);
			
			//convert it to n-triples format for iQvoc
			RDFParser rdfParser = new RDFXMLParser();
			RDFWriter rdfWriter = new NTriplesWriter(new FileOutputStream (ntFile));
			rdfParser.setRDFHandler(rdfWriter);
			try {
				rdfParser.parse(inputStream, namespace);

			} catch (RDFParseException | RDFHandlerException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("converted RDF file " + rdfFile + " and saved it to NT File " + ntFile);
			inputStream.close();
			
//			//Just for testing another conversion (this time to Turtle format)
//			inputStream  = new FileInputStream (rdfFileName);
//			RDFWriter rdfWriterTTL = new TurtleWriter(new FileOutputStream (ttlFileName));
//			rdfParser.setRDFHandler(rdfWriterTTL);
//			try {
//				rdfParser.parse(inputStream, URINOKIS);
//			} catch (RDFParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (RDFHandlerException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			System.out.println("TTL File saved!");
//			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
