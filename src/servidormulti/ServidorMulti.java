package servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {

    static HashMap<String, UnCliente> clientes = new HashMap<String, UnCliente>();

    public static void main(String[] args) throws IOException {
        ServerSocket servidorSocket = new ServerSocket(8080);
        int contador = 0;
        while (true) {
            Socket s = servidorSocket.accept();
            // asigno nombre al hilo para que el mismo cliente sepa cual es su identificador
            String nombreHilo = Integer.toString(contador);
            UnCliente unCliente = new UnCliente(s, nombreHilo);
            // al hilo se le puede poner nombre con una coma y lo que sigue
            Thread hilo = new Thread(unCliente, "Cliente " + nombreHilo);
            clientes.put(Integer.toString(contador), unCliente);
            hilo.start();
            System.out.println("Se conecto el wey n: " + contador);
            contador++;
        }
    }
}
