package ddsi.ademat;

import java.sql.*;
import java.util.Vector;

public class GestionHotel {
    // Función auxiliar para mostrar el contenido de una tabla
    public static void mostrarTabla(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT * FROM " + tableName;
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(metaData.getColumnName(i) + ": " + rs.getObject(i) + " | ");
                }
                System.out.println();
            }
        }
    }

    public static void borrarTablas(Connection conn) {
        Vector<String> tablas = new Vector<>();
        tablas.add("Incorpora");
        tablas.add("Factura");
        tablas.add("Reserva");
        tablas.add("Suplemento");
        tablas.add("Habitacion");
        tablas.add("Contrata");
        tablas.add("Actividad");
        tablas.add("Suministro");
        tablas.add("Trabajadores");
        tablas.add("Cliente");

        for (String tabla : tablas) {
            try {
                borrarTabla(conn, tabla);

            } catch (SQLException e) {
                System.out.println("Error al borrar la tabla " + tabla + ": " + e.getMessage());
            }
        }

    }

    public static void borrarTabla(Connection conn, String tableName) throws SQLException {
        String sql = "DROP TABLE " + tableName;
        Statement stmt = conn.createStatement();
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // Ignorar el error si la tabla no existe
            if (!e.getMessage().contains("ORA-00942")) {
                throw e;
            }
        }
    }
}