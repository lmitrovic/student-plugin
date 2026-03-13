# RAF-LMS
RAF sistem za upravljanje učenjem, tj. RAF LMS, u svom osnovnom obliku, predstavlja platformu za kreiranje, izradu, pregledanje i ocenjivanje kolokvijuma i ispita iz predmeta koji se bave programiranjem.

Sistem ima serversku i klijentsku stranu. Serverska strana služi za skladištenje postavki, kao i urađenih zadataka koji dolaze na kolokvijumima ili ispitima, dok klijentska strana podržava integraciju sa Intellij razvojnim okruženjem.
Dva ključna tipa korisnika sistema su student i nastavnik.

## Mogući slučajevi upotrebe
- __Prijava studenta na sistem i preuzimanje zadataka__\
  Prilikom izrade kolokvijuma ili ispita, student se prijavljuje na sistem, unosi potrebne podatke za odabir zadataka, kao što je, na primer, grupa zadataka, ukoliko postoji više grupa, a zatim se sa serverske strane preuzima postavka zadataka.
- __Izrada zadataka__\
  Nakon prijave na sistem i preuzimanja zadataka, student počinje sa izradom zadatka, tj. kucanjem koda u Intellij razvojnom okruženju.
- __Predaja rada__\
  Po završetku izrade zadataka student može da preda rad pozivom odgovarajuće komande, koja studentski rad skladišti na serveru.
- __Konfigurisanje podataka za izradu kolokvijuma ili ispita__\
  Nastavnik ima mogućnost da konfiguriše podatke za izradu kolokvijuma ili ispita, da postavi tekstove zadataka i definiše grupe.
- __Preuzimanje studentskih radova i ocenjivanje__\
  Nakon što studenti predaju svoje radove, nastavnik može da preuzme sve zadatke ili samo deo zadataka na osnovu odabira određenih kriterijuma, npr. samo za jednu grupu, i da ih otvori u svom razvojnom okruženju, tj. kroz Intellij, iz kog može da pregleda zadatke i unese studentu poene za svaki pojedinačni zadatak.
- __Pregled radova i kolektivnih rezultata__\
  Prilikom unošenja poena za zadatke informacija o tome se čuva sa serverske strane u bazi podatadaka za ocenjivanje, na osnovu čega nastavnik može da zatraži da dobije zbirni prikaz osvojenih poena svih studenata, kao i da razlikuje pregledane radove od onih koji još uvek nisu pregledani.
- __Prosleđivanje rezultata studentima putem imejla__\
  Nastavnik, po završetku pregledanja studentskih radova i unošenja poena, može pozivom odgovarajuće metode da prosledi imejl sa rezultatima svim studentima.

## Dijagram arhitekture

