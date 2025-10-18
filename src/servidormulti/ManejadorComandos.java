package servidormulti;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class ManejadorComandos {
    
    private final comandosDAO comando;
    private final String nombreHilo;
    private final DataOutputStream salida;
    private String usuarioAutenticado;
    
    public ManejadorComandos(comandosDAO comando, String nombreHilo, DataOutputStream salida) {
        this.comando = comando;
        this.nombreHilo = nombreHilo;
        this.salida = salida;
        this.usuarioAutenticado = nombreHilo; 
    }

    public void setUsuarioAutenticado(String usuario) {
        this.usuarioAutenticado = usuario;
    }

    public boolean manejarComandoDeBloqueo(String input) throws IOException, SQLException {
        input = input.trim();
        
        if (input.toLowerCase().startsWith("bloquear @")) {
            String idDestino = input.substring(10).trim(); 
            return manejarBloqueo(idDestino);
        } else if (input.toLowerCase().startsWith("desbloquear @")) {
            String idDestino = input.substring(13).trim(); 
            return manejarDesbloqueo(idDestino);
        }
        return false;
    }

    private boolean manejarBloqueo(String idDestino) throws IOException, SQLException {
        String bloqueadorDB = this.usuarioAutenticado; 
        
        if (idDestino.equals(this.nombreHilo)) {
            salida.writeUTF("Error: No puedes bloquearte a ti mismo.");
            return true;
        }
        
        if (!ServidorMulti.clientes.containsKey(idDestino)) {
            salida.writeUTF("Error: El usuario @" + idDestino + " no existe o no está conectado.");
            return true;
        }
        
        try {
            comando.bloquearUsuario(bloqueadorDB, idDestino);
            
            comando.bloquearUsuario(idDestino, bloqueadorDB); 
            
            // Éxito
            UnCliente cliente = ServidorMulti.clientes.get(idDestino);
            String idVisible = cliente != null ? cliente.nombreHilo : idDestino;
            salida.writeUTF("Has bloqueado bidireccionalmente a @" + idVisible + ".");

            return true; 
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique constraint")) {
                salida.writeUTF("Advertencia: La restricción de bloqueo con @" + idDestino + " ya existía parcialmente.");
            } else {
                salida.writeUTF("Error: Fallo al intentar registrar el bloqueo en la base de datos.");
                System.err.println("Error SQL al bloquear: " + e.getMessage());
            }
            return true;
        }
    }

    private boolean manejarDesbloqueo(String idDestino) throws IOException, SQLException {
        String bloqueadorDB = this.usuarioAutenticado;
        
        boolean exito1 = comando.desbloquearUsuario(bloqueadorDB, idDestino);
        
        boolean exito2 = comando.desbloquearUsuario(idDestino, bloqueadorDB); 

        UnCliente cliente = ServidorMulti.clientes.get(idDestino);
        String idVisible = cliente != null ? cliente.nombreHilo : idDestino;
        
        if (exito1 || exito2) {
            salida.writeUTF("Has desbloqueado bidireccionalmente a @" + idVisible + ".");
        } else {
            salida.writeUTF("Error: @" + idDestino + " no estaba bloqueado por ti."); 
        }
        return true;
    }
}