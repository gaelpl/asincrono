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

    public boolean guardarNuevoUsuario(String usuario, String contrasena, String idHilo) throws SQLException {
        String sql = "INSERT INTO USUARIOS (usuario, contrasena, id_hilo) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setString(2, contrasena);
            pstmt.setString(3, idHilo);
            return pstmt.executeUpdate() > 0;
        }
    }

    public String autenticarUsuario(String usuario, String contrasena) throws SQLException {
        String sql = "SELECT usuario FROM USUARIOS WHERE usuario = ? AND contrasena = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setString(2, contrasena);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("usuario");
            }
            return null;
        }
    }

    public boolean bloquearUsuario(String bloqueador, String bloqueado) throws SQLException {
        String sql = "INSERT INTO BLOQUEOS (bloqueador, bloqueado) VALUES (?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean desbloquearUsuario(String bloqueador, String bloqueado) throws SQLException {
        String sql = "DELETE FROM BLOQUEOS WHERE bloqueador = ? AND bloqueado = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean estaBloqueadoPor(String emisor, String receptor) throws SQLException {
        String sql = "SELECT 1 FROM BLOQUEOS WHERE bloqueador = ? AND bloqueado = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, receptor);
            pstmt.setString(2, emisor);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public void actualizarIdHilo(String usuario, String nuevoIdHilo) throws SQLException {
        String sql = "UPDATE USUARIOS SET id_hilo = ? WHERE usuario = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nuevoIdHilo);
            pstmt.setString(2, usuario);
            pstmt.executeUpdate();
        }
    }

    public String obtenerIdHilo(String usuario) throws SQLException {
        String sql = "SELECT id_hilo FROM USUARIOS WHERE usuario = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("id_hilo");
            }
            return null;
        }
    }

    public String obtenerUsuarioPorIdHilo(String idHilo) throws SQLException {
        String sql = "SELECT usuario FROM USUARIOS WHERE id_hilo = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idHilo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("usuario");
            }
            return idHilo;
        }
    }

    public boolean crearGrupo(String nombreGrupo) throws SQLException {
        String sql = "INSERT INTO GRUPOS (nombre) VALUES (?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean borrarGrupo(String nombreGrupo) throws SQLException {
        if (nombreGrupo.equalsIgnoreCase("Todos"))
            return false;
        String sql = "DELETE FROM GRUPOS WHERE nombre = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean unirseAGrupo(String usuario, String grupo) throws SQLException {
        String sql = "INSERT OR IGNORE INTO MEMBRESIA (usuario, grupo) VALUES (?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.setString(2, grupo);
            return pstmt.executeUpdate() > 0;
        }
    }

    public void abandonarGrupo(String usuario, String grupo) throws SQLException {
        if (grupo.equalsIgnoreCase("Todos"))
            return;
        String sql = "DELETE FROM MEMBRESIA WHERE usuario = ? AND grupo = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.setString(2, grupo);
            pstmt.executeUpdate();
        }
    }

    public boolean esMiembro(String usuario, String grupo) throws SQLException {
        String sql = "SELECT 1 FROM MEMBRESIA WHERE usuario = ? AND grupo = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setString(2, grupo);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public String obtenerUsuariosRegistrados() throws SQLException {
        String sql = "SELECT usuario FROM USUARIOS ORDER BY usuario";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            StringBuilder sb = new StringBuilder("\nUsuarios registrados\n");
            int count = 0;

            while (rs.next()) {
                sb.append(rs.getString("usuario")).append(", ");
                count++;
            }

            if (count > 0) {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n--");
            return sb.toString();
        }
    }

}
