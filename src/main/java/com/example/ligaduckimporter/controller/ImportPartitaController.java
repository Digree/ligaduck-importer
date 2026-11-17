package com.example.ligaduckimporter.controller;

import com.example.ligaduckimporter.model.Partita;
import com.example.ligaduckimporter.service.ImportClientService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/import/partita")
public class ImportPartitaController {

    private final ImportClientService importClientService;

    public ImportPartitaController(ImportClientService importClientService) {
        this.importClientService = importClientService;
    }

    @PostMapping(value = "/{idGiornata}/{campionato}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> importa(
            @RequestBody List<Partita> partite,
            @PathVariable String idGiornata,
            @PathVariable String campionato) {
        return importClientService.inviaPartita(campionato, partite, idGiornata);
    }
}
