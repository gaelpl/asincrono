package JuegoDelGato;

import servidormulti.UnCliente;

public class Jugador {
    private final UnCliente cliente;
    private char signo; 

    public Jugador(UnCliente cliente) {
        this.cliente = cliente;
        this.signo = ' '; 
    }
    
    public Jugador(UnCliente cliente, char signo) {
        this.cliente = cliente;
        this.signo = signo;
    }

    public String getIdHilo() { 
        return cliente.nombreHilo;
    }
    
    public char getMarca() {
        return signo;
    }

    public UnCliente getCliente() {
        return cliente;
    }

    public void setMarca(char signo) {
        this.signo = signo;
    }
}
