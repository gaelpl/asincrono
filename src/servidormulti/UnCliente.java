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
    String nombreHilo;

    UnCliente(Socket s, String nombreHilo) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.nombreHilo = nombreHilo;
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        String mensaje;
        boolean existe = false;
        login login = new login();
        Registro registro = new Registro();
        int intentos = 0;
        boolean denegado = false;
        // ahora el while va dentro del try
        try {

            while (true) {
                //primera verificacon si es que no existe y ya se le acabaron los mensajes anonimos
                if(!existe && intentos >=3){
                    salida.writeUTF("se te acabaron los mensajes, inicia sesion o registrate");
                }

                //si no existe lo fuerzo a que escoja una opcion
                while (!existe) {
                //logica para login o registro
                salida.writeUTF("Bienvenido. Escribe 'login' para iniciar sesion o 'register' para crear cuenta.");
                    String accion = entrada.readUTF();

                    if (accion == null) {
                        break;
                    }
                    if ("login".equalsIgnoreCase(accion)) {
                        existe = login.manejarLogin(salida, entrada);
                    } else if ("register".equalsIgnoreCase(accion)) {
                        registro.manejarRegistro(salida, entrada);
                    } else {
                        salida.writeUTF("Accion no reconocida. Intenta de nuevo.");
                    }
                }
                if(existe) {
                         salida.writeUTF("¡Inicio de sesión exitoso! Puedes enviar mensajes ilimitados.");
                    }
                }
                //logica para enviar mensajes
                this.   salida.writeUTF("Elige la opcion 1:si quieres mandar mensaje general, 2:un usuario en especifico, 3:varios usuarios?");
                mensaje = entrada.readUTF();
                switch (mensaje) {
                    case "1":
                        this.salida.writeUTF("Escribe tu mensaje para todos");
                        mensaje = entrada.readUTF();
                        for (UnCliente cliente : ServidorMulti.clientes.values()) {
                            // puedes obtener nombre del hilo con Thread.currentThread().getName()
                            // if(Thread.currentThread().getName() = cliente.salida) continue;
                            if (cliente.nombreHilo.equals(this.nombreHilo)) {
                                continue;
                            }

                            // continue;
                            cliente.salida.writeUTF("@" + this.nombreHilo + ": " + mensaje);
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
            }catch (Exception ex) {
        } 
        }
    }


