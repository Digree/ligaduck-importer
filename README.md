# Spring Boot PDF Parser

Estrae dal PDF esclusivamente:

- numero
- nome
- squadra

## IMPORTANTE: struttura del PDF

Il PDF è impaginato graficamente a blocchi.

Per esempio, nel primo blocco:

```text
BLACK FURY
1 RUBEN
2 BRONSON
3 CHAPMAN
...
```

RUBEN, BRONSON, CHAPMAN ecc. appartengono a BLACK FURY.

Successivamente:

```text
CACTUS CITY
1 GRENTON
2 RENNIER
3 DARWIN
...
```

GRENTON, RENNIER, DARWIN ecc. appartengono a CACTUS CITY.

Il testo estratto linearmente da PDFBox può però presentare le intestazioni in un ordine diverso a causa della struttura a colonne. Per questo il parser usa le **coordinate geometriche del PDF** (pagina, X, Y e dimensione del carattere), non semplicemente l'ordine del testo.

## Avvio

```bash
mvn spring-boot:run
```

## Endpoint

```bash
curl -X POST "http://localhost:8080/api/pdf/estrai"   -H "Content-Type: multipart/form-data"   -F "file=@serie-a.pdf"
```

Le importazioni sono disponibili tramite `POST /api/import/giocatori/{campionato}` e
`POST /api/import/partita`, entrambi con body JSON. L'endpoint della partita inoltra
il body ricevuto al backend mantenendone la struttura originale.

## Risposta

```json
[
  {
    "numero": 1,
    "nome": "RUBEN",
    "squadra": "BLACK FURY"
  },
  {
    "numero": 2,
    "nome": "BRONSON",
    "squadra": "BLACK FURY"
  },
  {
    "numero": 3,
    "nome": "CHAPMAN",
    "squadra": "BLACK FURY"
  },
  {
    "numero": 1,
    "nome": "GRENTON",
    "squadra": "CACTUS CITY"
  }
]
```

Il progetto usa Apache PDFBox. Se in futuro il PDF fosse una scansione senza testo selezionabile, va aggiunto OCR.
