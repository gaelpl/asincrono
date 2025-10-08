package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Registro implements Runnable {
    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;

    public Registro(Socket s) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        try {
            manejarRegistro();
        } catch (IOException e) {
            System.out.println("Error en el registro: " + e.getMessage());
        }

    }

    // metodo para manejar los registros de usuarios
    private void manejarRegistro() throws IOException {
        salida.writeUTF("Elige un usuario:");
        String nuevoUsuario = teclado.readLine();

        // buscamos si el usuario existe
        if (ServidorMulti.usuarios.containsKey(nuevoUsuario)) {
            salida.writeUTF("El usuario ya existe. Intenta iniciar sesion.");
        } else {
            salida.writeUTF("Elige una contrasena:");
            String nuevaContrasena = teclado.readLine();
            ServidorMulti.usuarios.put(nuevoUsuario, nuevaContrasena);
            // falta el metodo guardarUsuarios
            guardarUsuarios();
            salida.writeUTF("Registro exitoso. Ahora puedes iniciar sesion.");
        }
    }

    private void guardarUsuarios() {

    }

}
