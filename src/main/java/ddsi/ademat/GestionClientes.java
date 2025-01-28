package ddsi.ademat;

import java.sql.*;
import java.util.Scanner;

import oracle.jdbc.proxy.annotation.Pre;

public class GestionClientes {

    /*
     * Función para comprobar que una cadena sea entera y que no salte una excepción
     * que acabe con la ejecución de nuestro programa
     */
    static boolean esEntero(String cadena) {
        try {
            Integer.parseInt(cadena);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static boolean esUnica(Statement stmt, String dni) {
        String sql = "select * from Cliente where dni = '" + dni + "'";
        boolean esUnica = true;
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                esUnica = false;
            }
        } catch (Exception e) {

        }
        return esUnica;
    }

    // private static final String[] VALID_RANGOS = {"Inicial", "Avanzado", "VIP", "Platino"};

    // private static boolean esRangoValido(String rango) {
    //     for (String validRango : VALID_RANGOS) {
    //         if (validRango.equals(rango)) {
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    public static void crearTablas(Connection conn) {
        

        try {
            Statement stmt = conn.createStatement();
            GestionHotel.borrarTabla(conn, "Trabajador");
            stmt.executeUpdate("CREATE TABLE Cliente ("
                    + "nombre VARCHAR(20),"
                    + "apellidos VARCHAR(40),"
                    + "telefono VARCHAR(20),"
                    + "dni VARCHAR(9),"
                    + "domicilio VARCHAR(60),"
                    + "email VARCHAR(30) CONSTRAINT email_clave_candidata UNIQUE NOT NULL,"
                    + "puntos INTEGER,"
                    + "rango VARCHAR(20),"
                    + "tarjeta VARCHAR(20),"
                    + "PRIMARY KEY (dni)"
                    + ")");
            stmt.executeUpdate(
                    "INSERT INTO Cliente (nombre, apellidos, telefono, dni, domicilio, email, puntos, rango, tarjeta) VALUES ('Rafael','Córdoba Lopez','684848493','28394823G','Mesones 54','rafacorlopg@gmail.com', 0, 'Inicial', '1234567890')");
            stmt.executeUpdate(
                    "INSERT INTO Cliente (nombre, apellidos, telefono, dni, domicilio, email, puntos, rango, tarjeta) VALUES ('Néstor','Martinez Saez','764665788','78943659L','Puentezuelas 12','nestormm@hotmail.es', 0, 'Inicial', '0987654321')");
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error al crear la tabla Cliente: " + e.getMessage());
            
        }

        // Create trigger to validate rango
        try {
            Statement stmt = conn.createStatement();
            String triggerSQL = "CREATE OR REPLACE TRIGGER trg_verificar_rango "
                    + "BEFORE INSERT OR UPDATE ON Cliente "
                    + "FOR EACH ROW "
                    + "BEGIN "
                    + "    IF :NEW.rango NOT IN ('Inicial', 'Avanzado', 'VIP', 'Platino') THEN "
                    + "        raise_application_error(-20681, 'El rango debe ser uno de los siguientes valores: Inicial, Avanzado, VIP, Platino'); "
                    + "    END IF; "
                    + "END;";

            stmt.execute(triggerSQL);
            System.out.println("Disparador 'trg_verificar_rango' creado o reemplazado correctamente.");
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error al crear el disparador: " + e.getMessage());
            
        }        
    }

    public static void mostrarTablas(Connection conn) {
        try {
            System.out.println("---- Contenido de Clientes ----");
            GestionHotel.mostrarTabla(conn, "Cliente");
        } catch (SQLException e) {
            System.out.println("Error al mostrar las tablas: " + e.getMessage());
        }
    }

    public static void bucleInteractivo(Connection conn) {
        // Hacer savepoints para poder descartar cambios
        Savepoint sp = null;
        
        try {
            sp = conn.setSavepoint();
        } catch (SQLException e) {
            System.out.println("Error al crear el savepoint: " + e.getMessage());
        }
        
        boolean terminar = false;
        Scanner scanner = new Scanner(System.in);

        while (!terminar) {
            System.out.println("\n--- Menú de Clientes ---");
            System.out.println("1. Dar de alta cliente");
            System.out.println("2. Dar de baja cliente");
            System.out.println("3. Consultar información de cliente");
            System.out.println("4. Modificar información de cliente");
            System.out.println("5. Consultar rango de cliente");
            System.out.println("6. Descartar cambios");
            System.out.println("0. Guardar y Salir");

            System.out.print("Elige una opción: ");
            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consumir el salto de línea

                switch (choice) {
                    case 1:
                        darAltaCliente(conn, scanner);
                        break;
                    case 2:
                        darBajaCliente(conn, scanner);
                        break;
                    case 3:
                        consultarCliente(conn, scanner);
                        break;
                    case 4:
                        modificarCliente(conn, scanner);
                        break;
                    case 5:
                        consultarRangoCliente(conn, scanner);
                        break;
                    case 6:
                        try {
                            conn.rollback(sp);
                            GestionHotel.mostrarTabla(conn, "Cliente");
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
                        System.out.println("Saliendo del subsistema de Cliente...");
                        break;
                    default:
                        System.out.println("Opción inválida.");
                }
            }
        }
    }

