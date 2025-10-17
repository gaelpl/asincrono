package servidormulti;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class comandosDAO {
    
    private Connection getConnection() throws SQLException {
        return ServidorMulti.getManejador().conectar();
    }

    public boolean verificarUsuarioExistente(String usuario) throws SQLException {
        String sql = "SELECT usuario FROM USUARIOS WHERE usuario = ?";
        try (Connection conn = getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); 
        }
    }
}
