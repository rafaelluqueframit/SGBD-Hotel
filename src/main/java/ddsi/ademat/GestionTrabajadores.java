package ddsi.ademat;

import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;

public class GestionTrabajadores {
    public static void crearTablas(Connection conn) {
        
        // Creamos la tabla Trabajador
        try {
            Statement stmt = conn.createStatement();
            GestionHotel.borrarTabla(conn, "Trabajador");
            stmt.executeUpdate("CREATE TABLE Trabajador ("
                + "dni CHAR(9) NOT NULL,"
                + "nombre VARCHAR(20) NOT NULL,"
                + "apellidos VARCHAR(50) NOT NULL,"
                + "domicilio VARCHAR(50) NOT NULL,"
                + "telefono VARCHAR(20) NOT NULL,"
                + "email VARCHAR(50) NOT NULL,"
                + "puesto VARCHAR(20) NOT NULL CHECK (puesto IN ('ADMINISTRADOR', 'RECEPCIONISTA', 'LIMPIADOR')),"
                + "nomina DECIMAL(10, 2) NOT NULL,"
                + "fecha_ultimo_aumento DATE DEFAULT SYSDATE,"
                + "PRIMARY KEY (dni)"
                + ")");
            conn.commit(); // Hacer commit después de crear la tabla
        } catch (SQLException e) {
            System.out.println("Error al crear la tabla Trabajador: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }

        // Creamos el disparador trg_verificar_sueldo
        try {
            Statement stmt = conn.createStatement();
            String triggerSQL = "CREATE OR REPLACE TRIGGER trg_verificar_sueldo "
            + "BEFORE INSERT OR UPDATE ON Trabajador "
            + "FOR EACH ROW "
            + "DECLARE "
            + "    salario_minimo CONSTANT NUMBER := 1134; "
            + "    fecha_actual DATE := SYSDATE; "
            + "BEGIN "
            + "    IF :NEW.nomina < salario_minimo THEN "
            + "        raise_application_error(-20601, 'El salario no puede ser inferior al salario mínimo interprofesional: ' || salario_minimo || ' euros'); "
            + "    END IF; "
            + "    IF ABS(MONTHS_BETWEEN(fecha_actual, :NEW.fecha_ultimo_aumento)) > 24 THEN "
            + "        raise_application_error(-20602, 'Han pasado más de 2 años sin que se modifique el sueldo del trabajador con DNI: ' || :NEW.dni); "
            + "    END IF; "
            + "    IF UPDATING AND :NEW.nomina = :OLD.nomina THEN "
            + "        IF ABS(MONTHS_BETWEEN(:OLD.fecha_ultimo_aumento, :NEW.fecha_ultimo_aumento)) > 24 THEN "
            + "            raise_application_error(-20602, 'Han pasado más de 2 años sin que se modifique el sueldo del trabajador con DNI: ' || :NEW.dni); "
            + "        END IF; "
            + "    END IF; "
            + "END;";

            // Ejecutar el código del disparador
            stmt.execute(triggerSQL);
            System.out.println("Disparador 'trg_verificar_sueldo' creado o reemplazado correctamente.");
            conn.commit(); // Hacer commit después de crear el disparador
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error al crear el disparador: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }

        // Insertamos dos trabajadores de ejemplo
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(
                "INSERT INTO Trabajador (dni, nombre, apellidos, domicilio, telefono, email, puesto, nomina, fecha_ultimo_aumento) VALUES ('12345678A', 'Juan', 'Cuesta', 'Calle Desengaño 21', '123456789', 'juan.perez@example.com', 'ADMINISTRADOR', 1500.00, TO_DATE('2025-01-01', 'YYYY-MM-DD'))");
            conn.commit(); // Hacer commit después de insertar cada fila
            stmt.executeUpdate(
                "INSERT INTO Trabajador (dni, nombre, apellidos, domicilio, telefono, email, puesto, nomina) VALUES ('87654321B', 'Ana', 'García', 'Avenida Andalucía 742', '987654321', 'ana.garcia@example.com', 'RECEPCIONISTA', 1200.00)");
            conn.commit(); // Hacer commit después de insertar cada fila
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error al insertar los trabajadores de ejemplo: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }
    }

    public static void bucleInteractivo(Connection conn, Scanner scanner) {

        boolean terminar = false;

        try {
            conn.commit(); // Hacer commit antes de empezar el bucle interactivo

            while (!terminar) {
                System.out.println("\n--- Menú de Gestión de Trabajadores ---");
                System.out.println("1. Dar de alta trabajador");
                System.out.println("2. Dar de baja trabajador");
                System.out.println("3. Modificar datos de trabajador");
                System.out.println("4. Consultar datos de trabajador");
                System.out.println("5. Mostrar listado de trabajadores");
                System.out.println("6. Deshacer cambios");
                System.out.println("0. Salir");

                System.out.print("Elige una opción: ");

                if (scanner.hasNextInt()) {
                    int choice = scanner.nextInt();
                    scanner.nextLine(); // Consumir el salto de línea

                    switch (choice) {
                        case 1:
                            insertarTrabajador(conn, scanner);
                            break;
                        case 2:
                            eliminarTrabajador(conn, scanner);
                            break;
                        case 3:
                            modificarTrabajador(conn, scanner);
                            break;
                        case 4:
                            consultarTrabajador(conn, scanner);
                            break;
                        case 5:
                            mostrarTablas(conn);
                            break;
                        case 6:
                            try {
                                conn.rollback();
                                System.out.println("Cambios deshechos.");
                            } catch (SQLException e) {
                                System.out.println("Error al hacer rollback: " + e.getMessage());
                            }
                            break;
                        case 0:
                            terminar = true;
                            conn.commit();
                            System.out.println("Saliendo del subsistema de Gestión de Trabajadores...");
                            break;
                        default:
                            System.out.println("Opción desconocida. Por favor, elige una opción válida.");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error en el bucle interactivo: " + e.getMessage());
        }
    }

    public static void insertarTrabajador(Connection conn, Scanner scanner) {
        try {
            System.out.println("Introduce los datos del trabajador:");
            System.out.print("DNI: ");
            String dni = scanner.nextLine();
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine();
            System.out.print("Apellidos: ");
            String apellidos = scanner.nextLine();
            System.out.print("Domicilio: ");
            String domicilio = scanner.nextLine();
            System.out.print("Teléfono: ");
            String telefono = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Puesto (ADMINISTRADOR, RECEPCIONISTA, LIMPIADOR): ");
            String puesto = scanner.nextLine();
            System.out.print("Fecha del último aumento (YYYY-MM-DD) [opcional]: ");
            String fechaUltimoAumentoStr = scanner.nextLine();

            Savepoint sp = conn.setSavepoint(); // Creamos un savepoint después de leer los datos
    
            boolean success = false;
            while (!success) {
                System.out.print("Nómina: ");
                double nomina = Double.parseDouble(scanner.nextLine());

                String sql = "INSERT INTO Trabajador (dni, nombre, apellidos, domicilio, telefono, email, puesto, nomina, fecha_ultimo_aumento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, dni);
                    pstmt.setString(2, nombre);
                    pstmt.setString(3, apellidos);
                    pstmt.setString(4, domicilio);
                    pstmt.setString(5, telefono);
                    pstmt.setString(6, email);
                    pstmt.setString(7, puesto);
                    pstmt.setDouble(8, nomina);
                    pstmt.setDate(9, fechaUltimoAumentoStr.isEmpty() ? Date.valueOf(LocalDate.now()) : Date.valueOf(fechaUltimoAumentoStr));
                    pstmt.executeUpdate();
                    // conn.commit();
                    System.out.println("Trabajador insertado correctamente.");
                    success = true;
                } catch (SQLException e) {
                    if (e.getErrorCode() == 20601 || e.getErrorCode() == 20602) {
                        System.out.println("Error del disparador: " + e.getMessage());
                        conn.rollback(sp);
                        System.out.println("Por favor, introduce un nuevo valor para la nómina.");
                    } else {
                        System.out.println("Error al insertar el trabajador: " + e.getMessage());
                        conn.rollback();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error al insertar el trabajador: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }
    }

    public static void eliminarTrabajador(Connection conn, Scanner scanner) {
        try {
            System.out.print("Introduce el DNI del trabajador a eliminar: ");
            String dni = scanner.nextLine();

            String sql = "DELETE FROM Trabajador WHERE dni = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, dni);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Trabajador eliminado correctamente.");
                } else {
                    System.out.println("No se encontró ningún trabajador con el DNI proporcionado.");
                }
            }

            // conn.commit();
        } catch (Exception e) {
            System.out.println("Error al eliminar el trabajador: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }
    }

    public static void modificarTrabajador(Connection conn, Scanner scanner) {
        try {
            System.out.print("Introduce el DNI del trabajador a modificar: ");
            String dni = scanner.nextLine();

            String checkSql = "SELECT COUNT(*) FROM Trabajador WHERE dni = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, dni);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                if (rs.getInt(1) == 0) {
                    System.out.println("No se encontró ningún trabajador con el DNI proporcionado.");
                    return;
                }
            }

            System.out.println("Introduce los nuevos datos del trabajador (dejar en blanco para no modificar):");
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine();
            System.out.print("Apellidos: ");
            String apellidos = scanner.nextLine();
            System.out.print("Domicilio: ");
            String domicilio = scanner.nextLine();
            System.out.print("Teléfono: ");
            String telefono = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Puesto (ADMINISTRADOR, RECEPCIONISTA, LIMPIADOR): ");
            String puesto = scanner.nextLine();

            Savepoint savepoint1 = conn.setSavepoint("Savepoint1"); // Creamos un savepoint después de leer los datos
    
            boolean success = false;
            while (!success) {

                StringBuilder sql = new StringBuilder("UPDATE Trabajador SET ");
                boolean first = true;

                if (!nombre.isEmpty()) {
                    sql.append("nombre = ?");
                    first = false;
                }
                if (!apellidos.isEmpty()) {
                    if (!first)
                        sql.append(", ");
                    sql.append("apellidos = ?");
                    first = false;
                }
                if (!domicilio.isEmpty()) {
                    if (!first)
                        sql.append(", ");
                    sql.append("domicilio = ?");
                    first = false;
                }
                if (!telefono.isEmpty()) {
                    if (!first)
                        sql.append(", ");
                    sql.append("telefono = ?");
                    first = false;
                }
                if (!email.isEmpty()) {
                    if (!first)
                        sql.append(", ");
                    sql.append("email = ?");
                    first = false;
                }
                if (!puesto.isEmpty()) {
                    if (!first)
                        sql.append(", ");
                    sql.append("puesto = ?");
                    first = false;
                }

                System.out.print("Nómina: ");
                String nominaStr = scanner.nextLine();
                if (!nominaStr.isEmpty()) {
                    if (!first)
                        sql.append(", ");
                    sql.append("nomina = ?, fecha_ultimo_aumento = ?");
                    first = false;
                }
    
                sql.append(" WHERE dni = ?");

                try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                    int index = 1;
                    if (!nombre.isEmpty()) pstmt.setString(index++, nombre);
                    if (!apellidos.isEmpty()) pstmt.setString(index++, apellidos);
                    if (!domicilio.isEmpty()) pstmt.setString(index++, domicilio);
                    if (!telefono.isEmpty()) pstmt.setString(index++, telefono);
                    if (!email.isEmpty()) pstmt.setString(index++, email);
                    if (!puesto.isEmpty()) pstmt.setString(index++, puesto);
                    if (!nominaStr.isEmpty()) {
                        pstmt.setString(index++, nominaStr);
                        pstmt.setDate(index++, new java.sql.Date(System.currentTimeMillis())); // fecha_ultimo_aumento
                    }
                    pstmt.setString(index, dni);

                    int out = pstmt.executeUpdate();
                    if (out != 0) {
                        System.out.println("Trabajador modificado correctamente.");
                        success = true;
                        // conn.commit();
                    } else {
                        System.out.println("No ha proporcionado ningún dato.");
                        conn.rollback();
                    }
                } catch (SQLException e) {
                    if (e.getErrorCode() == 20601 || e.getErrorCode() == 20602) {
                        System.out.println("Error del disparador: " + e.getMessage());
                        conn.rollback(savepoint1);
                        System.out.println("Por favor, introduce un nuevo valor para la nómina.");
                    } else {
                        System.out.println("Error al modificar el trabajador: " + e.getMessage());
                        try {
                            conn.rollback(); // Hacer rollback en caso de error
                        } catch (SQLException ex) {
                            System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error al modificar el trabajador: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }
    }

    public static void consultarTrabajador(Connection conn, Scanner scanner) {
        try {
            System.out.print("Introduce el DNI del trabajador a consultar: ");
            String dni = scanner.nextLine();

            String sql = "SELECT * FROM Trabajador WHERE dni = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, dni);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    System.out.println("DNI: " + rs.getString("dni"));
                    System.out.println("Nombre: " + rs.getString("nombre"));
                    System.out.println("Apellidos: " + rs.getString("apellidos"));
                    System.out.println("Domicilio: " + rs.getString("domicilio"));
                    System.out.println("Teléfono: " + rs.getString("telefono"));
                    System.out.println("Email: " + rs.getString("email"));
                    System.out.println("Puesto: " + rs.getString("puesto"));
                    System.out.println("Nómina: " + rs.getDouble("nomina"));
                    System.out.println("Fecha del último aumento: " + rs.getTimestamp("fecha_ultimo_aumento"));
                } else {
                    System.out.println("No se encontró ningún trabajador con el DNI proporcionado.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error al consultar el trabajador: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }
    }

    public static void mostrarTablas(Connection conn) {
        try {
            System.out.println("\n--- Contenido de Trabajadores ---");
            GestionHotel.mostrarTabla(conn, "Trabajador");
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Error al mostrar las tablas: " + e.getMessage());
            try {
                conn.rollback(); // Hacer rollback en caso de error
            } catch (SQLException ex) {
                System.out.println("Error al tratar de hacer rollback: " + ex.getMessage());
            }
        }
    }
}
