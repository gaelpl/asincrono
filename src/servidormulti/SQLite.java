package servidormulti;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLite {
    // url de la base de datos
    private static final String URL = "jdbc:sqlite:datos.db";

    public SQLite() {
        try {
            // aqui pido cargar el driver JDBC
            Class.forName("org.sqlite.JDBC");
            System.out.println("Driver JDBC de SQLite cargado.");
            crearTablas();
        } catch (ClassNotFoundException e) {
            System.err
                    .println("Error: Driver JDBC de SQLite no encontrado. Asegúrate de tener el JAR en el classpath.");
        }

    }

    public Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void crearTablas() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS USUARIOS ("
                + "usuario TEXT PRIMARY KEY NOT NULL,"
                + "contrasena TEXT NOT NULL,"
                + "id_hilo TEXT"
                + ");";

        String sqlBloqueos = "CREATE TABLE IF NOT EXISTS BLOQUEOS ("
                + "bloqueador TEXT NOT NULL,"
                + "bloqueado TEXT NOT NULL,"
                + "FOREIGN KEY (bloqueador) REFERENCES USUARIOS(usuario) ON DELETE CASCADE,"
                + "FOREIGN KEY (bloqueado) REFERENCES USUARIOS(usuario) ON DELETE CASCADE,"
                + "PRIMARY KEY (bloqueador, bloqueado)"
                + ");";

        String sqlRanking = "CREATE TABLE IF NOT EXISTS RANKING ("
                + "usuario TEXT PRIMARY KEY NOT NULL,"
                + "puntos INTEGER DEFAULT 0,"
                + "victorias INTEGER DEFAULT 0,"
                + "derrotas INTEGER DEFAULT 0,"
                + "empates INTEGER DEFAULT 0,"
                + "FOREIGN KEY (usuario) REFERENCES USUARIOS(usuario) ON DELETE CASCADE"
                + ");";

        String sqlHistorial = "CREATE TABLE IF NOT EXISTS HISTORIAL_JUEGOS ("
                + "usuario_a TEXT NOT NULL,"
                + "usuario_b TEXT NOT NULL,"
                + "ganador TEXT NOT NULL,"
                + "FOREIGN KEY (usuario_a) REFERENCES USUARIOS(usuario) ON DELETE CASCADE,"
                + "FOREIGN KEY (usuario_b) REFERENCES USUARIOS(usuario) ON DELETE CASCADE"
                + ");";

        String sqlGrupos = "CREATE TABLE IF NOT EXISTS GRUPOS ("
                + "nombre TEXT PRIMARY KEY NOT NULL"
                + ");";

        String sqlMembresia = "CREATE TABLE IF NOT EXISTS MEMBRESIA ("
                + "usuario TEXT NOT NULL,"
                + "grupo TEXT NOT NULL,"
                + "ultimo_mensaje_visto INTEGER DEFAULT 0,"
                + "PRIMARY KEY (usuario, grupo),"
                + "FOREIGN KEY (usuario) REFERENCES USUARIOS(usuario) ON DELETE CASCADE,"
                + "FOREIGN KEY (grupo) REFERENCES GRUPOS(nombre) ON DELETE CASCADE"
                + ");";

        String sqlMensajes = "CREATE TABLE IF NOT EXISTS MENSAJES ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "grupo TEXT NOT NULL,"
                + "emisor TEXT NOT NULL,"
                + "contenido TEXT NOT NULL,"
                + "fecha INTEGER NOT NULL,"
                + "FOREIGN KEY (grupo) REFERENCES GRUPOS(nombre) ON DELETE CASCADE"
                + ");";
                
        // llamo al metodo conectar
        try (Connection conn = conectar();
                PreparedStatement pstmtUsuarios = conn.prepareStatement(sqlUsuarios);
                PreparedStatement pstmtBloqueos = conn.prepareStatement(sqlBloqueos);
                PreparedStatement pstmtRanking = conn.prepareStatement(sqlRanking);
                PreparedStatement pstmtHistorial = conn.prepareStatement(sqlHistorial);
                PreparedStatement pstmtGrupos = conn.prepareStatement(sqlGrupos);
                PreparedStatement pstmtMembresia = conn.prepareStatement(sqlMembresia);
                PreparedStatement pstmtMensajes = conn.prepareStatement(sqlMensajes);
                PreparedStatement pstmtInsertTodos = conn.prepareStatement("INSERT OR IGNORE INTO GRUPOS (nombre) VALUES ('Todos')")) {
            // 3. Ejecutar todos los comandos CREATE TABLE
            pstmtUsuarios.executeUpdate();
            pstmtBloqueos.executeUpdate();
            pstmtRanking.executeUpdate();
            pstmtHistorial.executeUpdate();
            pstmtGrupos.executeUpdate();
            pstmtMembresia.executeUpdate();
            pstmtMensajes.executeUpdate();

            pstmtInsertTodos.executeUpdate();

            System.out.println("Esquema de base de datos y grupo 'Todos' creados exitosamente.");

        } catch (SQLException e) {
            System.err.println("Error FATAL al crear las tablas: " + e.getMessage());
        }
    }
}
