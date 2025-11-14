package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Map;
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
    private tiposDeMensajes tiposDeMensajes; 
    private final RankingDAO rankingDAO = new RankingDAO(); 
    public String grupoActual = "Todos"; 
    private ManejadorGrupo grupoHandler;

    UnCliente(Socket s, String nombreHilo) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.nombreHilo = nombreHilo;
        this.manejador = new ManejadorComandos(comandos, nombreHilo, salida);
        this.tiposDeMensajes = new tiposDeMensajes(this, comandos, loginHandler);
        this.grupoHandler = new ManejadorGrupo(this, comandos, loginHandler);
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
                            "GRUPO ACTUAL: #" + grupoActual + " | Opciones: 1, 2, 3 o comandos: 'joingroup #', 'creategroup #', 'leave #G', 'bloquear @ID', 'jugar @nomre de usuario', 'ranking', , 'salir'");

                    mensaje = entrada.readUTF();

                    if (mensaje.equalsIgnoreCase("salir")) {
                        salida.writeUTF("Adiós. Conexión cerrada.");
                        break; 
                    }
                            
                    try {
                        if (grupoHandler.manejarComandosDeGrupo(mensaje, existe)) {
                            continue;
                        }
                    } catch (SQLException e) {
                        salida.writeUTF("Error interno: Fallo en la base de datos al manejar grupos.");
                        System.err.println("Error SQL en grupos: " + e.getMessage());
                        continue;
                    }

                    if (manejarComandoJuego(mensaje, juegoActivo)) {
                        continue;
                    }
                    if (manejarComandosRanking(mensaje, rankingDAO)) { 
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

                boolean mensajeValido = tiposDeMensajes.manejarOpcionesChat(mensaje);

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
    RankingDAO rankingDAO = new RankingDAO(); 
    
    Jugador jugadorActual = juego.getJugador(this.nombreHilo);
    Jugador oponente = juego.getContrincante(jugadorActual); 
    
    if (!juego.getTurnoActual().getIdHilo().equals(this.nombreHilo)) {
        salida.writeUTF("Esperando movimiento de @" + juego.getTurnoActual().getIdHilo() + " ("
                + juego.getTurnoActual().getMarca() + ")... Tablero:" + juego.obtenerEstadoTablero());
        try {
            Thread.sleep(500); 
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

        } else if (resultado.equals("VICTORIA") || resultado.equals("EMPATE")) {
            
            String nombreJugador = loginHandler.getUsuarioAutenticado(); 
            String nombreOponente;
            
            try {
                nombreOponente = comandos.obtenerUsuarioPorIdHilo(oponente.getIdHilo());
                
                if (nombreOponente == null) nombreOponente = oponente.getIdHilo();
                
            } catch (SQLException e) {
                nombreOponente = oponente.getIdHilo(); 
                System.err.println("Error SQL al obtener nombre del oponente: " + e.getMessage());
            }

            if (resultado.equals("VICTORIA")) {
                rankingDAO.actualizarEstadisticas(nombreJugador, "VICTORIA");
                rankingDAO.actualizarEstadisticas(nombreOponente, "DERROTA"); 
                rankingDAO.registrarPartida(nombreJugador, nombreOponente, nombreJugador); 
                
            } else if (resultado.equals("EMPATE")) {
                rankingDAO.actualizarEstadisticas(nombreJugador, "EMPATE");
                rankingDAO.actualizarEstadisticas(nombreOponente, "EMPATE");
                rankingDAO.registrarPartida(nombreJugador, nombreOponente, "EMPATE");
            }
            
            String resultadoFinal = resultado.equals("VICTORIA") ? "¡VICTORIA! Has ganado la partida." : "¡EMPATE! El tablero está lleno.";
            String msgTablero = juego.obtenerEstadoTablero();

            salida.writeUTF(resultadoFinal + msgTablero);
            oponente.getCliente().salida.writeUTF(juego.getGanador() != null ? "DERROTA. @" + jugadorActual.getIdHilo() + " ganó." + msgTablero: "EMPATE." + msgTablero);

            juegosManager.terminarPartida(juego); 

        } else {
            salida.writeUTF("Error de juego: " + resultado.replace("ERROR_", ""));
        }

    } catch (NumberFormatException ex) {
        salida.writeUTF("Error: Las coordenadas deben ser números enteros.");
    } catch (IllegalArgumentException ex) {
        salida.writeUTF("Error: Coordenadas inválidas. Usa formato 'fila,columna' (ej: 1,2).");
    } catch (SQLException ex) {
        System.err.println("Error SQL al actualizar ranking: " + ex.getMessage());
        salida.writeUTF("Error interno al registrar el resultado del juego.");
    }
}
    private boolean manejarComandosRanking(String comandoCompleto, RankingDAO rankingDAO) throws IOException, SQLException {
    String[] partes = comandoCompleto.trim().split(" ");
    String accion = partes[0].toLowerCase();
    
    if (accion.equals("ranking")) {
        String ranking = rankingDAO.obtenerRankingGeneral();
        salida.writeUTF(ranking);
        return true;
    } 
    else if (accion.equals("stats") && partes.length == 3) {
        
        String nombre1 = partes[1].startsWith("@") ? partes[1].substring(1) : partes[1];
        String nombre2 = partes[2].startsWith("@") ? partes[2].substring(1) : partes[2];
        
        Map<String, Double> porcentajes = rankingDAO.obtenerPorcentajeVictorias(nombre1, nombre2);
        
        if (porcentajes.containsKey(nombre1)) {
            String resultado = String.format(
                "\n--- ENFRENTAMIENTO DIRECTO (%s vs %s) ---\n" +
                "%s: %.1f%% Victorias | %s: %.1f%% Victorias | Empates: %.1f%%\n",
                nombre1, nombre2,
                nombre1, porcentajes.get(nombre1),
                nombre2, porcentajes.get(nombre2),
                porcentajes.get("EMPATE")
            );
            salida.writeUTF(resultado);
        } else {
            salida.writeUTF("Error: Uno o ambos usuarios no existen o no han jugado entre sí.");
        }
        return true;
    }
    return false;
}
    
}