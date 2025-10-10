package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Registro {
    // metodo para manejar los registros de usuarios
    // cambiamos el buferreader por data inputstream
    public void manejarRegistro(DataOutputStream salida, DataInputStream entrada) throws IOException {
        salida.writeUTF("Elige un usuario:");
        String nuevoUsuario = entrada.readUTF();

        // buscamos si el usuario existe
        if (ServidorMulti.usuarios.containsKey(nuevoUsuario)) {
            salida.writeUTF("El usuario ya existe. Intenta iniciar sesion.");
        } else {
            salida.writeUTF("Elige una contrasena:");
            String nuevaContrasena = entrada.readUTF();
            ServidorMulti.usuarios.put(nuevoUsuario, nuevaContrasena);
            // falta el metodo guardarUsuarios
            guardarUsuarios();
            salida.writeUTF("Registro exitoso. Ahora puedes iniciar sesion.");
        }
    }

    private void guardarUsuarios() {

    }

}
