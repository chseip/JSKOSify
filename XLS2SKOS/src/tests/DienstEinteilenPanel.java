package tests;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import be.ac.ucl.isys.grafixml.ui.editor.JComponentCellEditor;
import be.ac.ucl.isys.grafixml.ui.renderer.JComponentCellRenderer;
 
public class DienstEinteilenPanel extends JFrame {  
    private JTable m_table;
    private JScrollPane m_scrollPane;
    private DefaultTableModel m_tableModel;
    private JComboBox m_comboBox;
    private DefaultComboBoxModel m_comboModel;
    
    DienstEinteilenPanel() {
        initLayout();
    }
    
    void initLayout() {
        // Tabelle initialisieren
        String[] spaltenNamen = { "Position", "Person", "Solist", "Stimmführer" };
        Object[][] daten = { {"Geige", "", new Boolean(false), new Boolean(true)},
                            {"Flöte", "", new Boolean(false), new Boolean(false)},
                            {"Kontrabass", "", new Boolean(true), new Boolean(false)}
                            };
        m_tableModel = new DefaultTableModel(daten, spaltenNamen);
        m_table = new JTable(m_tableModel);
        m_comboModel = new DefaultComboBoxModel();
        m_comboBox = new JComboBox(m_comboModel);
        m_table.getColumnModel().getColumn(1).setCellEditor(
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
          final Object[] ob =
                { "Musiker 1", "Musiker 2", "Musiker 3", "Musiker 4",
                  "Musiker 5" };
          final DefaultComboBoxModel comboModel =
                (DefaultComboBoxModel) comboBox.getModel();
          comboModel.removeAllElements();
          for (Object o : ob) {
            comboModel.addElement(o);
          }
 
          comboModel.setSelectedItem(ob[0]);
        }
      });
                
        // Tabelle formatieren
        m_table.setIntercellSpacing(new Dimension(5, 5));
        m_table.setRowHeight(24);
        m_table.setRowSelectionAllowed(false);
        m_table.setShowGrid(false);
        m_table.setShowHorizontalLines(true);
        
        m_table.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
        m_table.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
        m_scrollPane = new JScrollPane(m_table);
        
        add(m_scrollPane);
    }
    
    public static void main(String[] args) {
        DienstEinteilenPanel d = new DienstEinteilenPanel();
        d.setSize(400, 500);
        d.setVisible(true);
        d.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}