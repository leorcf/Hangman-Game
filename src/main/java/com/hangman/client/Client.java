package com.hangman.client;

import com.hangman.shared.Protocol;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    // ── Cores ANSI ──────────────────────────────────────────────────────────
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String RED     = "\u001B[31m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String CYAN    = "\u001B[36m";
    private static final String WHITE   = "\u001B[37m";
    private static final String GRAY    = "\u001B[90m";

    // ── Estado do jogo (atualizado a cada mensagem) ──────────────────────────
    private static final Object SEND_LOCK = new Object();

    private static volatile int myId          = 0;
    private static volatile int totalPlayers  = 0;
    private static volatile int currentRound  = 0;
    private static volatile String mask       = "";
    private static volatile int attempts      = 0;
    private static volatile int maxAttempts   = Protocol.MAX_ATTEMPTS;
    private static volatile String usedLetters = "-";
    private static volatile boolean waitingForGuess = false;
    private static volatile boolean running = true;

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port    = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.PORT;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             Scanner sc = new Scanner(System.in)) {

            clearScreen();
            printBanner();

            System.out.println(CYAN + "  Ligado ao servidor " + host + ":" + port + RESET);
            System.out.println(GRAY + "  A aguardar início do jogo...\n" + RESET);

            startInputThread(out, sc);

            String line;
            while (running && (line = in.readLine()) != null) {
                handleMessage(line);
                if (!running) break;
            }
        } catch (ConnectException e) {
            System.out.println(RED + "\n  Não foi possível ligar ao servidor. Está em execução?" + RESET);
        } finally {
            running = false;
            waitingForGuess = false;
        }
    }

    // ── Handler de mensagens ─────────────────────────────────────────────────
    private static void handleMessage(String msg) {
        if (msg.startsWith(Protocol.FULL)) {
            waitingForGuess = false;
            running = false;
            clearScreen();
            System.out.println(RED + BOLD);
            System.out.println("  ╔══════════════════════════════╗");
            System.out.println("  ║   Servidor cheio!  (FULL)    ║");
            System.out.println("  ║   Tenta novamente mais tarde ║");
            System.out.println("  ╚══════════════════════════════╝");
            System.out.println(RESET);

        } else if (msg.startsWith(Protocol.WELCOME)) {
            String[] p = msg.split(" ");
            myId         = Integer.parseInt(p[1]);
            totalPlayers = Integer.parseInt(p[2]);
            clearScreen();
            printBanner();
            System.out.println(GREEN + "  Bem-vindo, Jogador #" + myId + "!" + RESET);
            System.out.println(GRAY  + "  Jogadores ligados: " + totalPlayers + " | A aguardar início...\n" + RESET);

        } else if (msg.startsWith(Protocol.START)) {
            String[] p = msg.split(" ");
            waitingForGuess = false;
            currentRound = 0;
            mask = p[1];
            attempts = Integer.parseInt(p[2]);
            usedLetters = "-";
            clearScreen();
            printGameBoard("JOGO INICIADO!", YELLOW);

        } else if (msg.startsWith(Protocol.ROUND)) {
            String[] p = msg.split(" ");
            currentRound = Integer.parseInt(p[1]);
            mask         = p[2];
            attempts     = Integer.parseInt(p[3]);
            usedLetters  = p[4];

            clearScreen();
            printGameBoard("Ronda " + currentRound, CYAN);
            waitingForGuess = true;
            askGuess();

        } else if (msg.startsWith(Protocol.STATE)) {
            String[] p = msg.split(" ");
            waitingForGuess = false;
            mask        = p[1];
            attempts    = Integer.parseInt(p[2]);
            usedLetters = p[3];
            clearScreen();
            printGameBoard("A aguardar outros jogadores...", GRAY);

        } else if (msg.startsWith(Protocol.CANCEL)) {
            waitingForGuess = false;
            running = false;
            String[] p = msg.split(" ", 2);
            String reason = p.length > 1 ? p[1] : "O jogo foi cancelado.";
            clearScreen();
            printCancel(reason);

        } else if (msg.startsWith(Protocol.END_WIN)) {
            String[] p = msg.split(" ");
            waitingForGuess = false;
            running = false;
            mask = p[3];
            clearScreen();
            printWin(p[2], p[3]);

        } else if (msg.startsWith(Protocol.END_LOSE)) {
            String[] p = msg.split(" ");
            waitingForGuess = false;
            running = false;
            clearScreen();
            printLose(p[2]);
        }
    }

    // ── Pede jogada ao utilizador ────────────────────────────────────────────
    private static void askGuess() {
        System.out.println();
        System.out.print(BOLD + YELLOW + "  ➤ A tua jogada (letra ou palavra): " + RESET);
        System.out.flush();
    }

    private static void startInputThread(PrintWriter out, Scanner sc) {
        Thread inputThread = new Thread(() -> {
            while (running) {
                String guess;
                try {
                    guess = sc.nextLine().trim();
                } catch (Exception e) {
                    return;
                }

                int roundToSend = currentRound;
                if (!waitingForGuess) {
                    continue;
                }

                synchronized (SEND_LOCK) {
                    out.println(Protocol.GUESS + " " + roundToSend + " " + guess);
                }

                waitingForGuess = false;
            }
        }, "console-input");

        inputThread.setDaemon(true);
        inputThread.start();
    }

    // ── Ecrã principal do jogo ───────────────────────────────────────────────
    private static void printGameBoard(String title, String titleColor) {
        System.out.println(BOLD + WHITE);
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.printf ("  ║  %-40s║%n", titleColor + title + WHITE);
        System.out.println("  ╠══════════════════════════════════════════╣");

        // Forca ASCII
        String[] gallows = buildGallows(maxAttempts - attempts);
        for (String row : gallows) {
            System.out.printf("  ║  %-40s║%n", row);
        }

        System.out.println("  ╠══════════════════════════════════════════╣");

        // Palavra com espaços entre letras
        String displayMask = mask.replace("", " ").trim().toUpperCase();
        System.out.printf("  ║  %-40s║%n", CYAN + "Palavra:  " + BOLD + displayMask + WHITE);

        // Barra de tentativas
        String bar = buildAttemptsBar(attempts, maxAttempts);
        System.out.printf("  ║  %-40s║%n", "Vidas:    " + bar);

        // Letras usadas
        String letras = usedLetters.equals("-") ? "nenhuma" : usedLetters.toUpperCase();
        System.out.printf("  ║  %-40s║%n", GRAY + "Letras:   " + letras + WHITE);

        // Info do jogador
        System.out.printf("  ║  %-40s║%n", GRAY + "Jogador:  #" + myId + " de " + totalPlayers + WHITE);

        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    // ── Forca ASCII por nível de erros ──────────────────────────────────────
    private static String[] buildGallows(int errors) {
        String head  = errors >= 1 ? RED + "O" + WHITE : " ";
        String body  = errors >= 2 ? RED + "|" + WHITE : " ";
        String larm  = errors >= 3 ? RED + "/" + WHITE : " ";
        String rarm  = errors >= 4 ? RED + "\\" + WHITE : " ";
        String lleg  = errors >= 5 ? RED + "/" + WHITE : " ";
        String rleg  = errors >= 6 ? RED + "\\" + WHITE : " ";

        return new String[]{
            WHITE + "     ____",
            WHITE + "    |    |",
            WHITE + "    |    " + head,
            WHITE + "    |   " + larm + body + rarm,
            WHITE + "    |   " + lleg + " " + rleg,
            WHITE + "    |",
            WHITE + "  __|__"
        };
    }

    // ── Barra de tentativas ──────────────────────────────────────────────────
    private static String buildAttemptsBar(int left, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(i < left ? GREEN + "█" : RED + "░");
        }
        sb.append(RESET + WHITE + " (" + left + "/" + max + ")");
        return sb.toString();
    }

    // ── Ecrã de vitória ─────────────────────────────────────────────────────
    private static void printWin(String winnerIds, String word) {
        System.out.println(BOLD + GREEN);
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║         🎉  VITÓRIA!  🎉                 ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf ("  ║  %-41s║%n", " Vencedores: Jogador(es) #" + winnerIds);
        System.out.printf ("  ║  %-41s║%n", " Palavra: " + word.toUpperCase());
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    // ── Ecrã de derrota ─────────────────────────────────────────────────────
    private static void printLose(String word) {
        System.out.println(BOLD + RED);
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║         💀  DERROTA!  💀                 ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf ("  ║  %-41s║%n", " A palavra era: " + word.toUpperCase());
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    private static void printCancel(String reason) {
        System.out.println(BOLD + YELLOW);
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║          JOGO CANCELADO                 ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf ("  ║  %-41s║%n", " " + reason);
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    // ── Banner inicial ───────────────────────────────────────────────────────
    private static void printBanner() {
        System.out.println(BOLD + CYAN);
        System.out.println("  ██╗  ██╗ █████╗ ███╗  ██╗ ██████╗ ███╗  ███╗ █████╗ ███╗  ██╗");
        System.out.println("  ██║  ██║██╔══██╗████╗ ██║██╔════╝ ████╗████║██╔══██╗████╗ ██║");
        System.out.println("  ███████║███████║██╔██╗██║██║  ███╗██╔████╔██║███████║██╔██╗██║");
        System.out.println("  ██╔══██║██╔══██║██║╚████║██║   ██║██║╚██╔╝██║██╔══██║██║╚████║");
        System.out.println("  ██║  ██║██║  ██║██║ ╚███║╚██████╔╝██║ ╚═╝ ██║██║  ██║██║ ╚███║");
        System.out.println("  ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚══╝ ╚═════╝ ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚══╝");
        System.out.println("                     ✦ MULTIPLAYER ✦");
        System.out.println(RESET);
    }

    // ── Limpa o ecrã ────────────────────────────────────────────────────────
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}