# Demo Guide: Book Exchange

Dieser kurze Guide ist für Personen gedacht, die das Live-Demo schnell ausprobieren möchten, ohne zuerst die komplette technische README zu lesen.

Die Hauptdokumentation des Repositories ist bewusst auf Englisch geschrieben, weil Code, API, Cloud-Setup und technische Reviews meistens international lesbar sein sollen. Dieser Guide beschreibt dagegen den praktischen Demo-Ablauf auf Deutsch.

## Demo-Link

Öffne den Demo-Link, den ich zusammen mit der Bewerbung oder im Portfolio bereitstelle:

```text
https://<cloudfront-domain>/?access=<demo-access-token>
```

Die konkrete Demo-URL wird nicht im öffentlichen Repository veröffentlicht. Sie ist nur über meinen Lebenslauf, das Anschreiben oder eine direkte Nachricht verfügbar, weil der Zugriff auf die Demo-Umgebung nicht für die breite Öffentlichkeit gedacht ist.

Der `access`-Parameter wird nur für den Demo-Zugang verwendet. Die Anwendung speichert danach eine Demo-Access-Session und entfernt den Token aus der sichtbaren URL.

Falls der Link ohne Access-Token geöffnet wird, zeigt die Anwendung eine Zugriffsmeldung statt der eigentlichen App.

## Video-Walkthrough

Nach dem ersten erfolgreichen Zugang über den Demo-Link öffnet die Anwendung automatisch einen deutschsprachigen Video-Walkthrough in einem Modal-Fenster. Er zeigt die wichtigsten User-Flows sowie zentrale Funktionen des Admin-Bereichs.

Das Video ist optional: Es kann geschlossen werden, damit die Demo direkt selbst ausprobiert werden kann.

## Was man testen kann

### 1. Login mit Demo-Accounts

Auf der Login-Seite gibt es eine Auswahl vorbereiteter Demo-Accounts. Beim Auswählen eines Accounts werden die Demo-Zugangsdaten automatisch eingetragen.

Die Demo-Accounts sind normale Benutzerkonten, keine Admin-Konten. Damit kann man die User-Flows realistisch ausprobieren:

- Bücher suchen und Detailseiten öffnen.
- Eigenes Profil ansehen.
- Eigene Bücher verwalten.
- Austausch-Anfragen erstellen.
- Angebote und Anfragen annehmen, ablehnen oder abbrechen.
- Update-Feed mit gelesen/ungelesen testen.
- Beschwerden für Bücher oder Benutzer erstellen.
- E-Mail-Benachrichtigungen im Demo-Inbox-Bereich ansehen.

### 2. Buchkatalog und Suche

Im Katalog kann man nach Büchern suchen, filtern und sortieren. Die Backend-Seite unterstützt stabile Pagination und optional Elasticsearch-Suche. Im Demo kann Elasticsearch aus Kostengründen deaktiviert sein; dann nutzt die API den JPA-Fallback.

Interessante Dinge zum Testen:

- Suche nach Titel/Autor/Kategorie.
- Filter nach Stadt, Kategorie oder Geschenk-Buch.
- Öffnen einer Buchdetailseite.
- Erstellen einer Austausch-Anfrage.
- Geschenk-Flow: Bei Geschenk-Büchern ist kein eigenes Buch als Gegenleistung notwendig.

### 3. Austausch-Flow

Ein typischer Test:

1. Als Demo-User A einloggen.
2. Ein Buch von Demo-User B suchen.
3. Austausch-Anfrage erstellen.
4. Ausloggen.
5. Als Demo-User B einloggen.
6. Anfrage unter Angeboten prüfen.
7. Anfrage annehmen oder ablehnen.
8. Als Demo-User A zurückwechseln und Updates/Historie prüfen.

Der Backend-Flow verschickt dabei Benachrichtigungen an beide beteiligten Benutzer. Im Demo werden diese E-Mails nicht real versendet, sondern in der Demo-Inbox angezeigt.

