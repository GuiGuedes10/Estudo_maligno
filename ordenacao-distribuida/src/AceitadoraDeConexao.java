import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AceitadoraDeConexao extends Thread implements AutoCloseable {
    private final ServerSocket servidor;
    private final int peso;

    public AceitadoraDeConexao(int porta, int peso) throws IOException {
        new Capacidade(peso); // Valida antes de abrir a porta.
        this.peso = peso;
        servidor = new ServerSocket(porta);
    }

    public int getPorta() { return servidor.getLocalPort(); }

    public void run() {
        while (!servidor.isClosed()) {
            try {
                Socket conexao = servidor.accept();
                new SupervisoraDeConexao(conexao, peso).start();
            } catch (IOException erro) {
                if (!servidor.isClosed())
                    System.err.println("Falha ao aceitar conexao: " + erro.getMessage());
                break;
            }
        }
    }

    public void close() throws IOException { servidor.close(); }
}
