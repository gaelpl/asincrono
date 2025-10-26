package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;
import JuegoDelGato.manejadorDeJuegos;
import JuegoDelGato.Juego;
import JuegoDelGato.Jugador;
import JuegoDelGato.Movimiento;

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
    private static manejadorDeJuegos juegosManager = new manejadorDeJuegos();

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

                Juego juegoActivo = juegosManager.getJuegoActivo(this.nombreHilo);

                if (juegoActivo != null && juegoActivo.estaActivo()) {
                    manejarTurnoDeJuego(juegoActivo);
                    continue;
                }

                boolean puedeMandar = existe || (intentos < intentosMaximos);

                if (puedeMandar) {
                    this.salida.writeUTF(
                            "Elige la opcion 1:si quieres mandar mensaje general, 2:un usuario en especifico, 3:varios usuarios, o escribe 'bloquear @ID' o 'desbloquear @ID', 'jugar @ID', 'aceptar @ID', 'perder'");
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

            Juego juego = juegosManager.getJuegoActivo(this.nombreHilo);
            if (juego != null && juego.estaActivo()) {
                Jugador ganador = juego.forzarVictoria(this.nombreHilo);
                if (ganador != null) {
                    try {
                        ganador.getCliente().salida.writeUTF(
                                "Ganaste la partida contra @" + this.nombreHilo + " por rendición/desconexión.");
                    } catch (IOException e) {
                        System.err.println("Advertencia: El ganador (@" + ganador.getIdHilo()
                                + ") se desconectó antes de recibir la notificación de victoria.");
                    }
                }
                juegosManager.terminarPartida(juego);
            }

            try {
                if (entrada != null)
                    entrada.close();
                if (salida != null)
                    salida.close();
            } catch (IOException e) {
                /* Ignorar */ }
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
                existe = login.manejarLogin(salida, entrada, this.nombreHilo);
                if (existe) {
                    manejador.setUsuarioAutenticado(loginHandler.getUsuarioAutenticado());
                }
            } else if ("register".equalsIgnoreCase(accion)) {
                registro.manejarRegistro(salida, entrada, this.nombreHilo);
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
        this.salida.writeUTF("Escribe a quien quieres mandar mensaje (pon @NombreDeUsuario al inicio)");

        String destinatarioEntrada = entrada.readUTF();
        this.salida.writeUTF("Escribe tu mensaje");
        String contenidoMensaje = entrada.readUTF();

        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : this.nombreHilo;
        String displayNombre = existe ? loginHandler.getUsuarioAutenticado() : this.nombreHilo;

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
                    this.salida.writeUTF(
                            "Error: El usuario @" + destinatarioNombre + " está registrado pero no conectado.");
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
                    cliente.salida.writeUTF("@" + displayNombre + ": " + contenidoMensaje);
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

    private boolean enviarMensajeVarios() throws IOException {
        this.salida.writeUTF(
                "Escribe a quienes quieres mandar mensaje, pon @NombreDeUsuario separados por comas al inicio");

        String destinatariosEntrada = entrada.readUTF();
        this.salida.writeUTF("Escribe tu mensaje");
        String contenidoMensaje = entrada.readUTF();

        String emisorDB = existe ? loginHandler.getUsuarioAutenticado() : this.nombreHilo;
        String displayNombre = existe ? loginHandler.getUsuarioAutenticado() : this.nombreHilo;

        if (destinatariosEntrada.startsWith("@")) {
            String[] nombresDestino = destinatariosEntrada.split(",");
            boolean enviadoAlmenosUno = false;

            try {
                for (String nombreCompleto : nombresDestino) {
                    String destinatarioNombre = nombreCompleto.substring(1).trim();

                    String destinatarioHilo = comandos.obtenerIdHilo(destinatarioNombre);

                    if (destinatarioHilo == null) {
                        this.salida
                                .writeUTF("Advertencia: Usuario @" + destinatarioNombre + " no registrado. Ignorado.");
                        continue;
                    }

                    UnCliente cliente = ServidorMulti.clientes.get(destinatarioHilo);

                    if (cliente != null) {
                        boolean bloqueadoPorEmisor = comandos.estaBloqueadoPor(emisorDB, destinatarioHilo);
                        boolean bloqueadoPorReceptor = comandos.estaBloqueadoPor(destinatarioHilo, emisorDB);

                        if (!bloqueadoPorEmisor && !bloqueadoPorReceptor) {
                            cliente.salida.writeUTF("@" + displayNombre + ": " + contenidoMensaje);
                            enviadoAlmenosUno = true;
                        } else {
                            this.salida.writeUTF("Fallo de entrega: @" + destinatarioNombre + " está restringido.");
                        }
                    } else {
                        this.salida
                                .writeUTF("Advertencia: Usuario @" + destinatarioNombre + " no conectado. Ignorado.");
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

    private boolean manejarComandoJuego(String comandoCompleto, Juego juegoActivo) throws IOException {
        String[] partes = comandoCompleto.trim().split(" ");
        String accion = partes[0].toLowerCase();

        if (accion.equals("perder")) {
            if (juegoActivo != null && juegoActivo.estaActivo()) {
                Jugador ganador = juegoActivo.forzarVictoria(this.nombreHilo);
                salida.writeUTF("Has abandonado la partida. Pierdes automáticamente.");
                if (ganador != null) {
                    try {
                        ganador.getCliente().salida
                                .writeUTF("Has ganado la partida contra @" + this.nombreHilo + " por rendición.");
                    } catch (IOException e) {
                        System.err.println("Advertencia: No se pudo notificar al ganador por error de red.");
                    }
                }
                juegosManager.terminarPartida(juegoActivo);
                return true;
            } else {
                salida.writeUTF("Error: No estás en ninguna partida activa para rendirte.");
                return true;
            }
        }

        String nombreDestino = partes.length > 1 && partes[1].startsWith("@") ? partes[1].substring(1) : null;

        if (!accion.equals("jugar") && !accion.equals("aceptar")) {
            return false;
        }
        if (nombreDestino == null) {
            salida.writeUTF("Error: Debes especificar un usuario con @NombreDeUsuario.");
            return true;
        }

        String idHiloDestino;
        try {
            idHiloDestino = comandos.obtenerIdHilo(nombreDestino);

            if (idHiloDestino == null) {
                salida.writeUTF("Error: El usuario @" + nombreDestino + " no está registrado en el sistema.");
                return true;
            }
            if (!ServidorMulti.clientes.containsKey(idHiloDestino)) {
                salida.writeUTF("Error: El usuario @" + nombreDestino + " está registrado pero no conectado.");
                return true;
            }
        } catch (SQLException e) {
            salida.writeUTF("Error interno al buscar el ID de usuario en la base de datos.");
            System.err.println("Error SQL al traducir nombre: " + e.getMessage());
            return true;
        }
        if (accion.equals("jugar")) {
            return manejarPropuesta(idHiloDestino, nombreDestino);
        } else if (accion.equals("aceptar")) {
            return manejarAceptar(idHiloDestino, nombreDestino);
        }
        return false;
    }

    private boolean manejarPropuesta(String idHiloDestino, String nombreDestino) throws IOException {
        String miIdHilo = this.nombreHilo;
        String miNombre = loginHandler.getUsuarioAutenticado();
        UnCliente clienteDestino = ServidorMulti.clientes.get(idHiloDestino);

        if (idHiloDestino.equals(this.nombreHilo)) {
            this.salida.writeUTF("Error: No puedes jugar contigo mismo.");
            return true;
        }
        if (juegosManager.tienePartida(miIdHilo, idHiloDestino)|| juegosManager.getJuegoActivo(idHiloDestino) != null) {
            this.salida.writeUTF("Error: Ya tienes una partida con @" + nombreDestino + " o él está ocupado.");
            return true;
        }
        juegosManager.registrarSolicitud(miIdHilo, idHiloDestino);
        clienteDestino.salida.writeUTF("El usuario @" + miNombre + " te reta. Escribe 'aceptar @"
                + miNombre + "' para aceptar.");
        this.salida.writeUTF("Reto enviado a @" + nombreDestino + ". Esperando respuesta...");

        return true;
    }

    private boolean manejarAceptar(String idHiloRetador, String nombreRetador) throws IOException {
        String miIdHilo = this.nombreHilo;
        String miNombre = loginHandler.getUsuarioAutenticado();
        String idEmisor = juegosManager.getSolicitudPendiente(miIdHilo);

        if (idEmisor == null || !idEmisor.equals(idHiloRetador)) {
            this.salida.writeUTF("Error: No tienes una solicitud de juego pendiente de @" + nombreRetador + ".");
            return true;
        }

        UnCliente clienteRetador = ServidorMulti.clientes.get(idHiloRetador);
        Jugador yo = new Jugador(this, ' ');
        Jugador retador = new Jugador(clienteRetador, ' ');

        if (clienteRetador != null && juegosManager.iniciarPartida(retador, yo)) {

            Juego juego = juegosManager.obtenerPartida(miIdHilo, idHiloRetador);
            Jugador yoConMarca = juego.getJugador(miIdHilo);
            Jugador retadorConMarca = juego.getJugador(idHiloRetador);
            Jugador turno = juego.getTurnoActual();
            String nombreRetadorReal = nombreRetador;
            String nombreTurno = turno.getIdHilo().equals(miIdHilo) ? miNombre : nombreRetadorReal;

            String msgInicio = "Partida iniciada con @" + nombreRetadorReal + ". Eres la marca '"+ yoConMarca.getMarca()+ "'.";
            String msgInicioRetador = "Partida iniciada con @" + miNombre + ". Eres la marca '"+ retadorConMarca.getMarca() + "'.";

            this.salida.writeUTF(msgInicio);
            clienteRetador.salida.writeUTF(msgInicioRetador);

            String turnoMsg = "Es el turno de @" + nombreTurno + " (" + turno.getMarca() + ")."+ juego.obtenerEstadoTablero();
            this.salida.writeUTF(turnoMsg);
            clienteRetador.salida.writeUTF(turnoMsg);

            juegosManager.removerSolicitud(miIdHilo);
            return true;
        } else {
            this.salida.writeUTF("Error al iniciar partida. El retador se desconectó o ya existe una partida.");
            return true;
        }
    }

    private void manejarTurnoDeJuego(Juego juego) throws IOException {
        Jugador jugadorActual = juego.getJugador(this.nombreHilo);
        Jugador oponente = juego.getContrincante(jugadorActual);

        if (!juego.getTurnoActual().getIdHilo().equals(this.nombreHilo)) {
            salida.writeUTF("Esperando movimiento de @" + juego.getTurnoActual().getIdHilo() + " ("+ juego.getTurnoActual().getMarca() + ")... Tablero:" + juego.obtenerEstadoTablero());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        salida.writeUTF("¡Es tu turno, " + jugadorActual.getMarca()+ "! Ingresa tu movimiento (fila, columna - ej: 1,2):" + juego.obtenerEstadoTablero());
        String input = entrada.readUTF();

        if (input.equalsIgnoreCase("perder")) {
            manejarComandoJuego("perder", juego);
            return;
        }

        try {
            String[] coords = input.trim().split(",");
            if (coords.length != 2)
                throw new IllegalArgumentException("Formato incorrecto. Deben ser dos números separados por coma.");

            int fila = Integer.parseInt(coords[0].trim());
            int columna = Integer.parseInt(coords[1].trim());
            String resultado = juego.procesarMovimiento(jugadorActual, new Movimiento(fila, columna));

            if (resultado.equals("VALIDO")) {
                String msgTablero = juego.obtenerEstadoTablero();
                String msgTurno = "Turno de @" + juego.getTurnoActual().getIdHilo() + " ("+ juego.getTurnoActual().getMarca() + ")";

                salida.writeUTF("Movimiento exitoso. " + msgTurno + msgTablero);
                oponente.getCliente().salida.writeUTF("Movimiento de @" + jugadorActual.getIdHilo() + " ("+ jugadorActual.getMarca() + "): " + msgTurno + msgTablero);

            } else if (resultado.equals("VICTORIA") || resultado.equals("EMPATE")) {
                String resultadoFinal = resultado.equals("VICTORIA") ? "¡VICTORIA! Has ganado la partida."
                        : "¡EMPATE! El tablero está lleno.";
                String msgTablero = juego.obtenerEstadoTablero();

                salida.writeUTF(resultadoFinal + msgTablero);
                oponente.getCliente().salida.writeUTF(
                        juego.getGanador() != null ? "DERROTA. @" + jugadorActual.getIdHilo() + " ganó." + msgTablero
                                : "EMPATE." + msgTablero);

                juegosManager.terminarPartida(juego);

            } else {
                salida.writeUTF("Error de juego: " + resultado.replace("ERROR_", ""));
            }

        } catch (NumberFormatException ex) {
            salida.writeUTF("Error: Las coordenadas deben ser números enteros.");
        } catch (IllegalArgumentException ex) {
            salida.writeUTF("Error: Coordenadas inválidas. Usa formato 'fila,columna' (ej: 1,2).");
        }
    }

}