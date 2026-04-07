package com.hangman.server;

import com.hangman.shared.Protocol;
import com.hangman.shared.WordList;

import java.util.*;

public class GameManager {
    private final String word;
    private final char[] mask;
    private int attemptsLeft;
    private final Set<Character> usedLetters;
    private final Set<Integer> winnerIds;

    public GameManager() {
        this.word         = WordList.getRandom().toLowerCase();
        this.mask         = new char[word.length()];
        this.attemptsLeft = Protocol.MAX_ATTEMPTS;
        this.usedLetters  = new LinkedHashSet<>();
        this.winnerIds    = new LinkedHashSet<>();
        Arrays.fill(mask, '_');
    }

    // Processa a jogada de um jogador — devolve true se contribuiu para o jogo
    public synchronized boolean processGuess(int playerId, String guess) {
        if (guess == null || guess.isBlank()) return false;

        guess = guess.trim().toLowerCase();

        // Proposta de palavra completa
        if (guess.length() > 1) {
            if (guess.equals(word)) {
                // Revela a palavra toda
                for (int i = 0; i < word.length(); i++) mask[i] = word.charAt(i);
                winnerIds.add(playerId);
                return true;
            }
            return false; // palavra errada
        }

        // Proposta de letra
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

        // Se a palavra ficou completa após esta letra
        if (found && getMask().equals(word)) winnerIds.add(playerId);

        return found;
    }

    public synchronized void consumeAttempt() {
        if (attemptsLeft > 0) attemptsLeft--;
    }

    public synchronized boolean isWordGuessed() {
        return getMask().equals(word);
    }

    public synchronized boolean isGameOver() {
        return attemptsLeft <= 0 || isWordGuessed();
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

    public synchronized List<Integer> getWinnerIds() {
        return new ArrayList<>(winnerIds);
    }

    public String getWord() {
        return word;
    }
}