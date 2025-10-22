package JuegoDelGato;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class manejadorDeJuegos {
    private final Map<String, Juego> partidas = new ConcurrentHashMap<>();

    // Inicia una nueva partida entre dos jugadores si no hay una activa entre ellos
    public synchronized boolean iniciarPartida(Jugador jugador1, Jugador jugador2) {
        String key = generarKey(jugador1.getIdHilo(), jugador2.getIdHilo());
        if (partidas.containsKey(key)) {
            return false;
        }

        Juego juego = new Juego(jugador1, jugador2);
        partidas.put(key, juego);
        return true;
    }

    public Juego obtenerPartida(String idJugador1, String idJugador2) {
        return partidas.get(generarKey(idJugador1, idJugador2));
    }

    public void terminarPartida(String idJugador1, String idJugador2) {
        partidas.remove(generarKey(idJugador1, idJugador2));
    }

    public boolean tienePartida(String idJugador1, String idJugador2) {
        return partidas.containsKey(generarKey(idJugador1, idJugador2));
    }

    private String generarKey(String j1, String j2) {
        return (j1.compareTo(j2) < 0) ? j1 + "-" + j2 : j2 + "-" + j1;
    }
}
