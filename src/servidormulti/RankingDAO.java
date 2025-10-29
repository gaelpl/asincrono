package servidormulti;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
}
