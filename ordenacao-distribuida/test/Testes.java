import java.net.ServerSocket;
import java.util.*;

public class Testes {
    private static int verificacoes;

    private static void conferir(boolean condicao, String descricao) {
        verificacoes++;
        if (!condicao) throw new AssertionError(descricao);
    }

    private static void conferirOrdenacao(int[] entrada, String[] enderecos) throws Exception {
        int[] esperado = entrada.clone();
        Arrays.sort(esperado);
        int[] copia = entrada.clone();
        int[] recebido = OrdenacaoDistribuida.ordenar(entrada, enderecos);
        conferir(Arrays.equals(esperado, recebido), "Resultado diferente de Arrays.sort");
        conferir(Arrays.equals(entrada, copia), "Entrada foi modificada");
    }

    public static void main(String[] args) throws Exception {
        conferir(Arrays.equals(OrdenacaoDistribuida.dividir(12, new int[] {1, 3}),
                new int[] {3, 9}), "Divisao proporcional 1:3");
        Random aleatorio = new Random(42);
        for (int rodada = 0; rodada < 1000; rodada++) {
            int tamanho = aleatorio.nextInt(10000);
            int[] pesos = {1 + aleatorio.nextInt(1000), 1 + aleatorio.nextInt(1000),
                    1 + aleatorio.nextInt(1000)};
            int[] divisao = OrdenacaoDistribuida.dividir(tamanho, pesos);
            conferir(Arrays.stream(divisao).sum() == tamanho, "Perda de elementos na divisao");
            for (int i = 0; i < pesos.length; i++) {
                double ideal = (double) tamanho * pesos[i] / Arrays.stream(pesos).sum();
                conferir(Math.abs(divisao[i] - ideal) < 1.000001, "Arredondamento desproporcional");
            }
        }
        conferir(Arrays.equals(Coordenador.lerVetor("3, -1; 2\n0"),
                new int[] {3, -1, 2, 0}), "Leitura do vetor");
        try {
            Coordenador.lerVetor("1,abc,2");
            throw new AssertionError("Aceitou numero invalido");
        } catch (NumberFormatException esperado) { verificacoes++; }

        try (AceitadoraDeConexao fraco = new AceitadoraDeConexao(0, 1);
             AceitadoraDeConexao forte = new AceitadoraDeConexao(0, 3)) {
            fraco.start();
            forte.start();
            String[] enderecos = {"localhost:" + fraco.getPorta(), "localhost:" + forte.getPorta()};
            conferirOrdenacao(new int[] {9, 3, 7, 1, 8, 2, 6, 0, 5, 4, -1, 3}, enderecos);
            conferirOrdenacao(new int[0], enderecos);
            conferirOrdenacao(new int[] {7}, enderecos);
            conferirOrdenacao(new int[] {2, 2, 2, 2, 2}, enderecos);
            conferirOrdenacao(new int[] {Integer.MAX_VALUE, 0, Integer.MIN_VALUE, -1}, enderecos);
            int[] grande = new int[100001];
            for (int i = 0; i < grande.length; i++) grande[i] = aleatorio.nextInt();
            conferirOrdenacao(grande, enderecos);
            conferirOrdenacao(new int[] {5, 4, 3, 2, 1}, new String[] {enderecos[0]});
            // Uma porta fechada simula falha imediata, sem aceitar resultado parcial.
            int portaFechada;
            try (ServerSocket reserva = new ServerSocket(0)) { portaFechada = reserva.getLocalPort(); }
            try {
                OrdenacaoDistribuida.ordenar(new int[] {2, 1},
                        new String[] {enderecos[0], "localhost:" + portaFechada});
                throw new AssertionError("Aceitou servidor indisponivel");
            } catch (java.io.IOException esperado) { verificacoes++; }
            try {
                OrdenacaoDistribuida.ordenar(new int[] {2, 1},
                        new String[] {enderecos[0], enderecos[0]});
                throw new AssertionError("Aceitou servidor duplicado");
            } catch (IllegalArgumentException esperado) { verificacoes++; }
        }
        System.out.println("OK: " + verificacoes + " verificacoes, incluindo sockets reais.");
    }
}
