package com.example.ligaduckimporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Partita(
        @JsonProperty("_id") String id,
        String idGiornata,
        String idSquadraHome,
        String idSquadraAway,
        int golHome,
        int golAway,
        Formazione formazioneHome,
        Formazione formazioneAway,
        int divisaHome,
        int divisaAway,
        List<EventoTabellino> tabellino,
        DataPartita data,
        boolean salvata,
        String idNazionaleHome,
        String idNazionaleAway,
        @JsonProperty("_class") String classe
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Formazione(
            List<GiocatoreFormazione> titolari,
            List<GiocatoreFormazione> panchina,
            List<GiocatoreFormazione> indisponibili,
            List<GiocatoreFormazione> nonConvocati,
            String allenatore,
            String modulo
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GiocatoreFormazione(
            int pos,
            String idGiocatore,
            String nome,
            boolean inCampo,
            boolean capitano
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EventoTabellino(
            String tipo,
            String squadra,
            String giocatore,
            String giocatoreUscito,
            String giocatoreEntrato,
            Integer minuto,
            Integer priorita
    ) {}

    public record DataPartita(
            @JsonProperty("$date") String date
    ) {}
}
