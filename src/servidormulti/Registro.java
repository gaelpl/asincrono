package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;

public class Registro {

        private final comandosDAO comando = new comandosDAO();

    // metodo para manejar los registros de usuarios
    // cambiamos el buferreader por data inputstream
    public void manejarRegistro(DataOutputStream salida, DataInputStream entrada, String clienteIdHilo) throws IOException {
        salida.writeUTF("Elige un usuario:");
        String nuevoUsuario = entrada.readUTF();

        try {
            if (comando.verificarUsuarioExistente(nuevoUsuario)) {
                salida.writeUTF("El usuario ya existe. Intenta iniciar sesion.");
            } else {
                salida.writeUTF("Elige una contrasena:");
                String nuevaContrasena = entrada.readUTF();
                
                boolean guardadoExitoso = comando.guardarNuevoUsuario(nuevoUsuario, nuevaContrasena, clienteIdHilo);
                
                if (guardadoExitoso) {
                    salida.writeUTF("Registro exitoso. Ahora puedes iniciar sesion.");
                } else {
                    salida.writeUTF("Error al guardar el usuario. Intenta de nuevo.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos durante el registro: " + e.getMessage());
            salida.writeUTF("Error interno del servidor al intentar registrar. Inténtalo de nuevo.");
        }
    }
}
