package JuegoDelGato;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class manejadorDeJuegos {
    
    private final Map<String, Juego> partidas = new ConcurrentHashMap<>();
    private final Map<String, String> solicitudesPendientes = Collections.synchronizedMap(new HashMap<>());

    public void registrarSolicitud(String idEmisor, String idDestino) {
        solicitudesPendientes.put(idDestino, idEmisor);
    }

    public String getSolicitudPendiente(String idDestino) {
        return solicitudesPendientes.get(idDestino);
    }

    public void removerSolicitud(String idDestino) {
        solicitudesPendientes.remove(idDestino);
    }

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