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
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

public class GestionReservas extends javax.swing.JFrame {
    DefaultTableModel model;
    Connection conn;
    Statement sent; 
    
    private int idLive;
    private int idUsuario;
    private javax.swing.Timer temporizador;
    private java.util.Map<Integer, Long> tiemposRestantes =
        new java.util.HashMap<>();
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GestionReservas.class.getName());

    /**
     * Creates new form GestionReservas
     */
    public GestionReservas(int idLive, int idUsuario) {
        initComponents();
        conn=Mysql.getConnection();
        
        this.idLive = idLive;
        this.idUsuario = idUsuario;
        
        GestionLive gestion = new GestionLive();
        gestion.actualizarReservas();
        cargarReservas();
    }
    
    void cargarReservas() {
        try {
            if (temporizador != null) {
                temporizador.stop();
            }

            tiemposRestantes.clear();
            vencerReservasExpiradas();

            String[] titulos = {
                "ID", "Producto", "Cantidad",
                "Expira", "Estado", "Tiempo restante"
            };

            String sql = "SELECT a.id_apartado, p.nombre, a.cantidad, "
                    + "DATE_FORMAT(a.fecha_expiracion, '%H:%i') AS expiracion, "
                    + "a.estado, "
                    + "TIMESTAMPDIFF(SECOND, NOW(), a.fecha_expiracion) "
                    + "AS segundos_restantes "
                    + "FROM apartados a "
                    + "INNER JOIN productos p "
                    + "ON a.id_producto = p.id_producto "
                    + "WHERE a.id_cliente = ? "
                    + "AND a.id_live = ? "
                    + "ORDER BY a.fecha_apartado DESC";

            model = new DefaultTableModel(null, titulos);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                ps.setInt(2, idLive);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idApartado = rs.getInt("id_apartado");
                        String estado = rs.getString("estado");

                        long segundos = rs.getLong("segundos_restantes");

                        String tiempo = "-";

                        if ("ACTIVO".equalsIgnoreCase(estado)) {
                            segundos = Math.max(0, segundos);
                            tiemposRestantes.put(idApartado, segundos);
                            tiempo = formatoTiempo(segundos);
                        }

                        String[] fila = {
                            String.valueOf(idApartado),
                            rs.getString("nombre"),
                            rs.getString("cantidad"),
                            rs.getString("expiracion"),
                            estado,
                            tiempo
                        };

                        model.addRow(fila);
                    }
                }
            }

            jTable1.setModel(model);
            iniciarTemporizador();

        } catch (Exception e) {
            System.out.println("Error al cargar reservas: "
                    + e.getMessage());
        }
    }

    
    String formatoTiempo(long segundos) {
        long minutos = segundos / 60;
        long segundosRestantes = segundos % 60;

        return String.format("%02d:%02d",
            minutos, segundosRestantes);
    }
    
    void iniciarTemporizador() {
        temporizador = new javax.swing.Timer(1000, e -> {
            boolean actualizar = false;
            for (int i = 0; i < jTable1.getRowCount(); i++) {
                int idApartado = Integer.parseInt(
                        jTable1.getValueAt(i, 0).toString());
                
                String estado = jTable1.getValueAt(i, 4).toString();
                if (!estado.equalsIgnoreCase("ACTIVO")) {
                    continue;
                }
                long segundos = tiemposRestantes.get(idApartado);
                if (segundos > 0) {
                    segundos--;
                    tiemposRestantes.put(idApartado, segundos);
                    jTable1.setValueAt(formatoTiempo(segundos), i, 5);
                }
                if (segundos <= 0) {
                    actualizar = true;
                }
            }
            if (actualizar) {
                int filas = vencerReservasExpiradas();
                if (filas > 0) {
                    cargarReservas();
                }
            }
        });

        temporizador.start();
    }
    
    int vencerReservasExpiradas() {
        try {
            String sql = "UPDATE apartados "
                    + "SET estado = 'VENCIDO' "
                    + "WHERE id_cliente = ? "
                    + "AND id_live = ? "
                    + "AND estado = 'ACTIVO' "
                    + "AND fecha_expiracion <= NOW()";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ps.setInt(2, idLive);

            return ps.executeUpdate();

        } catch (Exception e) {
            System.out.println("Error al vencer reservas: "
                    + e.getMessage());
            return 0;
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
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

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

        jButton1.setText("COMPRAR");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton2.setText("LIBERAR");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        jButton3.setText("ATRAS");
        jButton3.addActionListener(this::jButton3ActionPerformed);

        jTextField1.setBorder(javax.swing.BorderFactory.createTitledBorder("NOMBRE DE USUARIO"));

        jLabel1.setText("GESTION DE RESERVAS");

        jButton4.setText("REFRESCAR");
        jButton4.addActionListener(this::jButton4ActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addGap(49, 49, 49)
                                .addComponent(jButton2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton4)
                                .addGap(50, 50, 50)
                                .addComponent(jButton3))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(181, 181, 181)
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel1)
                .addGap(16, 16, 16)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton4))
                .addGap(21, 21, 21))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int fila = jTable1.getSelectedRow();

        if (fila == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una reserva primero.");
            return;
        }

        int idApartado = Integer.parseInt(
            jTable1.getValueAt(fila, 0).toString()
        );

        String estado = jTable1.getValueAt(fila, 4).toString();

        if (!estado.equalsIgnoreCase("ACTIVO")) {
            JOptionPane.showMessageDialog(this,
                "Esta reserva ya no está activa.");
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro de que desea liberar esta reserva?",
            "Liberar reserva",
            JOptionPane.YES_NO_OPTION
        );

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String sql = "UPDATE apartados "
                + "SET estado = 'LIBERADO' "
                + "WHERE id_apartado = ? "
                + "AND id_cliente = ? "
                + "AND id_live = ? "
                + "AND estado = 'ACTIVO'";

            int filas;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idApartado);
            ps.setInt(2, idUsuario);
            ps.setInt(3, idLive);

            filas = ps.executeUpdate();
        }

        if (filas > 0) {
            JOptionPane.showMessageDialog(this,
                    "Reserva liberada correctamente.");

            cargarReservas();
        } else {
            JOptionPane.showMessageDialog(this,
                    "No se pudo liberar la reserva.");
        }

    } catch (Exception e) {
        System.out.println("Error al liberar reserva: "
                + e.getMessage());

        JOptionPane.showMessageDialog(this,
                "No se pudo liberar la reserva.");
    }
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        int fila = jTable1.getSelectedRow();

        if (fila == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una reserva primero.");
            return;
        }

        int idApartado = Integer.parseInt(
                jTable1.getValueAt(fila, 0).toString());

        String estado = jTable1.getValueAt(fila, 4).toString();

        if (!estado.equalsIgnoreCase("ACTIVO")) {
            JOptionPane.showMessageDialog(this,
                "Esta reserva ya no está activa.");
            return;
        }
    ComprarReserva ventana = new ComprarReserva(
            idApartado,idLive,idUsuario,this);
        ventana.setVisible(true);
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        cargarReservas();
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    if (temporizador != null) {
        temporizador.stop();
    }
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
        java.awt.EventQueue.invokeLater(() -> new GestionReservas(1,1).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
