package com.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Main {
    private static String[] [] ESTENSIONI_AMMESSE = {
                                                    {"txt", "text/plain; charset=utf-8"}, 
                                                    {"md", "text/markdown; charset=utf-8"}, 
                                                    {"csv", "text/csv; charset=utf-8"}                                                
                                                };
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
                    // Se il client chiude senza inviare nulla
                    if (richiesta == null) {
                        continue;
                    }
                    System.out.println("Richiesta ricevuta: " + richiesta);
                    //Controllo richiesta vuota
                    String pulita = richiesta.trim();
                    if (pulita.isEmpty()) {
                        out.println("ERR SYNTAX");
                        out.println();
                        continue;
                    }
                    //Controllo comando
                    String[] parti = pulita.split("\\s+");
                    if (!parti[0].equals("LEGGI")) {
                        out.println("ERR UNKNOWNCMD");
                        out.println();
                        continue;
                    }
                    //Controllo numero argomenti e validità del nome file
                    if (parti.length != 2 || !isNomeValido(parti[1])) {
                        out.println("ERR SYNTAX");
                        out.println();
                        continue;
                    }
                    String nomeBase = parti[1];
                    File cartella = new File("documenti");
                    File file = null;
                    String estensioneTrovata = null;

                    for (String[] est : ESTENSIONI_AMMESSE) {
                        File candidato = new File(cartella, nomeBase + "." + est[0]);
                        if (candidato.isFile()) {
                            file = candidato;
                            estensioneTrovata = est[1];
                            break;
                        }
                    }
                    if (file == null) {
                        out.println("ERR NOTFOUND");
                        out.println();
                        continue;
                    }
                    System.out.println("Cerco in: " + file.getAbsolutePath());
                    try (BufferedReader lettoreFile = new BufferedReader(
                            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                        byte[] dati = Files.readAllBytes(file.toPath());
                        String contenuto = new String(dati, StandardCharsets.UTF_8);
                        out.println("OK FILE");
                        out.println("TYPE " + estensioneTrovata);
                        out.println("Content-Length: " + dati.length);
                        out.println();
                        out.print(contenuto);
                        out.flush(); 
                    } catch (IOException e) {
                    System.err.println("Errore nella gestione della connessione: " + e.getMessage());
                    }   
                }   
            }
        }
    }
    private static boolean isNomeValido(String nome) {
        if (nome == null || nome.isEmpty()) {
            return false;
        }
        if (nome.contains(".") || nome.contains("/") || nome.contains("\\") || nome.contains(" ")) {
            return false;
        }
        return true;
    }
}