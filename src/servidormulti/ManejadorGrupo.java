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

    private boolean crearNuevoGrupo(String nombreGrupo) throws IOException, SQLException {
        if (!existe) {
            salida.writeUTF("Error: Solo los usuarios registrados pueden crear grupos.");
            return true;
        }
        
        String usuarioDB = loginHandler.getUsuarioAutenticado();
        
        if (comandos.crearGrupo(nombreGrupo)) {
            comandos.unirseAGrupo(usuarioDB, nombreGrupo);
            
            cliente.grupoActual = nombreGrupo; 
            salida.writeUTF("Grupo #" + nombreGrupo + " creado y unido exitosamente.");
        } else {
            salida.writeUTF("Error: El grupo #" + nombreGrupo + " ya existe o el nombre es inválido.");
        }
        return true;
    }

    private boolean unirseAGrupo(String nombreGrupo) throws IOException, SQLException {
        String usuarioDB = existe ? loginHandler.getUsuarioAutenticado() : miIdHilo;
        
        if (!existe && !nombreGrupo.equalsIgnoreCase("Todos")) {
            salida.writeUTF("Error: Solo los usuarios registrados pueden unirse a grupos específicos.");
            return true;
        }
        
        if (cliente.grupoActual.equalsIgnoreCase(nombreGrupo) && comandos.esMiembro(usuarioDB, nombreGrupo)) {
             salida.writeUTF("Ya estás en el grupo #" + nombreGrupo + ".");
             return true;
        }
        
        if (comandos.unirseAGrupo(usuarioDB, nombreGrupo)) {
            cliente.grupoActual = nombreGrupo; 
            salida.writeUTF("Has entrado al grupo #" + nombreGrupo + ".");
        } else {
            salida.writeUTF("Error: El grupo #" + nombreGrupo + " no existe o ya eres miembro.");
        }
        return true;
    }

    private boolean abandonarGrupo(String nombreGrupo) throws IOException, SQLException {
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            salida.writeUTF("Error: No puedes abandonar el grupo 'Todos'.");
            return true;
        }
        if (!existe) {
             salida.writeUTF("Error: Los invitados no pueden abandonar ni crear grupos.");
             return true;
        }
        
        String usuarioDB = loginHandler.getUsuarioAutenticado();
        
        comandos.abandonarGrupo(usuarioDB, nombreGrupo);
        
        cliente.grupoActual = "Todos"; 
        salida.writeUTF("Has abandonado el grupo #" + nombreGrupo + ". Vuelves a 'Todos'.");
        return true;
    }
    
}
