# Jogo da Forca Multijogador

Projeto académico desenvolvido em Java para a unidade curricular de Sistemas Distribuídos. A aplicação segue uma arquitetura cliente-servidor sobre TCP/IP, com um servidor central que mantém o estado do jogo e vários clientes de consola ligados em simultâneo.

## Descrição

O projeto implementa uma versão multijogador do jogo da forca. O servidor escolhe uma palavra aleatória a partir de uma lista fixa de 100 palavras e coordena a partida até existir vitória ou esgotarem-se as tentativas disponíveis.

Cada cliente estabelece uma ligação TCP ao servidor, recebe o estado atualizado do jogo através de mensagens de texto e envia as suas jogadas pelo mesmo canal.

## Funcionalidades implementadas

- Arquitetura cliente-servidor com `ServerSocket` e `Socket`
- Suporte para `2` a `4` jogadores ligados em simultâneo
- Seleção aleatória de uma palavra a partir de uma lista fixa de `100` palavras
- Lobby com arranque automático quando entram `4` jogadores ou quando termina o tempo de espera
- Rejeição de novas ligações com a mensagem `FULL` depois de o jogo começar ou quando o servidor está cheio
- Número fixo de tentativas partilhadas por todos os jogadores
- Tratamento sem distinção entre maiúsculas e minúsculas
- Tempo limite de `30` segundos por jogada
- Interface de consola com representação ASCII/ANSI do estado do jogo

## Modelo de jogo adotado

No projeto foi adotado um modelo de jogo por turnos rotativos. No estado atual do código:

- cada ronda tem apenas um jogador ativo;
- a mensagem `ROUND` indica explicitamente de quem é a vez;
- a mensagem enviada pelo cliente é `GUESS <round> <text>`;
- a mensagem `END WIN` identifica apenas um vencedor;
- o temporizador do lobby começa no arranque do servidor e não na entrada do primeiro jogador.

Esta solução mantém a comunicação cliente-servidor, o controlo do estado no servidor e o uso de tempos limite, e simplifica a coordenação das jogadas.

## Estrutura do projeto

```text
.
|-- README.md
|-- bare_jrnl.tex
|-- pom.xml
`-- src/main/java/com/hangman
    |-- client
    |   `-- Client.java
    |-- server
    |   |-- Server.java
    |   |-- ClientHandler.java
    |   `-- GameManager.java
    `-- shared
        |-- Protocol.java
        `-- WordList.java
```

## Organização do código

- `com.hangman.server.Server`
  ponto de entrada do servidor; aceita ligações, gere o lobby, inicia o jogo e coordena a sequência das rondas.
- `com.hangman.server.ClientHandler`
  representa um cliente ligado, envia mensagens ao respetivo socket e recolhe a jogada com tempo limite.
- `com.hangman.server.GameManager`
  mantém o estado partilhado do jogo: palavra, máscara, tentativas, letras usadas e vencedor.
- `com.hangman.client.Client`
  cliente de consola que trata as mensagens do servidor e envia a jogada do utilizador.
- `com.hangman.shared.Protocol`
  concentra os nomes das mensagens do protocolo e os parâmetros principais da aplicação.
- `com.hangman.shared.WordList`
  contém a lista fixa de 100 palavras usadas pelo servidor.

## Requisitos

- JDK 17 ou superior
- Maven 3.x

Para confirmar as versões instaladas:

```bash
java -version
mvn -version
```

## Compilação

Para compilar o projeto:

```bash
mvn clean compile
```

Para criar o ficheiro JAR do servidor:

```bash
mvn package
```

## Execução

### Iniciar o servidor

```bash
java -cp target/classes com.hangman.server.Server
```

Em alternativa, depois de `mvn package`:

```bash
java -jar target/hangman-server.jar
```

### Iniciar um cliente

```bash
java -cp target/classes com.hangman.client.Client
```

### Iniciar um cliente com endereço e porto explícitos

```bash
java -cp target/classes com.hangman.client.Client localhost 8080
```

Para testar a aplicação, abra um terminal para o servidor e dois a quatro terminais adicionais para os clientes.

## Configuração atual

Os parâmetros principais encontram-se em `src/main/java/com/hangman/shared/Protocol.java`:

- porto: `8080`
- número mínimo de jogadores: `2`
- número máximo de jogadores: `4`
- número máximo de tentativas: `6`
- tempo limite do lobby: `20000 ms`
- tempo limite da jogada: `30000 ms`

## Protocolo de comunicação implementado

A comunicação é feita através de mensagens de texto, uma por linha.

### Servidor para cliente

- `WELCOME <id> <players_total>`
  identifica o jogador e informa quantos clientes estão ligados.
- `START <mask> <attempts> <players_total>`
  anuncia o início do jogo e envia o estado inicial.
- `ROUND <round> <active_player_id> <mask> <attempts> <used_letters>`
  informa o número da ronda, o jogador ativo e o estado atual.
- `STATE <mask> <attempts> <used_letters>`
  atualiza o estado depois de uma jogada ser processada.
- `END WIN <winner_id> <word>`
  termina o jogo com vitória de um jogador.
- `END LOSE <word>`
  termina o jogo sem vencedor.
- `CANCEL <reason>`
  cancela o lobby quando não existem jogadores suficientes para iniciar a partida.
- `FULL`
  rejeita uma nova ligação.

### Cliente para servidor

- `GUESS <round> <text>`
  envia uma letra ou uma palavra associada à ronda atual.

## Fluxo de execução

1. O servidor arranca e fica à escuta no porto configurado.
2. Os clientes estabelecem a ligação TCP e recebem `WELCOME`.
3. O jogo começa quando entram quatro jogadores ou quando termina o tempo de espera do lobby, desde que existam pelo menos dois.
4. O servidor envia `START` para todos os clientes.
5. Em cada ronda, o servidor difunde `ROUND` e indica de quem é a vez.
6. Apenas o jogador ativo envia `GUESS`.
7. O servidor processa a jogada, atualiza o estado e difunde `STATE`.
8. O jogo termina com `END WIN` ou `END LOSE`, seguindo-se o fecho das ligações.

## Observações úteis para demonstração

- Se o servidor arrancar e não existirem pelo menos dois jogadores antes de terminar o tempo limite do lobby, a sessão é cancelada com `CANCEL`.
- Se um cliente tentar ligar depois do início da partida, recebe `FULL` e a ligação é encerrada.
- Se o jogador ativo não responder dentro do tempo limite, a jogada é tratada como vazia e consome uma tentativa.

## Relatório

O ficheiro `bare_jrnl.tex` contém a base do relatório técnico em LaTeX, adaptada a este projeto.
