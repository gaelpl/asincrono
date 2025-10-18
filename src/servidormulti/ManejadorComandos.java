package servidormulti;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class ManejadorComandos {
    
    private final comandosDAO comando;
    private final String nombreHilo;
    private final DataOutputStream salida;
    
    public ManejadorComandos(comandosDAO comando, String nombreHilo, DataOutputStream salida) {
        this.comando = comando;
        this.nombreHilo = nombreHilo;
        this.salida = salida;
    }

    public boolean manejarComandoDeBloqueo(String input) throws IOException, SQLException {
        input = input.trim();
        
        if (input.toLowerCase().startsWith("bloquear @")) {
            String idDestino = input.substring(10).trim(); 
            return manejarBloqueo(idDestino);
        } else if (input.toLowerCase().startsWith("desbloquear @")) {
            String idDestino = input.substring(12).trim(); 
            return manejarDesbloqueo(idDestino);
        }
        return false;
    }

    private boolean manejarBloqueo(String idDestino) throws IOException, SQLException {
        if (idDestino.equals(this.nombreHilo)) {
            salida.writeUTF("Error: No puedes bloquearte a ti mismo.");
            return true;
        }
        
        if (!ServidorMulti.clientes.containsKey(idDestino)) {
            salida.writeUTF("Error: El usuario @" + idDestino + " no existe.");
            return true;
        }
        
        try {
            if (comando.bloquearUsuario(this.nombreHilo, idDestino)) {
                salida.writeUTF("Has bloqueado a @" + idDestino + ".");
            } else {
                salida.writeUTF("Ya habías bloqueado a @" + idDestino + ".");
            }
            return true; 
        } catch (SQLException e) {
            salida.writeUTF("Error: Fallo al intentar registrar el bloqueo en la base de datos.");
            System.err.println("Error SQL al bloquear: " + e.getMessage());
            return true;
        }
    }

    private boolean manejarDesbloqueo(String idDestino) throws IOException, SQLException {
        if (comando.desbloquearUsuario(this.nombreHilo, idDestino)) {
            salida.writeUTF("Has desbloqueado a @" + idDestino + ".");
        } else {
            salida.writeUTF("Error: @" + idDestino + " no estaba bloqueado.");
        }
        return true;
    }
}
