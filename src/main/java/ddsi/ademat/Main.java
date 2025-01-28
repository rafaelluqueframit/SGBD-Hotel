package ddsi.ademat;

import java.sql.*;
import java.util.Scanner;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String url = dotenv.get("URL");
        String user = dotenv.get("USER");
        String password = dotenv.get("PASSWORD");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Conexión establecida con éxito a la base de datos");

            // Informar al usuario sobre el tratamiento de sus datos y sus derechos
            System.out.println(
                    "\n\nSus datos personales serán tratados conforme al Reglamento General de Protección de Datos (RGPD) y la Ley Orgánica 3/2018, de 5 de diciembre, de Protección de Datos Personales y garantía de los derechos digitales (LOPDGDD).");
            System.out.println(
                    "Puede consustar sus derechos en la Agencia Española de Protección de Datos (www.aepd.es).");

            // Desactivar auto-commit
            try {
                conn.setAutoCommit(false);
            } catch (SQLException e) {
                System.out.println("Error al desactivar auto-commit: " + e.getMessage());
            }

            boolean exit = false;
            Scanner scanner = new Scanner(System.in);

            while (!exit) {
                System.out.println("\n---- Menú Principal ----");
                System.out.println("1. Borrar y crear tablas");
                System.out.println("2. Subsistema de Gestión de Habitaciones");
                System.out.println("3. Subsistema de Facturación");
                System.out.println("4. Subsistema de Gestión de Trabajadores");
                System.out.println("5. Subsistema de Gestión de Suministros");
                System.out.println("6. Subsistema de Gestión de Clientes");
                System.out.println("7. Subsistema de Gestión de Servicios");
                System.out.println("8. Mostrar contenido de las tablas");
                System.out.println("0. Salir");

                System.out.print("Elige una opción: ");
                try {
                    while (!scanner.hasNextInt()) {
                        System.out.println("Introduce un número válido");
                        scanner.next();
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        borrarYCrearTablas(conn);
                        break;
                    case 2:
                        GestionReservas.bucleInteractivo(conn);
                        break;
                    case 3:
                        Facturacion.bucleInteractivo(conn);
                        break;
                    case 4:
                        GestionTrabajadores.bucleInteractivo(conn, scanner);
                        break;
                    case 5:
                        GestionSuministros.bucleInteractivo(conn);
                        break;
                    case 6:
                        GestionClientes.bucleInteractivo(conn);
                        break;
                    case 7:
                        Servicios.bucleOpciones(conn);
                        break;
                    case 8:
                        mostrarTablas(conn);
                        break;
                    case 0:
                        exit = true;
                        System.out.println("Cerrando aplicación...");
                        break;
                    default:
                        System.out.println("Opción inválida.");
                }
            }
            scanner.close();
        } catch (SQLException e) {
            System.out.println("Error en la conexión: " + e.getMessage());
        }
    }

    public static void borrarYCrearTablas(Connection conn) {
        GestionHotel.borrarTablas(conn);
        GestionClientes.crearTablas(conn);
        GestionReservas.crearTablas(conn);
        GestionTrabajadores.crearTablas(conn);
        Facturacion.crearTablas(conn);
        Servicios.crearTablas(conn);
        GestionSuministros.crearTablas(conn);
        try {
            conn.commit();
        } catch (SQLException e) {
            System.out.println("Error al hacer commit: " + e.getMessage());
        }
        System.out.println("Tablas borradas y creadas con éxito");
    }

    public static void mostrarTablas(Connection conn) {
        GestionClientes.mostrarTablas(conn);
        GestionReservas.mostrarTablas(conn);
        GestionTrabajadores.mostrarTablas(conn);
        Facturacion.mostrarTablas(conn);
        Servicios.mostrarTablas(conn);
        GestionSuministros.mostrarTablas(conn);
    }
}