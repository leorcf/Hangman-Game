package com.hangman.server;

import com.hangman.shared.Protocol;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int playerId;
    private final Server server;

    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean closed = false;

    public ClientHandler(Socket socket, int playerId, Server server) {
        this.socket   = socket;
        this.playerId = playerId;
        this.server   = server;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            server.playerReady(this);

            synchronized (this) {
                while (!closed) wait();
            }

        } catch (Exception e) {
            if (!closed) {
                System.out.println("[Player " + playerId + "] Desligou: " + e.getMessage());
            }
        } finally {
            close();
        }
    }

    // Envia mensagem ao cliente
    public synchronized boolean send(String message) {
        if (closed || out == null) return false;

        out.println(message);
        if (out.checkError()) {
            close();
            return false;
        }

        return true;
    }

    // Recolhe a jogada do cliente com timeout
    public String collectGuess(int expectedRound) {
        if (closed || in == null) return "";

        try {
            socket.setSoTimeout(Protocol.ROUND_TIMEOUT_MS);

            while (!closed) {
                String line = in.readLine();
                if (line == null) {
                    close();
                    return "";
                }

                if (!line.startsWith(Protocol.GUESS + " ")) {
                    continue;
                }

                String[] parts = line.split(" ", 3);
                if (parts.length < 3) {
                    continue;
                }

                int roundNumber;
                try {
                    roundNumber = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (roundNumber != expectedRound) {
                    continue;
                }

                return parts[2].trim();
            }
        } catch (java.net.SocketTimeoutException e) {
            return "";
        } catch (IOException e) {
            if (!closed) {
                System.out.println("[Player " + playerId + "] Erro ao receber jogada: " + e.getMessage());
            }
            close();
            return "";
        } finally {
            try {
                socket.setSoTimeout(0);
            } catch (IOException ignored) {}
        }

        return "";
    }

    public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            notifyAll();
        }

        try { socket.close(); } catch (Exception ignored) {}
        server.playerDisconnected(this);
    }

    public int getPlayerId() { return playerId; }

    public boolean isConnected() {
        return !closed && !socket.isClosed();
    }
}