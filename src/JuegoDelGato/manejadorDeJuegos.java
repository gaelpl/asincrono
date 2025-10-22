package JuegoDelGato;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;

public class manejadorDeJuegos {

    private Map<String, Juego> partidas = new ConcurrentHashMap<>();

    public synchronized boolean iniciarPartida(String jugador1, String jugador2) {
        String key = generarKey(jugador1, jugador2);
        if (partidas.containsKey(key))
            return false; // Ya hay una partida

        Juego juego = new Juego();
        juego.setJugadores(jugador1, jugador2);
        juego.sortearInicio(); // turno aleatorio

        partidas.put(key, juego);
        return true;
    }

}
