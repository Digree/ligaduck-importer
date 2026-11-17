package com.example.ligaduckimporter.controller;

import com.example.ligaduckimporter.model.Giocatore;
import com.example.ligaduckimporter.service.PdfParserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfParserService pdfParserService;

    public PdfController(PdfParserService pdfParserService) {
        this.pdfParserService = pdfParserService;
    }

    @PostMapping(
            value = "/estrai",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<Giocatore> estrai(@RequestParam("file") MultipartFile file)
            throws IOException {

        return pdfParserService.estraiGiocatori(file);
    }

    @PostMapping(
            value = "/debug",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<String> debug(@RequestParam("file") MultipartFile file)
            throws IOException {

        return pdfParserService.debugLinee(file);
    }
}
