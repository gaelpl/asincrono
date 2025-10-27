package servidormulti;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class tiposDeMensajes {

    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private final comandosDAO comandos;
    private final String miIdHilo;
    private final boolean existe;
    private final login loginHandler;

    public tiposDeMensajes(UnCliente cliente, comandosDAO comandos, login loginHandler) {
        this.salida = cliente.salida;
        this.entrada = cliente.entrada;
        this.comandos = comandos;
        this.miIdHilo = cliente.nombreHilo;
        this.existe = cliente.existe;
        this.loginHandler = loginHandler;
    }

    public boolean manejarOpcionesChat(String mensaje) throws IOException {
        boolean mensajeValido = false;

        switch (mensaje) {
            case "1":
                mensajeValido = enviarMensajeGeneral();
                break;
            case "2":
                mensajeValido = enviarMensajePrivado();
                break;
            case "3":
                mensajeValido = enviarMensajeVarios();
                break;
            default:
                return false;
        }
        return mensajeValido;
    }

    public boolean enviarMensajeGeneral() throws IOException {
        this.salida.writeUTF("Escribe tu mensaje para todos");
        String mensaje = entrada.readUTF();
        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : miIdHilo;

        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.nombreHilo.equals(this.miIdHilo)) {
                continue;
            }

            try {
                boolean bloqueadoPorEmisor = comandos.estaBloqueadoPor(emisorDB, cliente.nombreHilo);
                boolean bloqueadoPorReceptor = comandos.estaBloqueadoPor(cliente.nombreHilo, emisorDB);

                if (bloqueadoPorEmisor || bloqueadoPorReceptor) {
                    continue;
                }

                cliente.salida.writeUTF("@" + loginHandler.getUsuarioAutenticado() + ": " + mensaje);
            } catch (SQLException e) {
                System.err.println("Error DB al verificar bloqueo para " + cliente.nombreHilo + ": " + e.getMessage());
            }
        }
        return true;
    }

    public boolean enviarMensajePrivado() throws IOException {
        this.salida.writeUTF("Escribe a quien quieres mandar mensaje (pon @NombreDeUsuario al inicio)");
        String destinatarioEntrada = entrada.readUTF();
        this.salida.writeUTF("Escribe tu mensaje");
        String contenidoMensaje = entrada.readUTF();
        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : miIdHilo;

        if (destinatarioEntrada.startsWith("@")) {
            String destinatarioNombre = destinatarioEntrada.trim().substring(1).split(" ")[0];

            try {
                String destinatarioHilo = comandos.obtenerIdHilo(destinatarioNombre);
                if (destinatarioHilo == null) {
                    this.salida.writeUTF("Error: El usuario @" + destinatarioNombre + " no está registrado.");
                    return false;
                }

                UnCliente cliente = ServidorMulti.clientes.get(destinatarioHilo);

                if (cliente == null) {
                    this.salida.writeUTF("Error: El usuario @" + destinatarioNombre + " está registrado pero no conectado.");
                    return false;
                }

                boolean bloqueadoPorEmisor = comandos.estaBloqueadoPor(emisorDB, destinatarioHilo);
                boolean bloqueadoPorReceptor = comandos.estaBloqueadoPor(destinatarioHilo, emisorDB);

                if (bloqueadoPorEmisor || bloqueadoPorReceptor) {
                    String mensajeFallo;
                    if (bloqueadoPorEmisor) {
                        mensajeFallo = "Fallo en el envío: Tú has bloqueado a @" + destinatarioNombre + ".";
                    } else {
                        mensajeFallo = "Fallo en el envío: @" + destinatarioNombre + " te tiene bloqueado.";
                    }
                    this.salida.writeUTF(mensajeFallo);
                } else {
                    cliente.salida.writeUTF("@" + loginHandler.getUsuarioAutenticado() + ": " + contenidoMensaje);
                }
            } catch (SQLException e) {
                this.salida.writeUTF("Error interno: Fallo en la base de datos al verificar el bloqueo.");
                System.err.println("Error SQL en privado: " + e.getMessage());
            }
            return true;
        } else {
            this.salida.writeUTF("Formato incorrecto para mensaje privado. Intenta usar @NombreDeUsuario.");
            return false;
        }
    }

    public boolean enviarMensajeVarios() throws IOException {
        this.salida.writeUTF("Escribe a quienes quieres mandar mensaje, pon @NombreDeUsuario separados por comas al inicio");
        String destinatariosEntrada = entrada.readUTF();
        this.salida.writeUTF("Escribe tu mensaje");
        String contenidoMensaje = entrada.readUTF();
        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : miIdHilo;

        if (destinatariosEntrada.startsWith("@")) {
            String[] nombresDestino = destinatariosEntrada.split(",");
            boolean enviadoAlmenosUno = false;

            try {
                for (String nombreCompleto : nombresDestino) {
                    String destinatarioNombre = nombreCompleto.substring(1).trim();
                    String destinatarioHilo = comandos.obtenerIdHilo(destinatarioNombre);

                    if (destinatarioHilo == null) {
                        this.salida.writeUTF("Advertencia: Usuario @" + destinatarioNombre + " no registrado. Ignorado.");
                        continue;
                    }

                    UnCliente cliente = ServidorMulti.clientes.get(destinatarioHilo);

                    if (cliente != null) {
                        boolean bloqueadoPorEmisor = comandos.estaBloqueadoPor(emisorDB, destinatarioHilo);
                        boolean bloqueadoPorReceptor = comandos.estaBloqueadoPor(destinatarioHilo, emisorDB);

                        if (!bloqueadoPorEmisor && !bloqueadoPorReceptor) {
                            cliente.salida.writeUTF("@" + loginHandler.getUsuarioAutenticado() + ": " + contenidoMensaje);
                            enviadoAlmenosUno = true;
                        } else {
                            this.salida.writeUTF("Fallo de entrega: @" + destinatarioNombre + " está restringido.");
                        }
                    } else {
                        this.salida.writeUTF("Advertencia: Usuario @" + destinatarioNombre + " no conectado. Ignorado.");
                    }
                }
            } catch (SQLException e) {
                this.salida.writeUTF("Error interno: Fallo en la base de datos al verificar bloqueos.");
                System.err.println("Error SQL en envío a varios: " + e.getMessage());
                return false;
            }
            return enviadoAlmenosUno;
        } else {
            this.salida.writeUTF("Formato incorrecto para mensaje a varios. Intenta usar @Nombre1,@Nombre2...");
            return false;
        }
    }
}