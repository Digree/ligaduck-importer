package com.example.ligaduckimporter.service;

import com.example.ligaduckimporter.model.Giocatore;
import com.example.ligaduckimporter.model.Partita;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ImportClientService {

    private final RestClient restClient;
    private final String importUrl;

    public ImportClientService(
            RestClient.Builder restClientBuilder,
            @Value("${import.target-url:https://ligaduck-bff-465861915593.europe-west1.run.app}") String importUrl) {
        this.restClient = restClientBuilder.build();
        this.importUrl = importUrl;
    }

    public ResponseEntity<String> inviaGiocatori(String campionato, List<Giocatore> giocatori) {
        return restClient.post()
                .uri(importUrl + "/{campionato}/import/giocatori", campionato)
                .body(giocatori)
                .retrieve()
                .toEntity(String.class);
    }

    public ResponseEntity<String> inviaPartita(String campionato, List<Partita> partite, String idGiornata) {
        return restClient.post()
                .uri(importUrl + "/{campionato}/import/partita/{idGiornata}", campionato, idGiornata)
                .body(partite)
                .retrieve()
                .toEntity(String.class);
    }
}
