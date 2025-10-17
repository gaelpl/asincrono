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

    public boolean guardarNuevoUsuario(String usuario, String contrasena) throws SQLException {
        String sql = "INSERT INTO USUARIOS (usuario, contrasena) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, usuario);
            pstmt.setString(2, contrasena);
            return pstmt.executeUpdate() > 0;
        }
    }
}
