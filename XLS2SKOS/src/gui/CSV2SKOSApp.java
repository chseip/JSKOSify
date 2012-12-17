package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import main.CSV2SKOS;

import org.jdom2.Document;
import org.json.JSONException;

import tools.CustomFileChooser;
import javax.swing.JRadioButton;


/**
 * GUI for CSV2SKOS
 * 
 * updates 06.11.2012: 
 * 						- moved the format selection from the menu to the conversion options
 * 						- improved the save dialog
 * 
 * @author Christian Rüh
 * @date 17.08.2012
 * 
 */

public class CSV2SKOSApp {

	private static String column1 = "Found Headers";
	private static String column2 = "SKOS Correspondent";
	private static String column3 = "Language";
	private static String column4 = "Hierarchy Level";
	private static String column5 = "Use this label for URI";
	private static String column6 = "Category";
	
	private String ns;
	private String topTerm;
	private String subTopTerm;
	private String labelTopDE;
	private String labelTopEN;
	
	private JTextField txt_Namespace;
	private JTextField txt_topTerm;
	private JTextField txt_labelTopDE;
	private JTextField txt_labelTopEN;
	private JTextField txtGemetThesaurusURI;
	private JTextField txtGemetDomain;
	private JTextField txt_subTopTerm;
	
	private JCheckBox chckbxUmthes;
	private JCheckBox chckbxGemet;
	private JCheckBox chckbxMdide;
	private JCheckBox chckbxBroader;
	private JCheckBox chckbxStartOnly;
	private JCheckBox chckbxRelated;
	private JCheckBox chckbxAgrovoc;
	
	private JComboBox cBoxDelim;
	private JComboBox cBoxMainLang;
	private JComboBox cBoxGemetSearchMode;
	private JComboBox comboBoxAgrovoc;
	
	private JRadioButton rdbtnXml;
	private JRadioButton rdbtnTurtle;
	private JRadioButton rdbtnNT;
	
	private JProgressBar progressBar;
	private JButton startButton; 
	private JButton abortButton;
	private JLabel progressLabel;
	SwingWorker<String, Void> worker;
	SwingWorker<String, Void> worker2;
	SwingWorker<String, Void> worker3;
	
	private boolean exportedToRDF;
	private boolean canceled;
	
	private File file;
	private Document doc;
	
	String[] headers = new String[100];
	String[][] tableArray = new String[100][6];
	private JFrame frmTest;
	JFileChooser fc;
	CustomFileChooser cfc;
	JTextArea log;
	private JTable table;
	private JTextField textSubTerm;
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					CSV2SKOSApp window = new CSV2SKOSApp();
					window.frmTest.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public CSV2SKOSApp() {
		initialize();
	}
	
	//Source: http://answers.yahoo.com/question/index?qid=20100205165858AA71RDq
	private void setMaxSize(JComponent jc) {
		Dimension max = jc.getMaximumSize();
		Dimension pref = jc.getPreferredSize();
		max.height = pref.height;
		jc.setMaximumSize(max);
	}
	
	public class MyComboBoxRenderer extends JComboBox implements TableCellRenderer {
    public MyComboBoxRenderer(String[] items) {
        super(items);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }

