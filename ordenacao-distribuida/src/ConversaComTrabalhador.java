import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

// Uma thread por servidor, como no somador distribuido do professor.
public class ConversaComTrabalhador extends Thread implements AutoCloseable {
    private final String endereco;
    private final Parceiro trabalhador;
    private final int peso;
    private int[] pedaco;
    private int[] resultado;
    private Exception falha;

    public ConversaComTrabalhador(String endereco) throws Exception {
        this.endereco = endereco;
        int separador = endereco.lastIndexOf(':');
        if (separador <= 0 || separador == endereco.length() - 1)
            throw new IllegalArgumentException("Use host:porta: " + endereco);
        String host = endereco.substring(0, separador);
        int porta = Integer.parseInt(endereco.substring(separador + 1));
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, porta), 5000);
            trabalhador = new Parceiro(socket);
            Comunicado mensagem = trabalhador.envie();
            if (!(mensagem instanceof Capacidade))
                throw new IOException("Servidor nao informou sua capacidade.");
            peso = ((Capacidade) mensagem).getPeso();
            if (peso < 1 || peso > 1000) throw new IOException("Peso recebido invalido.");
        } catch (Exception erro) {
            socket.close();
            throw new IOException("Falha ao conectar a " + endereco + ": " + erro.getMessage(), erro);
        }
    }

    public int getPeso() { return peso; }
    public void setPedaco(int[] pedaco) { this.pedaco = pedaco; }

    public void run() {
        try {
            System.out.println(endereco + " (peso " + peso + ") recebe " + pedaco.length + " elementos.");
            trabalhador.receba(new PedidoDeOrdenacao(pedaco));
            Comunicado mensagem = trabalhador.envie();
            if (!(mensagem instanceof Resultado)) throw new IOException("Resposta inesperada.");
            resultado = ((Resultado) mensagem).getVetor();
            if (resultado.length != pedaco.length) throw new IOException("Tamanho da resposta incorreto.");
            for (int i = 1; i < resultado.length; i++)
                if (resultado[i - 1] > resultado[i]) throw new IOException("Resposta nao esta ordenada.");
            trabalhador.receba(new PedidoParaSair());
        } catch (Exception erro) {
            falha = erro;
        } finally {
            try { close(); } catch (IOException erro) { /* Socket encerrado. */ }
        }
    }

    public int[] getResultado() throws IOException {
        if (falha != null) throw new IOException("Falha em " + endereco + ": " + falha.getMessage(), falha);
        if (resultado == null) throw new IOException("Servidor ainda nao respondeu: " + endereco);
        return resultado;
    }

    public void close() throws IOException { trabalhador.close(); }
}
