package com.hangman.server;

import com.hangman.shared.Protocol;
import com.hangman.shared.WordList;

import java.util.*;

public class GameManager {
    private final String word;
    private final char[] mask;
    private int attemptsLeft;
    private final Set<Character> usedLetters;
    private Integer winnerId;

    public GameManager() {
        this.word = WordList.getRandom().toLowerCase();
        this.mask = new char[word.length()];
        this.attemptsLeft = Protocol.MAX_ATTEMPTS;
        this.usedLetters = new LinkedHashSet<>();
        this.winnerId = null;
        Arrays.fill(mask, '_');
    }

    // Processa a jogada de um jogador; devolve true se revelou letras ou acertou na palavra.
    public synchronized boolean processGuess(int playerId, String guess) {
        if (guess == null || guess.isBlank() || isGameOver()) return false;

        guess = guess.trim().toLowerCase();

        if (guess.length() > 1) {
            if (guess.equals(word)) {
                for (int i = 0; i < word.length(); i++) {
                    mask[i] = word.charAt(i);
                }
                winnerId = playerId;
                return true;
            }
            return false;
        }

        char letter = guess.charAt(0);
        if (usedLetters.contains(letter)) return false;
        usedLetters.add(letter);

        boolean found = false;
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == letter) {
                mask[i] = letter;
                found = true;
            }
        }

        if (found && getMask().equals(word) && winnerId == null) {
            winnerId = playerId;
        }

        return found;
    }

    public synchronized void consumeAttempt() {
        if (attemptsLeft > 0) attemptsLeft--;
    }

    public synchronized boolean isGameOver() {
        return attemptsLeft <= 0 || winnerId != null;
    }

    public synchronized String getMask() {
        return new String(mask);
    }

    public synchronized int getAttemptsLeft() {
        return attemptsLeft;
    }

    public synchronized String getUsedLetters() {
        if (usedLetters.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (char c : usedLetters) sb.append(c).append(',');
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    public synchronized Integer getWinnerId() {
        return winnerId;
    }

    public String getWord() {
        return word;
    }
}
