/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
import dba.GestionLive;
import javax.swing.table.DefaultTableModel;
import dba.Mysql;
import java.sql.Array.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import javax.swing.JOptionPane;
/**
 *
 * @author NEW
 */
public class ResumenLive extends javax.swing.JFrame {
    DefaultTableModel model;
    Connection conn;
    Statement sent;  
    
    private int idLive;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ResumenLive.class.getName());

    /**
     * Creates new form ResumenLive
     */
    public ResumenLive(int idLive) {
        initComponents();
        conn=Mysql.getConnection();
        
        this.idLive = idLive;
        jTextField1.setEditable(false);
        jTextField2.setEditable(false);
        jTextField3.setEditable(false);
        jTextField4.setEditable(false);
        cargarResumen();
    }
    void cargarResumen() {
        calcularUnidadesVendidas();
        calcularIngresoTotal();
        obtenerProductoEstrella();
        calcularDuracion();
        cargarDetalleProductos();
    }
    void calcularUnidadesVendidas() {
        String sql = "SELECT COALESCE(SUM(dv.cantidad), 0) AS total "
                   + "FROM ventas v "
                   + "INNER JOIN detalle_ventas dv "
                   + "ON v.id_venta = dv.id_venta "
                   + "WHERE v.id_live = ? AND v.canal = 'LIVE'";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLive);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    jTextField1.setText(
                        String.valueOf(rs.getInt("total"))
                    );
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al calcular las unidades vendidas: " + e.getMessage());
        }
    }

    void calcularIngresoTotal() {
        String sql = "SELECT COALESCE(SUM(dv.subtotal), 0) AS ingreso "
                   + "FROM ventas v "
                   + "INNER JOIN detalle_ventas dv "
                   + "ON v.id_venta = dv.id_venta "
                   + "WHERE v.id_live = ? AND v.canal = 'LIVE'";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLive);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double ingreso = rs.getDouble("ingreso");

                    jTextField2.setText(
                        String.format(java.util.Locale.US, "%.2f", ingreso)
                    );
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al calcular el ingreso total: " + e.getMessage());
        }
    }
    void obtenerProductoEstrella() {
        String sql = "SELECT p.nombre, SUM(dv.cantidad) AS cantidad_total "
                   + "FROM ventas v "
                   + "INNER JOIN detalle_ventas dv "
                   + "ON v.id_venta = dv.id_venta "
                   + "INNER JOIN productos p "
                   + "ON dv.id_producto = p.id_producto "
                   + "WHERE v.id_live = ? AND v.canal = 'LIVE' "
                   + "GROUP BY p.id_producto, p.nombre "
                   + "HAVING SUM(dv.cantidad) = ( "
                   + "    SELECT MAX(total) FROM ( "
                   + "        SELECT SUM(dv2.cantidad) AS total "
                   + "        FROM ventas v2 "
                   + "        INNER JOIN detalle_ventas dv2 "
                   + "        ON v2.id_venta = dv2.id_venta "
                   + "        WHERE v2.id_live = ? AND v2.canal = 'LIVE' "
                   + "        GROUP BY dv2.id_producto "
                   + "    ) AS productos_vendidos "
                   + ") "
                   + "ORDER BY p.nombre ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLive);
            ps.setInt(2, idLive);

            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder productos = new StringBuilder();

                while (rs.next()) {
                    if (productos.length() > 0) {
                        productos.append(", ");
                    }

                    productos.append(rs.getString("nombre"))
                             .append(" (")
                             .append(rs.getInt("cantidad_total"))
                             .append(")");
                }

                if (productos.length() > 0) {
                    jTextField3.setText(productos.toString());
                } else {
                    jTextField3.setText("Sin ventas");
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al obtener el producto estrella: " + e.getMessage());
        }
    }
    
    void calcularDuracion() {
        String sql = "SELECT fecha, hora_inicio, hora_fin "
                   + "FROM lives WHERE id_live = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLive);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.time.LocalDate fecha =
                        rs.getDate("fecha").toLocalDate();

                    java.time.LocalTime horaInicio =
                        rs.getTime("hora_inicio").toLocalTime();

                    java.time.LocalTime horaFin =
                        rs.getTime("hora_fin").toLocalTime();

                    java.time.LocalDateTime inicio =
                        java.time.LocalDateTime.of(fecha, horaInicio);

                    java.time.LocalDateTime fin =
                        java.time.LocalDateTime.of(fecha, horaFin);

                    java.time.Duration duracion =
                        java.time.Duration.between(inicio, fin);

                    long horas = duracion.toHours();
                    long minutos = duracion.toMinutesPart();

                    jTextField4.setText(
                        horas + " h " + minutos + " min"
                    );
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al calcular la duración: " + e.getMessage());
        }
    }
    
    void cargarDetalleProductos() {
        String sql = "SELECT p.nombre, SUM(dv.cantidad) AS cantidad_total "
                   + "FROM ventas v "
                   + "INNER JOIN detalle_ventas dv "
                   + "ON v.id_venta = dv.id_venta "
                   + "INNER JOIN productos p "
                   + "ON dv.id_producto = p.id_producto "
                   + "WHERE v.id_live = ? AND v.canal = 'LIVE' "
                   + "GROUP BY p.id_producto, p.nombre "
                   + "ORDER BY cantidad_total DESC";

        model = new DefaultTableModel(
            new Object[]{"Producto", "Cantidad vendida"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLive);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = {
                        rs.getString("nombre"),
                        rs.getInt("cantidad_total")
                    };

                    model.addRow(fila);
                }
            }

            jTable2.setModel(model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar el detalle de productos: " + e.getMessage());
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("RESUMEN DEL DIRECTO");

        jTextField1.setBorder(javax.swing.BorderFactory.createTitledBorder("UNIDADES VENDIDAS"));

        jTextField2.setBorder(javax.swing.BorderFactory.createTitledBorder("INGRESO TOTAL"));

        jTextField3.setBorder(javax.swing.BorderFactory.createTitledBorder("PRODUCTO ESTRELLA"));

        jTextField4.setBorder(javax.swing.BorderFactory.createTitledBorder("DURACIÓN LIVE"));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        jButton1.setText("ACEPTAR");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButton1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                                    .addComponent(jTextField2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextField3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 332, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(37, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(34, 34, 34)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        this.dispose();
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ResumenLive(1).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    // End of variables declaration//GEN-END:variables
}
