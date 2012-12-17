package tests;

import javax.swing.table.DefaultTableModel;
import javax.swing.*;

import java.awt.BorderLayout;
import java.util.Vector;
 
public class GUIFilmTabelle extends JPanel {
 
  //Vektor für Spaltennamen
  private Vector columnNames = new Vector();
 
  //Vektor für Daten
  private Vector data = new Vector();
 
  public GUIFilmTabelle()
  {
    super(new BorderLayout());
    //TableModel: Tabellenmanipulation, Daten
    MyDefaultTableModel model = new MyDefaultTableModel(data, columnNames);
    //Tabelle: Anzeige
    JTable table = new JTable(model); 
    model = (MyDefaultTableModel) table.getModel();
    JComboBox comboBox = new JComboBox();
    comboBox.addItem("UP");
    comboBox.addItem("DOWN");
    table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(comboBox));
    // ScrollPane zu JPanel hinzufügen
    add(new JScrollPane(table), BorderLayout.CENTER);    
  }
 
  // Inner class MyDefaultTableModel: Tabellen-Model
  public class MyDefaultTableModel extends DefaultTableModel {
 
    public MyDefaultTableModel(Vector data, Vector columnNames) {
      super(data, columnNames); 
      setDataVector(data,columnNames);
      this.addColumn("Name");
      this.addColumn("UP/DOWN");   
    }
 
    public Class getColumnClass(int col) {
      Vector v = (Vector) this.getDataVector().elementAt(0);
      return v.elementAt(col).getClass();
    }
 
    public boolean isCellEditable(int row, int col) {
      Class columnClass = getColumnClass(col);
      return columnClass != ImageIcon.class;
    }
  }
  
  /**
   * Create the GUI and show it.  For thread safety,
   * this method should be invoked from the
   * event dispatch thread.
   */
  private static void createAndShowGUI() {
      //Create and set up the window.
      JFrame frame = new JFrame("GUIFilmTabelle");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      //Add content to the window.
      frame.add(new GUIFilmTabelle());

      //Display the window.
      frame.pack();
      frame.setVisible(true);
  }
  
  public static void main(String[] args) {
      //Schedule a job for the event dispatch thread:
      //creating and showing this application's GUI.
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              //Turn off metal's use of bold fonts
              UIManager.put("swing.boldMetal", Boolean.FALSE); 
              createAndShowGUI();
          }
      });
  }
}