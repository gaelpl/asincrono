package servidormulti;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLite {
    //url de la base de datos
    private static final String URL = "jdbc:sqlite:datos.db";

    public SQLite() {
    try {
        //aqui pido cargar el driver JDBC
            Class.forName("org.sqlite.JDBC");
            System.out.println("Driver JDBC de SQLite cargado.");
            crearTablas();
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Driver JDBC de SQLite no encontrado. Asegúrate de tener el JAR en el classpath.");
        }
    
}

public Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void crearTablas() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS USUARIOS ("
                + "usuario TEXT PRIMARY KEY NOT NULL,"
                + "contrasena TEXT NOT NULL"
                + ");";

        String sqlBloqueos = "CREATE TABLE IF NOT EXISTS BLOQUEOS ("
                + "bloqueador TEXT NOT NULL,"
                + "bloqueado TEXT NOT NULL,"
                + "FOREIGN KEY (bloqueador) REFERENCES USUARIOS(usuario) ON DELETE CASCADE,"
                + "FOREIGN KEY (bloqueado) REFERENCES USUARIOS(usuario) ON DELETE CASCADE,"
                + "PRIMARY KEY (bloqueador, bloqueado)"
                + ");";
    
        //llamo al metodo conectar
    try (Connection conn = conectar();
            //PreparedStatement es un objeto que representa una sentencia SQL precompilada
             PreparedStatement pstmtUsuarios = conn.prepareStatement(sqlUsuarios);
             PreparedStatement pstmtBloqueos = conn.prepareStatement(sqlBloqueos)) {
            
            pstmtUsuarios.executeUpdate();
            pstmtBloqueos.executeUpdate();
            System.out.println("comandos ejecutados.");
            
        } catch (SQLException e) {
            System.err.println("Error FATAL al crear las tablas: " + e.getMessage());
        }
    }
}

