# RconMC

RconMC ist ein Discord-Bot in Java (Spring Boot + JDA), der Discord-Nutzer über ein
Registrierungsformular ihren Minecraft-Namen hinterlegen lässt und sie automatisiert per RCON auf die
Whitelist eines Minecraft-Servers setzt. Discord- und Minecraft-Identität werden dauerhaft verknüpft
gespeichert. Das Projekt ist die Java-Portierung von RustyrconMC; Funktionsumfang und Architektur sind
übernommen, Stack und Paketaufbau folgen HolonetBridge.

## Features

- Slash-Command `/whitelist start` postet ein Embed mit „Registrieren"-Button
- Button öffnet ein Modal zur Eingabe des Minecraft-Namens (Java)
- Verifizierung über die Mojang-API (Name → kanonischer Name + UUID)
- Automatisches Hinzufügen per RCON (`whitelist add`) bzw. Vormerken, solange die Whitelist geschlossen ist
- Eindeutigkeit: ein Minecraft-Account gehört höchstens einem Discord-Nutzer
- Admin-Commands: `open`, `close`, `open-all`, `close-all`, `user`, `dcuser`
- Persistenz in einer H2-Dateidatenbank (übersteht Neustarts)

## Tech-Stack

- Java 25, Spring Boot 4.1 (`spring-boot-starter`, Validation, RestClient, Data JPA)
- JDA 6 (keine zusätzlichen Gateway-Intents)
- H2 (Datei) über Spring Data JPA / Hibernate
- eigene Minimalimplementierung des Source-RCON-Protokolls (`RconClient`)

## Paketaufbau

Basispaket `gg.endergebnis.rconmc`:

| Paket | Inhalt |
| --- | --- |
| `discord` | `DiscordProperties`, `DiscordGatewayConfiguration` (JDA + Executor), darunter `command` (`/whitelist`), `button` (Registrieren), `modal` (Abgabe des Formulars) und `listener` (JDA-`ListenerAdapter`, verteilen nur an die Actions) |
| `whitelist` | `WhitelistService` – die gesamte Fachlogik; `RegistrationResult` (sealed) und `BatchResult` als Ergebnistypen |
| `player` | JPA-Entities `Player`, `BotState` und ihre Repositories |
| `rcon` | `RconClient`, `RconProperties`, `RconException` |
| `verify` | `AccountVerifier`-Schnittstelle, `MojangVerifier` und dessen Verdrahtung – eine Bedrock/Floodgate-Variante implementiert dieselbe Schnittstelle |

Die Discord-Schicht kennt weder RCON noch Datenbank; langsame Arbeit (Mojang, RCON) läuft auf einem
Virtual-Thread-Executor, die Interaction wird vorher per `deferReply` zurückgestellt.

## Konfiguration (Environment-Variablen)

Die Namen sind identisch zur Rust-Version.

| Variable | Beschreibung |
|---|---|
| `DISCORD_TOKEN` | Bot-Token |
| `GUILD_ID` | Discord-Server-ID |
| `WHITELIST_CHANNEL_ID` | Channel für die Registrierungs-Nachricht |
| `ADMIN_ROLE_ID` | (optional) Rolle für Admin-Commands, zusätzlich zur Berechtigung „Server verwalten" |
| `RCON_HOST` | IP/Hostname des Minecraft-Servers |
| `RCON_PORT` | RCON-Port (Standard `25575`) |
| `RCON_PASSWORD` | RCON-Passwort |
| `DB_PATH` | Basispfad der H2-Datei **ohne Endung** (H2 legt `<DB_PATH>.mv.db` an) |
| `WHITELIST_MESSAGE` | (optional) Text der Registrierungs-Nachricht |

Fehlende Pflichtwerte brechen den Start ab (fail fast); Token und RCON-Passwort tauchen in keiner
`toString()`-Ausgabe auf. Nicht committen – siehe `.env.example`.

## Build und Deployment

```bash
mvn package
docker compose up -d
```

`docker-compose.yml` startet `eclipse-temurin:25-jre` mit dem gemounteten `target/rconmc.jar` (read-only)
und einem Volume `./data` für die Datenbank.

Tests: `mvn test` (Repository-Test gegen H2, RCON-Client gegen einen Fake-Server, Mojang-Verifier mit
`MockRestServiceServer`, Fachlogik und Command-Berechtigungen mit Mockito, Kontext-Smoke-Test).

## Befehle

| Command | Beschreibung |
|---|---|
| `/whitelist start` | Postet die Registrierungs-Nachricht |
| `/whitelist open` | Öffnet die Whitelist, fügt alle Pending-Spieler hinzu |
| `/whitelist close` | Schließt die Whitelist (neue Registrierungen → pending) |
| `/whitelist open-all` | Fügt alle DB-Einträge der Whitelist hinzu |
| `/whitelist close-all` | Entfernt alle DB-Einträge von der Whitelist |
| `/whitelist user <mc-name>` | Zeigt Discord-Namen + ID zu einem MC-Namen |
| `/whitelist dcuser <discord-name>` | Zeigt MC-Namen zu einem Discord-Namen |

## Unterschiede zur Rust-Version

- Datenbank ist H2 statt SQLite (wie in HolonetBridge); eine vorhandene `bot.db` wird nicht übernommen.
- RCON beendet die Antwort per Sentinel-Paket statt auf ein leeres Antwortpaket zu warten.
- Minecraft-Namen werden vor dem Mojang-Aufruf gegen `[A-Za-z0-9_]{1,16}` geprüft.
