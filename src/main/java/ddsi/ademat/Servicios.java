package ddsi.ademat;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Servicios {
    public static void bucleOpciones(Connection conn) {
        boolean terminar = false;
        Scanner scanner = new Scanner(System.in);

        Savepoint sp = null;

        try {
            sp = conn.setSavepoint();
        } catch (SQLException e) {
            System.out.println("Error al crear el savepoint: " + e.getMessage());
        }

        while (!terminar) {
            System.out.println("\n--- Menú de Servicios ---");
            System.out.println("1. Añadir actividad");
            System.out.println("2. Eliminar actividad");
            System.out.println("3. Contratar actividad");
            System.out.println("4. Cancelar actividad");
            System.out.println("5. Mostrar todas actividades");
            System.out.println("6. Mostrar actividades con filtrado");
            System.out.println("7. Descartar cambios");
            System.out.println("0. Guardar y salir");

            System.out.print("Elige una opción: ");

            try {
                int opcion = scanner.nextInt();
                scanner.nextLine();

                switch (opcion) {
                    case 1:
                        registrarActividad(conn, scanner);
                        break;
                    case 2:
                        eliminarActividad(conn, scanner);
                        break;
                    case 3:
                        contratarActividad(conn, scanner);
                        break;
                    case 4:
                        cancelarActividad(conn, scanner);
                        break;
                    case 5:
                        mostrarActividadesCliente(conn);
                        break;
                    case 6:
                        mostrarActividadesConFiltrado(conn, scanner);
                        break;
                    case 7:
                        try {
                            conn.rollback(sp);
                            GestionHotel.mostrarTabla(conn, "Actividad");
                            GestionHotel.mostrarTabla(conn, "contrata");
                        } catch (SQLException e) {
                            System.out.println("Error al descartar los cambios: " + e.getMessage());
                        }
                        System.out.println("Cambios descartados.");
                        break;
                    case 0:
                        try {
                            conn.commit();
                        } catch (SQLException e) {
                            System.out.println("Error al guardar los cambios: " + e.getMessage());
                        }

                        terminar = true;
                        System.out.println("Saliendo del subsistema de Servicios...");
                        break;
                    default:
                        System.out.println("Opción inválida. Por favor, selecciona una opción válida.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Opción inválida. Por favor, introduce un número.");
                scanner.nextLine();
            }
        }
    }

    private static void registrarActividad(Connection conn, Scanner scanner) {
        try {
            System.out.print("Introduce el nombre de la actividad: ");
            String nombre = scanner.nextLine();
            System.out.print("Introduce el precio de la actividad: ");
            double precio = scanner.nextDouble();

            if (precio < 0) {
                System.out.println("El precio debe ser mayor o igual que 0.");
                return;
            }

            scanner.nextLine();
            System.out.print("Introduce la fecha y hora (DD/MM/YYYY HH:MM): ");
            String fechaHora = scanner.nextLine();
            System.out.print("Introduce el aforo máximo: ");
            int aforo = scanner.nextInt();

//            if (aforo <= 0) {
//                System.out.println("El aforo debe ser mayor que 0.");
//                return;
//            }

            String sql = "BEGIN " +
                    "INSERT INTO Actividad (nombre, precio, horario, aforo) " +
                    "VALUES (?, ?, TO_DATE(?, 'DD/MM/YYYY HH24:MI'), ?) " +
                    "RETURNING id INTO ?; " +
                    "END;";

            CallableStatement stmt = conn.prepareCall(sql);
            stmt.setString(1, nombre);
            stmt.setDouble(2, precio);
            stmt.setString(3, fechaHora);
            stmt.setInt(4, aforo);

            stmt.registerOutParameter(5, java.sql.Types.INTEGER);

            stmt.executeUpdate();

            int idGenerado = stmt.getInt(5);

            System.out.println("Actividad añadida correctamente con id: " + idGenerado);
        } catch (SQLException e) {
            System.out.println("Error al añadir la actividad: " + e.getMessage());
        }
    }

    private static void eliminarActividad(Connection conn, Scanner scanner) {
        try {
            System.out.println("Actividades creadas:");
            mostrarActividades(conn);

            System.out.print("Introduce el ID de la actividad a eliminar: ");
            int id = scanner.nextInt();

            PreparedStatement deleteContrataStmt = conn.prepareStatement("DELETE FROM contrata WHERE id = ?");
            deleteContrataStmt.setInt(1, id);
            deleteContrataStmt.executeUpdate();

            PreparedStatement deleteActividadStmt = conn.prepareStatement("DELETE FROM Actividad WHERE id = ?");
            deleteActividadStmt.setInt(1, id);
            int filasAfectadas = deleteActividadStmt.executeUpdate();

            if (filasAfectadas > 0) {
                System.out.println("Actividad eliminada correctamente.");
            } else {
                System.out.println("No se encontró ninguna actividad con ese ID.");
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar la actividad: " + e.getMessage());
        }
    }

    private static void contratarActividad(Connection conn, Scanner scanner) {
        try {
            System.out.println("Actividades disponibles:");
            mostrarActividadesCliente(conn);

            System.out.print("Introduce tu DNI: ");
            String dni = scanner.nextLine();

            PreparedStatement checkClienteStmt = conn.prepareStatement(
                    "SELECT COUNT(*) AS cuenta FROM Cliente WHERE dni = ?");
            checkClienteStmt.setString(1, dni);
            ResultSet checkClienteRs = checkClienteStmt.executeQuery();

            if (checkClienteRs.next() && checkClienteRs.getInt("cuenta") == 0) {
                System.out.println("El DNI no está registrado como cliente. No es posible contratar actividades.");
                return;
            }

            System.out.print("Introduce el nombre de la actividad a contratar: ");
            String nombreActividad = scanner.nextLine();

            PreparedStatement checkDniStmt = conn.prepareStatement(
                    "SELECT COUNT(*) AS cuenta FROM contrata WHERE dni = ?");
            checkDniStmt.setString(1, dni);
            ResultSet checkDniRs = checkDniStmt.executeQuery();

            if (checkDniRs.next() && checkDniRs.getInt("cuenta") > 0) {
                System.out.println("Ya tienes contratada una actividad. No puedes contratar otra.");
                return;
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, aforo - (SELECT COUNT(*) FROM contrata WHERE contrata.id = Actividad.id) AS aforo_disponible "
                            +
                            "FROM Actividad WHERE nombre = ?");
            stmt.setString(1, nombreActividad);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int idActividad = rs.getInt("id");
                int aforoDisponible = rs.getInt("aforo_disponible");

                if (aforoDisponible > 0) {
                    stmt = conn.prepareStatement("INSERT INTO contrata (dni, id) VALUES (?, ?)");
                    stmt.setString(1, dni);
                    stmt.setInt(2, idActividad);
                    stmt.executeUpdate();

                    System.out.println("Actividad contratada correctamente.");
                } else {
                    System.out.println("El aforo de esta actividad está completo. No es posible contratarla.");
                }
            } else {
                System.out.println("No se encontró una actividad con ese nombre.");
            }
        } catch (SQLException e) {
            System.out.println("Error al contratar la actividad: " + e.getMessage());
        }
    }

    private static void cancelarActividad(Connection conn, Scanner scanner) {
        try {
            System.out.print("Introduce tu DNI: ");
            String dni = scanner.nextLine();

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM contrata WHERE dni = ?");
            stmt.setString(1, dni);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Actividad cancelada correctamente.");
            } else {
                System.out.println("No se encontró ninguna actividad contratada por ese DNI.");
            }
        } catch (SQLException e) {
            System.out.println("Error al cancelar la actividad: " + e.getMessage());
        }
    }

    private static void mostrarActividades(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, nombre, precio, horario, aforo FROM Actividad");

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Nombre: " + rs.getString("nombre") + ", Precio: "
                        + rs.getDouble("precio") + ", Horario: " + rs.getString("horario") + ", Aforo: "
                        + rs.getInt("aforo"));
            }
        } catch (SQLException e) {
            System.out.println("Error al mostrar actividades: " + e.getMessage());
        }
    }

    private static void mostrarActividadesCliente(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT nombre, precio, horario, aforo FROM Actividad");

            while (rs.next()) {
                System.out.println("Nombre: " + rs.getString("nombre") + ", Precio: " + rs.getDouble("precio")
                        + ", Horario: " + rs.getString("horario") + ", Aforo: " + rs.getInt("aforo"));
            }
        } catch (SQLException e) {
            System.out.println("Error al mostrar actividades: " + e.getMessage());
        }
    }

    private static void mostrarActividadesConFiltrado(Connection conn, Scanner scanner) {
        try {
            System.out.print("Introduce el criterio de filtrado (e.g., nombre, precio): ");
            String criterio = scanner.nextLine();
            System.out.print("Introduce el valor para filtrar: ");
            String valor = scanner.nextLine();

            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Actividad WHERE " + criterio + " = ?");
            stmt.setString(1, valor);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Nombre: " + rs.getString("nombre") + ", Precio: " + rs.getDouble("precio") + ", Horario: " + rs.getString("horario") + ", Aforo: " + rs.getInt("aforo"));
            }
        } catch (SQLException e) {
            System.out.println("Error al mostrar actividades filtradas: " + e.getMessage());
        }
    }

    public static void crearTablas(Connection conn) {
        try {
            Statement stmt = conn.createStatement();

            // Tabla Actividad
            String crearTablaActividad = """
            CREATE TABLE Actividad (
                id NUMBER GENERATED BY DEFAULT AS IDENTITY,
                nombre VARCHAR2(50) NOT NULL,
                precio NUMBER(10, 2) NOT NULL,
                horario DATE NOT NULL,
                aforo NUMBER NOT NULL,
                PRIMARY KEY (id)
            )
            """;
            stmt.executeUpdate(crearTablaActividad);

            // Tabla Contrata
            String crearTablaContrata = """
            CREATE TABLE contrata (
                dni VARCHAR2(20) NOT NULL,
                id NUMBER NOT NULL,
                PRIMARY KEY (dni, id),
                FOREIGN KEY (dni) REFERENCES Cliente(dni),
                FOREIGN KEY (id) REFERENCES Actividad(id)
            )
            """;
            stmt.executeUpdate(crearTablaContrata);

            // Crear el disparador
            String triggerServicios = """
            CREATE OR REPLACE TRIGGER trg_validar_actividad
            BEFORE INSERT ON Actividad
            FOR EACH ROW
            BEGIN
                IF :NEW.aforo <= 0 THEN
                    RAISE_APPLICATION_ERROR(-60001, 'El aforo debe ser mayor que 0.');
                END IF;

                IF :NEW.horario < SYSDATE THEN
                    RAISE_APPLICATION_ERROR(-60002, 'La fecha debe ser igual o posterior a la actual.');
                END IF;
            END;
            """;
            stmt.execute(triggerServicios);

            // Valores de prueba
            String datosPrueba = """
            BEGIN
                INSERT INTO Actividad (nombre, precio, horario, aforo) 
                VALUES ('Yoga', 10.0, TO_DATE('06/07/2025 10:00', 'DD/MM/YYYY HH24:MI'), 10);
                
                INSERT INTO Actividad (nombre, precio, horario, aforo) 
                VALUES ('Pilates', 15.0, TO_DATE('06/07/2025 12:00', 'DD/MM/YYYY HH24:MI'), 15);
                
                INSERT INTO Actividad (nombre, precio, horario, aforo) 
                VALUES ('Spinning', 20.0, TO_DATE('06/07/2025 14:00', 'DD/MM/YYYY HH24:MI'), 20);
            END;
        """;
            stmt.executeUpdate(datosPrueba);

            stmt.close();

        } catch (SQLException e) {
            System.out.println("Error al crear las tablas: " + e.getMessage());
        }
    }

    public static void mostrarTablas(Connection conn) {
        try {
            System.out.println("Contenido de Actividad:");
            GestionHotel.mostrarTabla(conn, "Actividad");
        } catch (SQLException e) {
            System.out.println("Error al mostrar las tablas: " + e.getMessage());
        }
    }
}