package clientemulti;
import java.io.IOException;
import java.net.Socket;
public class ClienteMulti {

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("10.22.20.237",8080);
        ParaMandar paraMandar = new ParaMandar(s);
        Thread hiloParaMandar = new Thread(paraMandar);
        hiloParaMandar.start();

        ParaRecibir paraRecibir = new ParaRecibir(s);
        Thread hiloParaRecibir = new Thread(paraRecibir);
        hiloParaRecibir.start();
    }

}
