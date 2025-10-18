package servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServidorMulti {

    static Map<String, UnCliente> clientes = Collections.synchronizedMap(new HashMap<>());
    private static SQLite manejador;

    public static SQLite getManejador() {
        return manejador;
    }

    public static void main(String[] args) throws IOException {
        manejador = new SQLite();

        ServerSocket servidorSocket = new ServerSocket(8080);
        int contador = 0;

        while (true) {
            Socket s = servidorSocket.accept();
            String nombreHilo = Integer.toString(contador);
            UnCliente unCliente = new UnCliente(s, nombreHilo);

            Thread hilo = new Thread(unCliente, "Cliente " + nombreHilo);

            clientes.put(Integer.toString(contador), unCliente);
            hilo.start();

            System.out.println("Se conecto el wey n: " + contador);
            contador++;
        }
    }
}