![Početni dijagram arhitekture sistema](https://github.com/RAFSoftLab/RAF-LMS/assets/37117249/f6e4e5ee-6d56-477d-a4ed-0f7f70e09d5c)

![image](https://github.com/RAFSoftLab/RAF-LMS/assets/43738975/a7d43947-c50b-4bc6-8909-bb9387a438f6)

## Tabele u bazi

![image](https://github.com/RAFSoftLab/RAF-LMS/assets/43738975/2d528f90-c496-4a69-9daa-182809a4ad5d)

## Uputstvo za postavljanje git http servera

### Šta će vam biti potrebno

Da biste uspešno pokrenuli HTTP Git Server, trebaće vam sledeće:

1. Funkcionalna instanca Ubuntu Server 18.04.
2. Korisnik sa sudo privilegijama.

### Ažuriranje i Nadogradnja

Prva stvar koju želite uraditi je ažurirati i nadograditi vašu instancu Ubuntu servera.
Međutim, zapamtite, ukoliko se u procesu ažuriranja menja jezgro (kernel),
moraćete ponovo pokrenuti server.
Zbog toga se pobrinite da izvršite ažuriranje/nadogradnju u trenutku kada je ponovno pokretanje moguće.

Prijavite se na svoj Ubuntu server i ažurirajte apt uz pomoć komande:

````
sudo apt-get update
````

Nakon što je apt ažuriran, nadogradite server uz pomoć komande:

````
sudo apt-get upgrade -y
````

Kada se ovaj proces završi, ponovo pokrenite server (ako je potrebno).

Instaliranje Zavisnosti
Sve što je potrebno za HTTP Git Server možete instalirati jednom komandom. Vratite se u terminal i izdajte:

````
sudo apt-get install nginx git nano fcgiwrap apache2-utils -y
````

To je sve što je potrebno za instalaciju softvera na vašem serveru.

Kreiranje Git Direktorijuma za Repozitorijume
Sada kada je sve instalirano, kreirajte direktorijum za smeštanje Git repozitorijuma pomoću komande:

````
sudo mkdir /srv/git
````

Dodelite odgovarajući vlasništvo tom direktorijumu uz pomoć komande:

````
sudo chown -R www-data:www-data /srv/git
````

Konfiguracija NGINX-a
NGINX sada mora biti konfigurisan tako da zna kako da posluži repozitorijume na serveru. Da biste to postigli,
otvorite podrazumevani NGINX konfiguracioni fajl sa komandom:

````
sudo nano /etc/nginx/sites-available/default
````

Potražite sledeći odeljak:

````
location / {
                # First attempt to serve request as file, then
                # as directory, then fall back to displaying a 404.
                try_files $uri $uri/ =404;
        }
````


U tom odeljku, unesite sledeće:


````
location ~ (/.*) {
    client_max_body_size 0; # Git pushes can be massive, just to make sure nginx doesn't suddenly cut the connection add this.
    auth_basic "Git Login"; # Whatever text will do.
    auth_basic_user_file "/srv/git/htpasswd";
    include /etc/nginx/fastcgi_params; # Include the default fastcgi configs
    fastcgi_param SCRIPT_FILENAME /usr/lib/git-core/git-http-backend; # Tells fastcgi to pass the request to the git http backend executable
    fastcgi_param GIT_HTTP_EXPORT_ALL "";
    fastcgi_param GIT_PROJECT_ROOT /srv/git; # /srv/git is the location of all of your git repositories.
    fastcgi_param REMOTE_USER $remote_user;
    fastcgi_param PATH_INFO $1; # Takes the capture group from our location directive and gives git that.
    fastcgi_pass  unix:/var/run/fcgiwrap.socket; # Pass the request to fastcgi
}
````

Sačuvajte i zatvorite fajl.

Pokrenite NGINX test konfiguracije komandom:

````
sudo nginx -t
````

Trebalo bi da vidite sledeće poruke:
````
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
````

Ukoliko vidite greške, vratite se u konfiguracioni fajl i proverite da li je gornji kod unešen u odgovarajućem odeljku.

Kreirajte Korisnički Nalog
Sada treba da kreirate korisnika koji će imati pristup HTTP Git Serveru. To možete uraditi pomoću htpasswd komande. Demonstriraću kreiranje korisnika "jack". Naravno, trebalo bi da kreirate korisnika koji odgovara vašim potrebama.

Za kreiranje novog korisnika, izdajte komandu:
````
sudo htpasswd -c /srv/git/htpasswd foo
````

Biće vam zatraženo da unesete i potvrdite novu lozinku za korisnika. Kada to završite, ponovo pokrenite NGINX komandom:
````
sudo systemctl restart nginx
````

Kreirajte Prvi Repozitorijum
Vreme je da kreirate prvi repozitorijum. Pošto smo upravo kreirali korisnika "foo", držaćemo se toga.
Ipak, zapamtite da kreirate repozitorijum sa istim imenom koje ste koristili prilikom kreiranja novog korisničkog naloga.

Da biste kreirali novi repozitorijum, promenite se u git direktorijum komandom:
````
cd /srv/git
````

Sada kreirajte repozitorijum komandom:
````
sudo mkdir project1.git
````

Uđite u ovaj novi direktorijum komandom:
````
cd project1.git
````

Sada ćemo inicializovati repozitorijum komandom:
````
sudo git --bare init
````

Zatim želimo ažurirati Git server kako bi bio svestan promena. Izdajte komandu:
````
sudo git update-server-info
````

Promenite vlasništvo nad novim repozitorijumom komandom:
````
sudo chown -R www-data:www-data .
````

Promenite dozvole repozitorijuma komandom:
````
sudo chmod -R 755 .
````

Povežite se sa Repozitorijumom

Sada mozete klonirati repozitorijum sa servera sledecom komandom:
````
git clone http://foo@SERVER_IP:/project1.git
````

Bićete upitani za lozinku korisnika koju ste kreirali.
Nakon uspešne autentifikacije, git će klonirati repozitorijum i trebalo bi da vidite novi direktorijum
(u ovom slučaju, jack) u vašem trenutnom radnom direktorijumu.

Kako biste mogli da commitujete na server, izvrsite sledecu komandu:
````
git remote add origin http://foo@SERVER_IP/project1.git
````

## Uputstvo za isporučivanje Plagina za IntelliJ IDEA

### Sadržaj

1. [Postavljanje Strukture Projekta](#postavljanje-strukture-projekta)
2. [Dodavanje Zavisnosti u Vaš Plagin](#dodavanje-zavisnosti-u-vaš-plagin)
3. [Uključivanje Zavisnosti u .jar Plagina](#uključivanje-zavisnosti-u-jar-plagina)
4. [Rešavanje Problema sa Dupliciranim Unosima Fajlova](#rešavanje-problema-sa-dupliciranim-unosima-fajlova)
5. [Izgradnja instalacije Plagina](#izrada-pluga)

### Postavljanje Strukture Projekta

Nakon kreiranja projekta, potrebno je postaviti strukturu projekta na sledeći način:

1. U IntelliJ IDEA, idite na `File -> Project Structure`.
2. U prozoru Project Structure, kliknite na `Modules` u levom oknu.
3. Izaberite svoj projektni modul.
4. U desnom oknu, kliknite na tab `Dependencies`.
5. Kliknite na dugme `+` i izaberite opciju `JARs or directories...`.
6. Idite do direktorijuma `lib` unutar direktorijuma gde vam je instaliran IntelliJ IDEA i izaberite jar fajlove, zatim kliknite `OK`.

#### Postavljanje IntelliJ Platform Plugin SDK

1. Ponovo idite na `File -> Project Structure -> SDKs` (u levom oknu).
2. Kliknite na ikonu `+` na vrhu okna SDKs i izaberite opciju `IntelliJ Platform Plugin SDK`.
3. Popunite lokaciju korenskog direktorijuma instalacije vašeg IntelliJ IDEA i kliknite `OK`.

#### Provera Lokacije plugin.xml

Vaš plugin.xml fajl je potrebno da se nalazi u resources/META-INF/ direktorijumu vašeg modula. Ako se ne nalazi, premestite ga tamo.

#### Ažuriranje module.iml (ukoliko je moduel.iml prisutan)

U korenskom direktorijumu vašeg projekta, imate .iml fajl koji se zove isto kao vaš projektni modul.
1. Otvorite ovaj fajl i pronađite sledeću liniju:
   `<module type="JAVA_MODULE" version="4">`
2. Ako postoji, zamenite `JAVA_MODULE` sa `PLUGIN_MODULE`, tako da imate:
   `<module type="PLUGIN_MODULE" version="4">`

### Dodavanje Zavisnosti u Vaš Plagin

Nastavite odakle smo stali i dodajte neophodne zavisnosti u build.gradle.kts ili build.gradle fajl u korenskom direktorijumu vašeg projekta.

1. Otvorite fajl `build.gradle.kts` u vašem projektu.
2. Pronađite odeljak `dependencies`.
3. Dodajte novu liniju u ovom odeljku za vašu zavisnost.

Primer dodavanja zavisnosti izgleda ovako:
```kotlin
dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.5.+"){
        exclude(group = "org.slf4j")
    }
}
```

### Uključivanje Zavisnosti u .jar Plagina

Izmenite vaš build.gradle.kts skript da uključuje ove zavisnosti prilikom izgradnje .jar fajla.

```kotlin
tasks {
    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}
```

### Rešavanje Problema sa Dupliciranim Unosima Fajlova

Može se pojaviti problem sa dupliciranim fajlovima prilikom kreiranja .jar fajla. Određeni fajlovi mogu biti prisutni više puta u različitim bibliotekama. Da biste rešili ovaj problem, postavite duplicatesStrategy za jar task:

```kotlin
tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}
```

### Izgradnja instalacije Plagina

Nakon što napravite navedene promene u vašem build.gradle.kts skriptu, pratite ove korake kako biste izgradili plugin:

1. Otvorite Terminal u IntelliJ IDEA.
2. Pokrenite komandu `gradlew build`

Alternativno, ukoliko vam je projekat plagina otvoren u razvojnom okruženju, možete i direktno da pokrenete jar task u sklopu Grejdlvog build ciklusa

Ovo bi trebalo da izgradi vaš plugin, napravi .jar fajl sa potrebnim zavisnostima i reši potencijalni ClassNotFoundException.

To je to! Uspešno ste izgradili IntelliJ IDEA plugin sa potrebnim zavisnostima. Kako modifikujete svoj plugin, nastavite sa izgradnjom i testiranjem kako je prikazano gore. Ako naiđete na greške u vezi sa nedostajućim potrebnim klasnim fajlovima tokom izvršavanja, prođite kroz korake dodavanja zavisnosti i uključivanja istih u vašu .jar izgradnju.

## Uputstvo za instalaciju Plagina za IntelliJ IDEA zasnovano na upotrebi lokalnog jar fajla:

1. **Otvorite IntelliJ IDEA**: Pokrenite IntelliJ IDEA na svom računaru.

2. **Otvorite Postavke (Settings)**: Ukoliko vam je već otvoren neki projekat, na vrhu ekrana, idite na opciju "File" (Fajl) pa zatim "Settings" (Postavke). Na MacOS računarima, umesto "File" koristite "IntelliJ IDEA", a zatim "Preferences". Ukoliko vam se otvori ekran dobrdošlice, ovaj korak možete da preskočite.

3. **Pronađite Plugins (Dodaci)**: U dijalogu Postavki (Settings), sa leve strane pronađite opciju "Plugins" (Dodaci).

4. **Izaberite opciju za instalaciju sa diska**: Na stranici Dodaci (Plugins), kliknite na dugme koje ima ikonu zupčanika, a zatim odaberite opciju "Install Plugin from Disk".

5. **Izaberite Plugin fajl**: Pronađite lokaciju gde ste sačuvali fajl Plagina (može biti u ZIP ili JAR formatu), označite ga i kliknite na "OK".

6. **Potvrdite promene**: Kliknite na "OK" u dijalogu Postavki (Settings/Preferences) kako biste sačuvali promene.

7. **Ponovo pokrenite IntelliJ IDEA**: Ukoliko vas sistem obavesti da je potrebno ponovno pokretanje, kliknite na dugme za restartovanje IDE, u suprotnom ugasite i upalite ponovo IDE.

Sada je Plugin uspešno instaliran i spreman za upotrebu.

## Student Use case dijagram
![StudentUseCaseDiagramWithExtends](https://github.com/RAFSoftLab/RAF-LMS/assets/37117249/482440a5-37b0-4348-bbce-e3ff60b42ec4)

## Dijagram aktivnosti studenta

![DijagramAktivnostiStudenta](https://github.com/RAFSoftLab/RAF-LMS/assets/37117249/bca52050-e300-4450-89e8-cf744e565589)
