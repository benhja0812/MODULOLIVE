/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dba;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
/**
 *
 * @author NEW
 */
public class GestionLive {
    
    private Connection conn;

    public GestionLive() {
        conn = Mysql.getConnection();
    }

    public void actualizarEstados() {
        try {

            String sqlProgramados =
                "UPDATE lives "
              + "SET estado = 'EN_CURSO' "
              + "WHERE estado = 'PROGRAMADO' "
              + "AND fecha = CURDATE() "
              + "AND hora_inicio <= CURTIME() "
              + "AND hora_fin > CURTIME()";

            try (PreparedStatement ps = conn.prepareStatement(sqlProgramados)) {
                ps.executeUpdate();
            }

            String sqlFinalizados =
                "SELECT id_live FROM lives "
              + "WHERE estado = 'EN_CURSO' "
              + "AND (fecha < CURDATE() "
              + "OR (fecha = CURDATE() AND hora_fin <= CURTIME()))";

            ArrayList<Integer> livesFinalizados = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(sqlFinalizados);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    livesFinalizados.add(rs.getInt("id_live"));
                }
            }

            for (int id : livesFinalizados) {
                finalizarLive(id, false);
            }
            
            String sqlProgramadosPasados =
                "UPDATE lives "
              + "SET estado = 'CERRADO' "
              + "WHERE estado = 'PROGRAMADO' "
              + "AND (fecha < CURDATE() "
              + "OR (fecha = CURDATE() AND hora_fin <= CURTIME()))";

            try (PreparedStatement ps = conn.prepareStatement(sqlProgramadosPasados)) {
                ps.executeUpdate();
            }

        } catch (Exception e) {
            System.out.println("Error al actualizar estados: " + e.getMessage());
        }
    }
    
    public int verificarStock(int idLive, int idProducto) {
        int cantidad = 0;

        try {
            String sql = "SELECT lp.cantidad - COALESCE(SUM(a.cantidad), 0) AS disponible "
                + "FROM live_productos lp "
                + "LEFT JOIN apartados a "
                + "ON lp.id_live = a.id_live "
                + "AND lp.id_producto = a.id_producto "
                + "AND a.estado = 'ACTIVO' "
                + "WHERE lp.id_live = ? "
                + "AND lp.id_producto = ? "
                + "GROUP BY lp.cantidad";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, idLive);
            ps.setInt(2, idProducto);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                cantidad = rs.getInt("disponible");
            }

        } catch (Exception e) {
            System.out.println("Error al verificar stock: "
                + e.getMessage());
        }

        return cantidad;
    }
    public boolean descontarStock(int idLive, int idProducto, int cantidad) {
        try {
            String sql = "UPDATE live_productos lp "
                + "SET lp.cantidad = lp.cantidad - ? "
                + "WHERE lp.id_live = ? "
                + "AND lp.id_producto = ? "
                + "AND lp.cantidad - ("
                + "SELECT COALESCE(SUM(a.cantidad), 0) "
                + "FROM apartados a "
                + "WHERE a.id_live = ? "
                + "AND a.id_producto = ? "
                + "AND a.estado = 'ACTIVO'"
                + ") >= ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, cantidad);
            ps.setInt(2, idLive);
            ps.setInt(3, idProducto);
            ps.setInt(4, idLive);
            ps.setInt(5, idProducto);
            ps.setInt(6, cantidad);

            int filas = ps.executeUpdate();

            return filas > 0;

        } catch (Exception e) {
            System.out.println("Error al descontar stock: "
                + e.getMessage());
            return false;
        }
    }
    public void actualizarReservas() {
        try {
            String sql = "UPDATE apartados "
                + "SET estado = 'VENCIDO' "
                + "WHERE estado = 'ACTIVO' "
                + "AND fecha_expiracion <= NOW()";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeUpdate();

        } catch (Exception e) {
            System.out.println("Error al actualizar reservas: "
                + e.getMessage());
        }
    }
    
    
    public boolean finalizarLive(int idLive, boolean cierreManual) {
        boolean autoCommitAnterior = true;

        try {
            autoCommitAnterior = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // Bloquear el Live para evitar compras simultáneas.
            String sqlEstado =
                "SELECT estado FROM lives "
              + "WHERE id_live = ? FOR UPDATE";

            try (PreparedStatement psEstado = conn.prepareStatement(sqlEstado)) {
                psEstado.setInt(1, idLive);

                try (ResultSet rs = psEstado.executeQuery()) {
                    if (!rs.next() || !"EN_CURSO".equals(rs.getString("estado"))) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Actualizar el estado del Live.
            String sqlFinalizar;

            if (cierreManual) {
                sqlFinalizar =
                    "UPDATE lives "
                  + "SET estado = 'CERRADO', hora_fin = CURTIME() "
                  + "WHERE id_live = ?";
            } else {
                sqlFinalizar =
                    "UPDATE lives "
                  + "SET estado = 'CERRADO' "
                  + "WHERE id_live = ?";
            }

            try (PreparedStatement psFinalizar = conn.prepareStatement(sqlFinalizar)) {
                psFinalizar.setInt(1, idLive);

                if (psFinalizar.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            // Registrar las salidas del inventario general.
            String sqlStock =
                "INSERT INTO movimientos_stock "
              + "(id_producto, tipo_movimiento, cantidad, fecha_hora, motivo) "
              + "SELECT dv.id_producto, 'SALIDA', SUM(dv.cantidad), "
              + "NOW(), 'Venta de Live' "
              + "FROM ventas v "
              + "INNER JOIN detalle_ventas dv "
              + "ON v.id_venta = dv.id_venta "
              + "WHERE v.id_live = ? "
              + "AND v.canal = 'LIVE' "
              + "GROUP BY dv.id_producto";

            try (PreparedStatement psStock = conn.prepareStatement(sqlStock)) {
                psStock.setInt(1, idLive);
                psStock.executeUpdate();
            }

            // Liberar apartados que todavía estén activos.
            String sqlApartados =
                "UPDATE apartados "
              + "SET estado = 'LIBERADO' "
              + "WHERE id_live = ? "
              + "AND estado = 'ACTIVO'";

            try (PreparedStatement psApartados = conn.prepareStatement(sqlApartados)) {
                psApartados.setInt(1, idLive);
                psApartados.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.out.println("Error en rollback: " + ex.getMessage());
            }

            System.out.println("Error al finalizar Live: " + e.getMessage());
            return false;

        } finally {
            try {
                conn.setAutoCommit(autoCommitAnterior);
            } catch (SQLException e) {
                System.out.println("Error al restaurar AutoCommit: " + e.getMessage());
            }
        }
    }

}
