package tests;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterRegistry;

/**
 * Converts an RDF file from one serialization format into another
 * 
 * @author Enrico Minack
 */
public class RDF2RDF {
	private final String[] sources;
	private final String[] destinations;
	private final Set<String> created = new HashSet<String>();
	
	public static void main(String[] args) throws Exception {
		if(args.length >= 2) {
			new RDF2RDF(args).run();
			return;
		}
		printUsage();
		System.exit(1);
	}
	
	private static void printUsage() {
		System.err.println("RDF2RDF converts RDF data from any serialization format to any other.");
		System.err.println("This tool was written in 2008 by Enrico Minack.");
		System.err.println();
		System.err.println("usage:");
		System.err.println("  java -jar rdf2rdf.jar FILENAME.EXT [FILENAME.EXT ...] | -.EXT  FILENAME.EXT | .EXT | -.EXT");
		System.err.println();
		System.err.println("The format has to be indicated by the filename extension.");
		System.err.println("Filename - stands for stdin or stdout. In this case, please indicate the format");
		System.err.println("by adding the associated filename extension to the -, e.g -.nt or -.rdf");
		System.err.println("If the format can not be determined, RDF/XML is chosen.");
		System.err.println();
		System.err.println("Available RDF serializations:");
		for(RDFFormat format : RDFFormat.values()) {
			String defaultExt = format.getDefaultFileExtension();
			System.err.print("  " + format.getName() + " (" + format.getDefaultMIMEType() + "): ");
			for(String ext : format.getFileExtensions()) {
				if(ext.compareTo(defaultExt) == 0)
					System.err.print("[" + ext + "] ");
				else
					System.err.print(ext + " ");
			}
			System.err.println();
		}
	}

	private RDF2RDF(String[] args) {
		List<String> sources = new ArrayList<String>(args.length - 1);
		List<String> destinations = new ArrayList<String>(args.length - 1);
		if(args.length >= 2) {
			getSourcesAndDestinations(args, sources, destinations);
		} else {
			throw new IllegalArgumentException("There must be at least two arguments being given!");
		}
		
		this.sources = sources.toArray(new String[0]);
		this.destinations = destinations.toArray(new String[0]);
	}
	
	protected static void getSourcesAndDestinations(String[] args, List<String> sources, List<String> destinations) {
		String dest = args[args.length-1];
		
		for(int i=0; i<args.length-1; i++) {
			sources.add(args[i]);

			// construct the filename of the respective destination
			String destination = args[i];
			if(dest.startsWith(".")) {
				// we rename the input file to use the given extension
				String ext = dest;
				
				// remove the .gz from the input
				if(destination.endsWith(".gz")) {
					destination = destination.substring(0, destination.length() - 3);
				}

				// add the extension to the input for the output
				int pos = destination.lastIndexOf('.');
				if(pos < 0)
					pos = destination.length();
				destination = destination.substring(0, pos) + ext;
			} else {
				// we simply write to the given destination
				destination = dest;
			}
			
			destinations.add(destination);
		}
	}

	private void run() {
		if(this.sources.length != this.destinations.length)
			throw new IllegalStateException("There are not as many source files as destination files, this should never happen!");
		
		String lastDestination = null;
		OutputStream lastOut = null;
		
		for(int i=0; i<sources.length; i++) {
			try {
				String source = this.sources[i];
				String destination = this.destinations[i];
				
				System.err.print("converting " + source + " to " + destination);

				// either take the file or stdin
				InputStream in = null;
				if(source.startsWith("-")) {
					in = System.in;
				} else {
					in = new FileInputStream(source);
				}
				
				// either take the file or stdout
				OutputStream out = null;
				if(destination.startsWith("-")) {
					out = System.out;
				} else {
					// if this program did not yet create it, then we should not add to the destination
					boolean create = this.created.add(destination);
					out = new FileOutputStream(destination, !create);
				}

				// unzipp / zipp if necessary
				if(source.endsWith(".gz")) {
					source = source.substring(0, source.length() - 3);
					in = new GZIPInputStream(in);
				}
				if(destination.endsWith(".gz")) {
					destination = destination.substring(0, destination.length() - 3);
					out = new GZIPOutputStream(out);
				}

				// check if the last destination was not the same as this is
				if((lastDestination != null) && ! lastDestination.equals(destination)) {
					// if so, we need to close the last out stream
					if(lastOut != null)
						lastOut.close();
				}
				lastDestination = destination;
				lastOut = out;
				

				RDFFormat sourceFormat = RDFParserRegistry.getInstance().getFileFormatForFileName(source, RDFFormat.RDFXML);
				RDFFormat destinationFormat = RDFParserRegistry.getInstance().getFileFormatForFileName(destination, RDFFormat.RDFXML);

				System.err.println(" (" + sourceFormat.getName() + " to " + destinationFormat.getName() + ")");
				
				RDFParser parser = RDFParserRegistry.getInstance().get(sourceFormat).getParser();
				RDFWriter writer = RDFWriterRegistry.getInstance().get(destinationFormat).getWriter(out);
				
				parser.setRDFHandler(writer);
				parser.parse(in, "unknown:namespace");

				// flush output
				out.flush();
				
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RDFParseException e) {
				e.printStackTrace();
			} catch (RDFHandlerException e) {
				e.printStackTrace();
			}
		}

		try {
			if(lastOut != null)
				lastOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
