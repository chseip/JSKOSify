package tests;

import java.io.FileWriter;
import java.io.IOException;
import org.jdom2.*;
import org.jdom2.input.*;
import org.jdom2.output.*;
 
public class WriteXMLFileWithJDOM {
	public static void main(String[] args) {
 
	  try {
 		
		Namespace nsRDF = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		Namespace nsSKOS = Namespace.getNamespace("skos", "http://www.w3.org/2004/02/skos/core#");
		/* Für xml:lang bräuchte man eigentlich den namespace "xml", den mann so definieren könnte:
		Namespace nsp = Namespace.getNamespace("xml","http://www.w3.org/XML/1998/namespace");
		produziert Fehlermeldung "The name "xml" is not legal for JDOM/XML Namespace prefixs: Namespace prefixes cannot begin with "xml" in any combination of case."
		Lösung: "org.jdom2.Namespace.XML_NAMESPACE" als namespace
		*/
		
		Element rdf = new Element("RDF",nsRDF);
		rdf.addNamespaceDeclaration(nsRDF);
		rdf.addNamespaceDeclaration(nsSKOS);
		
		Document doc = new Document(rdf);
 
		//Das übergeordnete NOKIS Wortlisten Element
		Element descrNOKIS = new Element("Description",nsRDF);
		descrNOKIS.setAttribute("about","http://nokis.de#NOKIS",nsRDF);
		
		Element typeNOKIS = new Element("type",nsRDF);
		typeNOKIS.setAttribute("resource","http://www.w3.org/2004/02/skos/core#Concept",nsRDF);
		
		Element prefLabelDENOKIS = new Element("prefLabel",nsSKOS);
		prefLabelDENOKIS.setAttribute("lang","de",org.jdom2.Namespace.XML_NAMESPACE);
		prefLabelDENOKIS.setText("NOKIS Wortliste");
		
		Element prefLabelENNOKIS = new Element("prefLabel",nsSKOS);
		prefLabelENNOKIS.setAttribute("lang","en",org.jdom2.Namespace.XML_NAMESPACE);
		prefLabelENNOKIS.setText("NOKIS Wortliste");
		
		descrNOKIS.addContent(typeNOKIS);
		descrNOKIS.addContent(prefLabelDENOKIS);
		descrNOKIS.addContent(prefLabelENNOKIS);
		
		doc.getRootElement().addContent(descrNOKIS);
		
		//Schleife, die die Elemente der Liste dem RDF hinzufügen
		Element descr = new Element("Description",nsRDF);
		descr.setAttribute("about","http://nokis.de#Abbruchkante2",nsRDF);
		
		Element type = new Element("type",nsRDF);
		type.setAttribute("resource","http://www.w3.org/2004/02/skos/core#Concept",nsRDF);
		
		Element prefLabelDE = new Element("prefLabel",nsSKOS);
		prefLabelDE.setAttribute("lang","de",org.jdom2.Namespace.XML_NAMESPACE);
		prefLabelDE.setText("Abbruchkante2");
		
		Element prefLabelEN = new Element("prefLabel",nsSKOS);
		prefLabelEN.setAttribute("lang","en",org.jdom2.Namespace.XML_NAMESPACE);
		prefLabelEN.setText("Abbruchkante2");
		
		Element definitionDE = new Element("definition",nsSKOS);
		definitionDE.setAttribute("lang","de",org.jdom2.Namespace.XML_NAMESPACE);
		definitionDE.setText("keine");
		
		Element definitionEN = new Element("definition",nsSKOS);
		definitionEN.setAttribute("lang","en",org.jdom2.Namespace.XML_NAMESPACE);
		definitionEN.setText("A more or less continuous line of cliffs or steep slopes facing in one general direction which are caused by erosion or faulting. Also SCARP.");

		Element broader = new Element("broader",nsSKOS);
		broader.setAttribute("resource","http://nokis.de#NOKIS",nsRDF);

		descr.addContent(type);
		descr.addContent(prefLabelDE);
		descr.addContent(prefLabelEN);
		descr.addContent(definitionDE);
		descr.addContent(definitionEN);
		descr.addContent(broader);
		
		doc.getRootElement().addContent(descr);
 
		//Dateispeicherung
		// new XMLOutputter().output(doc, System.out);
		XMLOutputter xmlOutput = new XMLOutputter();
 
		// display nice nice
		xmlOutput.setFormat(Format.getPrettyFormat());
//		xmlOutput.output(doc, new FileWriter("c:\\file.xml"));
		xmlOutput.output(doc, new FileWriter("file.rdf"));
 
		System.out.println("File Saved!");
	  } catch (IOException io) {
		System.out.println(io.getMessage());
	  }
	}
}