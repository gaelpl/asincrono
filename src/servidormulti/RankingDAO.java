package servidormulti;

import java.sql.Connection;
import java.sql.SQLException;

public class RankingDAO {
    private static final int PUNTOS_VICTORIA = 2;
    private static final int PUNTOS_EMPATE = 1;
    
    private Connection getConnection() throws SQLException {
        return ServidorMulti.getManejador().conectar();
    }

    public void actualizarEstadisticas(String usuario, String resultado) throws SQLException {
        String campo;
        int puntos;

        switch (resultado) {
            case "VICTORIA":
                campo = "victorias";
                puntos = PUNTOS_VICTORIA;
                break;
            case "EMPATE":
                campo = "empates";
                puntos = PUNTOS_EMPATE;
                break;
            case "DERROTA":
                campo = "derrotas";
                puntos = 0;
                break;
            default:
                return;
        }
    }
}
