import java.io.EOFException;
import java.net.Socket;
import java.util.Arrays;

public class SupervisoraDeConexao extends Thread {
    private final Socket conexao;
    private final int peso;

    public SupervisoraDeConexao(Socket conexao, int peso) {
        this.conexao = conexao;
        this.peso = peso;
        setDaemon(true);
    }

    public void run() {
        try (Parceiro cliente = new Parceiro(conexao)) {
            cliente.receba(new Capacidade(peso));
            for (;;) {
                Comunicado mensagem = cliente.envie();
                if (mensagem instanceof PedidoParaSair) return;
                if (!(mensagem instanceof PedidoDeOrdenacao))
                    throw new java.io.IOException("Esperado pedido de ordenacao.");
                int[] pedaco = ((PedidoDeOrdenacao) mensagem).getVetor();
                System.out.println("Ordenando " + pedaco.length + " elementos (peso " + peso + ").");
                Arrays.sort(pedaco);
                cliente.receba(new Resultado(pedaco));
            }
        } catch (EOFException erro) {
            // O cliente encerrou a conexao.
        } catch (Exception erro) {
            System.err.println("Falha ao atender cliente: " + erro.getMessage());
        }
    }
}
