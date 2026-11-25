package com.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Main {
    private static final String[][] ESTENSIONI_AMMESSE = {
        {"txt", "text/plain; charset=utf-8"},
        {"md", "text/markdown; charset=utf-8"},
        {"csv", "text/csv; charset=utf-8"}
    };
    private static String version = "ARCH/1.0";
    public static void main(String[] args) throws IOException {
        try (ServerSocket ss = new ServerSocket(3000)) {
            System.out.println("Server avviato sulla porta 3000...");

            while (true) {
                try (
                    Socket socket = ss.accept();
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
                ) {
                    String richiesta = in.readLine();
                    if (richiesta == null) {
                        continue;
                    }
                    System.out.println("Richiesta ricevuta: " + richiesta);

                    //Controllo richiesta vuota
                    String pulita = richiesta.trim();
                    if (pulita.isEmpty()) {
                        inviaRisposta(out, 400, "Bad Request", "text/plain; charset=utf-8", "Richiesta vuota.");
                        continue;
                    }

                    //Controllo comando
                    String[] parti = pulita.split("\\s+");
                    if (!parti[0].equals("LEGGI")) {
                        inviaRisposta(out, 501, "Not Implemented", "text/plain; charset=utf-8", "Comando \"" + parti[0] + "\" non supportato.");
                        continue;
                    }

                    //Controllo numero argomenti e validità del nome file
                    if (parti.length != 3 || !isNomeValido(parti[1]) || !isVersionValid(parti[2])) {
                        inviaRisposta(out, 400, "Bad Request", "text/plain; charset=utf-8", "Sintassi o nome file non valido.");
                        continue;
                    }

                    //Controllo versione
                    if (!parti[2].equals(version)){
                        inviaRisposta(out, 505, "Verision Not Supported", "text/plain; charset=utf-8","Versione " + parti[2] + " non supportata.");
                        continue;
                    }

                    //Ricerca file
                    String nomeBase = parti[1];
                    File cartella = new File("documenti");
                    File file = null;
                    String contentTypeTrovato = null;

                    for (String[] est : ESTENSIONI_AMMESSE) {
                        File candidato = new File(cartella, nomeBase + "." + est[0]);
                        if (candidato.isFile()) {
                            file = candidato;
                            contentTypeTrovato = est[1];
                            break;
                        }
                    }

                    if (file == null) {
                        inviaRisposta(out, 404, "Not Found", "text/plain; charset=utf-8", "Il file \"" + nomeBase + "\" non esiste.");
                        continue;
                    }

                    //Lettura e invio file
                    System.out.println("Cerco in: " + file.getAbsolutePath());
                    try {
                        byte[] dati = Files.readAllBytes(file.toPath());
                        String contenuto = new String(dati, StandardCharsets.UTF_8);
                        inviaRisposta(out, 200, "OK", contentTypeTrovato, contenuto);
                    } catch (IOException e) {
                        System.err.println("Errore nella lettura del file: " + e.getMessage());
                        inviaRisposta(out, 500, "Internal Server Error", "text/plain; charset=utf-8", "Errore interno durante la lettura del file.");
                    }
                }
            }
        }
    }

    private static void inviaRisposta(PrintWriter out, int codice, String frase, String contentType, String contenuto) {
        byte[] bytesContenuto = contenuto.getBytes(StandardCharsets.UTF_8);
        out.println(version + " " + codice + " " + frase);
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + bytesContenuto.length);
        out.println();
        out.print(contenuto);
        out.flush();
    }

    private static boolean isNomeValido(String nome) {
        if (nome == null || nome.isEmpty()) {
            return false;
        }
        return nome.contains("/") && !nome.contains(".") && !nome.contains("\\") && !nome.contains(" ");
    }
    private static boolean isVersionValid(String vers){
        String[] versDivisa = vers.split("(?<=/)");//utilizzo del lookbehind positivo per mantenere / nella stringa
        if(versDivisa[0].equals("ARCH/")){
            return true;
        }
        return false;
    }
}
