package com.hangman.shared;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class WordList {
    private static final List<String> WORDS = Arrays.asList(
        "abacate", "biblioteca", "computador", "dinossauro", "elefante",
        "fotografia", "guitarra", "horizonte", "internet", "janela",
        "karate", "laranja", "montanha", "notebook", "oceano",
        "palmeira", "quadrado", "relampago", "serpente", "telefone",
        "universo", "ventania", "xadrez", "yogurte", "zebra",
        "alpinismo", "borboleta", "caverna", "diamante", "estrela",
        "floresta", "girassol", "hospital", "iceberg", "jardim",
        "kilowatt", "lagosta", "musica", "nuvem", "orquestra",
        "pirata", "queijo", "robô", "submarino", "tartaruga",
        "urso", "violino", "wizard", "xenofobia", "yakuza",
        "abismo", "bicicleta", "crocodilo", "dragao", "espelho",
        "furacão", "golfinho", "hipopótamo", "ilha", "juridico",
        "koala", "leopardo", "microfone", "navegador", "oxigenio",
        "papagaio", "quimera", "rinoceronte", "satelite", "tomate",
        "ursula", "vampiro", "windsurf", "xilofone", "zodiaco",
        "asterisco", "baleia", "canivete", "deserto", "enxofre",
        "fantasma", "gorila", "harpa", "inverno", "joelho",
        "kilometre", "lâmpada", "mosquito", "nave", "osmose",
        "pinguim", "quartzo", "raposa", "salamandra", "trovao",
        "ungulado", "vespa", "wolverine", "xenonio", "zumbi"
    );

    public static String getRandom() {
        return WORDS.get(new Random().nextInt(WORDS.size()));
    }
}