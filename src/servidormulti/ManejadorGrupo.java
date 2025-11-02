package servidormulti;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import JuegoDelGato.manejadorDeJuegos;

public class ManejadorGrupo {

    private final comandosDAO comandos;
    private final DataOutputStream salida;
    private final login loginHandler;
    private final String miIdHilo;
    private final boolean existe;
    private UnCliente cliente; 

    public ManejadorGrupo(UnCliente cliente, comandosDAO comandos, login loginHandler) {
        this.cliente = cliente;
        this.comandos = comandos;
        this.salida = cliente.salida;
        this.loginHandler = loginHandler;
        this.miIdHilo = cliente.nombreHilo;
        this.existe = cliente.existe;
    }

    public boolean manejarComandosDeGrupo(String input) throws IOException, SQLException {
        String[] partes = input.trim().split(" ");
        String accion = partes[0].toLowerCase();

        if (!accion.equals("creategroup") && !accion.equals("joingroup") && !accion.equals("leavegroup")) {
            return false;
        }

        String nombreGrupo = partes.length == 2 && partes[1].startsWith("#") ? partes[1].substring(1) : null;
        
        if (nombreGrupo == null) {
             salida.writeUTF("Error: Debes especificar el grupo con #nombreGrupo.");
             return true;
        }

        if (accion.equals("creategroup")) {
            return crearNuevoGrupo(nombreGrupo);
        } else if (accion.equals("joingroup")) {
            return unirseAGrupo(nombreGrupo);
        } else if (accion.equals("leavegroup")) {
            return abandonarGrupo(nombreGrupo);
        }
        
        return false;
    }
    
}