    public static void darAltaCliente(Connection conn, Scanner scanner) {
        String sql;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("\nPor favor indique el NOMBRE del Cliente: \n");
        String nombreCliente = scanner.nextLine();
        while (nombreCliente.length() > 20) {
            System.out.println("\nNombre no válido, intentelo de nuevo\n");
            nombreCliente = scanner.nextLine();
        }

        System.out.println("\nPor favor indique los APELLIDOS del Cliente: \n");
        String apellidoCliente = scanner.nextLine();
        while (apellidoCliente.length() > 40) {
            System.out.println("\nApellidos no válidos, intentelo de nuevo\n");
            apellidoCliente = scanner.nextLine();
        }

        System.out.println("\nPor favor indique el TELÉFONO del Cliente: \n");
        String telefono = scanner.nextLine();
        while (telefono.length() > 20 || (!esEntero(telefono) && telefono.length() > 1)) {
            System.out.println("\nTelefono no válido, intentelo de nuevo\n");
            telefono = scanner.nextLine();
        }

        System.out.println("\nPor favor indique el DNI del Cliente: \n");
        String dni = scanner.nextLine();
        while (dni.length() > 9 || dni.length() < 1) {
            System.out.println("\nDNI no válido, intentelo de nuevo\n");
            dni = scanner.nextLine();
        }

        try {
            PreparedStatement checkClienteStmt = conn.prepareStatement(
                "SELECT COUNT(*) AS cuenta FROM Cliente WHERE dni = ?");
            checkClienteStmt.setString(1, dni);
            ResultSet checkClienteRs = checkClienteStmt.executeQuery();

            if (checkClienteRs.next() && checkClienteRs.getInt("cuenta") != 0) {
                System.out.println("El DNI ya existe en la Base de Datos.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("\nPor favor indique el DOMICILIO del cliente: \n");
        String domicilio = scanner.nextLine();
        while (domicilio.length() > 60) {
            System.out.println("\nDomicilio no válido, intentelo de nuevo\n");
            domicilio = scanner.nextLine();
        }

        System.out.println("\nPor favor indique el CORREO del cliente: \n");
        String correo = scanner.nextLine();
        while (correo.length() > 30 || correo.isEmpty() || !esUnica(stmt, correo)) {
            System.out.println("\nCorreo no válido, ya existente o vacío, intentelo de nuevo\n");
            correo = scanner.nextLine();
        }

        System.out.println("\nPor favor indique los PUNTOS del cliente: \n");
        String puntos = scanner.nextLine();
        while (!esEntero(puntos)) {
            System.out.println("\nPuntos no válidos, intentelo de nuevo\n");
            puntos = scanner.nextLine();
        }

        System.out.println("\nPor favor indique el RANGO del cliente ('Inicial', 'Avanzado', 'VIP', 'Platino'): \n");
        String rango = scanner.nextLine();
        // while (!esRangoValido(rango)) {
        //     System.out.println("\nRango no válido, intentelo de nuevo ('Inicial', 'Avanzado', 'VIP', 'Platino')\n");
        //     rango = scanner.nextLine();
        // }

        System.out.println("\nPor favor indique la TARJETA del cliente: \n");
        String tarjeta = scanner.nextLine();
        while (tarjeta.length() > 20 || !esEntero(tarjeta)) {
            System.out.println("\nTarjeta no válida, intentelo de nuevo\n");
            tarjeta = scanner.nextLine();
        }

        sql = "INSERT INTO Cliente (nombre, apellidos, telefono, dni, domicilio, email, puntos, rango, tarjeta) VALUES ('"
                + nombreCliente + "','" + apellidoCliente + "','" + telefono + "','" + dni + "','" + domicilio + "','"
                + correo + "','" + puntos + "','" + rango + "','" + tarjeta + "')";
        try {
            stmt.executeUpdate(sql);
            System.out.println("\nCliente dado de alta correctamente. \n");
            GestionHotel.mostrarTabla(conn, "Cliente");
        } catch (SQLException e) {
            System.out.println("\nERROR: al añadir el cliente" + e.getMessage());
        }
    }

    public static void darBajaCliente(Connection conn, Scanner scanner) {

        String sql;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("\nPor favor indique el DNI del cliente a eliminar: \n");
        String DNI;
        DNI = scanner.nextLine();
        while (DNI.length() > 20) {
            System.out.println("\nDNI no válido, intentelo de nuevo\n");
            DNI = scanner.nextLine();
        }

        sql = "select * from Cliente where dni='" + DNI + "'";

        try {
            stmt.executeQuery(sql);
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("\nBorrando el cliente con DNI:" + rs.getString("DNI") + " " + " \n");
                sql = "delete from Cliente where DNI='" + DNI + "'";
                stmt.executeQuery(sql);
                System.out.println("\nCliente dado de baja correctamente.\n");
            } else {
                System.out.println("\nNo existe el Cliente\n");
            }
        } catch (Exception e) {
            System.out.println("\nERROR: El cliente no existe o hay problemas en la conexión\n");
        }

        try {
            stmt.executeUpdate(sql);
            System.out.println("\nCliente dado de baja correctamente. \n");
            GestionHotel.mostrarTabla(conn, "Cliente");
        } catch (SQLException e) {
            System.out.println("Error al eliminar el Cliente: " + e.getMessage());
        }
    }

    public static void consultarCliente(Connection conn, Scanner scanner) {
        String sql;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("\nPor favor indique el DNI del cliente a consultar: \n");
        String DNI = scanner.nextLine();
        while (DNI.length() > 20) {
            System.out.println("\nDNI no válido, intentelo de nuevo\n");
            DNI = scanner.nextLine();
        }

        sql = "select * from Cliente where dni='" + DNI + "'";

        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("\nNombre: " + rs.getString("nombre") + "\n");
                System.out.println("\nApellidos: " + rs.getString("apellidos") + "\n");
                System.out.println("\nTelefono: " + rs.getString("telefono") + "\n");
                System.out.println("\nDNI: " + rs.getString("dni") + "\n");
                System.out.println("\nDomicilio: " + rs.getString("domicilio") + "\n");
                System.out.println("\nCorreo: " + rs.getString("email") + "\n");
                System.out.println("\nPuntos: " + rs.getString("puntos") + "\n");
                System.out.println("\nRango: " + rs.getString("rango") + "\n");
                System.out.println("\nTarjeta: " + rs.getString("tarjeta") + "\n");
            } else {
                System.out.println("\nNo existe el Cliente\n");
            }
        } catch (Exception e) {
            System.out.println("ERROR: El cliente no existe o hay problemas en la conexión" + e.getMessage());
        }

        try {
            stmt.executeUpdate(sql);
            System.out.println("\nCliente mostrado correctamente. \n");
            GestionHotel.mostrarTabla(conn, "Cliente");
        } catch (SQLException e) {
            System.out.println("Error al consultar el cliente: " + e.getMessage());
            
        }
    }

