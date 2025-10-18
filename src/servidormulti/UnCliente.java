package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException; 

public class UnCliente implements Runnable {
	final DataOutputStream salida;
	final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
	final DataInputStream entrada;
	String nombreHilo;
	private int intentos = 0;
	private final int intentosMaximos = 3;
	boolean existe = false;
	private final comandosDAO comandos = new comandosDAO();
	private ManejadorComandos manejador;

	UnCliente(Socket s, String nombreHilo) throws IOException {
		salida = new DataOutputStream(s.getOutputStream());
		entrada = new DataInputStream(s.getInputStream());
		this.nombreHilo = nombreHilo;
		this.manejador = new ManejadorComandos(comandos, nombreHilo, salida); 
	}

	@Override
	public void run() {
		String mensaje = "";
		login login = new login();
		Registro registro = new Registro();
		
		try {

			while (true) {
				// primera verificacon si es que no existe y ya se le acabaron los mensajes anonimos
				if (!existe && intentos >= intentosMaximos) {
					salida.writeUTF("se te acabaron los mensajes, inicia sesion o registrate");

					// si no existe lo fuerzo a que escoja una opcion
					exigirLoginRegister(login, registro);
				}

				// boolean que verifica si puede mandar mensajes
				boolean puedeMandar = existe || (intentos < intentosMaximos);

				if (puedeMandar) {
					this.salida.writeUTF("Elige la opcion 1:si quieres mandar mensaje general, 2:un usuario en especifico, 3:varios usuarios, o escribe 'bloquear @ID' o 'desbloquear @ID'"); 
					mensaje = entrada.readUTF();

					try {
						if (manejador.manejarComandoDeBloqueo(mensaje)) {
							continue; 
						}
					} catch (SQLException e) {
						salida.writeUTF("Error interno: Fallo en la base de datos al procesar comando.");
						System.err.println("Error SQL en comando de cliente: " + e.getMessage());
						continue;
					}
				} else {
					this.salida.writeUTF("Solo puedes recibir mensajes. Por favor, autentícate para enviar.");
					continue;
				}
				boolean mensajeValido = false;
				switch (mensaje) {
					case "1":
						mensajeValido = enviarMensajeGeneral();
						break;

					case "2":
						mensajeValido = enviarMensajePrivado();
						break;

					case "3":
						mensajeValido = enviarMensajeVarios();
						break;

					default:
						salida.writeUTF("opcion no reconocida");
						break;
				}

				// aumentamos el contador si se envio el mensaje aninimo
				if (mensajeValido && !existe) {
					intentos++;
					int restantes = intentosMaximos - intentos;
					if (restantes > 0) {
						this.salida.writeUTF(
								"Tienes " + restantes + " mensajes restantes antes de requerir login/registro.");
					}
				}
			}
		} catch (Exception ex) {
			System.err.println("Error en el hilo de cliente: " + ex.getMessage());
		} finally { 
			try {
				if (entrada != null) entrada.close();
				if (salida != null) salida.close();
			} catch (IOException e) { }
			ServidorMulti.clientes.remove(this.nombreHilo);
		}
	 }

	 private void exigirLoginRegister(login login, Registro registro) throws IOException {
		 while (!existe) {
			 salida.writeUTF("Bienvenido. Escribe 'login' para iniciar sesion o 'register' para crear cuenta.");
			 String accion = entrada.readUTF();

			 if (accion == null) {
				 break;
			 }
			 if ("login".equalsIgnoreCase(accion)) {
				 existe = login.manejarLogin(salida, entrada);
			 } else if ("register".equalsIgnoreCase(accion)) {
				 registro.manejarRegistro(salida, entrada);
			 } else {
				 salida.writeUTF("Accion no reconocida. Intenta de nuevo.");
			 }
		 }
		 if (existe) {
			 salida.writeUTF("¡Inicio de sesión exitoso! Puedes enviar mensajes ilimitados.");
		 }
	 }

	 private boolean enviarMensajeGeneral() throws IOException {
		 this.salida.writeUTF("Escribe tu mensaje para todos");
		 String mensaje = entrada.readUTF();
		 for (UnCliente cliente : ServidorMulti.clientes.values()) {
			 if (cliente.nombreHilo.equals(this.nombreHilo)) {
				 return true;
			 }
			 cliente.salida.writeUTF("@" + this.nombreHilo + ": " + mensaje);
		 }
		 return true;
	 }

	 private boolean enviarMensajePrivado() throws IOException {
		 this.salida.writeUTF("Escribe a quien quieres mandar mensaje (pon @numeroDeUsuario al inicio)");
		 String mensaje = entrada.readUTF();
		 this.salida.writeUTF("Escribe tu mensaje");
		 String contenidoMensaje = entrada.readUTF();
		 if (mensaje.startsWith("@")) {
			 String[] partes = mensaje.split(" ");
			 String aQuien = partes[0].substring(1);
			 UnCliente cliente = ServidorMulti.clientes.get(aQuien);
			 cliente.salida.writeUTF("@" + Thread.currentThread().getName() + ": " + contenidoMensaje);
		 } else {
			 for (UnCliente cliente : ServidorMulti.clientes.values()) {
				 cliente.salida.writeUTF(contenidoMensaje);
			 }
		 }
		 return true;
	 }

	 private boolean enviarMensajeVarios() throws IOException {
		 this.salida.writeUTF("Escribe a quienes quieres mandar mensaje, pon @numeroDeUsuario separados por comas al inicio");
		 String mensaje = entrada.readUTF();
		 this.salida.writeUTF("Escribe tu mensaje");
		 String contenidoMensaje = entrada.readUTF();
		 if (mensaje.startsWith("@")) {
			 String[] partes = mensaje.split(",");
			 for (int i = 0; i < partes.length; i++) {
				 String aQuien = partes[i].substring(1);
				 UnCliente cliente = ServidorMulti.clientes.get(aQuien);
				 cliente.salida
						 .writeUTF("@" + Thread.currentThread().getName() + ": " + contenidoMensaje);
			 }
		 } else {
			 for (UnCliente cliente : ServidorMulti.clientes.values()) {
				 cliente.salida.writeUTF(contenidoMensaje);
			 }
		 }
		 return true;
	 }

}
