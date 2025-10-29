package servidormulti;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

        String sql = "INSERT INTO RANKING (usuario, puntos, " + campo + ") VALUES (?, ?, 1) "
                + "ON CONFLICT(usuario) DO UPDATE SET "
                + "puntos = puntos + ?,"
                + campo + " = " + campo + " + 1;";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setInt(2, puntos);
            pstmt.setInt(3, puntos);
            pstmt.executeUpdate();
        }
    }

    public String obtenerRankingGeneral() throws SQLException {
        String sql = "SELECT usuario, puntos, victorias, derrotas, empates FROM RANKING ORDER BY puntos DESC, victorias DESC";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            StringBuilder sb = new StringBuilder("\n--- RANKING GLOBAL DEL GATO ---\n");
            sb.append("POS | USUARIO | PTS | V | D | E\n");
            int pos = 1;

            while (rs.next()) {
                sb.append(String.format("%-3d | %-7s | %-3d | %-1d | %-1d | %-1d\n",
                        pos++,
                        rs.getString("usuario"),
                        rs.getInt("puntos"),
                        rs.getInt("victorias"),
                        rs.getInt("derrotas"),
                        rs.getInt("empates")));
            }
            return sb.toString();
        }
    }

    public void registrarPartida(String user1, String user2, String ganador) throws SQLException {
        String usuario_a = user1.compareTo(user2) < 0 ? user1 : user2;
        String usuario_b = user1.compareTo(user2) < 0 ? user2 : user1;

        String sql = "INSERT INTO HISTORIAL_JUEGOS (usuario_a, usuario_b, ganador) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario_a);
            pstmt.setString(2, usuario_b);
            pstmt.setString(3, ganador);
            pstmt.executeUpdate();
        }
    }

    public Map<String, Double> obtenerPorcentajeVictorias(String user1, String user2) throws SQLException {
        String usuario_a = user1.compareTo(user2) < 0 ? user1 : user2;
        String usuario_b = user1.compareTo(user2) < 0 ? user2 : user1;
        String sql = "SELECT ganador FROM HISTORIAL_JUEGOS WHERE usuario_a = ? AND usuario_b = ?";

        Map<String, Integer> resultados = new HashMap<>();
        resultados.put(user1, 0);
        resultados.put(user2, 0);
        resultados.put("EMPATE", 0);
        int totalPartidas = 0;

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario_a);
            pstmt.setString(2, usuario_b);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String ganador = rs.getString("ganador");
                if (resultados.containsKey(ganador)) {
                    resultados.put(ganador, resultados.get(ganador) + 1);
                } else if (ganador.equals("EMPATE")) {
                    resultados.put("EMPATE", resultados.get("EMPATE") + 1);
                }
                totalPartidas++;
            }
        }

        Map<String, Double> porcentaje = new HashMap<>();
        if (totalPartidas > 0) {
            porcentaje.put(user1, (resultados.get(user1) * 100.0) / totalPartidas);
            porcentaje.put(user2, (resultados.get(user2) * 100.0) / totalPartidas);
            porcentaje.put("EMPATE", (resultados.get("EMPATE") * 100.0) / totalPartidas);
        } else {
            porcentaje.put(user1, 0.0);
            porcentaje.put(user2, 0.0);
            porcentaje.put("EMPATE", 0.0);
        }
        return porcentaje;
    }
}
