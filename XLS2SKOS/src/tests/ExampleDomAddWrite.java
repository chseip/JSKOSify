package tests;

import java.io.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ExampleDomAddWrite
{
  public static void main( String[] argv )
  {
    if( argv.length != 6 )
    {
      System.err.println( "Usage:" );
      System.err.println( "java ExampleDomAddWrite <XmlFile> <NewFile>"
                            + " <ParentElem> <Child> <FindText> <New>" );
      System.err.println( "Example:" );
      System.err.println( "java ExampleDomAddWrite MyXmlFile.xml NewXmlFile.xml"
                            + " Button Title \"Mein dritter Button\""
                            +                " \"Mein neuer Button\"" );
      System.exit( 1 );
    }

    try {
      // ---- Parse XML file ----
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // factory.setNamespaceAware( true );
      DocumentBuilder builder  = factory.newDocumentBuilder();
      Document        document = builder.parse( new File( argv[0] ) );
      Node            rootNode = document.getDocumentElement();

      // ---- Get list of nodes to given tag ----
      NodeList ndList = document.getElementsByTagName( argv[2] );
      // System.out.println( "\nNode list at the beginning:" );
      // printNodesFromList( ndList );

      // ---- Loop through the list of main nodes ----
      for( int i=0; i<ndList.getLength(); i++ )
      {
        Node     nodeMain     = ndList.item( i );
        Node     nodeChild    = null;
        NodeList ndListChilds = nodeMain.getChildNodes();
        boolean  bChildFound  = false;
        if( null == ndListChilds )  continue;
        // Loop through the list of child nodes
        for( int j=0; j<ndListChilds.getLength(); j++ )
        {
          nodeChild = ndListChilds.item( j );
          if( null == nodeChild )  continue;
          String sNodeName = nodeChild.getNodeName();
          if( null == sNodeName )  continue;
          if( sNodeName.equals( argv[3] ) )
          {
            bChildFound = true;
            break;
          }
        }
        if( !bChildFound || nodeChild == null )  continue;
        Node nodeData = nodeChild.getFirstChild();
        if( null == nodeData ||
            !(nodeData instanceof org.w3c.dom.Text) )  continue;
        String sData = nodeData.getNodeValue();
        if( null == sData ||
            !sData.equals( argv[4] ) )  continue;

        // ---- Create a new node element and insert it ----
        Node newMainNode  = document.createElement(  argv[2] );
        Node newChildNode = document.createElement(  argv[3] );
        Text newTextNode  = document.createTextNode( argv[5] );
        newChildNode.appendChild( newTextNode );
        newMainNode.appendChild( newChildNode );
        rootNode.insertBefore( newMainNode, nodeMain );
        rootNode.normalize();
        // System.out.println( "Node list after modification:" );
        // printNodesFromList( ndList );
        break;
      }

      // ---- Use a XSLT transformer for writing the new XML file ----
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      DOMSource        source = new DOMSource( document );
      FileOutputStream os     = new FileOutputStream( new File( argv[1] ) );
      StreamResult     result = new StreamResult( os );
      transformer.transform( source, result );

      // ---- Error handling ----
    } catch( TransformerConfigurationException tce ) {
        System.out.println( "\n** Transformer Factory error" );
        System.out.println( "   " + tce.getMessage() );
        Throwable e = ( tce.getException() != null ) ? tce.getException() : tce;
        e.printStackTrace();
    } catch( TransformerException tfe ) {
        System.out.println( "\n** Transformation error" );
        System.out.println( "   " + tfe.getMessage() );
        Throwable e = ( tfe.getException() != null ) ? tfe.getException() : tfe;
        e.printStackTrace();
    } catch( SAXParseException spe ) {
        System.out.println( "\n** Parsing error, line " + spe.getLineNumber()
                                            + ", uri "  + spe.getSystemId() );
        System.out.println( "   " + spe.getMessage() );
        Exception e = ( spe.getException() != null ) ? spe.getException() : spe;
        e.printStackTrace();
    } catch( SAXException sxe ) {
        Exception e = ( sxe.getException() != null ) ? sxe.getException() : sxe;
        e.printStackTrace();
    } catch( ParserConfigurationException pce ) {
        pce.printStackTrace();
    } catch( IOException ioe ) {
        ioe.printStackTrace();
    }
  }
}