        // Select the current value
        setSelectedItem(value);
        return this;
    	}
	}

	public class MyComboBoxEditor extends DefaultCellEditor {
	    public MyComboBoxEditor(String[] items) {
	        super(new JComboBox(items));
	    }
	}

	private void updateTable(String[] headers) {
		DefaultTableModel model = (DefaultTableModel)table.getModel();
		model.getDataVector().removeAllElements();
		
		if (headers!=null && headers.length>=2){
	    	for (String header : headers) {
	    		// Add some columns
	    		if (header!="")	model.addRow(new Object[]{header});
	    		}
	    	}
	}
	
	private void createTable() {
//		log.append("Sprung in createTable() erfolgreich!");
		table = new JTable();
		
		Object[][] data = {
//			    {"Kathy", "Smith",
//			     "Snowboarding", new Integer(5), new Boolean(true)},
//			    {"Joe", "Brown",
//			     "Pool", new Integer(10), new Boolean(false)}
			};
				
		String[] columnNames = {column1, column2, column3, column4, column5, column6};
		
		table.setModel(new DefaultTableModel(data, columnNames));
		
		//Add hierarchy level ComboBoxes to the fourth column
		//TODO: A editable Combobox would be better
		addComboboxToTable(3, new String[]{"","1", "2", "3", "4", "5", "6", "7", "8", "9", "10"});
		
		//Add Language ComboBoxes to the third column
		addComboboxToTable(2, new String[]{"","German", "English", "French"});
		
		//Add skos correspondents to the second column
		addComboboxToTable(1, new String[]{"skos:prefLabel", "skos:altLabel", "skos:editorialNote", "skos:definition", "skos:broader", "skos:narrower"});
					
		//If not set 25 the default height makes it pretty ugly
		table.setRowHeight(25);
			    
		table.getColumnModel().getColumn(4).setCellRenderer(table.getDefaultRenderer(Boolean.class));
		table.getColumnModel().getColumn(4).setCellEditor(table.getDefaultEditor(Boolean.class));
		
		table.getColumnModel().getColumn(5).setCellRenderer(table.getDefaultRenderer(Boolean.class));
		table.getColumnModel().getColumn(5).setCellEditor(table.getDefaultEditor(Boolean.class));
		
		JScrollPane scrollPane = new JScrollPane(table);
		frmTest.getContentPane().add(scrollPane, BorderLayout.CENTER);
	}

	private void addComboboxToTable(int vColIndex, String[] values) {
		DefaultTableModel model = (DefaultTableModel)table.getModel();

		TableColumn col = table.getColumnModel().getColumn(vColIndex);
		col.setCellEditor(new MyComboBoxEditor(values));

		// If the cell should appear like a combobox in its
		// non-editing state, also set the combobox renderer
		col.setCellRenderer(new MyComboBoxRenderer(values));
		
	}
	
	private void addComboboxWithHeadersToTableColumn(int col,final String[] headers) {
//		log.append("bin jetzt in Methode addComboboxWithHeadersToTableColumn");
	    DefaultComboBoxModel m_comboModel = new DefaultComboBoxModel();
		JComboBox m_comboBox = new JComboBox(m_comboModel);
        table.getColumnModel().getColumn(col).setCellEditor(
	    new DefaultCellEditor(m_comboBox) {
	 
	        @Override
	        public Component getTableCellEditorComponent(
	              JTable table,
	              Object value,
	              boolean isSelected,
	              int row,
	              int column) {
	          adjustComboBoxValues();
	          return super.getTableCellEditorComponent(table, value,
	                isSelected, row, column);
	        }
	 
	        private void adjustComboBoxValues() {
	          final JComboBox comboBox = (JComboBox) editorComponent;
	          final DefaultComboBoxModel comboModel =
	                (DefaultComboBoxModel) comboBox.getModel();
	          comboModel.removeAllElements();
	          for (String header : headers) {
	            comboModel.addElement(header);
	          }
	 
	          comboModel.setSelectedItem(headers[0]);
	        }
	      });
//        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
//        table.repaint();
	}
	
	private void saveFile(Document doc) throws IOException {
		if (rdbtnXml.isSelected()) cfc = new CustomFileChooser("rdf");
		else if (rdbtnTurtle.isSelected()) cfc = new CustomFileChooser("ttl");
		else if (rdbtnNT.isSelected()) cfc = new CustomFileChooser("nt");
		
		int returnValCust = cfc.showSaveDialog(frmTest);
		if (returnValCust == JFileChooser.APPROVE_OPTION) {
          File file = cfc.getSelectedFile();
          log.append("Saving: " + file.getName() + ".\n");
          //saveRDF(Document doc, String namespace, String rdfFile, String formatRDF)
          if (rdbtnXml.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "RDFXML");
          else if (rdbtnTurtle.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "Turtle");
          else if (rdbtnNT.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "NTriples");
          log.append("Saved SKOS document as " + file.getName() + " successfully. \n");
          progressLabel.setText("Finished Saving");
      } else {
          log.append("Save command cancelled by user.\n");
          progressLabel.setText("Canceled Saving");
      }
	}
//		int returnVal = fc.showSaveDialog(frmTest);
//		if (returnVal == JFileChooser.APPROVE_OPTION) {
//	        File file = fc.getSelectedFile(); // plus the rest of your code  
//	        if (file.exists())  {  
//	        	int answer = JOptionPane.showConfirmDialog(  
//	        			frmTest, file + " exists. Overwrite?", "Overwrite?",  
//	                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE  
//	                );  
//	            if (answer != JOptionPane.OK_OPTION) {  
//	                // start the loop again  
//	                return;  
//	            }
//	        }
//	        else {
//	            log.append("Saving: " + file.getName() + ".\n");
//	            //saveRDF(Document doc, String namespace, String rdfFile, String formatRDF)
//	            if (mntmExportToRdfxml.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "RDFXML");
//	            else if (mntmExportToRdfturtle.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "Turtle");
//	            else if (mntmExportToRdfntriples.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "NTriples");
//	            log.append("Saved SKOS document as " + file.getName() + " successfully. \n");
//	            progressLabel.setText("Finished Saving");
//	        }
////	        else log.append("Save command cancelled by user.\n");
//        }
//		if (returnVal == JFileChooser.CANCEL_OPTION) {
//			log.append("Save command cancelled by user.\n");
//			progressLabel.setText("Canceled Saving");
//		}
//	    
//        
//	}
		
