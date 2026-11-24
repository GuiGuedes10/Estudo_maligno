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

## Testes

Depois de compilar, usando o mesmo JDK:

```powershell
javac -encoding UTF-8 --release 17 -cp out -d out test/Testes.java
java -cp out Testes
```

Os testes iniciam dois servidores locais em portas disponíveis e usam sockets reais. Conferem proporção 1:3, arredondamento, vetor vazio, um elemento, repetições, negativos, extremos de inteiros, 100.001 números aleatórios, um único trabalhador e falha de conexão. Comparam a resposta distribuída com uma ordenação local de referência.
