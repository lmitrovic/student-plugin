<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Student-testing-IntelliJ-plugin Changelog

## [Unreleased]
### Added
- Izrada zadatka se čuva između pokretanja IDE-a: nakon restarta student nastavlja rad i predaje zadatak bez ponovnog započinjanja (`StudentSessionService`).
- Backup postojećeg sadržaja projekta u `~/.raf-lms-backup/` pre preuzimanja novog zadatka.
- Zaštita od preuzimanja praznog zadatka i od ponovnog započinjanja već aktivne izrade.

### Changed
- Uklonjeno polje "Studentska grupa"; ka API-ju se šalje prazan string.
- Svi tracking listeneri, tajmeri i `KeyEventDispatcher` vezani su za `Disposable` i uredno se gase (bez curenja).
- Stub servisi (`StudentStubService`, `TrackingStubService`) su sada deljeni po projektu; lista testova se učitava u pozadini (ne blokira EDT).
- Direktan HTTP prebačen na `HttpRequests`, JSON se serijalizuje bezbedno, svi URL-ovi i konstante centralizovani u `RafConfig`.
- Čitanje grešaka iz editora više ne koristi interni `DaemonCodeAnalyzerImpl`.
- `println` zamenjen logovanjem preko `Logger`.

### Removed
- Zaostali kod iz šablona (`MyProjectService`, `MyApplicationActivationListener`, `MyBundle`, test podaci).
- Nekorišćene zavisnosti (`okhttp`, dupli `junit`), `java-library-distribution` plugin.
