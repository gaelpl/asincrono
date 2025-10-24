package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;

import JuegoDelGato.Juego;
import JuegoDelGato.Jugador;

public class UnCliente implements Runnable {
    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;
    public String nombreHilo; 
    private int intentos = 0;
    private final int intentosMaximos = 3;
    boolean existe = false;
    
    private final comandosDAO comandos = new comandosDAO();
    private ManejadorComandos manejador;
    private final login loginHandler = new login();

    UnCliente(Socket s, String nombreHilo) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.nombreHilo = nombreHilo;
        this.manejador = new ManejadorComandos(comandos, nombreHilo, salida); 
    }

    @Override
    public void run() {
        String mensaje = "";
        Registro registro = new Registro();
        
        try {
            while (true) {
                if (!existe && intentos >= intentosMaximos) {
                    salida.writeUTF("se te acabaron los mensajes, inicia sesion o registrate");
                    exigirLoginRegister(loginHandler, registro);
                }

                Juego juegoActivo = GestorJuegos.getJuegoActivo(this.nombreHilo);
                
                if (juegoActivo != null && juegoActivo.estaActivo()) {
                    manejarTurnoDeJuego(juegoActivo);
                    continue; 
                }

                boolean puedeMandar = existe || (intentos < intentosMaximos);

                if (puedeMandar) {
                    this.salida.writeUTF("Elige la opcion 1:si quieres mandar mensaje general, 2:un usuario en especifico, 3:varios usuarios, o escribe 'bloquear @ID' o 'desbloquear @ID', 'jugar @ID', 'aceptar @ID', 'perder'"); 
                    mensaje = entrada.readUTF();

                    if (manejarComandoJuego(mensaje, juegoActivo)) {
                        continue;
                    }

                    try {
                        if (manejador.manejarComandoDeBloqueo(mensaje)) {
                            continue; 
                        }
                    } catch (SQLException e) {
                        salida.writeUTF("Error interno: Fallo en la base de datos al procesar comando.");
                        System.err.println("Error SQL en comando de cliente: " + e.getMessage());
                        continue;
                    }
                } else {
                    this.salida.writeUTF("Solo puedes recibir mensajes. Por favor, autentícate para enviar.");
                    continue;
                }
                
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
                        salida.writeUTF("opcion no reconocida");
                        break;
                }

                if (mensajeValido && !existe) {
                    intentos++;
                    int restantes = intentosMaximos - intentos;
                    if (restantes > 0) {
                        this.salida.writeUTF(
                                "Tienes " + restantes + " mensajes restantes antes de requerir login/registro.");
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error en el hilo de cliente: " + ex.getMessage());
        } finally { 

            Juego juego = GestorJuegos.getJuegoActivo(this.nombreHilo);
            if (juego != null && juego.estaActivo()) {
                Jugador ganador = juego.forzarVictoria(this.nombreHilo);
                if (ganador != null) {
                    ganador.getCliente().salida.writeUTF("Ganaste la partida contra @" + this.nombreHilo + " por rendición/desconexión.");
                }
                GestorJuegos.terminarJuego(juego);
            }

            try {
                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
            } catch (IOException e) { /* Ignorar */ }
            ServidorMulti.clientes.remove(this.nombreHilo);
        }
    }

    private void exigirLoginRegister(login login, Registro registro) throws IOException {
        while (!existe) {
            salida.writeUTF("Bienvenido. Escribe 'login' para iniciar sesion o 'register' para crear cuenta.");
            String accion = entrada.readUTF();

            if (accion == null) {
                break;
            }
            if ("login".equalsIgnoreCase(accion)) {
                existe = login.manejarLogin(salida, entrada);
                if (existe) {
                    manejador.setUsuarioAutenticado(loginHandler.getUsuarioAutenticado());
                }
            } else if ("register".equalsIgnoreCase(accion)) {
                registro.manejarRegistro(salida, entrada);
            } else {
                salida.writeUTF("Accion no reconocida. Intenta de nuevo.");
            }
        }
        if (existe) {
            salida.writeUTF("¡Inicio de sesión exitoso! Puedes enviar mensajes ilimitados.");
        }
    }

    private boolean enviarMensajeGeneral() throws IOException {
        this.salida.writeUTF("Escribe tu mensaje para todos");
        String mensaje = entrada.readUTF();
        
        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : this.nombreHilo; 

        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.nombreHilo.equals(this.nombreHilo)) {
                continue; 
            }
            
            try {
                boolean bloqueadoPorEmisor = comandos.estaBloqueadoPor(emisorDB, cliente.nombreHilo); 

                boolean bloqueadoPorReceptor = comandos.estaBloqueadoPor(cliente.nombreHilo, emisorDB);
                
                if (bloqueadoPorEmisor || bloqueadoPorReceptor) {
                    continue; 
                }
                
                cliente.salida.writeUTF("@" + this.nombreHilo + ": " + mensaje);
            } catch (SQLException e) {
                System.err.println("Error DB al verificar bloqueo para " + cliente.nombreHilo + ": " + e.getMessage());
            }
        }
        return true;
    }

    private boolean enviarMensajePrivado() throws IOException {
        this.salida.writeUTF("Escribe a quien quieres mandar mensaje (pon @numeroDeUsuario al inicio)");
        String destinatarioEntrada = entrada.readUTF();
        this.salida.writeUTF("Escribe tu mensaje");
        String contenidoMensaje = entrada.readUTF();
        
        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : this.nombreHilo; 

        if (destinatarioEntrada.startsWith("@")) {
            String aQuien = destinatarioEntrada.trim().substring(1).split(" ")[0];
            UnCliente cliente = ServidorMulti.clientes.get(aQuien);
            
            if (cliente == null) {
                this.salida.writeUTF("Error: Usuario @" + aQuien + " no encontrado o desconectado.");
                return false;
            }

            try {
                boolean bloqueadoPorEmisor = comandos.estaBloqueadoPor(emisorDB, cliente.nombreHilo); 

                boolean bloqueadoPorReceptor = comandos.estaBloqueadoPor(cliente.nombreHilo, emisorDB);

                if (bloqueadoPorEmisor || bloqueadoPorReceptor) {
                    String mensajeFallo;
                    if (bloqueadoPorEmisor) {
                        mensajeFallo = "Fallo en el envío: Tú has bloqueado a @" + cliente.nombreHilo + ".";
                    } else {
                        mensajeFallo = "Fallo en el envío: @" + cliente.nombreHilo + " te tiene bloqueado.";
                    }
                    this.salida.writeUTF(mensajeFallo);
                } else {
                    cliente.salida.writeUTF("@" + this.nombreHilo + ": " + contenidoMensaje);
                }
            } catch (SQLException e) {
                this.salida.writeUTF("Error interno: Fallo en la base de datos al verificar el bloqueo.");
                System.err.println("Error SQL en privado: " + e.getMessage());
            }
            return true;
        } else {
            this.salida.writeUTF("Formato incorrecto para mensaje privado. Intenta usar @ID.");
            return false;
        }
    }

    private boolean enviarMensajeVarios() throws IOException {
        this.salida.writeUTF("Escribe a quienes quieres mandar mensaje, pon @numeroDeUsuario separados por comas al inicio");
        String destinatariosEntrada = entrada.readUTF();
        this.salida.writeUTF("Escribe tu mensaje");
        String contenidoMensaje = entrada.readUTF();
        
        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : this.nombreHilo; 

        if (destinatariosEntrada.startsWith("@")) {
            String[] partes = destinatariosEntrada.split(",");
            boolean enviadoAlmenosUno = false;
            
            for (int i = 0; i < partes.length; i++) {
                String aQuien = partes[i].substring(1).trim();
                UnCliente cliente = ServidorMulti.clientes.get(aQuien);

                if (cliente != null) {
                    try {
                        boolean bloqueadoPorEmisor = comandos.estaBloqueadoPor(emisorDB, cliente.nombreHilo); 

                        boolean bloqueadoPorReceptor = comandos.estaBloqueadoPor(cliente.nombreHilo, emisorDB);
                        
                        if (!bloqueadoPorEmisor && !bloqueadoPorReceptor) {
                            cliente.salida.writeUTF("@" + this.nombreHilo + ": " + contenidoMensaje);
                            enviadoAlmenosUno = true;
                        }
                    } catch (SQLException e) {
                        System.err.println("Error DB al verificar bloqueo en envío a varios: " + e.getMessage());
                    }
                }
            }
            return enviadoAlmenosUno;
        } else {
            this.salida.writeUTF("Formato incorrecto para mensaje a varios. Intenta usar @ID1,@ID2...");
            return false;
        }
    }
}