//		//FileChooser to save
//		int returnVal = fc.showSaveDialog(frmTest);
//        if (returnVal == JFileChooser.APPROVE_OPTION) {
//            File file = fc.getSelectedFile();
//            log.append("Saving: " + file.getName() + ".\n");
//            //saveRDF(Document doc, String namespace, String rdfFile, String formatRDF)
//            if (mntmExportToRdfxml.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "RDFXML");
//            else if (mntmExportToRdfturtle.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "Turtle");
//            else if (mntmExportToRdfntriples.isSelected()) CSV2SKOS.saveRDF(doc, ns, file.getAbsolutePath(), "NTriples");
//            log.append("Saved SKOS document as " + file.getName() + " successfully. \n");
//        } else {
//            log.append("Save command cancelled by user.\n");
//        }
//        progressLabel.setText("Finished...");

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmTest = new JFrame();
		frmTest.setTitle("CSV2SKOS 0.2");
		frmTest.setBounds(100, 100, 1024, 768);
		frmTest.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel testPanel = new JPanel();
		testPanel.setLayout(new FlowLayout());
		//Create the log first, because the action listeners need to refer to it
        log = new JTextArea(3,60);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(log);
        testPanel.add(scrollPane);
        
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
        
        startButton = new JButton("Start conversion");
        startButton.setActionCommand("start");
        startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				startButton.setEnabled(false);
				abortButton.setEnabled(true);
				final int rows = table.getRowCount();
				int columns = table.getColumnCount();
				boolean gemeterror = false;
				int URICounter = 0;
				boolean hierarchical = false;
				//Finjd out if provided word list contains hierarchies (if so more than one URI is perfectly fine)
				for (int z=0; z<rows; z++){
					Object hierarchicalTableValue = table.getValueAt(z, 3);
					if (hierarchicalTableValue!=null) {
						String hierarchicalTableString = (String) hierarchicalTableValue;
						if (hierarchicalTableString.equals("")) hierarchical = false;
						else hierarchical = true;
					}
				}
				//Appearance:
				//col0   -     col1           -  col2    - col3              -    col4    - col5
				//header - skos:correspondent - language - hierarchylevel - elemAsUri?   - category?
				for (int i=0; i<rows; i++){
					for (int j=0; j<columns; j++){
						Object tableValue = table.getValueAt(i, j);
						if (tableValue!=null) {
							if (tableValue.getClass().getCanonicalName()=="java.lang.String") tableArray[i][j] = (String) tableValue;
							//True auf für j = 5 setzen und zählen?
							else if (tableValue.getClass().getCanonicalName()=="java.lang.Boolean"){
								tableArray[i][j] = String.valueOf(tableValue);
								//Count the appearances of elemAsUri to use it for error detection
								if ((boolean) (tableValue=true) && j==4) URICounter++;
							}
						}
						else {
							if (j<=3) tableArray[i][j] = "";
							//To not get into trouble later on all the empty value in the elemAsUri? column are set to false
							else tableArray[i][j] = "false";
//								log.append("ACHTUNG NULL! \n");
						}
//							log.append(tableArray[i][j]);
					}
//						log.append("\n");
				}
				
				//Check if everything is set for searching GEMET if GEMET closematches are activated
				if (chckbxGemet.isSelected()) {
					if (txtGemetThesaurusURI.getText().isEmpty()) {
						JOptionPane.showMessageDialog(null, "Please specify the GEMET ThesaurusURI.", "Error!", JOptionPane.ERROR_MESSAGE);
						gemeterror = true;
					}
					if (txtGemetDomain.getText().isEmpty()) {
						JOptionPane.showMessageDialog(null, "Please specify the GEMET Domain.", "Error!", JOptionPane.ERROR_MESSAGE);
						gemeterror = true;
					}
				}
				
				//Test if exactly one elemAsUri is given if the data set does not contain hierarchies
				if (URICounter>1 && hierarchical == false) JOptionPane.showMessageDialog(null, "Please specify only ONE term for usage in the URI", "Error!", JOptionPane.ERROR_MESSAGE);
				//Test if an elemAsUri is given at all
				else if (URICounter==0) {
					JOptionPane.showMessageDialog(null, "Please specify a term for usage in the URI", "Error!", JOptionPane.ERROR_MESSAGE);
					startButton.setEnabled(true);
				}
				else if (txt_Namespace.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Please specify a Namespace please.", "Error!", JOptionPane.ERROR_MESSAGE);
					startButton.setEnabled(true);
				}
				else if (txt_topTerm.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Please specify a top term please.", "Error!", JOptionPane.ERROR_MESSAGE);
					startButton.setEnabled(true);
				}
				else if (txt_labelTopDE.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Please specify a German label for the top term please.", "Error!", JOptionPane.ERROR_MESSAGE);
					startButton.setEnabled(true);
				}
				else if (txt_labelTopEN.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Please specify an English label for the top term please.", "Error!", JOptionPane.ERROR_MESSAGE);
					startButton.setEnabled(true);
				}
				else if (gemeterror) {
					JOptionPane.showMessageDialog(null, "Please revise the GEMET details or deactivate the option if you do not want to use it.", "Error!", JOptionPane.ERROR_MESSAGE);
					startButton.setEnabled(true);
				}
				else {
					ns = txt_Namespace.getText();
	                topTerm = txt_topTerm.getText();
	                subTopTerm = txt_subTopTerm.getText();
	                labelTopDE = txt_labelTopDE.getText();
	                labelTopEN = txt_labelTopEN.getText();
					
					//setupDocument(String baseURI, String topTerm, String topLabel_de, String topLabel_en)
					doc = CSV2SKOS.setupDocument(ns, topTerm, labelTopDE, labelTopEN);
					progressLabel.setText("Phase 1: Setting up Doc");
					log.append("Setup SKOS document successfully. \n");
					
					progressLabel.setText("Phase 2: Filling Doc");
					// Construct a new SwingWorker
					worker = new SwingWorker<String, Void>(){
						
						@Override
						protected String doInBackground() throws Exception{
//							while (!worker.isCancelled()) {
								//TODO: Not working this way
								//maybe doing "while (terms.readRecord())" here and sending each line of the CSV file over to the fillDocument method
								//but don't know if this works out
							String delim = (String) cBoxDelim.getSelectedItem();
//			                if (!cBoxDelim.getSelectedItem().toString().isEmpty()) doc = CSV2SKOS.fillDocument(doc, ns, topTerm, tableArray, rows, file.getAbsolutePath(), delim.charAt(0), progressBar, chckbxUmthes.isSelected(), chckbxGemet.isSelected(), Integer.parseInt((String) cBoxGemetSearchMode.getSelectedItem()), chckbxAgrovoc.isSelected(), (String) comboBoxAgrovoc.getSelectedItem());
			                if (!cBoxDelim.getSelectedItem().toString().isEmpty()) doc = CSV2SKOS.fillDocument2(doc, ns, topTerm, subTopTerm, tableArray, rows, file.getAbsolutePath(), delim.charAt(0), progressBar, chckbxUmthes.isSelected(), chckbxGemet.isSelected(), Integer.parseInt((String) cBoxGemetSearchMode.getSelectedItem()), chckbxAgrovoc.isSelected(), (String) comboBoxAgrovoc.getSelectedItem(), (String) cBoxMainLang.getSelectedItem());
			                //Old textfield version
			                //if (!txt_Delimiter.getText().isEmpty()) headers = CSV2SKOS.getHeaders(file.getAbsolutePath(), txt_Delimiter.getText().charAt(0));
			                else JOptionPane.showMessageDialog(null, "Please specify a delimiter.", "Error!", JOptionPane.ERROR_MESSAGE);
//							}
							return labelTopDE;
						}
						
						@Override
						protected void done(){
							startButton.setEnabled(true);
							progressBar.setValue(100);
							log.append("Filled SKOS document successfully with terms. \n");
							if (chckbxBroader.isSelected() || chckbxRelated.isSelected()) {
								progressBar.setValue(0);
								progressLabel.setText("Phase 3: Adding Semantics/Related");
								worker2 = new SwingWorker<String, Void>(){
									@Override
									protected String doInBackground(){
										//addSemantics(Document doc, String namespace, String topTerm, boolean startOnly, JProgressBar progressBar)
										if (chckbxBroader.isSelected()) doc = CSV2SKOS.addSemantics(doc, ns, topTerm, chckbxStartOnly.isSelected(), progressBar);
										if (chckbxRelated.isSelected()) doc = CSV2SKOS.addRelatedTerms(doc, ns, topTerm, progressBar);
										return labelTopDE;
									}
									
									@Override
									protected void done(){
										if (!canceled) {
											progressLabel.setText("Phase 4: Saving Doc");
											try {
												saveFile(doc);
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}

									}
								};
								worker2.execute();
							}
							else {
								if (!canceled) {
									progressLabel.setText("Phase 3: Saving Doc");
									try {
										saveFile(doc);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									}
							}
							
						}
					};
					// Execute the SwingWorker; the GUI will not freeze
					worker.execute();	
				}
			}
		});
        progressPanel.add(startButton);
        
        abortButton = new JButton("Cancel");
        abortButton.setActionCommand("cancel");
        abortButton.setEnabled(false);
        abortButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent event) {
        		canceled=true;
        		abortButton.setEnabled(false);
        		worker.cancel(true);
        		worker2.cancel(true);
//        		if (worker2!=null) worker2.cancel(true);
                startButton.setEnabled(true);
            }
        });
        progressPanel.add(abortButton);
        
        progressLabel = new JLabel("Press Start Conversion...");
        progressPanel.add(progressLabel);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar);
        
        testPanel.add(progressPanel);
		frmTest.getContentPane().add(testPanel, BorderLayout.SOUTH);
		
		JMenuBar menuBar = new JMenuBar();
		frmTest.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmImport = new JMenuItem("Import CSV");
		mntmImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(frmTest);
				

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                file = fc.getSelectedFile();
	                log.append("\nOpening: " + file.getName() + "." + "\n");
	                String delim = (String) cBoxDelim.getSelectedItem();
	                if (!cBoxDelim.getSelectedItem().toString().isEmpty()) headers = CSV2SKOS.getHeaders(file.getAbsolutePath(), delim.charAt(0));
	                //Old textfield version
	                //if (!txt_Delimiter.getText().isEmpty()) headers = CSV2SKOS.getHeaders(file.getAbsolutePath(), txt_Delimiter.getText().charAt(0));
	                else JOptionPane.showMessageDialog(null, "Please specify a delimiter.", "Error!", JOptionPane.ERROR_MESSAGE);
	                int i = 0;
	                if (headers!=null && headers.length>=2){
	                	updateTable(headers);
//	                	addComboboxWithHeadersToTableColumn(3, headers);
	                	log.append("Read CSV file successfully. \n");
	                }
	                else log.append("Error while reading the headers in CSV file with path: "+file.getAbsolutePath() + "\nMaybe a wrong delimiter was specified");
	                
	            } else {
	                log.append("Open command cancelled by user." + "\n");
	            }
	            log.setCaretPosition(log.getDocument().getLength());
			}
		});
		mnFile.add(mntmImport);
		
		JMenuItem mntmQuit = new JMenuItem("Quit");
		mntmQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.exit( 0 );
			}
		});
		mnFile.add(mntmQuit);
		
		JMenu mnTools = new JMenu("Tools");
		menuBar.add(mnTools);
		
		ButtonGroup group = new ButtonGroup();
		
		JMenuItem mntmRDF2NT = new JMenuItem("Convert RDF to NT");
		mntmRDF2NT.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				log.append("Converting..." + ".\n");
				//FileChooser to save
				fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(frmTest);
		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            
		            //convertRDF2NT(String rdfFile, String ntFile, String namespace)
		            try {
						CSV2SKOS.convertRDF2NT(file.getAbsolutePath(), file.getAbsolutePath()+".nt", "XxYyZz");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            log.append("converted RDF file " + file.getAbsolutePath() + " and saved it to NT File " + file.getAbsolutePath()+".nt" + ".\n");
		        }
			}
			});
		mnTools.add(mntmRDF2NT);

		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmHelp = new JMenuItem("Help");
		mnHelp.add(mntmHelp);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(null, "© 2012 Christian Rüh, Universität Rostock", "About CSV2SKOS", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		mnHelp.add(mntmAbout);
		
		/*
		 * Begin Tabbed Area
		 */
        //First pane: Import Options
        JPanel firstPaneBorders = new JPanel();
        FlowLayout experimentLayout = new FlowLayout();
        firstPaneBorders.setLayout(experimentLayout);
        JLabel spaceFP1 = new JLabel("<html>&nbsp</html>");
        firstPaneBorders.add(spaceFP1);
        
        JLabel lblDelim = new JLabel("Specify Delimiter:");
        firstPaneBorders.add(lblDelim);
        
        String[] delimExamples = {
                ";",
                ",",
                "|",
                "-",
                "$"
       };

       cBoxDelim = new JComboBox(delimExamples);
       cBoxDelim.setEditable(true);
       cBoxDelim.setPreferredSize(new Dimension(35,20));
       cBoxDelim.setToolTipText("<html>Specify the delimiter of your CSV file by: <br>- Choosing one of the list<br>- Typing a single character into the field</html>");

       firstPaneBorders.add(cBoxDelim); 
       
        //Second pane: Conversion Options
        JPanel secondPaneBorders = new JPanel();
        secondPaneBorders.setLayout(new BoxLayout(secondPaneBorders, BoxLayout.Y_AXIS));
        
        JLabel lblSpecifyOutputFormat = new JLabel("Specify output format");
        lblSpecifyOutputFormat.setAlignmentX(Component.CENTER_ALIGNMENT);
        secondPaneBorders.add(lblSpecifyOutputFormat);
        
        JPanel panelSKOSFormat = new JPanel();
        secondPaneBorders.add(panelSKOSFormat);
        
        rdbtnXml = new JRadioButton("RDF/XML (*.rdf)");
        rdbtnXml.setSelected(true);
        panelSKOSFormat.add(rdbtnXml);
        
        rdbtnTurtle = new JRadioButton("RDF/Turtle (*.ttl)");
        panelSKOSFormat.add(rdbtnTurtle);
        
        rdbtnNT = new JRadioButton("RDF/N-Triples (*.nt)");
        panelSKOSFormat.add(rdbtnNT);
        
		ButtonGroup formatGroup = new ButtonGroup();
		formatGroup.add(rdbtnXml);
		formatGroup.add(rdbtnTurtle);
		formatGroup.add(rdbtnNT);
     
        setMaxSize(panelSKOSFormat);
        
        JLabel spaceSP7 = new JLabel("<html>&nbsp</html>");
        secondPaneBorders.add(spaceSP7);
        
        JLabel lblNS = new JLabel("Specify Namespace");
        lblNS.setAlignmentX(Component.CENTER_ALIGNMENT);
        secondPaneBorders.add(lblNS);
        
        txt_Namespace = new JTextField();
        txt_Namespace.setColumns(10);
        txt_Namespace.setText("http://139.30.111.16:3000/");
        txt_Namespace.setMaximumSize(new Dimension(Integer.MAX_VALUE, txt_Namespace.getPreferredSize().height));
        txt_Namespace.setToolTipText("<html>Specify the namespace to be used.<br>Usually this the address of your iQvoc server<br>with an addiontal term</html>");
        secondPaneBorders.add(txt_Namespace);
        
        JLabel spaceSP0 = new JLabel("<html>&nbsp</html>");
        secondPaneBorders.add(spaceSP0);
        
        JLabel lblSpecifyThesaurusMain = new JLabel("Specify thesaurus main language");
        lblSpecifyThesaurusMain.setAlignmentX(Component.CENTER_ALIGNMENT);
        secondPaneBorders.add(lblSpecifyThesaurusMain);
        
        String[] langExamples = {
                "German",
                "English",
                "French",
                "Dutch",
                "Swedish"
        };
        cBoxMainLang = new JComboBox(langExamples);
        cBoxMainLang.setToolTipText("<html>Specify the main language of the thesaurus you use by: <br>- Choosing one of the list<br>- Typing a single one into the box</html>");
//        cBoxMainLang.setPreferredSize(new Dimension(70, 70));
        setMaxSize(cBoxMainLang);
        cBoxMainLang.setEditable(true);
        secondPaneBorders.add(cBoxMainLang);
        
        JLabel spaceSP1 = new JLabel("<html>&nbsp</html>");
        secondPaneBorders.add(spaceSP1);
        
        JLabel lblTopTerm = new JLabel("Short top term");
        lblTopTerm.setAlignmentX(Component.CENTER_ALIGNMENT);
        secondPaneBorders.add(lblTopTerm);
        
        txt_topTerm = new JTextField();
		txt_topTerm.setText("NOKIS");
		txt_topTerm.setColumns(10);
		txt_topTerm.setMaximumSize(new Dimension(Integer.MAX_VALUE, txt_topTerm.getPreferredSize().height));
		txt_topTerm.setToolTipText("<html>Specify the top term for your wordlist<br>This is the root shown in the concepts in iQvoc</html>");
		secondPaneBorders.add(txt_topTerm);
		JLabel spaceSP2 = new JLabel("<html>&nbsp</html>");
		secondPaneBorders.add(spaceSP2);
        
		JLabel lblTopLabelDE = new JLabel("Label for top term (German)");
		lblTopLabelDE.setAlignmentX(Component.CENTER_ALIGNMENT);
		secondPaneBorders.add(lblTopLabelDE);
		
        txt_labelTopDE = new JTextField();
		txt_labelTopDE.setText("NOKIS_de");
		txt_labelTopDE.setColumns(10);
		txt_labelTopDE.setMaximumSize(new Dimension(Integer.MAX_VALUE, txt_labelTopDE.getPreferredSize().height));
		txt_labelTopDE.setToolTipText("<html>A German label for the above top term</html>");
		secondPaneBorders.add(txt_labelTopDE);
		JLabel spaceSP3 = new JLabel("<html>&nbsp</html>");
		secondPaneBorders.add(spaceSP3);
        
		JLabel lblTopLabelEN = new JLabel("Label for top term (English)");
		lblTopLabelEN.setAlignmentX(Component.CENTER_ALIGNMENT);
		secondPaneBorders.add(lblTopLabelEN);
		
        txt_labelTopEN = new JTextField();
		txt_labelTopEN.setText("NOKIS_en");
		txt_labelTopEN.setColumns(10);
		txt_labelTopEN.setMaximumSize(new Dimension(Integer.MAX_VALUE, txt_labelTopEN.getPreferredSize().height));
		txt_labelTopEN.setToolTipText("<html>An English label for the above top term</html>");
		secondPaneBorders.add(txt_labelTopEN);
		JLabel spaceSP4 = new JLabel("<html>&nbsp</html>");
		secondPaneBorders.add(spaceSP4);

        //Third pane: Added Semantics Options
        final JPanel thirdPaneBorders = new JPanel();
        thirdPaneBorders.setLayout(new BoxLayout(thirdPaneBorders, BoxLayout.Y_AXIS));
       
        JLabel lblMatches = new JLabel("Add matches for terms found in:");
        thirdPaneBorders.add(lblMatches);
        
        chckbxMdide = new JCheckBox("MareThes");
        //TODO: implement MDI-DE search
        chckbxMdide.setEnabled(false);
        chckbxMdide.setToolTipText("<html>Set skos:closeMatches for all<br>terms which also exist in Marethes</html>");
        chckbxUmthes = new JCheckBox("UMTHES");
        chckbxUmthes.setToolTipText("<html>Set skos:closeMatches for all<br>terms which also exist in Umthes</html>");
        thirdPaneBorders.add(chckbxMdide);
        thirdPaneBorders.add(chckbxUmthes);
        
        chckbxGemet = new JCheckBox("GEMET");
        chckbxGemet.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		if (chckbxGemet.isSelected()) {
        			txtGemetDomain.setEnabled(true);
        			txtGemetThesaurusURI.setEnabled(true);
        			cBoxGemetSearchMode.setEnabled(true);
        		}
        		else {
        			txtGemetDomain.setEnabled(false);
        			txtGemetThesaurusURI.setEnabled(false);
        			cBoxGemetSearchMode.setEnabled(false);
        		}
        	}
        });
        
        chckbxAgrovoc = new JCheckBox("AgroVoc");
        chckbxAgrovoc.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		if (chckbxAgrovoc.isSelected()) comboBoxAgrovoc.setEnabled(true);
        		else comboBoxAgrovoc.setEnabled(false);
        	}
        });
        
        JLabel spaceSP5 = new JLabel("<html>&nbsp</html>");
        thirdPaneBorders.add(spaceSP5);
        thirdPaneBorders.add(chckbxAgrovoc);
        
        JLabel lblAgrovocSearchMode = new JLabel("AgroVoc search mode:");
        thirdPaneBorders.add(lblAgrovocSearchMode);
        
        String[] searchModesAgrovoc = {
                "Contains",
                "Exact Match",
                "Starts With",
                "Ends With",
                "Exact Word"
        };
        
        comboBoxAgrovoc = new JComboBox(searchModesAgrovoc) {

            /** 
             * @inherited <p>
             */
            @Override
            public Dimension getMaximumSize() {
                Dimension max = super.getMaximumSize();
                max.height = getPreferredSize().height;
                max.width = getPreferredSize().width;
                return max;
            }

        };
        
        comboBoxAgrovoc.setEnabled(false);
        thirdPaneBorders.add(comboBoxAgrovoc);
        
        JLabel spaceSP6 = new JLabel("<html>&nbsp</html>");
        thirdPaneBorders.add(spaceSP6);
        
        thirdPaneBorders.add(chckbxGemet);
        
        JLabel lblGemetDomain = new JLabel("GEMET Domain:");
        thirdPaneBorders.add(lblGemetDomain);
        
        txtGemetDomain = new JTextField();
        txtGemetDomain.setText("http://www.eionet.europa.eu/gemet/");
        txtGemetDomain.setColumns(8);
        txtGemetDomain.setMaximumSize(new Dimension(Integer.MAX_VALUE, txtGemetDomain.getPreferredSize().height));
        txtGemetDomain.setToolTipText("<html>Domain where GEMET installation is hosted</html>");
        txtGemetDomain.setEnabled(false);
        thirdPaneBorders.add(txtGemetDomain);
        
        JLabel lblGemetThesaurusURI = new JLabel("GEMET Thesaurus URI:");
        thirdPaneBorders.add(lblGemetThesaurusURI);
        
        txtGemetThesaurusURI = new JTextField();
        txtGemetThesaurusURI.setText("http://www.eionet.europa.eu/gemet/concept/");
        txtGemetThesaurusURI.setColumns(8);
        txtGemetThesaurusURI.setMaximumSize(new Dimension(Integer.MAX_VALUE, txtGemetThesaurusURI.getPreferredSize().height));
        txtGemetThesaurusURI.setToolTipText("<html>URI of the Thesaurus to search</html>");
        txtGemetThesaurusURI.setEnabled(false);
        thirdPaneBorders.add(txtGemetThesaurusURI);
        
        JLabel lblGemetSearchMode = new JLabel("GEMET Search Mode:");
        thirdPaneBorders.add(lblGemetSearchMode);
        
        String[] searchModes = {
                "0",
                "1",
                "2",
                "3",
                "4"
       };

       //source: http://stackoverflow.com/questions/7581846/swing-boxlayout-problem-with-jcombobox-without-using-setxxxsize
       cBoxGemetSearchMode = new JComboBox(searchModes) {

            /** 
             * @inherited <p>
             */
            @Override
            public Dimension getMaximumSize() {
                Dimension max = super.getMaximumSize();
                max.height = getPreferredSize().height;
                max.width = getPreferredSize().width;
                return max;
            }

        };

        //Set the default to searchmode to "1" (suffix regex)
        cBoxGemetSearchMode.setSelectedIndex(1);
