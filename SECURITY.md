# Student Plugin — Bezbednosna konfiguracija

Ovaj dokument opisuje bezbednosne mehanizme u IntelliJ student pluginu i šta je potrebno podesiti pre build-a.

---

## Pre svakog build-a — obavezno

Plugin mora da nosi isti auth token kao što je podešen na serveru (`RAF_AUTH_TOKEN` env varijabla).

### Fajlovi koje treba ažurirati

**1. `IntelliJ-Plugin/src/main/kotlin/.../config/RafConfig.kt`**
```kotlin
const val AUTH_TOKEN: String = "ovde_unesi_token_sa_servera"
```

**2. `raflms-modular/trackingstub/src/main/resources/trackingstub.properties`**
```
auth.token=ovde_unesi_token_sa_servera
```

Oba moraju biti **identični** tokenu koji je podešen kao `RAF_AUTH_TOKEN` na serveru.

### Generisanje novog tokena (na serveru)
```bash
openssl rand -hex 32
```

---

## Bezbednosni mehanizmi u pluginu

### RISK-13 — Privatnost clipboard-a
Plugin **ne čita sadržaj clipboarda**. Umesto toga, meri samo dužinu zalepljenog teksta koristeći razliku u dužini dokumenta pre i posle paste operacije (`editor.document.textLength`).

### RISK-14 — Relativne putanje fajlova
Plugin nikad ne šalje apsolutne putanje (koje bi otkrile korisničko ime, npr. `/Users/ime.prezime/...`). Sve putanje su relativne u odnosu na root projekta (npr. `src/Main.java`), koristeći `VfsUtilCore.getRelativePath()`.

### RISK-15 — Kategorije umesto sadržaja koda
Plugin ne šalje tekst grešaka niti sadržaj autocomplete predloga. Umesto toga šalje:
- `errorCategory` — jedna od vrednosti: `UNRESOLVED_SYMBOL`, `TYPE_ERROR`, `SYNTAX_ERROR`, `NULL_SAFETY`, `UNUSED_SYMBOL`, `OTHER`
- `completionType` — jedna od vrednosti: `METHOD`, `FIELD`, `CLASS`, `KEYWORD`, `OTHER`

### RISK-16 — GDPR saglasnost
Pre nego što praćenje aktivnosti počne, studentu se prikazuje dialog koji objašnjava koje kategorije podataka se prikupljaju. Praćenje se pokreće **isključivo** ako student klikne "Da". Ako odbije, rad na zadatku se nastavlja normalno bez praćenja.

### RISK-22 — Backup pre brisanja foldera
Pre brisanja lokalnog download foldera (koji sadrži zadatak), plugin automatski pravi timestampovani backup:
```
~/student-plugin-temp_backup_yyyyMMdd_HHmmss/
```
Ako backup ne uspe, operacija se prekida i korisniku se prikazuje poruka o grešci — podaci se ne brišu.

### Autentifikacija ka serveru
Svi HTTP pozivi ka `serverapi` i `activitytrackingapi` šalju `Authorization: Bearer <token>` header. Token je bake-ovan pri build-u (videti gore). Isti token mora biti podešen kao `RAF_AUTH_TOKEN` na serveru.

---

## Šta plugin prikuplja

| Podatak | Šta se šalje | Šta se NE šalje |
|---------|-------------|-----------------|
| Greške u kodu | Kategorija greške | Tekst poruke o grešci |
| Autocomplete | Tip predloga (METHOD, FIELD…) | Sadržaj predloga |
| Putanje fajlova | Relativna putanja unutar projekta | Apsolutna putanja, korisničko ime |
| Lepljenje teksta | Broj zalepljenih karaktera | Sadržaj clipboarda |
| Vreme događaja | Timestamp | — |

Podaci se čuvaju na serveru **730 dana** (2 godine), nakon čega se automatski brišu.
