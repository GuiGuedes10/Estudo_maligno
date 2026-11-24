public class Trabalhador {
    public static void main(String[] args) {
        try {
            if (args.length > 2) throw new IllegalArgumentException("Argumentos em excesso.");
            int porta = args.length > 0 ? Integer.parseInt(args[0]) : 3001;
            int peso = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            if (porta < 1 || porta > 65535)
                throw new IllegalArgumentException("Porta deve estar entre 1 e 65535.");
            try (AceitadoraDeConexao servidor = new AceitadoraDeConexao(porta, peso)) {
                servidor.start();
                System.out.println("Trabalhador ativo na porta " + porta + ", peso " + peso + ". Ctrl+C para sair.");
                servidor.join();
            }
        } catch (Exception erro) {
            System.err.println("Erro: " + erro.getMessage());
            System.err.println("Uso: java -cp out Trabalhador [porta] [peso de 1 a 1000]");
            System.exit(1);
        }
    }
}