//       cBoxGemetSearchMode = new JComboBox(searchModes);
//       cBoxGemetSearchMode.setPreferredSize(new Dimension(25,20));
       cBoxGemetSearchMode.setPreferredSize(cBoxGemetSearchMode.getMaximumSize());
        
       cBoxGemetSearchMode.setToolTipText("<html>0 – no wildcarding of any type ('accident' becomes '^accident$'). SQL syntax: term = 'accident'" +
        		"<br>1 – suffix regex ('accident' becomes '^accident.+$'). SQL syntax: term LIKE 'accident%'" +
        		"<br>2 – prefix regex ('accident' becomes '^.+accident$'). SQL syntax: term LIKE '%accident'" +
        		"<br>3 – prefix/suffix combined ('accident' becomes '^.+accident.+$'). SQL syntax: term LIKE '%accident%'" +
        		"<br>4 – auto search: each of the previous four expansions is tried in ascending order until a match is found.</html>");
       ToolTipManager.sharedInstance().setDismissDelay(24000);
       ToolTipManager.sharedInstance().registerComponent(cBoxGemetSearchMode);
       cBoxGemetSearchMode.setEnabled(false);
        thirdPaneBorders.add(cBoxGemetSearchMode);

//        thirdPaneBorders.add(space);
        JLabel spaceTP1 = new JLabel("<html>&nbsp</html>");
        thirdPaneBorders.add(spaceTP1);
        
        JLabel lblBroader = new JLabel("Look for broader terms:");
        String toolTipBroader = new String("<html>Look automatically for terms starting with a broader term<br>(e.g. 'beach scarp' gets 'beach' as broder term)<br>or containing a broader term (e.g. ???)</html>");
        lblBroader.setToolTipText(toolTipBroader);
        thirdPaneBorders.add(lblBroader);
        
        chckbxBroader = new JCheckBox("Activate");
        chckbxBroader.setToolTipText("<html>Look automatically for terms starting with a broader term<br>(e.g. 'beach scarp' gets 'beach' as broder term)<br>or containing a broader term (e.g. ???)</html>");
        chckbxBroader.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		if (chckbxBroader.isSelected()) chckbxStartOnly.setEnabled(true);
        		else chckbxStartOnly.setEnabled(false);
        	}
        });
        thirdPaneBorders.add(chckbxBroader);
        chckbxStartOnly = new JCheckBox("Check beginning of words only");
        chckbxStartOnly.setToolTipText("<html>e.g. 'beach scarp' gets 'beach' as broder term<br>but 'sandy beach' won't get 'beach' as broader term</html>");
        chckbxStartOnly.setEnabled(false);
		thirdPaneBorders.add(chckbxStartOnly);
        JLabel spaceTP2 = new JLabel("<html>&nbsp</html>");
        thirdPaneBorders.add(spaceTP2);
        
        chckbxRelated = new JCheckBox("Check for related terms");
        chckbxRelated.setToolTipText("<html>If related terms are outlined with 's.' or 's.a.'<br>a skos:related is set to these connections</html>");
		thirdPaneBorders.add(chckbxRelated);
        JLabel spaceTP3 = new JLabel("<html>&nbsp</html>");
        thirdPaneBorders.add(spaceTP3);
        
        //Fourth pane: Further Options
