package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class login {
    private String usuarioAutenticado;

    public boolean manejarLogin(DataOutputStream salida, DataInputStream entrada) throws IOException {
        salida.writeUTF("Introduce tu usuario:");
        String usuario = entrada.readUTF();
        salida.writeUTF("Introduce tu contrasena:");
        String contrasena = entrada.readUTF();

        if (ServidorMulti.usuarios.containsKey(usuario) && ServidorMulti.usuarios.get(usuario).equals(contrasena)) {
            this.usuarioAutenticado = usuario;
            salida.writeUTF("Inicio de sesion exitoso. ¡Bienvenido " + usuario + "!");
            return true;
        } else {
            salida.writeUTF("Usuario o contrasena incorrectos.");
            return false;
        }
    }

    public String getUsuarioAutenticado() {
        return usuarioAutenticado;
    }
}
