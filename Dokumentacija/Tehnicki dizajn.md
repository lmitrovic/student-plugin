# Razvoj sistema za proveru znanja iz programiranja

## 1. Uvod

1.1 Svrha dokumenta

Ovaj dokument ima za cilj detaljno opisivanje tehničkog dizajna i arhitekture sistema za proveru znanja iz programiranja unutar RAF platforme za učenje programiranja.

1.2 Opseg

Opseg ovog dokumenta obuhvata sve aspekte dizajna sistema, uključujući i front-end i back-end komponente, kako bi se omogućila uspešna implementacija funkcionalnosti provere znanja.

1.3 Auditorijum

Ovaj dokument je namenjen razvojnom timu, timu za osiguranje kvaliteta, menadžerima projekta, sistemskim arhitektama i ostalim relevantnim zainteresovanim stranama.

## 2. Arhitektura sistema

2.1 Pregled

Sistem za proveru znanja iz programiranja ima za cilj omogućavanje studenata da polažu kolokvijume i ispite iz programiranja, dok istovremeno pruža nastavnicima alate za pregledanje i ocenjivanje radova studenata.

2.2 Arhitektura visokog nivoa

Arhitektura visokog nivoa sistema sastoji se od dva ključna dela: serverske strane za skladištenje i upravljanje zadacima i klijentske strane integrisane sa razvojnim okruženjima.

2.3 Komponente i moduli

Serverska strana:

Implementacija serverske strane za skladištenje zadataka putem RAF Git-a.
Implementacija skladištenja podataka o broju bodova i prisustvu ispita putem Postgres relacione baze podataka.

Klijentska strana:

Integracija klijentske strane sa razvojnim okruženjima poput IntelliJ i Visual Studio Code putem razvoja plugin-a.
Dodavanje funkcionalnosti za unos, pregled, i ocenjivanje radova studenata putem RAF Git-a i RaF learning platforme implementirane pomoću Angular.js (klijentska strana) i Java Spring-a (serverska strana).

2.4 Dijagrami toka podataka

