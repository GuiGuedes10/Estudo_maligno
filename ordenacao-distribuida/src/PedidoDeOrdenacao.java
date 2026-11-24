public class PedidoDeOrdenacao extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final int[] vetor;

    public PedidoDeOrdenacao(int[] vetor) { this.vetor = vetor.clone(); }
    public int[] getVetor() { return vetor.clone(); }
}