### 4. Demo-Inbox

Die Demo-Inbox zeigt E-Mails, die für den aktuell aktiven Demo-Kontext relevant sind. Das ist absichtlich so gebaut, damit Tester keine globale Mailpit-UI durchsuchen müssen.

Typische E-Mails:

- Registrierung / E-Mail-Bestätigung.
- Passwort zurücksetzen.
- Austausch erstellt, angenommen, abgelehnt oder abgebrochen.
- Moderations- und Report-Benachrichtigungen.

### 5. Reports

Normale Benutzer können Bücher oder andere Benutzer melden. Das Backend speichert dabei einen historischen Snapshot des Report-Ziels. Dadurch bleibt der Report auch dann verständlich, wenn das gemeldete Buch oder der gemeldete Benutzer später gelöscht oder verändert wird.

Admin-Entscheidungen zu Reports werden ebenfalls per Benachrichtigung kommuniziert.

### 6. Admin-Bereich

Admin-Zugangsdaten werden im öffentlichen Demo nicht direkt veröffentlicht. Der Grund ist simpel: Der Admin-Bereich enthält bewusst echte Moderationsfunktionen wie Benutzer sperren, Bücher löschen, Reports bearbeiten und Demo-Daten zurücksetzen.

Der deutschsprachige Video-Walkthrough wird beim ersten erfolgreichen Demo-Zugang automatisch angezeigt und deckt den wichtigsten Teil der Admin-Flows ab. Weitere Details kann ich bei Bedarf live im Gespräch zeigen.

Der Admin-Bereich umfasst:

- Benutzer suchen, sperren, entsperren und löschen.
- Bücher suchen, bearbeiten, löschen und wiederherstellen.
- Reports prüfen, ablehnen oder lösen.
- Austausch-Vorgänge moderieren.
- Admin-Rechte vergeben oder entziehen, wenn der eingeloggte Account `SUPER_ADMIN` ist.
- Demo-Reset manuell auslösen.

## Technische Punkte, auf die man achten kann

Beim Ausprobieren sieht man nicht nur UI-Funktionen, sondern auch mehrere Backend-Entscheidungen:

- API ist versioniert unter `/api/v1`.
- Schreiboperationen nutzen optimistische Sperren mit `ETag` / `If-Match`.
- Demo-Zugang läuft über einen Access-Token und danach über ein HttpOnly Cookie.
- Direkter Zugriff auf den Elastic-Beanstalk-Origin ist durch einen CloudFront-Origin-Header geschützt.
- E-Mail-Versand ist demo-sicher über Mailpit und Demo-Inbox gelöst.
- Bilder werden nicht als Base64 in der Datenbank gespeichert, sondern in S3 abgelegt.
- Reports behalten historische Snapshots.
- Der Demo-Datenbestand kann aus einem privaten Seed wiederhergestellt werden.

## Erwartete Grenzen des Demos

Das Demo ist kein Produktionssystem:

- Bitte keine echten personenbezogenen Daten eingeben.
- Die Daten können regelmäßig zurückgesetzt werden.
- Admin-Zugangsdaten sind nicht öffentlich.
- E-Mails werden nicht an echte Postfächer versendet.
- Einige Infrastruktur-Features können aus Kostengründen bewusst kleiner dimensioniert oder deaktiviert sein.

## Lokaler Start für technische Review

Wenn das Projekt lokal zusammen mit dem Frontend gestartet werden soll, kann im Backend-Repository Folgendes ausgeführt werden:

```powershell
docker compose -f .\docker-compose.local.yml up --build --watch
```

Danach:

```text
Frontend:        http://localhost:5173
Backend API:     http://localhost:8080/api/v1
Swagger:         http://localhost:8080/api/v1/swagger-ui/index.html
Mailpit:         http://localhost:8025
Actuator health: http://localhost:8081/actuator/health
```

Für den kompletten Testlauf:

```powershell
.\run-tests.ps1
```
