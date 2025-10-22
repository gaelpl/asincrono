package JuegoDelGato;

import java.util.Arrays;

public class Tablero {
    
    private static final int TAMANO = 3;
    private char[][] matriz; 

    public Tablero() {
        matriz = new char[TAMANO][TAMANO];
        for (char[] fila: matriz) {
            Arrays.fill(fila, ' '); 
        }
    }

    public boolean hacerMovimiento(Movimiento mov, char signo) {
        if (mov.fila < 0 || mov.fila >= TAMANO || 
            mov.columna < 0 || mov.columna >= TAMANO || 
            matriz[mov.fila][mov.columna] != ' ') {
            
            return false;
        }
        matriz[mov.fila][mov.columna] = signo;
        return true;
    }

    public char verificarGanador() {
        //Filas y Columnas
        for (int i = 0; i < TAMANO; i++) {
            if (matriz[i][0] != ' ' && matriz[i][0] == matriz[i][1] && matriz[i][0] == matriz[i][2]) {
                return matriz[i][0]; 
            }
            if (matriz[0][i] != ' ' && matriz[0][i] == matriz[1][i] && matriz[0][i] == matriz[2][i]) {
                return matriz[0][i]; 
            }
        }
        //Diagonales
        if (matriz[0][0] != ' ' && matriz[0][0] == matriz[1][1] && matriz[0][0] == matriz[2][2]) {
            return matriz[0][0]; 
        }
        if (matriz[0][2] != ' ' && matriz[0][2] == matriz[1][1] && matriz[0][2] == matriz[2][0]) {
            return matriz[0][2]; 
        }

        return ' '; 
    }

    public boolean estaLleno() {
        for (int i = 0; i < TAMANO; i++) {
            for (int j = 0; j < TAMANO; j++) {
                if (matriz[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n  0 1 2\n");
        for (int i = 0; i < TAMANO; i++) {
            sb.append(i).append(" ");
            for (int j = 0; j < TAMANO; j++) {
                sb.append(matriz[i][j] != ' ' ? matriz[i][j] : "-").append(j < TAMANO - 1 ? "|" : "");
            }
            sb.append("\n");
            if (i < TAMANO - 1) {
                sb.append("  --\n");
            }
        }
        return sb.toString();
    }
}