//        JPanel fourthPaneBorders = new JPanel();
//        fourthPaneBorders.setLayout(new BoxLayout(fourthPaneBorders, BoxLayout.Y_AXIS));
//        
//        JLabel spaceFoP1 = new JLabel("<html>&nbsp</html>");
//        fourthPaneBorders.add(spaceFoP1);
//        
//        JLabel lblTF = new JLabel("Blubb blubb: ");
//        fourthPaneBorders.add(lblTF);
//        JTextField tf = new JTextField(8);
//        tf.setText("Blabla");
//        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height) );
//        fourthPaneBorders.add(tf);
//        
//        JLabel spaceFoP2 = new JLabel("<html>&nbsp</html>");
//        fourthPaneBorders.add(spaceFoP2);
//        
//        JLabel lblTF2 = new JLabel("Blubb2 blubb2: ");
//        fourthPaneBorders.add(lblTF2);
//        JTextField tf2 = new JTextField(8);
//        tf2.setText("Blabla2");
//        tf2.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf2.getPreferredSize().height) );
//        fourthPaneBorders.add(tf2);
 
        JTabbedPane tabbedPane = new JTabbedPane();
        //Don't really like hardcoding this but otherwise the tab pane does not look good
        tabbedPane.setPreferredSize(new Dimension(375, 50));
