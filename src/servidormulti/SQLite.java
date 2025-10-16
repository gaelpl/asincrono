package servidormulti;

public class SQLite {
    //url de la base de datos
    private static final String URL = "jdbc:sqlite:datos.db";

    try {
        //aqui pido cargar el driver JDBC
            Class.forName("org.sqlite.JDBC");
            System.out.println("Driver JDBC de SQLite cargado.");
            crearTablas(); //metodo todavia no implementado
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Driver JDBC de SQLite no encontrado. Asegúrate de tener el JAR en el classpath.");
        }
    

}
