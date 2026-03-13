# Funkcionalni i nefunkcionalni zahtevi za privatni Git server

## 1. Uvod

Ovaj dokument predstavlja detaljnu analizu funkcionalnih i nefunkcionalnih zahteva za razvoj privatnog Git servera. Cilj je pružiti jasno razumevanje svih karakteristika i performansi sistema kako bi se obezbedilo očekivano ponašanje, uključujući podršku za Git protokol, kontrolu pristupa i privatnost repozitorijuma.

## 2. Funkcionalni Zahtevi

2.1 Git Protokol

Kloniranje Repozitorijuma:

Omogućiti kloniranje repozitorijuma putem Git protokola.
Osigurati podršku za HTTPS i SSH protokole.

Push i Pull Operacije:

Implementirati operacije push i pull na repozitorijumima.
Osigurati integritet podataka tokom prenosa.

Rad sa Granama:

Pružiti podršku za rad sa granama, uključujući kreiranje, brisanje i spajanje grana.

Pregled Istorije i Commit-a:

Omogućiti korisnicima pregled istorije repozitorijuma i pojedinačnih commit-a.

Upiti za Stanje Repozitorijuma:

Implementirati upite za stanje repozitorijuma kako bi korisnici mogli dobiti informacije o trenutnom stanju.

2.2 Kontrola Pristupa

Autentikacija i Autorizacija:

Zahtevati autentikaciju korisnika prilikom pristupa repozitorijumima.
Omogućiti kontrolu pristupa na nivou repozitorijuma na osnovu korisničkih prava.

Kreiranje i Upravljanje Nalogom:

Pružiti mehanizam za kreiranje i upravljanje korisničkim nalozima sa odgovarajućim privilegijama.
Omogućiti resetovanje lozinke i ažuriranje korisničkih informacija.

2.3 Privatnost Repozitorijuma

Enkripcija Podataka:

Implementirati enkripciju podataka tokom prenosa i čuvanja na serveru.
Obezbediti sigurno skladištenje lozinki i ključeva.

Privatni Repozitorijumi:

Omogućiti označavanje repozitorijuma kao privatnih.
Onemogućiti javni pristup privatnim repozitorijumima.

## 3. Nefunkcionalni Zahtevi

3.1 Performanse

Brzina Prenosa Podataka:

Postići optimalnu brzinu prenosa podataka tokom push i pull operacija.
Minimizovati kašnjenje tokom kloniranja velikih repozitorijuma.

Efikasnost Resursa:

Osigurati efikasno korišćenje resursa servera.
Obezbediti horizontalno skaliranje za rastuću upotrebu.

3.2 Pouzdanost i Dostupnost

Redundancija i Rezervno Kopiranje:

Implementirati mehanizme za redundanciju podataka.
Redovno vršiti rezervno kopiranje podataka radi oporavka u slučaju kvara.

Dostupnost 99.9%:

Ciljati dostupnost sistema od najmanje 99.9%.
Brzo identifikovati i rešiti probleme koji mogu dovesti do prekida usluge.

3.3 Bezbednost

Prevencija Od Napada:

Implementirati mehanizme za prevenciju napada poput brute-force napada.
Pratiti sigurnosne zakrpe i redovno ažurirati sistem.

Audit Tragovi:

Obezbediti detaljne audit tragove za sve akcije koje se odnose na pristup i promene u repozitorijumima.

3.4 Korisničko Iskustvo

Intuitivan Interfejs:

Razviti interfejs koji je intuitivan i prijateljski nastrojen prema korisnicima.
Pružiti dokumentaciju i podršku kako bi se olakšalo korišćenje.

Brza Odzivnost:

Osigurati brzu odzivnost sistema prilikom interakcije sa korisnicima.
