import java.util.*;

public class OrdenacaoDistribuida {
    // Metodo dos maiores restos: distribui todos os elementos, sem perdas.
    public static int[] dividir(int tamanho, int[] pesos) {
        if (tamanho < 0 || pesos.length == 0) throw new IllegalArgumentException("Divisao invalida.");
        long total = 0;
        for (int peso : pesos) {
            if (peso < 1 || peso > 1000) throw new IllegalArgumentException("Peso invalido.");
            total += peso;
        }
        int[] quantidades = new int[pesos.length];
        long[] restos = new long[pesos.length];
        int distribuidos = 0;
        for (int i = 0; i < pesos.length; i++) {
            long produto = (long) tamanho * pesos[i];
            quantidades[i] = (int) (produto / total);
            restos[i] = produto % total;
            distribuidos += quantidades[i];
        }
        for (int falta = tamanho - distribuidos; falta > 0; falta--) {
            int maior = 0;
            for (int i = 1; i < restos.length; i++)
                if (restos[i] > restos[maior]) maior = i;
            quantidades[maior]++;
            restos[maior] = -1;
        }
        return quantidades;
    }

    // Intercalacao de K partes: O(N log K). Concatenar nao ordenaria o vetor final.
    public static int[] intercalar(List<int[]> partes) {
        PriorityQueue<int[]> fila = new PriorityQueue<>((a, b) ->
                Integer.compare(partes.get(a[0])[a[1]], partes.get(b[0])[b[1]]));
        int tamanho = 0;
        for (int i = 0; i < partes.size(); i++) {
            tamanho = Math.addExact(tamanho, partes.get(i).length);
            if (partes.get(i).length > 0) fila.add(new int[] {i, 0});
        }
        int[] ordenado = new int[tamanho];
        int destino = 0;
        while (!fila.isEmpty()) {
            int[] cursor = fila.remove();
            int[] parte = partes.get(cursor[0]);
            ordenado[destino++] = parte[cursor[1]++];
            if (cursor[1] < parte.length) fila.add(cursor);
        }
        return ordenado;
    }

    public static int[] ordenar(int[] vetor, String[] enderecos) throws Exception {
        if (vetor.length > 10000000) throw new IllegalArgumentException("Limite: 10 milhoes de elementos.");
        if (enderecos.length == 0) throw new IllegalArgumentException("Informe pelo menos um trabalhador.");
        if (new HashSet<>(Arrays.asList(enderecos)).size() != enderecos.length)
            throw new IllegalArgumentException("Nao repita o mesmo endereco de trabalhador.");
        List<ConversaComTrabalhador> conversas = new ArrayList<>();
        try {
            int[] pesos = new int[enderecos.length];
            for (int i = 0; i < enderecos.length; i++) {
                ConversaComTrabalhador conversa = new ConversaComTrabalhador(enderecos[i]);
                conversas.add(conversa);
                pesos[i] = conversa.getPeso();
            }
            int[] quantidades = dividir(vetor.length, pesos);
            int inicio = 0;
            for (int i = 0; i < conversas.size(); i++) {
                int fim = inicio + quantidades[i];
                conversas.get(i).setPedaco(Arrays.copyOfRange(vetor, inicio, fim));
                inicio = fim;
            }
            for (ConversaComTrabalhador conversa : conversas) conversa.start();
            List<int[]> partes = new ArrayList<>();
            for (ConversaComTrabalhador conversa : conversas) {
                conversa.join();
                partes.add(conversa.getResultado());
            }
            return intercalar(partes);
        } finally {
            // Inclui conexoes abertas antes de uma eventual falha de outro servidor.
            for (ConversaComTrabalhador conversa : conversas)
                try { conversa.close(); } catch (java.io.IOException erro) { /* Ja fechada. */ }
        }
    }
}
