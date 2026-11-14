/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
import javax.swing.table.DefaultTableModel;
import dba.Mysql;
import java.sql.Array.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import dba.GestionLive;
/**
 *
 * @author NEW
 */
public class VentasLiveAnf extends javax.swing.JFrame {
 DefaultTableModel model;
 Connection conn;
 Statement sent; 
 private int idLive;
 private javax.swing.Timer temporizador;
 private java.util.Map<Integer, Long> tiemposRestantes =
        new java.util.HashMap<>();
 
 private java.util.Map<Integer, Integer> filasReservas =
        new java.util.HashMap<>();
 
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(VentasLiveAnf.class.getName());

    /**
     * Creates new form VentasLiveAnf
     */
    public VentasLiveAnf(int idLive) {       
        initComponents();
        conn=Mysql.getConnection();
        this.idLive = idLive;
        addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
            if (temporizador != null) {
                temporizador.stop();
        }
        }});
        
        cargarAnfitrion();
        cargarProductos();
        cargarVentas();
        jLabel1.setText("DIRECTO #" + idLive);
    }
    
    
    void cargarAnfitrion() {
        try {
            String sql = "SELECT u.nombre, u.apellido "
                       + "FROM lives l "
                       + "INNER JOIN usuarios u "
                       + "ON l.id_anfitrion = u.id_usuario "
                       + "WHERE l.id_live = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idLive);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String nombre = rs.getString("nombre");
                String apellido = rs.getString("apellido");

                jTextField1.setText(nombre + " " + apellido);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void cargarProductos() {
        try {
            String[] titulos = {
            "ID", "Producto", "Precio", "Cantidad"
            };

            String sql = "SELECT p.id_producto, p.nombre, lp.precio_live, "
            + "lp.cantidad - COALESCE(SUM(a.cantidad), 0) AS cantidad "
            + "FROM live_productos lp "
            + "INNER JOIN productos p "
            + "ON lp.id_producto = p.id_producto "
            + "LEFT JOIN apartados a "
            + "ON lp.id_live = a.id_live "
            + "AND lp.id_producto = a.id_producto "
            + "AND a.estado = 'ACTIVO' "
            + "WHERE lp.id_live = ? "
            + "GROUP BY p.id_producto, p.nombre, "
            + "lp.precio_live, lp.cantidad";

            model = new DefaultTableModel(null, titulos);

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idLive);

            ResultSet rs = ps.executeQuery();

            String[] fila = new String[4];

            while (rs.next()) {
                fila[0] = rs.getString("id_producto");
                fila[1] = rs.getString("nombre");
                fila[2] = String.format("%.2f", rs.getDouble("precio_live"));
                fila[3] = rs.getString("cantidad");

                model.addRow(fila);
            }

            jTable2.setModel(model);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void cargarVentas() {
        if (temporizador != null) {
            temporizador.stop();
        }
        tiemposRestantes.clear();
        filasReservas.clear();
        vencerReservasExpiradas();
        DefaultTableModel modelo = new DefaultTableModel(
            new String[]{
                "Tipo", "Cliente", "Producto", "Cantidad",
                "Precio", "Total", "Hora", "Expiración",
                "Tiempo restante", "Estado"
            }, 0);
        String sql =
            "SELECT * FROM ("
            + "SELECT "
            + "'VENTA' AS tipo, "
            + "c.usuario_plataforma AS cliente, "
            + "p.nombre AS producto, "
            + "dv.cantidad AS cantidad, "
            + "dv.precio_unitario AS precio, "
            + "dv.subtotal AS total, "
            + "DATE_FORMAT(v.fecha_hora, '%H:%i') AS hora, "
            + "NULL AS expiracion, "
            + "NULL AS estado, "
            + "NULL AS id_apartado, "
            + "0 AS segundos, "
            + "v.fecha_hora AS orden "

            + "FROM ventas v "
            + "INNER JOIN detalle_ventas dv "
            + "ON v.id_venta = dv.id_venta "
            + "INNER JOIN clientes c "
            + "ON v.id_cliente = c.id_cliente "
            + "INNER JOIN productos p "
            + "ON dv.id_producto = p.id_producto "
            + "WHERE v.id_live = ? "
            + "AND v.canal = 'LIVE' "

            + "UNION ALL "
            + "SELECT "
            + "'RESERVA' AS tipo, "
            + "c.usuario_plataforma AS cliente, "
            + "p.nombre AS producto, "
            + "a.cantidad AS cantidad, "
            + "lp.precio_live AS precio, "
            + "(a.cantidad * lp.precio_live) AS total, "
            + "DATE_FORMAT(a.fecha_apartado, '%H:%i') AS hora, "
            + "DATE_FORMAT(a.fecha_expiracion, '%H:%i') AS expiracion, "
            + "a.estado AS estado, "
            + "a.id_apartado AS id_apartado, "
            + "GREATEST(TIMESTAMPDIFF(SECOND, NOW(), "
            + "a.fecha_expiracion), 0) AS segundos, "
            + "a.fecha_apartado AS orden "

            + "FROM apartados a "
            + "INNER JOIN clientes c "
            + "ON a.id_cliente = c.id_cliente "
            + "INNER JOIN productos p "
            + "ON a.id_producto = p.id_producto "
            + "INNER JOIN live_productos lp "
            + "ON a.id_live = lp.id_live "
            + "AND a.id_producto = lp.id_producto "
            + "WHERE a.id_live = ? "
            + "AND a.estado IN ('ACTIVO', 'LIBERADO', 'VENCIDO') "
            + ") AS movimientos "
            + "ORDER BY orden";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, idLive);
            ps.setInt(2, idLive);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String tipo = rs.getString("tipo");
                String estado = rs.getString("estado");
                String tiempo = "-";

                if ("RESERVA".equals(tipo)
                        && "ACTIVO".equals(estado)) {

                    int idApartado = rs.getInt("id_apartado");
                    long segundos = rs.getLong("segundos");

                    tiemposRestantes.put(idApartado, segundos);
                    filasReservas.put(idApartado, modelo.getRowCount());
                    tiempo = formatoTiempo(segundos);
                }
                modelo.addRow(new Object[]{
                    tipo,
                    rs.getString("cliente"),
                    rs.getString("producto"),
                    rs.getInt("cantidad"),
                    String.format(java.util.Locale.US, "%.2f", rs.getDouble("precio")),
                    String.format(java.util.Locale.US, "%.2f", rs.getDouble("total")),
                    rs.getString("hora"),
                    rs.getString("expiracion") == null
                        ? "-" : rs.getString("expiracion"),
                    tiempo,
                    estado == null ? "-" : estado
                });
            }

            jTable1.setModel(modelo);
            calcularIngresos();
            iniciarTemporizador();

        } catch (Exception e) {
            System.out.println("Error al cargar movimientos: "
                    + e.getMessage());
        }
    }
    
    String formatoTiempo(long segundos) {
        long minutos = segundos / 60;
        long segundosRestantes = segundos % 60;

        return String.format("%02d:%02d",
            minutos, segundosRestantes);
    }
    
    int vencerReservasExpiradas() {
        String sql = "UPDATE apartados "
                + "SET estado = 'VENCIDO' "
                + "WHERE id_live = ? "
                + "AND estado = 'ACTIVO' "
                + "AND fecha_expiracion <= NOW()";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idLive);

            return ps.executeUpdate();
        } catch (Exception e) {

            System.out.println("Error al vencer reservas: "
                    + e.getMessage());
            return 0;
        }
    }
    
    void iniciarTemporizador() {
        if (temporizador != null) {
            temporizador.stop();
        }
        temporizador = new javax.swing.Timer(1000, e -> {
            boolean actualizarTabla = false;

            for (Integer idApartado :
                    new java.util.ArrayList<>(tiemposRestantes.keySet())) {
                long segundos = tiemposRestantes.get(idApartado) - 1;
                if (segundos <= 0) {

                    actualizarTabla = true;
                    break;
                } else {
                    tiemposRestantes.put(idApartado, segundos);
                    Integer fila = filasReservas.get(idApartado);

                    if (fila != null) {
                        jTable1.setValueAt(
                                formatoTiempo(segundos), fila, 8);
                    }
                }
            }
            if (actualizarTabla) {

                vencerReservasExpiradas();
                cargarVentas();
            }else {
                cargarProductos();
            }
        });
        temporizador.start();
    }
    void calcularIngresos() {
        double ingresos = 0;

        for (int i = 0; i < jTable1.getRowCount(); i++) {
            String tipo = jTable1.getValueAt(i, 0).toString();

            if (tipo.equals("VENTA")) {
                String total = jTable1.getValueAt(i, 5).toString();
                ingresos += Double.parseDouble(total);
            }
        }
        jTextField2.setText(String.format("%.2f", ingresos));
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jTextField2 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jTextField1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "NOMBRE ANFITRION", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12))); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(47, Short.MAX_VALUE)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 12, Short.MAX_VALUE)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

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

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton1.setText("REFRESCAR");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jTable2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12))); // NOI18N
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

        jLabel2.setText("TABLA DE PRODUCTOS");

        jLabel3.setText("TABLA DE VENTAS");

        jTextField2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "INGRESOS", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12))); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jButton2.setText("FINALIZAR DIRECTO");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        jButton3.setText("ATRAS");
        jButton3.addActionListener(this::jButton3ActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(43, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(81, 81, 81)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(93, 93, 93))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(200, 200, 200))))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(186, 186, 186)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(211, 211, 211)
                        .addComponent(jLabel3)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 444, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(57, 57, 57)
                        .addComponent(jButton2)
                        .addGap(63, 63, 63)
                        .addComponent(jButton3)
                        .addGap(26, 26, 26))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 505, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(27, 27, 27)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addGap(19, 19, 19))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        cargarProductos();
        cargarVentas();
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int opcion = JOptionPane.showConfirmDialog(
        this,
        "¿Está seguro de que desea finalizar este Live?",
        "Finalizar Live",
        JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            GestionLive gestionLive = new GestionLive();
            if (gestionLive.finalizarLive(idLive, true)) {

                JOptionPane.showMessageDialog(
                this,
                "El Live se ha finalizado correctamente."
             );
            if (temporizador != null) {
                temporizador.stop();
            }    
            ResumenLive resumen = new ResumenLive(idLive);
            resumen.setLocationRelativeTo(null);
            resumen.setVisible(true);

            }else{

            JOptionPane.showMessageDialog(
                this,
                "No se pudo finalizar el Live. "
              + "Compruebe su estado o revise los errores."
                );
            }
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        DirectosAnf directos = new DirectosAnf();
        directos.setVisible(true);
        this.dispose();
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new VentasLiveAnf(1).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
