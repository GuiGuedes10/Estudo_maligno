public class Resultado extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final int[] vetor;

    public Resultado(int[] vetor) { this.vetor = vetor.clone(); }
    public int[] getVetor() { return vetor.clone(); }
}
