# Hangman-Game

Projeto de Jogo da Forca multiplayer em Java, baseado em arquitetura cliente-servidor com comunicacao por sockets TCP.

## O que ja esta feito

- Servidor TCP para gerir o jogo multiplayer.
- Cliente de terminal com interface ASCII/ANSI.
- Ligacao de multiplos jogadores ao mesmo servidor.
- Lobby com minimo de 2 jogadores e maximo de 4 jogadores.
- Timeout de lobby de 20 segundos.
- Inicio automatico do jogo quando entram 4 jogadores.
- Cancelamento do lobby se nao existirem jogadores suficientes para arrancar.
- Rejeicao de novos clientes com mensagem `FULL` quando o jogo ja começou ou o servidor esta cheio.
- Escolha aleatoria da palavra a partir de uma lista local.
- Mascara da palavra com atualizacao progressiva e visivel para todos os jogadores.
- Controlo partilhado de letras usadas.
- Controlo partilhado de tentativas restantes.
- Rondas com timeout de 30 segundos por jogada.
- Jogo competitivo por turnos: joga apenas um jogador de cada vez.
- Palavra unica e partilhada por todos os jogadores.
- Processamento de jogadas por letra ou por palavra completa.
- Associacao da jogada ao numero da ronda para evitar jogadas atrasadas entrarem na ronda errada.
- Mensagens de protocolo para `WELCOME`, `START`, `ROUND`, `STATE`, `CANCEL`, `END WIN`, `END LOSE` e `FULL`.
- Encerramento do jogo com um unico vencedor ou derrota coletiva.
- Remocao de jogadores desligados durante a partida.

## Configuracao atual

- Porta: `8080`
- Jogadores minimos: `2`
- Jogadores maximos: `4`
- Numero maximo de tentativas: `6`
- Timeout do lobby: `20000 ms`
- Timeout de cada ronda: `30000 ms`

## Estrutura principal

- `src/main/java/com/hangman/server/Server.java`: aceita ligacoes, gere lobby, rondas e fim de jogo.
- `src/main/java/com/hangman/server/ClientHandler.java`: gere a comunicacao com cada jogador.
- `src/main/java/com/hangman/server/GameManager.java`: guarda o estado global da palavra, tentativas, letras usadas e vencedor.
- `src/main/java/com/hangman/client/Client.java`: cliente de terminal.
- `src/main/java/com/hangman/shared/Protocol.java`: constantes do protocolo.
- `src/main/java/com/hangman/shared/WordList.java`: lista de palavras.

## Como correr

Compilar:

```bash
mvn compile
```

Arrancar o servidor:

```bash
java -cp target/classes com.hangman.server.Server
```

Arrancar um cliente:

```bash
java -cp target/classes com.hangman.client.Client
```

Arrancar um cliente com host e porta explicitos:

```bash
java -cp target/classes com.hangman.client.Client localhost 8080
```
