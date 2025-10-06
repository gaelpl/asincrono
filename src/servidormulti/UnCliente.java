package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class UnCliente implements Runnable {
    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;

    UnCliente(Socket s) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());

    }

    @Override
    public void run() {
        String mensaje;
        while (true) {
            try {
                this.salida.writeUTF(
                        "Elige la opcion 1:si quieres mandar mensaje general, 2:un usuario en especifico, 3:varios usuarios?");
                mensaje = entrada.readUTF();
                switch (mensaje) {
                    case "1":
                        this.salida.writeUTF("Escribe tu mensaje para todos");
                        mensaje = entrada.readUTF();
                        for (UnCliente cliente : ServidorMulti.clientes.values()) {
                            // puedes obtener nombre del hilo con Thread.currentThread().getName()
                            cliente.salida.writeUTF("@" + Thread.currentThread().getName() + ": " + mensaje);
                        }
                        break;

                    case "2":
                        this.salida.writeUTF("Escribe a quien quieres mandar mensaje (pon @numeroDeUsuario al inicio)");
                        mensaje = entrada.readUTF();
                        this.salida.writeUTF("Escribe tu mensaje");
                        String contenidoMensaje = entrada.readUTF();
                        if (mensaje.startsWith("@")) {
                            String[] partes = mensaje.split(" ");
                            String aQuien = partes[0].substring(1);
                            UnCliente cliente = ServidorMulti.clientes.get(aQuien);
                            cliente.salida.writeUTF("@" + Thread.currentThread().getName() + ": " + contenidoMensaje);
                        } else {
                            for (UnCliente cliente : ServidorMulti.clientes.values()) {
                                cliente.salida.writeUTF(contenidoMensaje);
                            }
                        }
                        break;

                    case "3":
                        this.salida.writeUTF(
                                "Escribe a quienes quieres mandar mensaje, pon @numeroDeUsuario separados por comas al inicio");
                        mensaje = entrada.readUTF();
                        this.salida.writeUTF("Escribe tu mensaje");
                        contenidoMensaje = entrada.readUTF();
                        if (mensaje.startsWith("@")) {
                            String[] partes = mensaje.split(",");
                            for (int i = 0; i < partes.length; i++) {
                                String aQuien = partes[i].substring(1);
                                UnCliente cliente = ServidorMulti.clientes.get(aQuien);
                                cliente.salida
                                        .writeUTF("@" + Thread.currentThread().getName() + ": " + contenidoMensaje);
                            }
                        } else {
                            for (UnCliente cliente : ServidorMulti.clientes.values()) {
                                cliente.salida.writeUTF(contenidoMensaje);
                            }
                        }

                        break;

                    default:
                        break;
                }
            } catch (Exception ex) {
            }
        }
    }

}