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
    private UnCliente cliente; 

    public ManejadorGrupo(UnCliente cliente, comandosDAO comandos, login loginHandler) {
        this.cliente = cliente;
        this.comandos = comandos;
        this.salida = cliente.salida;
        this.loginHandler = loginHandler;
        this.miIdHilo = cliente.nombreHilo;
    }

    public boolean manejarComandosDeGrupo(String input, boolean estaLogueado) throws IOException, SQLException {
        estaLogueado = cliente.existe; 
        
        String[] partes = input.trim().split(" ");
        String accion = partes[0].toLowerCase();

        if (!accion.equals("creargrupo") && !accion.equals("unirsegrupo") && !accion.equals("abandonargrupo")) {
            return false;
        }

        String nombreGrupo = partes.length == 2 && partes[1].startsWith("#") ? partes[1].substring(1) : null;
        
        if (nombreGrupo == null) {
             salida.writeUTF("Error: Debes especificar el grupo con #nombreGrupo.");
             return true;
        }

        if (accion.equals("creargrupo")) {
            return crearNuevoGrupo(nombreGrupo, estaLogueado);
        } else if (accion.equals("unirsegrupo")) {
            return unirseAGrupo(nombreGrupo, estaLogueado);
        } else if (accion.equals("abandonargrupo")) {
            return abandonarGrupo(nombreGrupo, estaLogueado);
        }
        
        return false;
    }

    private boolean crearNuevoGrupo(String nombreGrupo, boolean estaLogueado) throws IOException, SQLException {
        if (!estaLogueado) { 
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

    private boolean unirseAGrupo(String nombreGrupo, boolean estaLogueado) throws IOException, SQLException {
        String usuarioDB = estaLogueado ? loginHandler.getUsuarioAutenticado() : miIdHilo; 
        
        if (!estaLogueado && !nombreGrupo.equalsIgnoreCase("Todos")) {
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

    private boolean abandonarGrupo(String nombreGrupo, boolean estaLogueado) throws IOException, SQLException {
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            salida.writeUTF("Error: No puedes abandonar el grupo 'Todos'.");
            return true;
        }
        if (!estaLogueado) { 
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