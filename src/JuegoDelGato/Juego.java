package JuegoDelGato;

import java.util.Random;

public class Juego {
    private final Tablero tablero;
    private final Jugador jugadorA; //jugador que empieza con X
    private final Jugador jugadorB; //jugador que empieza con O
    
    private Jugador turnoActual;
    private Jugador ganador;
    private boolean activo; 

    //p1 Primer participante (retador) p2 Segundo participante (aceptante)
    public Juego(Jugador p1, Jugador p2) {
        this.tablero = new Tablero();
        this.activo = true;
        
        if (new Random().nextBoolean()) {
            this.jugadorA = new Jugador(p1.getCliente(), 'X');
            this.jugadorB = new Jugador(p2.getCliente(), 'O');
        } else {
            this.jugadorA = new Jugador(p2.getCliente(), 'X');
            this.jugadorB = new Jugador(p1.getCliente(), 'O');
        }
        
        this.turnoActual = jugadorA;
    }
    
    // Genera la clave de la partida (usando el ID del cliente que tiene X primero)
    public String getIdPartida() {
        return jugadorA.getIdHilo() + "-" + jugadorB.getIdHilo();
    }

    public boolean estaActivo() {
        return activo;
    }

    public Jugador getTurnoActual() {
        return turnoActual;
    }
    
    public Jugador getJugador(String idHilo) {
        if (jugadorA.getIdHilo().equals(idHilo)) return jugadorA;
        if (jugadorB.getIdHilo().equals(idHilo)) return jugadorB;
        return null;
    }
    
    public Jugador getContrincante(Jugador jugador) {
        return jugador.getIdHilo().equals(jugadorA.getIdHilo()) ? jugadorB : jugadorA;
    }

    public String procesarMovimiento(Jugador jugador, Movimiento mov) {
        if (!activo) {
            return "ERROR_TERMINADO";
        }
        if (!jugador.getIdHilo().equals(turnoActual.getIdHilo())) {
            return "ERROR_TURNO";
        }
        
        if (tablero.hacerMovimiento(mov, jugador.getMarca())) {
            char resultado = tablero.verificarGanador();
            
            if (resultado != ' ') {
                ganador = turnoActual;
                activo = false;
                return "VICTORIA";
            } else if (tablero.estaLleno()) {
                activo = false;
                return "EMPATE";
            } else {
                // Cambiar el turno
                turnoActual = getContrincante(jugador);
                return "VALIDO";
            }
        } else {
            return "ERROR MOVIMIENTO INVALIDO";
        }
    }
   
    public Jugador forzarVictoria(String idPerdedor) {
        if (activo) {
            Jugador perdedor = getJugador(idPerdedor);
            if (perdedor != null) {
                ganador = getContrincante(perdedor);
                activo = false;
                return ganador;
            }
        }
        return null;
    }
    
    public String obtenerEstadoTablero() {
        return tablero.toString();
    }
    
    public Jugador getGanador() {
        return ganador;
    }
}
