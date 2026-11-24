# Ordenação distribuída em Java

Adaptação do exemplo **2) 1 cliente (coordenador) e N servidores (trabalhadores)** do professor. Os exemplos originais foram preservados. Sem Maven, bibliotecas externas ou interface gráfica: usa terminal, threads, sockets TCP e objetos serializados.

## Funcionamento

1. O cliente escolhe um vetor de inteiros digitando os números ou informando um arquivo.
2. Conecta aos trabalhadores; cada um informa seu peso de capacidade.
3. Divide o vetor em partes proporcionais aos pesos.
4. Uma `ConversaComTrabalhador` por servidor envia as partes em paralelo.
5. A `SupervisoraDeConexao` de cada servidor ordena sua parte com `Arrays.sort` e devolve um `Resultado`.
6. O cliente **intercala** as partes ordenadas com uma fila de prioridade, produzindo o vetor inteiro em ordem crescente. Apenas concatenar as respostas não bastaria.

## Requisitos e compilação

JDK **17 ou superior** em todos os computadores (`java -version` e `javac -version`). O Java 8 sozinho não atende ao projeto.

Abra o PowerShell nesta pasta:

```powershell
.\compilar.ps1
```

Alternativa manual, com o JDK no PATH:

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 --release 17 -d out src/*.java
```

Compile somente esta pasta; os exemplos do professor têm classes com os mesmos nomes e não devem entrar na mesma compilação.

Se houver um JDK local em `../.tools/jdk*`, os scripts `compilar.ps1` e `executar.ps1` o usam automaticamente, sem alterar o Java do Windows. Nesse caso, substitua `java -cp out` por `.\executar.ps1` em todos os comandos abaixo, por exemplo:

```powershell
.\executar.ps1 Trabalhador 3001 1
.\executar.ps1 Trabalhador 3002 3
.\executar.ps1 Coordenador "9,3,7,1" localhost:3001 localhost:3002
```

Cada trabalhador deve ficar aberto no seu próprio terminal.

## Demonstração no mesmo PC

Abra três terminais nesta pasta. No primeiro, simule o PC menos capaz:

```powershell
java -cp out Trabalhador 3001 1
```

No segundo, o PC mais capaz:

```powershell
java -cp out Trabalhador 3002 3
```

No terceiro, execute o cliente:

```powershell
java -cp out Coordenador "9,3,7,1,8,2,6,0,5,4,-1,3" localhost:3001 localhost:3002
```

O primeiro recebe **3** elementos e o segundo **9**. Resultado:

```text
[-1, 0, 1, 2, 3, 3, 4, 5, 6, 7, 8, 9]
```

A ordem dos logs de envio pode variar porque as conversas são simultâneas.

Também é possível abrir o modo interativo:

```powershell
java -cp out Coordenador
```

Ou ler de arquivo e salvar o resultado completo (um inteiro por linha):

```powershell
java -cp out Coordenador --arquivo exemplo-vetor.txt resultado.txt localhost:3001 localhost:3002
```

O arquivo de saída é criado ou substituído apenas depois de todos os trabalhadores responderem. Entrada aceita espaços, vírgulas, ponto e vírgula e quebras de linha. Para vetores grandes, o terminal mostra somente os primeiros 50 elementos; use arquivo para salvar todos.

## Computadores diferentes

Copie esta pasta e compile em cada PC. Inicie um trabalhador em cada máquina e substitua `localhost` pelo IP dela no comando do coordenador, por exemplo:

```powershell
java -cp out Coordenador "9,3,7,1" 192.168.1.10:3001 192.168.1.20:3001
```

As máquinas devem conseguir se comunicar na rede; permita a porta escolhida no firewall dos servidores. É possível usar a mesma porta em PCs diferentes. Ctrl+C encerra cada trabalhador.

## Como definir o melhor e o pior PC

O segundo argumento do trabalhador é um **peso relativo manual**, entre 1 e 1000. Peso maior recebe uma parte maior, proporcionalmente. Exemplos: pesos 1 e 3 dividem aproximadamente 25% e 75%; pesos iguais dividem igualmente. Os elementos restantes são distribuídos pelo método dos maiores restos, com desempate pela ordem dos servidores.

Esta versão não mede o hardware automaticamente: o peso é informado por quem executa, conforme a capacidade conhecida ou medições anteriores. Assim, executar dois trabalhadores no mesmo PC demonstra a divisão, mas não simula diferenças reais de velocidade. Pesos representam uma estimativa; ordenação e transmissão têm custos distintos, portanto essa proporção não garante tempo mínimo.

Se houver mais servidores que elementos, alguns recebem partes vazias. Vetor vazio, negativos, repetidos e limites de `int` são aceitos. Limite por vetor: 10 milhões de inteiros; exige memória suficiente em cada processo. Prazo para conectar: 5 segundos; prazo de espera por leitura da conexão: 30 segundos. Uma falha interrompe a operação com mensagem de erro e código de saída 1, sem apresentar resultado parcial como completo.

## Relação com o código da aula

| Exemplo do professor | Adaptação |
| --- | --- |
| `Coordenador` divide intervalos de soma | Cliente escolhe vetor, divide por pesos e intercala respostas |
| `PedidoDeSoma` | `PedidoDeOrdenacao` transporta um pedaço do vetor |
| `Resultado` contém um `long` | `Resultado` contém um `int[]` ordenado |
| `Trabalhador`, `AceitadoraDeConexao`, `SupervisoraDeConexao` | Mantêm os papéis de servidor, aceitação e atendimento por thread |
| `Comunicado`, `Parceiro`, `PedidoParaSair` | Mantêm o protocolo baseado em objetos e a finalização da conexão |
| Divisão igual | `Capacidade` informa o peso; `OrdenacaoDistribuida` calcula as quantidades |

As classes de protocolo ficam em uma única pasta compartilhada, evitando manter cópias divergentes no cliente e no servidor. `Parceiro.receba` envia e `Parceiro.envie` recebe, mantendo a convenção do professor. Foram acrescentados fechamento de conexões, limites de espera e filtro das classes recebidas. Projeto didático para rede confiável, sem autenticação ou criptografia.

## Explicação de cada classe

As classes do sistema estão em `src/`. A classe de testes está em `test/`. Cliente e servidor usam as mesmas classes de mensagens para interpretar os objetos enviados pela rede.

### Coordenador

É o programa principal do **cliente**. Seu método `main` recebe os endereços dos servidores e o vetor, que pode vir dos argumentos, do teclado ou de um arquivo. O método `lerVetor` transforma o texto em um `int[]`, aceitando os separadores descritos acima. Depois, o coordenador chama `OrdenacaoDistribuida.ordenar`, mostra o resultado e o tempo total e, quando solicitado, salva o vetor ordenado em arquivo. Também apresenta os erros de entrada ou de execução.

### OrdenacaoDistribuida

Contém a lógica que organiza o trabalho entre os servidores. Seus três métodos principais são:

- `dividir`: calcula quantos elementos cada trabalhador receberá conforme seu peso. Usa o método dos maiores restos para distribuir os elementos que sobram do arredondamento. Por exemplo, 12 elementos com pesos 1 e 3 resultam em partes de 3 e 9 elementos.
- `ordenar`: abre as conversas com os trabalhadores, consulta os pesos, recorta o vetor e inicia uma thread por servidor. Aguarda as respostas, reúne as partes ordenadas e fecha as conexões, inclusive se ocorrer uma falha.
- `intercalar`: combina as partes já ordenadas em um único vetor crescente. Usa uma fila de prioridade para escolher, a cada passo, o menor elemento disponível entre as partes.

### ConversaComTrabalhador

Representa a comunicação do cliente com **um único servidor** e herda de `Thread`. No construtor, conecta ao endereço `host:porta` e recebe a capacidade informada pelo trabalhador. O método `setPedaco` define a parte que será enviada.

Quando a thread começa, o método `run` envia um `PedidoDeOrdenacao`, espera um `Resultado`, confere o tamanho e a ordem dos elementos recebidos e envia um `PedidoParaSair`. A resposta fica disponível em `getResultado`; se houver falha, esse método lança uma exceção. Como cada servidor tem sua própria conversa, os envios e as esperas pelas respostas podem ocorrer em paralelo.

### Trabalhador

É o programa principal do **servidor**. Seu método `main` lê a porta e o peso informados ao executar o programa, usando porta 3001 e peso 1 como padrões. Após validar os argumentos, cria e inicia uma `AceitadoraDeConexao`. O trabalhador permanece ativo aguardando clientes até ser encerrado, por exemplo, com Ctrl+C.

### AceitadoraDeConexao

É a thread que mantém o `ServerSocket` aberto na porta do trabalhador. Seu método `run` espera novas conexões usando `accept`. Para cada cliente conectado, cria uma `SupervisoraDeConexao`, permitindo que o servidor aceite outras conexões enquanto uma solicitação é atendida.

O método `getPorta` informa a porta utilizada, o que também permite aos testes descobrir uma porta escolhida automaticamente pelo sistema. O método `close` fecha o socket de escuta e encerra a aceitação de novas conexões.

### SupervisoraDeConexao

É a thread do servidor responsável por atender **uma conexão de cliente**. Primeiro, envia uma mensagem `Capacidade` com o peso do trabalhador. Em seguida, espera pedidos: ao receber um `PedidoDeOrdenacao`, obtém o pedaço do vetor, ordena com `Arrays.sort` e devolve um `Resultado`.

Ao receber um `PedidoParaSair`, encerra o atendimento. Também fecha a conexão quando o cliente desconecta ou ocorre um erro. A ordenação de cada pedaço acontece nesta classe.

### Parceiro

Encapsula a comunicação por um `Socket`, usando `ObjectOutputStream` para escrever objetos e `ObjectInputStream` para lê-los. É usado tanto pelo cliente quanto pelo servidor.

- `receba`: **envia** um `Comunicado` para a outra ponta da conexão.
- `envie`: **recebe** um `Comunicado` da outra ponta.
- `close`: fecha a conexão.

Os nomes `receba` e `envie` preservam a convenção do código do professor. A classe também configura o tempo de espera das leituras e filtra as classes permitidas na desserialização. Na abertura, envia o cabeçalho do fluxo de saída antes de criar o fluxo de entrada, evitando que os dois lados fiquem esperando esse cabeçalho um do outro.

### Comunicado

É a classe abstrata que serve de base para todas as mensagens do protocolo. Implementa `Serializable`, permitindo transformar essas mensagens em dados que podem ser transmitidos pelos fluxos de objetos. Não transporta dados próprios: cada subclasse define o conteúdo de um tipo de mensagem.

### Capacidade

É a mensagem enviada pelo servidor assim que a conexão é estabelecida. Guarda o peso relativo do trabalhador, validado no construtor entre 1 e 1000, e o disponibiliza por `getPeso`. O cliente usa esse valor para calcular o tamanho da parte destinada àquela máquina. O peso é configurado manualmente; esta classe não mede o desempenho do PC.

### PedidoDeOrdenacao

É a mensagem que o cliente envia para solicitar a ordenação de um pedaço do vetor. Armazena um `int[]`, acessível por `getVetor`. Tanto o construtor quanto o método de leitura fazem uma cópia do vetor, evitando que alterações externas modifiquem diretamente os dados guardados na mensagem.

### Resultado

É a mensagem de resposta do servidor. Transporta o pedaço já ordenado, que o cliente obtém por `getVetor` para depois intercalá-lo com as outras respostas. Assim como `PedidoDeOrdenacao`, faz cópias do vetor ao armazenar e devolver os dados.

### PedidoParaSair

É uma mensagem sem dados adicionais que informa ao servidor que o cliente terminou de usar aquela conexão. A `SupervisoraDeConexao` reconhece esse pedido e encerra o atendimento daquele cliente. O programa trabalhador continua disponível para novas conexões.

### Testes

É a classe executável de verificação, localizada em `test/Testes.java`. Seu método `main` testa a divisão proporcional, a leitura do vetor e a ordenação completa com dois servidores locais e sockets reais. Inclui vetores vazios, repetidos, negativos, extremos de `int`, um vetor grande, servidor indisponível e endereço duplicado.

O método auxiliar `conferirOrdenacao` compara o resultado distribuído com uma cópia ordenada localmente por `Arrays.sort` e verifica que o vetor de entrada foi preservado. O método `conferir` conta as verificações e lança um `AssertionError` quando alguma condição falha.

### Sequência das mensagens

Para cada trabalhador, a conversa segue esta ordem:

```text
Servidor -> Cliente: Capacidade(peso)
Cliente -> Servidor: PedidoDeOrdenacao(pedaco)
Servidor:           Arrays.sort(pedaco)
Servidor -> Cliente: Resultado(pedacoOrdenado)
Cliente -> Servidor: PedidoParaSair
```

Depois de receber os resultados de todos os trabalhadores, o cliente chama `intercalar` para montar o vetor final.

## Testes

Depois de compilar, usando o mesmo JDK:

```powershell
javac -encoding UTF-8 --release 17 -cp out -d out test/Testes.java
java -cp out Testes
```

Os testes iniciam dois servidores locais em portas disponíveis e usam sockets reais. Conferem proporção 1:3, arredondamento, vetor vazio, um elemento, repetições, negativos, extremos de inteiros, 100.001 números aleatórios, um único trabalhador e falha de conexão. Comparam a resposta distribuída com uma ordenação local de referência.
