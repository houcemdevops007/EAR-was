# Rapport de build — EFI Academy WebSphere Application Lab

- **Date** : 2026-10-15 15:10:06 CEST
- **Hôte de build** : Darwin x86_64
- **JDK** : javac 1.8.0_421 — `/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home`
- **Maven** : 3.9.9
- **Cible** : Java EE 6 / Servlet 3.0 / bytecode Java 6 (major 50) — IBM WebSphere ND 8.5.5
- **Options** : -o
- **Durée totale** : 309 s

| # | Application | Context root | Build | Tests JUnit | WAR | EAR | Vérification EAR |
|---|---|---|---|---|---|---|---|
| 01 | EFI Welcome (`efi-welcome.ear`) | `/efi-welcome` | **BUILD SUCCESS** | 9 (0 échec) | 39.1 Ko | 38.2 Ko | FAIL (16/17) |
| 02 | EFI Calendar & Weather (`efi-calendar-weather.ear`) | `/efi-calendar` | **BUILD SUCCESS** | 14 (0 échec) | 50.5 Ko | 49.3 Ko | FAIL (16/17) |
| 03 | EFI Chat Room (`efi-chat.ear`) | `/efi-chat` | **BUILD SUCCESS** | 17 (0 échec) | 47.7 Ko | 46.4 Ko | FAIL (16/17) |
| 04 | EFI Task Manager (`efi-tasks.ear`) | `/efi-tasks` | **BUILD SUCCESS** | 15 (0 échec) | 48.6 Ko | 47.3 Ko | FAIL (16/17) |
| 05 | EFI Quiz WebSphere (`efi-quiz.ear`) | `/efi-quiz` | **BUILD SUCCESS** | 14 (0 échec) | 57.4 Ko | 56.0 Ko | FAIL (16/17) |
| 06 | EFI Server Dashboard (`efi-dashboard.ear`) | `/efi-dashboard` | **BUILD SUCCESS** | 11 (0 échec) | 45.7 Ko | 44.3 Ko | FAIL (16/17) |
| 07 | EFI Notes (`efi-notes.ear`) | `/efi-notes` | **BUILD SUCCESS** | 8 (0 échec) | 40.7 Ko | 40.0 Ko | FAIL (16/17) |
| 08 | EFI File Center (`efi-files.ear`) | `/efi-files` | **BUILD SUCCESS** | 23 (0 échec) | 61.6 Ko | 60.6 Ko | FAIL (16/17) |
| 09 | EFI Calculator (`efi-calculator.ear`) | `/efi-calculator` | **BUILD SUCCESS** | 8 (0 échec) | 39.2 Ko | 38.6 Ko | FAIL (16/17) |
| 10 | EFI Mini Shop (`efi-shop.ear`) | `/efi-shop` | **BUILD SUCCESS** | 16 (0 échec) | 75.0 Ko | 71.7 Ko | FAIL (16/17) |

**Tests JavaScript (Nashorn)** : PASS — 27 réussi(s), 0 échec(s)

Contrôles de `verify-ear.sh` : application.xml Java EE 6, context root, WAR embarqué, web.xml 3.0,
JSP d'accueil et pages 404/500, CSS/JS, bytecode major 50, absence de `jakarta.*`, d'API conteneur embarquée et de secrets.

Journaux détaillés : `dist/logs/`. Empreintes : `dist/SHA256SUMS`.

> Ce rapport couvre la construction et les tests hors conteneur. Le déploiement réel sur WebSphere
> (scripts/deploy-all.sh) n'est jamais lancé par ce script.
