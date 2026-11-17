package com.example.ligaduckimporter.service;

import com.example.ligaduckimporter.model.Giocatore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfParserService {

    private static final Pattern GIOCATORE_PATTERN =
            Pattern.compile("^\\s*(\\d{1,2}|All\\.)\\s+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);

    // Trova il primo giocatore concatenato nella parte destra di una riga multi-entry.
    private static final Pattern EMBEDDED_PATTERN =
            Pattern.compile("(?i)\\s+(\\d{1,2}|All\\.)\\s+(.+)$");

    // Il pannello "Albo d'oro" è una colonna separata a destra della pagina, mai roster giocatori.
    private static final float ALBO_X_THRESHOLD = 450f;

    // Margine di tolleranza tra la y di un'intestazione squadra e quella dei suoi giocatori
    // (la seconda colonna di un blocco può avere una y leggermente minore dell'header stesso).
    private static final float HEADER_Y_TOLERANCE = 30f;

    public List<Giocatore> estraiGiocatori(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Il PDF è vuoto o non è stato fornito.");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PositionAwareStripper stripper = new PositionAwareStripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);

            return parse(stripper.lines());
        }
    }

    /** Solo per debug: ritorna le righe grezze estratte da PDFBox con metadati di posizione. */
    public List<String> debugLinee(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Il PDF è vuoto o non è stato fornito.");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PositionAwareStripper stripper = new PositionAwareStripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);

            return stripper.lines().stream()
                    .sorted(Comparator
                            .comparingInt(PdfLine::page)
                            .thenComparingDouble(PdfLine::y)
                            .thenComparingDouble(PdfLine::x))
                    .map(l -> String.format("p%d x=%.0f y=%.0f font=%.0f | %s",
                            l.page(), l.x(), l.y(), l.maxFontSize(), l.text()))
                    .toList();
        }
    }

    /**
     * IMPORTANTISSIMO:
     * il PDF è impaginato graficamente a blocchi.
     *
     * L'intestazione della squadra NON va associata alla riga
     * successiva in base all'ordine testuale estratto da PDFBox.
     * Va invece associata ai giocatori che si trovano sotto
     * quella intestazione nella pagina.
     *
     * Esempio reale del PDF:
     *
     * BLACK FURY
     * 1 RUBEN
     * 2 BRONSON
     * ...
     *
     * CACTUS CITY
     * 1 GRENTON
     * 2 RENNIER
     * ...
     *
     * L'estrazione testuale del PDF può restituire CACTUS CITY
     * prima di BLACK FURY a causa della struttura a colonne.
     */
    private List<Giocatore> parse(List<PdfLine> lines) {
        List<Giocatore> risultato = new ArrayList<>();

        // Ordine geometrico: pagina -> coordinata verticale -> orizzontale.
        List<PdfLine> ordinate = lines.stream()
                .sorted(Comparator
                        .comparingInt(PdfLine::page)
                        .thenComparingDouble(PdfLine::y)
                        .thenComparingDouble(PdfLine::x))
                .toList();

        // Prima passata: individua tutte le intestazioni squadra con la loro posizione.
        // Un blocco squadra ha due colonne (numeri 1-11 a font 18, numeri 12-20 a font 15):
        // la seconda colonna può avere una y leggermente MINORE di quella dell'intestazione
        // stessa, quindi non si può assumere che l'header preceda sempre i suoi giocatori
        // nell'ordine sequenziale.
        List<Header> headers = new ArrayList<>();
        for (PdfLine line : ordinate) {
            if (line.x() >= ALBO_X_THRESHOLD) {
                continue;
            }

            String testo = normalizza(line.text());
            if (testo.isBlank() || !isIntestazioneSquadra(line)) {
                continue;
            }

            // L'header a volte è incollato al primo giocatore della colonna
            // (es. "paperpool 13 OKADA"): va scorporato per non inquinare il nome squadra.
            Matcher headerMatch = EMBEDDED_PATTERN.matcher(testo);
            String veroNome = headerMatch.find() ? testo.substring(0, headerMatch.start()).trim() : testo;

            if (veroNome.isBlank() || veroNome.toUpperCase().contains("ALBO")) {
                continue;
            }

            headers.add(new Header(line.page(), line.y(), veroNome));
            estraiConcatenati(testo, veroNome, risultato);
        }

        // Seconda passata: assegna ogni giocatore alla squadra con l'intestazione più vicina.
        for (PdfLine line : ordinate) {
            if (line.x() >= ALBO_X_THRESHOLD) {
                continue;
            }

            String testo = normalizza(line.text());
            if (testo.isBlank() || isIntestazioneSquadra(line)) {
                continue;
            }

            Matcher matcher = GIOCATORE_PATTERN.matcher(testo);
            if (!matcher.matches()) {
                continue;
            }

            String designatore = matcher.group(1);
            int numero;
            if (designatore.equalsIgnoreCase("All.")) {
                numero = 0; // allenatore
            } else {
                numero = Integer.parseInt(designatore);
                // Nel documento i numeri giocatore sono compresi tra 1 e 20.
                if (numero < 1 || numero > 20) {
                    continue;
                }
            }

            String squadra = trovaSquadra(headers, line.page(), line.y());
            if (squadra == null) {
                continue;
            }

            String nomeGrezzo = matcher.group(2);
            String nome = pulisciNome(nomeGrezzo);

            if (!nome.isBlank() && !isVoceCampionato(nome)) {
                risultato.add(new Giocatore(numero, nome, squadra));
            }

            estraiConcatenati(nomeGrezzo, squadra, risultato);
        }

        risultato.sort(Comparator.comparing(Giocatore::squadra).thenComparingInt(Giocatore::numero));
        return risultato;
    }

    /** Trova l'intestazione squadra più vicina (per pagina e coordinata verticale) a una riga. */
    private String trovaSquadra(List<Header> headers, int page, float y) {
        String migliore = null;
        float migliorY = Float.NEGATIVE_INFINITY;

        for (Header h : headers) {
            if (h.page() != page) {
                continue;
            }
            // Tollera intestazioni con y leggermente maggiore della riga stessa
            // (la seconda colonna del blocco può iniziare un po' più in alto dell'header).
            if (h.y() <= y + HEADER_Y_TOLERANCE && h.y() > migliorY) {
                migliore = h.nome();
                migliorY = h.y();
            }
        }

        return migliore;
    }

    /** Estrae ricorsivamente giocatori concatenati in una stessa riga (layout multi-colonna). */
    private void estraiConcatenati(String nomeGrezzo, String squadra, List<Giocatore> risultato) {
        Matcher m = EMBEDDED_PATTERN.matcher(nomeGrezzo);
        if (!m.find()) {
            return;
        }

        String designatore = m.group(1);
        String restoNome = m.group(2);

        int numero;
        if (designatore.equalsIgnoreCase("All.")) {
            numero = 0;
        } else {
            numero = Integer.parseInt(designatore);
            if (numero < 1 || numero > 20) {
                return;
            }
        }

        String nome = pulisciNome(restoNome);
        if (!nome.isBlank() && !isVoceCampionato(nome)) {
            risultato.add(new Giocatore(numero, nome, squadra));
        }

        estraiConcatenati(restoNome, squadra, risultato);
    }

    private boolean isIntestazioneSquadra(PdfLine line) {
        String testo = normalizza(line.text());

        if (testo.isBlank()) {
            return false;
        }

        // Nel PDF le squadre sono scritte con font ~24 pt,
        // mentre i giocatori hanno font più piccolo.
        return line.x() < 220
                && line.maxFontSize() >= 20
                && !testo.equalsIgnoreCase("SERIE A");
    }

    private String pulisciNome(String nome) {
        return nome
                // \\b dopo un '.' fallisce e non copre "cap." seguito da un trattino senza spazio:
                // si usa un lookahead negativo su lettera successiva.
                .replaceAll("(?i)\\bcap\\.?(?![a-zA-Z])", "")
                // "GIRARD ALBOD'ORO": frammento albo d'oro incollato senza un numero separatore.
                .replaceAll("(?i)\\s*ALBO\\s*D[’']?ORO.*$", "")
                .replace("*", "")
                .replace("—", "")
                // Rimuove voci di altri giocatori concatenate (es. "MARIO 2 LUIGI", "MARIO All. LUIGI")
                .replaceAll("(?i)\\s+(?:\\d{1,2}|All\\.)\\s.*$", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean isVoceCampionato(String nome) {
        // Voci dell'albo d'oro ("Camp. Serie B", "Coppa Uefa", "Mitropa cup", "Cop"...) non sono giocatori.
        // Si usa "contains" perché il frammento può comparire dopo un numero maglia incollato
        // per errore (es. nome="1 Mitropa cup" quando il vero giocatore manca).
        String u = nome.toUpperCase();
        return u.contains("CAMP.") || u.contains("COPPA") || u.contains("COPPE")
                || u.contains("SUPERCOPPA") || u.contains("ALBO") || u.contains("SCUDETTO")|| u.contains("SCUDETTI")
                || u.contains("FINALE") || u.contains("CHAMP.") || u.contains("SUP.")
                || u.contains("EUROPA") || u.contains("MITROPA") || u.contains("MUNDIALITI")
                || u.contains("CUP") || u.equals("COP") || u.startsWith("C. DELLE") || u.startsWith("C. INTERCONTI") || u.startsWith("C. DEI PAPERI");
    }

    private String normalizza(String line) {
        return line
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record PdfLine(
            int page,
            String text,
            float x,
            float y,
            float maxFontSize
    ) {}

    private record Header(int page, float y, String nome) {}

    private static class PositionAwareStripper extends PDFTextStripper {

        private final List<PdfLine> lines = new ArrayList<>();

        PositionAwareStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions)
                throws IOException {

            if (textPositions == null || textPositions.isEmpty()) {
                return;
            }

            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxFont = 0;

            for (TextPosition position : textPositions) {
                minX = Math.min(minX, position.getXDirAdj());
                minY = Math.min(minY, position.getYDirAdj());
                maxFont = Math.max(maxFont, position.getFontSizeInPt());
            }

            lines.add(new PdfLine(
                    getCurrentPageNo(),
                    text,
                    minX,
                    minY,
                    maxFont
            ));
        }

        List<PdfLine> lines() {
            return lines;
        }
    }
}