![Početni dijagram arhitekture sistema](https://github.com/RAFSoftLab/RAF-LMS/assets/37117249/f6e4e5ee-6d56-477d-a4ed-0f7f70e09d5c)

Ova funkcionalnost će se isporučiti kroz dva glavna dela: serversku stranu za skladištenje i upravljanje zadacima, i klijentsku stranu integrisanu sa razvojnim okruženjima za kreiranje i predaju radova.

## 3. Dizajnerski razmatranja

3.1 Dizajnerski principi

Dizajn sistema vodiće se principima jednostavnosti, modularnosti i proširivosti kako bi se olakšala održivost i buduće unapređenje.

3.2 Pretpostavke i zavisnosti

Pretpostavka: Pristup serverskoj strani je moguć putem RAF Git-a.

Zavisnost: Klijentska strana je integrisana sa odabranim razvojnim okruženjima.

3.3 Ograničenja

Ograničenja sistema uključuju minimalne troškove infrastrukture i održavanje jednostavnosti koda radi bolje operabilnosti.

3.4 Rizici

Rizik: Nestabilnost tokom implementacije.

Mera: Postepeno uvođenje funkcionalnosti uz mehanizme za rollback u slučaju problema.

## 4. Dizajn podataka

4.1 Modeli Podataka

RAF Git:

Struktura podataka za čuvanje informacija o zadacima i studentima - Korišćenje git repozitorijuma za skladištenje koda studenata, a metadata o zadacima i studentima će se čuvati u odgovarajućim JSON fajlovima unutar repozitorijuma.

Baza podataka:

Struktura podataka za čuvanje informacija o rezultatima ispita i studentima - Upotreba tabela u Postgres bazi podataka, gde će se čuvati podaci o ocenama, prisustvu na ispitima i drugi relevantni podaci.

## 5. Dizajn komponenti

5.3 Integracija sa spoljnim komponentama

RAF Git:

Integracija sa RAF Git-om za skladištenje i preuzimanje zadataka - Korišćenje git komandi i API-ja za manipulaciju repozitorijumima iz serverske strane.

5.4 API-ji i web servisi

Komunikacija sa Klijentskom Stranom:

API-ji za komunikaciju između klijentske i serverske strane - Implementacija REST API-ja za prenos podataka o zadacima, ocenama i drugim relevantnim informacijama.

## 6. Dizajn korisničkog interfejsa

6.1 Wireframe-ovi

[Umetnuti wireframe-ove ključnih korisničkih interfejsa za unos, pregled, i ocenjivanje.]

6.2 Mockup-ovi

[Prikazati mockup-ove visoke rezolucije korisničkih interfejsa za različite uređaje.]

6.3 Dijagrami Interakcije

[Kreirati dijagrame interakcije kako bi se ilustrovali korisnički tokovi i odgovori sistema.]

## 7. Dizajn bezbednosti

7.1 Autentikacija i autorizacija

Nalog nastavnika:

Autentikacija nastavnika putem JWT tokena.
Autorizacija za pristup ocenjivanju i pregledu radova studenata.

Sigurnost podataka:

Korišćenje HTTPS za enkripciju podataka.
Firewall i rate limiting zaštita od zloupotreba.

## 8. Performanse optimizacija preuzimanja zadataka:

Optimizacija sistema za brzo preuzimanje i skladištenje zadataka - Korišćenje efikasnih algoritama za brzu manipulaciju podacima.

## 9. Infrastruktura, rast, i skaliranje infrastruktura:

Korišćenje postojeće infrastrukture za serversku stranu - Integracija sa postojećim serverima i resursima.

Skalabilnost:

Planiranje skalabilnosti sistema kako bi se nosio sa povećanim brojem studenata i nastavnika - Korišćenje horizontalnog skaliranja za povećanje kapaciteta.

## 10. Troškovi

Infrastrukturni troškovi:

Troškovi infrastrukture za skladištenje i obradu zadataka minimalni zbog korišćenja postojeće infrastrukture - Redukcija troškova kroz efikasno korišćenje resursa.

## 11. Pouzdanost, otpornost i ispravnost

Mehanizmi pouzdanosti:

Upotreba durabilne memorije za čuvanje ocena i zadataka.
Proaktivna identifikacija i rešavanje potencijalnih problema sa ispravnošću ocena - Redovno praćenje i održavanje sistema.

## 12. Implementacija

Postepeno uvođenje:

Postepeno uvođenje funkcionalnosti u produkciju kako bi se izbegli prekidi za korisnike - Upotreba Continuous Deployment prakse.

Upotreba rollback mehanizama:

Primena mehanizama za rollback u slučaju problema tokom implementacije - Automatizovani procesi povratka na prethodne verzije sistema.

## 13. Opservabilnost, metrike, logovanje, i praćenje

Praćenje metrika performansi:

Izbor alata za praćenje metrika performansi i postavljanje upozorenja na odstupanja - Korišćenje alata poput Prometheus-a i Grafane za praćenje performansi.
Logovanje Informacija:

Izbor alata za logovanje relevantnih informacija za pregled i rešavanje problema - Implementacija sistema za logovanje događaja i grešaka.

## 14. Operabilnost, održavanje, podrška, debugovanje

Održavanje jednostavnosti koda:

Održavanje jednostavnosti koda radi lakšeg razumevanja i rešavanja problema - Pridržavanje Clean Code principa.

Servisni opis i priručnik:

Pisanje servisnog opisa i priručnika za rad sa novom funkcionalnošću - Dokumentacija koja olakšava podršku i održavanje sistema.

Alati i uvidi za podršku:

Pružanje alatki i uvida za podršku i održavanje sistema - Implementacija centralizovanog sistema za praćenje i rešavanje tiketa.

## 15. Test strategija

Raznovrsna testiranja:

Implementacija raznovrsnih testova (unit, integration, end-to-end) kako bi se garantovala kvalitetna isporuka sistema - Korišćenje alata poput JUnit, TestNG, i Selenium

## 16. Faze implementacije

Proof of concept (POC):

Razvoj i testiranje POC-a kako bi se potvrdila izvodljivost dizajna - Fokus na ključnim funkcionalnostima i identifikacija potencijalnih problema.

Kontinuirani Razvoj:

Kontinuirani razvoj sistema po kratkim fazama sve do postizanja potpune funkcionalnosti - Iterativni pristup omogućava prilagođavanje promenama i brže isporuke.
