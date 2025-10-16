package servidormulti;
import java.sql.Connection;
import java.sql.DriverManager;
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

private Connection conectar() throws SQLException {
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
    }
}
