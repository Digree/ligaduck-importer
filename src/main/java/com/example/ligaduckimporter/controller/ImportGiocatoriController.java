package com.example.ligaduckimporter.controller;

import com.example.ligaduckimporter.model.Giocatore;
import com.example.ligaduckimporter.service.ImportClientService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/import/giocatori")
public class ImportGiocatoriController {

    private final ImportClientService importClientService;

    public ImportGiocatoriController(ImportClientService importClientService) {
        this.importClientService = importClientService;
    }

    @PostMapping(value = "/{campionato}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> importa(
            @PathVariable String campionato,
            @RequestBody List<Giocatore> giocatori) {
        return importClientService.inviaGiocatori(campionato, giocatori);
    }
}
