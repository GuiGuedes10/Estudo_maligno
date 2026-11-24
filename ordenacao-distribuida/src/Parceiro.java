import java.io.*;
import java.net.Socket;

// Preserva os nomes do professor: receba envia; envie recebe.
public class Parceiro implements AutoCloseable {
    private final Socket conexao;
    private final ObjectOutputStream transmissor;
    private final ObjectInputStream receptor;

    public Parceiro(Socket conexao) throws IOException {
        this.conexao = conexao;
        try {
            conexao.setSoTimeout(30000);
            transmissor = new ObjectOutputStream(conexao.getOutputStream());
            transmissor.flush(); // Ambos enviam o cabecalho antes de tentar le-lo.
            receptor = new ObjectInputStream(conexao.getInputStream());
            // Somente as mensagens deste protocolo e vetores de inteiros.
            receptor.setObjectInputFilter(info -> {
                if (info.depth() > 10 || info.references() > 100000
                        || info.arrayLength() > 10000000)
                    return ObjectInputFilter.Status.REJECTED;
                Class<?> tipo = info.serialClass();
                if (tipo == null) return ObjectInputFilter.Status.UNDECIDED;
                return tipo == int[].class || tipo == Comunicado.class
                        || tipo == Capacidade.class || tipo == PedidoDeOrdenacao.class
                        || tipo == Resultado.class || tipo == PedidoParaSair.class
                        ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
        } catch (IOException | RuntimeException erro) {
            conexao.close();
            throw erro;
        }
    }

    public void receba(Comunicado comunicado) throws IOException {
        transmissor.writeObject(comunicado);
        transmissor.flush();
        transmissor.reset();
    }

    public Comunicado envie() throws IOException, ClassNotFoundException {
        Object objeto = receptor.readObject();
        if (!(objeto instanceof Comunicado)) throw new IOException("Mensagem invalida.");
        return (Comunicado) objeto;
    }

    public void close() throws IOException { conexao.close(); }
}
