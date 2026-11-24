import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Coordenador {
    public static int[] lerVetor(String texto) {
        texto = texto.trim();
        if (texto.isEmpty()) return new int[0];
        String[] numeros = texto.split("[,;\\s]+");
        if (numeros.length > 10000000) throw new IllegalArgumentException("Vetor muito grande.");
        int[] vetor = new int[numeros.length];
        for (int i = 0; i < numeros.length; i++) vetor[i] = Integer.parseInt(numeros[i]);
        return vetor;
    }

    private static void mostrar(String nome, int[] vetor) {
        int limite = Math.min(vetor.length, 50);
        System.out.println(nome + " (" + vetor.length + " elementos): "
                + Arrays.toString(Arrays.copyOf(vetor, limite))
                + (vetor.length > limite ? " ... (mostrando os primeiros 50)" : ""));
    }

    public static void main(String[] args) {
        try {
            String[] enderecos;
            int[] vetor;
            Path saida = null;
            if (args.length == 0) {
                Scanner teclado = new Scanner(System.in);
                System.out.println("Servidores separados por espaco (ex.: localhost:3001 localhost:3002):");
                enderecos = teclado.nextLine().trim().split("\\s+");
                System.out.println("Digite os inteiros do vetor separados por espaco ou virgula:");
                vetor = lerVetor(teclado.nextLine());
            } else if (args[0].equals("--arquivo")) {
                if (args.length < 4) throw new IllegalArgumentException("Informe entrada, saida e servidores.");
                vetor = lerVetor(new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8));
                saida = Paths.get(args[2]);
                enderecos = Arrays.copyOfRange(args, 3, args.length);
            } else {
                if (args.length < 2) throw new IllegalArgumentException("Informe vetor e servidores.");
                vetor = lerVetor(args[0]);
                enderecos = Arrays.copyOfRange(args, 1, args.length);
            }
            mostrar("Vetor original", vetor);
            long inicio = System.nanoTime();
            int[] ordenado = OrdenacaoDistribuida.ordenar(vetor, enderecos);
            mostrar("Vetor ordenado", ordenado);
            System.out.printf("Tempo total (conexoes, envio, ordenacao e intercalacao): %.2f ms%n",
                    (System.nanoTime() - inicio) / 1000000.0);
            if (saida != null) {
                StringBuilder texto = new StringBuilder();
                for (int valor : ordenado) texto.append(valor).append('\n');
                Files.write(saida, texto.toString().getBytes(StandardCharsets.UTF_8));
                System.out.println("Resultado completo salvo em " + saida.toAbsolutePath());
            }
        } catch (Exception erro) {
            if (erro instanceof InterruptedException) Thread.currentThread().interrupt();
            System.err.println("Ordenacao nao concluida: " + erro.getMessage());
            System.err.println("Uso: java -cp out Coordenador \"9,3,7,1\" host:porta [host:porta ...]");
            System.err.println("Ou:  java -cp out Coordenador --arquivo entrada.txt saida.txt host:porta [host:porta ...]");
            System.err.println("Sem argumentos: modo interativo.");
            System.exit(1);
        }
    }
}
