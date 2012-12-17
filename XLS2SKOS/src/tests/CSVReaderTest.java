package tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.csvreader.CsvReader;

public class CSVReaderTest {

	public static void main(String[] args) {
		try {
//			CsvReader products = new CsvReader("example.csv");
//			CsvReader products = new CsvReader("I:/_dev/workspace/XLS2SKOS/src/xmlTests/example.csv", ';');
			CsvReader products = new CsvReader("nokis_wortliste3.csv", ';');
		
			products.readHeaders();

//			while (products.readRecord())
//			{
//				String head1 = products.get("begriff_de");
//				String head2 = products.get("begriff_en");
//				String head3 = products.get("definition_de");
//				String head4 = products.get("definition_en");
//				
//				// perform program logic here
//				System.out.println(head1 + "->" + head2);
//			}
			int i=1;
			
			while (products.readRecord())
			{
				System.out.println(i + ".Zeile: " + products.get("begriff_de") + "->" + products.get("begriff_en"));
				i++;
			}
			
			products.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}