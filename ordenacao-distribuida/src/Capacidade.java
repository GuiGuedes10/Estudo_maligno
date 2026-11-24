public class Capacidade extends Comunicado {
    private static final long serialVersionUID = 1L;
    private final int peso;

    public Capacidade(int peso) {
        if (peso < 1 || peso > 1000)
            throw new IllegalArgumentException("Peso deve estar entre 1 e 1000.");
        this.peso = peso;
    }

    public int getPeso() { return peso; }
}
