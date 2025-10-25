package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;

public class login {
    private String usuarioAutenticado;
    private final comandosDAO comando = new comandosDAO();

    public boolean manejarLogin(DataOutputStream salida, DataInputStream entrada, String clienteIdHilo) throws IOException {
        salida.writeUTF("Introduce tu usuario:");
        String usuario = entrada.readUTF();
        salida.writeUTF("Introduce tu contrasena:");
        String contrasena = entrada.readUTF();


        try {
            String usuarioEncontrado = comando.autenticarUsuario(usuario, contrasena);

            if (usuarioEncontrado != null) {

                comando.actualizarIdHilo(usuarioEncontrado, clienteIdHilo);
                this.usuarioAutenticado = usuarioEncontrado;
                salida.writeUTF("Inicio de sesion exitoso. ¡Bienvenido " + usuarioEncontrado + "!");
                return true;
            } else {
                salida.writeUTF("Usuario o contrasena incorrectos.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos durante el login: " + e.getMessage());
            salida.writeUTF("Error interno del servidor al intentar autenticar. Inténtalo de nuevo.");
            return false;
        }
    }
    public String getUsuarioAutenticado() {
        return usuarioAutenticado;
    }
}
