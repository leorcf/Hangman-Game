package com.hangman.server;

import com.hangman.shared.Protocol;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private final List<ClientHandler> players = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private volatile ServerSocket serverSocket;
    private volatile GameManager gameManager;
    private volatile boolean gameStarted = false;
    private volatile boolean serverRunning = true;

    public static void main(String[] args) throws IOException {
        new Server().start();
    }

    public void start() throws IOException {
        System.out.println("[Server] A aguardar jogadores na porta " + Protocol.PORT + "...");
        startLobbyTimer();

        try (ServerSocket localServerSocket = new ServerSocket(Protocol.PORT)) {
            serverSocket = localServerSocket;
            localServerSocket.setSoTimeout(1000);

            while (serverRunning) {
                try {
                    Socket socket = localServerSocket.accept();
                    handleIncomingConnection(socket);
                } catch (SocketTimeoutException ignored) {
                    pruneDisconnectedPlayers();
                } catch (SocketException e) {
                    if (serverRunning) throw e;
                }
            }
        } finally {
            serverSocket = null;
        }
    }

    private void startLobbyTimer() {
        Thread lobbyTimer = new Thread(() -> {
            try {
                Thread.sleep(Protocol.LOBBY_TIMEOUT_MS);
                startGame();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "lobby-timer");

        lobbyTimer.setDaemon(true);
        lobbyTimer.start();
    }

    private void handleIncomingConnection(Socket socket) throws IOException {
        synchronized (this) {
            pruneDisconnectedPlayers();

            if (gameStarted || players.size() >= Protocol.MAX_PLAYERS) {
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                pw.println(Protocol.FULL);
                socket.close();
                return;
            }

            int id = nextPlayerId.getAndIncrement();
            ClientHandler handler = new ClientHandler(socket, id, this);
            players.add(handler);
            new Thread(handler, "client-" + id).start();
            System.out.println("[Server] Jogador " + id + " ligado.");
        }
    }

    public synchronized void playerReady(ClientHandler handler) {
        if (!players.contains(handler)) {
            handler.close();
            return;
        }

        if (!handler.send(Protocol.WELCOME + " " + handler.getPlayerId() + " " + players.size())) {
            playerDisconnected(handler);
            return;
        }

        if (players.size() == Protocol.MAX_PLAYERS) startGame();
    }

    public synchronized void playerDisconnected(ClientHandler handler) {
        if (!players.remove(handler)) return;

        System.out.println("[Server] Jogador " + handler.getPlayerId() + " desligou.");
    }

    private synchronized void startGame() {
        if (gameStarted) return;

        pruneDisconnectedPlayers();

        if (players.size() < Protocol.MIN_PLAYERS) {
            cancelLobby("Jogadores insuficientes para iniciar o jogo.");
            return;
        }

        gameStarted = true;
        gameManager = new GameManager();

        System.out.println("[Server] Jogo iniciado com " + players.size()
                + " jogadores! Palavra: " + gameManager.getWord());

        broadcast(Protocol.START + " " + gameManager.getMask()
                + " " + gameManager.getAttemptsLeft()
                + " " + players.size());

        new Thread(this::runGame, "game-loop").start();
    }

    private void runGame() {
        int round = 1;
        int currentTurnIndex = 0;

        while (!gameManager.isGameOver()) {
            pruneDisconnectedPlayers();
            if (players.isEmpty()) break;

            currentTurnIndex = normalizeTurnIndex(currentTurnIndex);
            ClientHandler activePlayer = players.get(currentTurnIndex);
            int currentRound = round;

            broadcast(Protocol.ROUND + " " + currentRound
                    + " " + activePlayer.getPlayerId()
                    + " " + gameManager.getMask()
                    + " " + gameManager.getAttemptsLeft()
                    + " " + gameManager.getUsedLetters());
            System.out.println("[Server] Ronda " + currentRound + " - vez do Jogador " + activePlayer.getPlayerId() + ".");

            String guess = activePlayer.collectGuess(currentRound);

            pruneDisconnectedPlayers();
            if (players.isEmpty()) break;

            if (!players.contains(activePlayer)) {
                currentTurnIndex = normalizeTurnIndex(currentTurnIndex);
                continue;
            }

            boolean progress = gameManager.processGuess(activePlayer.getPlayerId(), guess);
            if (!progress) {
                gameManager.consumeAttempt();
            }
            logPlayerAction(activePlayer.getPlayerId(), guess, progress);

            broadcast(Protocol.STATE + " " + gameManager.getMask()
                    + " " + gameManager.getAttemptsLeft()
                    + " " + gameManager.getUsedLetters());

            if (gameManager.isGameOver()) break;

            int activeIndex = players.indexOf(activePlayer);
            if (activeIndex == -1) {
                currentTurnIndex = normalizeTurnIndex(currentTurnIndex);
            } else {
                currentTurnIndex = (activeIndex + 1) % players.size();
            }

            round++;
        }

        endGame();
    }

    private synchronized void cancelLobby(String reason) {
        if (!players.isEmpty()) {
            broadcast(Protocol.CANCEL + " " + reason);
            for (ClientHandler player : players) player.close();
        }

        System.out.println("[Server] " + reason);
        shutdownServer();
    }

    private void endGame() {
        pruneDisconnectedPlayers();

        if (!players.isEmpty()) {
            Integer winnerId = gameManager.getWinnerId();
            if (winnerId != null) {
                broadcast(Protocol.END_WIN + " " + winnerId + " " + gameManager.getWord());
            } else {
                broadcast(Protocol.END_LOSE + " " + gameManager.getWord());
            }

            for (ClientHandler player : players) player.close();
        }

        System.out.println("[Server] Jogo terminado.");
        shutdownServer();
    }

    private void broadcast(String message) {
        for (ClientHandler player : players) {
            if (!player.send(message)) {
                players.remove(player);
            }
        }
    }

    private synchronized void shutdownServer() {
        serverRunning = false;
        ServerSocket localServerSocket = serverSocket;
        if (localServerSocket == null || localServerSocket.isClosed()) return;

        try {
            localServerSocket.close();
        } catch (IOException ignored) {}
    }

    private void pruneDisconnectedPlayers() {
        for (ClientHandler player : players) {
            if (!player.isConnected()) {
                players.remove(player);
            }
        }
    }

    private int normalizeTurnIndex(int currentTurnIndex) {
        if (players.isEmpty()) return 0;
        return currentTurnIndex % players.size();
    }

    private void logPlayerAction(int playerId, String guess, boolean progress) {
        if (guess == null || guess.isBlank()) {
            System.out.println("[Server] Jogador " + playerId + ": sem resposta (timeout).");
            return;
        }

        String normalizedGuess = guess.trim().toUpperCase();
        String outcome = progress ? "acertou" : "falhou";
        System.out.println("[Server] Jogador " + playerId + ": " + normalizedGuess + " -> " + outcome + ".");
    }
}
