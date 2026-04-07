package com.hangman.shared;

public class Protocol {
    // Servidor → Cliente
    public static final String WELCOME = "WELCOME";
    public static final String START   = "START";
    public static final String ROUND   = "ROUND";
    public static final String STATE   = "STATE";
    public static final String CANCEL  = "CANCEL";
    public static final String END_WIN  = "END WIN";
    public static final String END_LOSE = "END LOSE";
    public static final String FULL    = "FULL";

    // Cliente → Servidor
    public static final String GUESS   = "GUESS";

    // Configurações
    public static final int MAX_PLAYERS       = 4;
    public static final int MIN_PLAYERS       = 2;
    public static final int MAX_ATTEMPTS      = 6;
    public static final int LOBBY_TIMEOUT_MS  = 20000;  // 20 segundos
    public static final int ROUND_TIMEOUT_MS  = 30000;  // 30 segundos
    public static final int PORT              = 8080;
}