    public static void modificarCliente(Connection conn, Scanner scanner) {
        String sql;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("\nPor favor indique el DNI del cliente a modificar: \n");
        String DNI;
        DNI = scanner.nextLine();
        while (DNI.length() > 9 || DNI.length() < 1) {
            System.out.println("\nDNI no válido, intentelo de nuevo\n");
            DNI = scanner.nextLine();
        }

        sql = "select * from Cliente where dni='" + DNI + "'";

        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("\nNombre: " + rs.getString("nombre") + "\n");
                System.out.println("\nApellidos: " + rs.getString("apellidos") + "\n");
                System.out.println("\nTelefono: " + rs.getString("telefono") + "\n");
                System.out.println("\nDNI: " + rs.getString("dni") + "\n");
                System.out.println("\nDomicilio: " + rs.getString("domicilio") + "\n");
                System.out.println("\nCorreo: " + rs.getString("email") + "\n");
                System.out.println("\nPuntos: " + rs.getString("puntos") + "\n");
                System.out.println("\nRango: " + rs.getString("rango") + "\n");
                System.out.println("\nTarjeta: " + rs.getString("tarjeta") + "\n");
            } else {
                System.out.println("\nNo existe el Cliente\n");
                return;
            }
        } catch (Exception e) {
            System.out.println("\nERROR: El cliente no existe o hay problemas en la conexión\n");
            return;
        }

        System.out.println("\nPor favor indique el NOMBRE del Cliente: \n");
        String nombreCliente = scanner.nextLine();
        if (nombreCliente.length() > 20) {
            System.out.println("\nNombre no válido, intentelo de nuevo\n");
            return;
        }

        System.out.println("\nPor favor indique los APELLIDOS del Cliente: \n");
        String apellidoCliente = scanner.nextLine();
        if (apellidoCliente.length() > 40) {
            System.out.println("\nApellidos no válidos, intentelo de nuevo\n");
            return;
        }

        System.out.println("\nPor favor indique el TELÉFONO del Cliente: \n");
        String telefono = scanner.nextLine();
        if (telefono.length() > 20 || (!esEntero(telefono) && telefono.length() > 1)) {
            System.out.println("\nTelefono no válido, intentelo de nuevo\n");
            return;
        }

        System.out.println("\nPor favor indique el DOMICILIO del cliente: \n");
        String domicilio = scanner.nextLine();
        if (domicilio.length() > 60) {
            System.out.println("\nDomicilio no válido, intentelo de nuevo\n");
            return;
        }

        System.out.println("\nPor favor indique el CORREO del cliente: \n");
        String correo = scanner.nextLine();
        if (correo.length() > 20 || correo.isEmpty() || (!esUnica(stmt, correo) && correo.length() > 1)) {
            System.out.println("\nCorreo no válido, ya existente o vacío, intentelo de nuevo\n");
            return;
        }

        System.out.println("\nPor favor indique los PUNTOS del cliente: \n");
        String puntos = scanner.nextLine();
        if (!esEntero(puntos)) {
            System.out.println("\nPuntos no válidos, intentelo de nuevo\n");
            return;
        }

        System.out.println("\nPor favor indique el RANGO del cliente ('Inicial', 'Avanzado', 'VIP', 'Platino'): \n");
        String rango = scanner.nextLine();
        // if (!esRangoValido(rango)) {
        //     System.out.println("\nRango no válido, intentelo de nuevo ('Inicial', 'Avanzado', 'VIP', 'Platino')\n");
        //     return;
        // }

        System.out.println("\nPor favor indique la TARJETA del cliente: \n");
        String tarjeta = scanner.nextLine();
        if (tarjeta.length() > 20 || !esEntero(tarjeta)) {
            System.out.println("\nTarjeta no válida, intentelo de nuevo\n");
            return;
        }

        sql = "UPDATE Cliente SET "
                + "nombre = '" + nombreCliente + "', "
                + "apellidos = '" + apellidoCliente + "', "
                + "telefono = '" + telefono + "', "
                + "domicilio = '" + domicilio + "', "
                + "email = '" + correo + "', "
                + "puntos = " + puntos + ", "
                + "rango = '" + rango + "', "
                + "tarjeta = '" + tarjeta + "' "
                + "WHERE dni = '" + DNI + "'";
        try {
            stmt.executeUpdate(sql);
            System.out.println("\nCliente modificado correctamente. \n");
            GestionHotel.mostrarTabla(conn, "Cliente");
        } catch (SQLException e) {
            System.out.println("\nERROR: al modificar el cliente" + e.getMessage());
        }
    }

    public static void consultarRangoCliente(Connection conn, Scanner scanner) {
        String sql;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("\nPor favor indique el DNI del cliente a consultar: \n");
        String DNI;
        DNI = scanner.nextLine();
        while (DNI.length() > 20) {
            System.out.println("\nDNI no válido, intentelo de nuevo\n");
            DNI = scanner.nextLine();
        }

        sql = "select * from Cliente where dni='" + DNI + "'";

        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("\nRango: " + rs.getString("rango") + "\n");
            } else {
                System.out.println("\nNo existe el Cliente\n");
            }
        } catch (Exception e) {
            System.out.println("\nERROR: El cliente no existe o hay problemas en la conexión\n");
        }

        try {
            stmt.executeUpdate(sql);
            System.out.println("\nRango del Cliente consultado correctamente. \n");
            GestionHotel.mostrarTabla(conn, "Cliente");
        } catch (SQLException e) {
            System.out.println("Error al consultar el Rango: " + e.getMessage());           
        }
    }
}

// //INSERTAR AQUI DATOS CLIENTE
// sql = "INSERT INTO cliente VALUES ('Rafael','Cordoba
// Lopez','684848493','28394823G','','Mesones 54','rafacorlopg@gmail.com', 0,
// 'Inicial')";
// stmt.executeQuery(sql);
// sql = "INSERT INTO cliente VALUES ('Nestor','Martinez
// Saez','764665788','78943659L','','Puentezuelas 12','nestormm@hotmail.es', 0,
// 'Inicial')";
// stmt.executeQuery(sql);
// sql = "INSERT INTO cliente VALUES ('Luis','Bonilla
// Perez','656874677','7852279D','','Camino de Ronda 133','luisbp@gmail.com', 0,
// 'Inicial')";
// stmt.executeQuery(sql);
// sql = "INSERT INTO cliente VALUES ('Marta','Ruiz
// Gomez','638572999','78482227Y','','Recogidas 42','martarg@gmail.com', 0,
// 'Inicial')";
// stmt.executeQuery(sql);
// sql = "INSERT INTO cliente VALUES ('Manuel','Fuertes
// Gonzalez','649837468','28846380R','','Arabial 23','manuelfg@hotmail.com', 0,
// 'Inicial')";
// stmt.executeQuery(sql);