//        tabbedPane.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
        tabbedPane.addTab("Import Options", null, firstPaneBorders, null);
        tabbedPane.addTab("Conversion Options", null, secondPaneBorders, null);
        
        JLabel lblsubTerm = new JLabel("Term below the top term (optional)");
        lblsubTerm.setAlignmentX(Component.CENTER_ALIGNMENT);
        secondPaneBorders.add(lblsubTerm);
        
        txt_subTopTerm = new JTextField();
        txt_subTopTerm.setText("");
        txt_subTopTerm.setColumns(10);
        txt_subTopTerm.setMaximumSize(new Dimension(Integer.MAX_VALUE, txt_subTopTerm.getPreferredSize().height));
        txt_subTopTerm.setToolTipText("<html>Specify a term below the top term.<br>Leave empty if you do not want that.</html>");
        secondPaneBorders.add(txt_subTopTerm);
        
        tabbedPane.addTab("Added Semantics", null, thirdPaneBorders, null);
//        tabbedPane.addTab("Further Options", null, fourthPaneBorders, null);
        //Show first Tab upon start of the aplication
        tabbedPane.setSelectedIndex(0);

        frmTest.getContentPane().add(tabbedPane, BorderLayout.EAST);
        
		/*
		 * Tabbed Area End
		 */
		
		createTable();
	}
}