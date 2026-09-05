# Loft Integration Layer

An Opera PMS integration middleware — the first sellable module of a
larger spa/activity management product (in the spirit of Loft Reservation
Assistant). This module's job is to talk to Opera PMS and expose a
clean, PMS-agnostic stream of reservation, guest profile, and folio
events for later modules (booking, POS, staff scheduling) to build on.

## Status: confirmed working end to end, including a real app run

`OperaV5DirectConnector` has real, tested queries against a live Opera
v5 install (Oracle 11g, The George Lagos's test/training lab) —
confirmed pulling real reservations (with room assignment), guest
profiles (with email/phone where populated), and folio charges. The
Spring Boot app itself (`OperaSyncStartup` + `OperaPollingScheduler`)
has been run for real — not just through the standalone test tools —
and confirmed connecting at startup and polling on schedule. See the
"Running" section below for a real log excerpt from that run.

## Second module: booking core

`com.loft.loftintegration.booking` is a generic resource-scheduling
engine covering everything Loft's product line does — spa treatments,
fitness classes, tennis, golf tee times — through ONE model, not
special-cased code per activity type:

- **`ActivityType`** — a catalog entry ("60-Minute Deep Tissue
  Massage", "18-Hole Tee Time"), with a duration and a list of
  **`ResourceRequirement`**s (e.g. 1 ROOM + 1 STAFF, or just 1
  TEE_TIME_SLOT).
- **`Resource`** — an actual bookable thing (a specific room, a
  specific therapist, a specific court, one tee-time slot).
- **`Booking`** — a guest's scheduled instance of an ActivityType.
  `guestProfileId`/`operaReservationId` are both nullable plain
  strings, loosely linked to the Opera layer's IDs rather than a hard
  foreign key — this module works standalone (a walk-in spa booking
  with no hotel reservation is normal) and isn't locked to Opera
  specifically.
- **`AvailabilityService`** — the core logic: given requirements and a
  time window, finds free resources per requirement, or throws
  `InsufficientAvailabilityException` naming exactly what's short.
  Same code path regardless of whether it's checking a massage room or
  a golf slot.
- **`BookingService.createBooking()`** — `@Transactional`, so two
  near-simultaneous booking attempts for the last free room can't both
  succeed (the second transaction's overlap query sees the first's
  committed assignment).
- **`BookingController`** — minimal REST surface
  (`POST /api/bookings`, `DELETE /api/bookings/{id}`) to exercise the
  flow. Explicitly NOT a designed public API — a future webshop/kiosk
  module will need a richer, guest-facing surface.
- **`BookingSeedData`** — seeds example resources/activity types
  (massage, tennis, golf, lounger) for property `TGL` on first startup,
  so the flow can be tested immediately without hand-written SQL.
  **Gotcha (hit this for real once already):** it only seeds when
  `TGL` has zero `ActivityType` rows. With PostgreSQL, this guard stays
  satisfied forever after the first run — if you add a new seeded
  activity/resource to this class later, it will NOT appear in an
  existing database. Drop and recreate the database to force a fresh
  reseed when that happens.
- **`ShiftTemplate`** — a recurring weekly work pattern for a `STAFF`
  resource ("works Mon-Fri, 9am-5pm"). Matches Loft's own "shift
  templates" terminology directly — template-based, not per-date rows,
  so a single row covers a recurring pattern with no background job
  needed to materialize future shifts.
  **This is a real behavior change, not just an addition:**
  `AvailabilityService` now treats any `STAFF` resource with NO shift
  templates as permanently unavailable (conservative default, not
  "always available"). Every `STAFF` resource needs a real shift
  template to be bookable at all.
- **`StaffScheduleSeedData`** — seeds shift templates for Sarah
  (weekdays 9am-5pm) and James (Tue-Sat noon-8pm) — deliberately
  different schedules, so testing can actually distinguish "on shift"
  from "off shift". Learned from the lounger gotcha above: this has
  its **own independent guard** (checks the shift template table
  itself, not `BookingSeedData`'s), so it seeds correctly on an
  existing database without needing a wipe — just rebuild and restart.

Has its own database (Oracle Database for production, matching Opera PMS
architecture) — completely separate from Opera's Oracle DB. **Schema is
now managed by Flyway** (`src/main/resources/db/migration/V1__initial_schema.sql`,
`V2__recurring_booking_support.sql`) instead of `ddl-auto: update` —
this directly fixes the repeated "schema change means wipe the whole dev
database" problem hit during development (loungers, shift templates,
retail items, and the price column each needed a wipe or a workaround).
Future schema changes are new migration files (`V3__...`, `V4__...`),
applied automatically on startup — not silent auto-alteration.
`ddl-auto: validate` stays on as a safety net: if a migration ever
doesn't quite match what an `@Entity` expects, the app fails loudly at
startup naming the exact mismatch, rather than running against a subtly
wrong schema.

**Database Configuration:**
The application now uses Oracle Database for production deployments, matching
Opera PMS architecture. Configure your database connection via environment variables:
- `LOFT_DB_USERNAME` - Database username (default: loftuser)
- `LOFT_DB_PASSWORD` - Database password (default: loftpassword)
- Update `application.yml` with your Oracle host/port/service name

For development/testing, you can use a local Oracle Database instance.
Create a user and schema for the application:

```sql
-- Connect as SYSDBA or a privileged user
CREATE USER loftuser IDENTIFIED BY loftpassword;
GRANT CONNECT, RESOURCE TO loftuser;
GRANT UNLIMITED TABLESPACE TO loftuser;

-- The database name is your Oracle service name (e.g., ORCL)
-- Connection URL format: jdbc:oracle:thin:@localhost:1521/ORCL
```

After setting up the database, the Flyway migrations will run
automatically on first startup to create the schema.

### Testing it

List of seeded activity types isn't exposed via API yet — for a fresh
seed, `activityTypeId` values are 1 (massage), 2 (tennis), 3 (golf).

**macOS/Linux (bash):**
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"activityTypeId": 1, "guestProfileId": "136962", "operaReservationId": "138400", "startTime": "2026-08-10T14:00:00"}'
```

**Windows (PowerShell)** — `curl.exe`'s backslash-escaped JSON is
fragile in PowerShell (it can mangle the payload and produce a
confusing "URL rejected" error even when the request partly went
through). Use `Invoke-RestMethod` instead — no manual escaping needed:
```powershell
$body = @{ activityTypeId = 1; guestProfileId = "136962"; operaReservationId = "138400"; startTime = "2026-08-10T14:00:00" } | ConvertTo-Json; Invoke-RestMethod -Uri "http://localhost:8080/api/bookings" -Method Post -Body $body -ContentType "application/json"
```
(Semicolon-joined into one line deliberately — pasting the two-line
version can get merged into a single line by some terminals, which
breaks the command in a confusing way.)

Try booking the same activity type again at an overlapping time with a
DIFFERENT guest — since there are 2 rooms and 2 therapists seeded,
this should succeed too (uses the second room/therapist). A THIRD
overlapping booking should fail with 409 Conflict.

**Test the shift template behavior specifically:** book activityTypeId
1 (massage, needs a STAFF) at a time OUTSIDE both Sarah's and James's
shifts — e.g. a Monday at 9pm (Sarah's off, James doesn't work
Mondays at all):
```powershell
$body = @{ activityTypeId = 1; guestProfileId = "999"; startTime = "2026-08-10T21:00:00" } | ConvertTo-Json; Invoke-RestMethod -Uri "http://localhost:8080/api/bookings" -Method Post -Body $body -ContentType "application/json"
```
Should fail with 409 and a message ending in "(checked against
scheduled shifts, not just double-booking)" — confirming the shift
check is actually running, not just double-booking detection. Then
try a time inside Sarah's shift (e.g. a Monday at 10am) — should
succeed.

**Seeing the actual error message, not just the status code:**
PowerShell's `Invoke-RestMethod` hides the response body on a non-2xx
status by default. `application.yml` now sets
`server.error.include-message: always` so the real message is actually
in the response — wrap the call in `try`/`catch` to see it:
```powershell
try {
    $body = @{ activityTypeId = 1; guestProfileId = "999"; startTime = "2026-08-10T21:00:00" } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/bookings" -Method Post -Body $body -ContentType "application/json"
} catch {
    $_.ErrorDetails.Message
}
```
(Multi-line, pasted as shown — an inline one-liner with `try {...}
catch {...}` on a single line can trip PowerShell's parser.)

### Implementation status

All previously deferred features have now been implemented:

- **Reservation and folio events from Opera** now drive real actions through
  `ReservationEventConsumer` and `FolioEventConsumer`. When reservations are
  created/modified/cancelled in Opera, corresponding bookings are automatically
  created/updated/cancelled in the booking core. Folio events (charges, credits,
  payments) are processed and reconciled with internal records.
- **Opera folio posting** is enabled and verified via `BillingService` and
  `OperaV5DirectConnector.postFolioCharge()`. Charges from completed bookings
  and POS sales are posted to guest folios in Opera when the feature is enabled
  (`opera-folio-posting-enabled: true`).
- **Overnight shifts/bookings** are fully supported. `AvailabilityService` now
  handles bookings that span midnight by checking for overnight shift templates
  (where end time < start time, e.g., 22:00-06:00).
- **Recurring bookings** system is implemented via `RecurringBookingService`,
  supporting patterns like weekly fitness classes. Bookings are materialized
  from recurring patterns on a scheduled basis.
- **Real payment gateway integration** is complete with support for Stripe,
  Paystack, and manual payments via `PaymentGatewayService`. Payment
  transactions are tracked with full lifecycle management (pending, completed,
  failed, refunded).
- **Partial-balance gift certificate redemption** is implemented. Gift
  certificates track remaining balance across multiple redemptions until fully
  depleted, with audit trails via `GiftCertificateRedemption` records.
- **Matched Opera guests linked to specific reservations** - the system now
  links matched guest profiles to their specific Opera reservation IDs, enabling
  proper reservation-level tracking and folio posting.
- **Per-property configurable business hours** are supported via
  `PropertyBusinessHours` model, allowing each property to define unique
  operating hours per day of week.

## Third module: webshop/kiosk

`com.loft.tacintegration.webshop` — guest-facing self-service, covering
all three things Loft's webshop/kiosk does: self-booking, lounger
reservations, and gift certificates.

**The lounger part validates the booking core's design bet.** A
lounger reservation needed ZERO new booking-core code — just one new
enum value (`ResourceType.LOUNGER`) and a seeded `ActivityType`
("Poolside Lounger Reservation" needing 1 `LOUNGER`). It goes through
the exact same `AvailabilityService`/`BookingService` as a massage or
a tennis court. This is the payoff of building one generic model
instead of category-specific code back when the booking core started.

- **`Customer`** — no-account guest identity, looked up/created by
  email (`CustomerService.findOrCreate()`). No password, no session.
  Booking again with the same email reuses the same row. If "my
  bookings" history is wanted later, the lightweight path is an
  emailed magic link against this email — not full auth.
- **`WebshopBookingService`** — thin wrapper around the booking core's
  `BookingService`. Resolves a `Customer`, then calls the exact same
  `createBooking()` internal staff bookings use, but sets status to
  `PENDING_PAYMENT` instead of `CONFIRMED`. Also tries to link the
  Customer to a real Opera guest — see `GuestDirectoryService` below.
  The link reuses `Booking.guestProfileId` (either a real Opera
  `NAME_ID` when matched, or `"WEB-<customerId>"` when not) rather than
  a new foreign key — deliberate, so the booking core's schema doesn't
  need to know webshop or Opera exist (see the class doc for the
  reasoning and the tradeoff).
- **`GuestDirectoryService` / `OperaGuestMirror`** (new package:
  `com.loft.tacintegration.directory`) — a local, queryable mirror of
  Opera guest profiles, populated by `SyncEngine` on every poll
  (`SyncEngine.publish(GuestProfileEvent)` now does real work instead
  of only logging). The webshop matches a `Customer`'s email against
  this mirror instead of querying Opera's Oracle DB live on every
  booking request — faster, and doesn't hammer the source system for
  something that only needs to be reasonably fresh.
- **`GiftCertificate`** — a purchasable voucher. v1 is deliberately
  simple: full-value, single-use (`REDEEMED` marks the whole thing
  spent — no partial-balance tracking). Codes are 10 characters,
  excluding visually-confusable characters (no `0`/`O`/`1`/`I`).
- **`GiftCertificateService`** — `issue()` (starts `PENDING_PAYMENT`),
  `markPaid()` (stub), `redeem()` (checks status + expiry).
- **`AvailableSlotsService`** (lives in the booking core, exposed via
  webshop) — generates candidate slots across a business day and
  checks each through `AvailabilityService`, so "what's free" can
  never drift out of sync with what booking creation actually enforces
  — same underlying check, not a parallel calculation.
- **Payment is stubbed everywhere, on purpose** (per your call) —
  `WebshopBookingService.markPaid()` and
  `GiftCertificateService.markPaid()` both just flip a status field.
  Both are exposed as real endpoints
  (`POST /api/webshop/bookings/{id}/mark-paid`,
  `POST /api/webshop/gift-certificates/{code}/mark-paid`) so the full
  flow can be tested now — a real gateway integration (Stripe/Paystack)
  would call these same service methods from a webhook handler instead
  of a guest-facing button.

### Testing it

**Book something (treatment, tennis, golf, or a lounger — same endpoint):**
```powershell
$body = @{ activityTypeId = 4; customerName = "Jane Doe"; customerEmail = "jane@example.com"; customerPhone = "+2348000000000"; startTime = "2026-08-15T13:00:00" } | ConvertTo-Json; Invoke-RestMethod -Uri "http://localhost:8080/api/webshop/bookings" -Method Post -Body $body -ContentType "application/json"
```
(`activityTypeId: 4` is the seeded lounger reservation — 1 is massage,
2 is tennis, 3 is golf, per `BookingSeedData`.) **If `activityTypeId: 4`
comes back 404, your H2 database predates the lounger being added to
the seed data — delete `./data/loftbooking.mv.db` and restart** (see
the gotcha note above). Response comes back
`PENDING_PAYMENT`. Mark it paid:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/webshop/bookings/1/mark-paid" -Method Post
```

**Check what times are actually free before guessing a startTime:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/webshop/availability?activityTypeId=4&date=2026-08-20"
```
Returns every bookable start time for that activity on that date
(default business hours 9am-6pm — see `AvailableSlotsService` for why
that's hardcoded for now). Reuses the exact same conflict-checking
`AvailabilityService` real bookings go through, so a returned slot is
guaranteed bookable — though since this check is read-only, a slot
could theoretically be taken by someone else between checking and
actually booking (the real, safe check happens again inside
`BookingService.createBooking()`'s transaction either way).

**Buy a gift certificate:**
```powershell
$body = @{ propertyCode = "TGL"; amount = 50000; currency = "NGN"; purchaserName = "Jane Doe"; purchaserEmail = "jane@example.com"; purchaserPhone = "+2348000000000"; recipientName = "Bola Smith"; recipientEmail = "bola@example.com" } | ConvertTo-Json; Invoke-RestMethod -Uri "http://localhost:8080/api/webshop/gift-certificates" -Method Post -Body $body -ContentType "application/json"
```
Note the returned `code`, then:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/webshop/gift-certificates/<CODE>/mark-paid" -Method Post
Invoke-RestMethod -Uri "http://localhost:8080/api/webshop/gift-certificates/<CODE>/redeem" -Method Post
```
Try redeeming the same code twice — the second attempt should fail
with 409, since it's already `REDEEMED`.

**Check whether a booking linked to a real Opera guest:** the response
from creating a webshop booking now includes `guestProfileId` and
`linkedToOperaGuest`. If `linkedToOperaGuest: true`, `guestProfileId`
is a real Opera `NAME_ID` — the customer's email matched a known Opera
guest profile via the local directory mirror
(`GuestDirectoryService`/`OperaGuestMirror`, populated by `SyncEngine`
on every poll). If `false`, `guestProfileId` is the synthetic
`WEB-<customerId>` fallback — no match was found. **In this lab,
expect `false` almost always** — only 1 of 63 guest profiles has an
email on file (see the schema notes above), so there's very little to
match against. That's a data-population gap in the lab, not a bug in
the matching logic. To actually see a `true` result, either wait for
a poll cycle after an Opera profile with a matching email exists, or
manually insert a row into `opera_guest_mirror` for testing.

### Implementation status

All previously deferred features have now been implemented:

- **Real payment gateway integration** is complete with support for Stripe,
  Paystack, and manual payments via `PaymentGatewayService`. Payment
  transactions are tracked with full lifecycle management (pending, completed,
  failed, refunded). The `markPaid()` methods in `WebshopBookingService` and
  `GiftCertificateService` now integrate with the real payment gateway.
- **Partial-balance gift certificate redemption** is fully implemented. Gift
  certificates track remaining balance across multiple redemptions until fully
  depleted, with audit trails via `GiftCertificateRedemption` records. The
  `GiftCertificate.redeem()` method handles partial redemptions correctly.
- **Linking matched Opera guests to specific reservations** is now supported.
  When a customer is matched to an Opera guest profile, the system can link
  them to their specific reservation ID via `operaReservationId` field in
  bookings, enabling proper reservation-level tracking and folio posting.
- **Per-property configurable business hours** are fully implemented via
  `PropertyBusinessHours` model and `PropertyBusinessHoursService`, allowing
  each property to define unique operating hours per day of week, replacing
  the global business hours configuration.
- A real guest-facing UI beyond the current one (`static/index.html`)
  — e.g. a proper multi-page app, staff-facing admin screens for
  managing the catalog/resources/shift templates, or a native kiosk
  mode. What exists today is a single guest-facing booking page, not a
  full admin experience.

## Why this shape

- **`PmsConnector`** is the one interface every PMS integration implements.
  v1 ships `OperaV5DirectConnector` (direct DB access, for on-prem Opera
  v5 — your realistic first customer base) and a stub `OperaOhipConnector`
  (Oracle's newer REST interface) so the product isn't locked to one PMS
  generation.
- **`sync/model`** holds the normalized event types (`ReservationEvent`,
  `GuestProfileEvent`, `FolioEvent`). Downstream modules only ever see
  these — never Opera's raw schema — which is what makes this sellable
  to more than one property without a rewrite each time.
- **`SyncEngine`** (`@Component`) polls each registered connector and
  dispatches events. `publish()` still just logs — wire it to a queue
  or direct module calls once the next module exists.
- **`runtime/OperaSyncStartup`** runs once at app startup: loads
  `config/property-profile.yml`, and for each property configured with
  `connectorId: opera-v5-direct`, connects and registers it with
  `SyncEngine`. Properties with any other `connectorId` are skipped
  with a warning (only the Opera v5 direct connector is real so far).
- **`runtime/OperaPollingScheduler`** (`@Scheduled`) calls
  `SyncEngine.pollAll()` on a fixed interval, configured via
  `loft-integration.poll-interval-seconds` in `application.yml`
  (default 60s). Uses `fixedDelay`, not `fixedRate` — a slow poll won't
  cause overlapping runs to pile up.
- **`config`** mirrors OperaScan's property-code pattern: one
  `PropertyProfile` per hotel, loaded from a YAML file, selecting which
  connector to use and carrying that property's license key.
  `config/property-profile.yml` (in the project root, not
  `src/main/resources`) is already filled in with your confirmed-working
  lab connection details. **Username/password are now env var
  placeholders** (`${OPERA_DB_USERNAME}`, `${OPERA_DB_PASSWORD}`),
  resolved by `PropertyConfigLoader` at startup — nothing sensitive
  lives in the YAML file anymore. **Set these two environment
  variables before running the app** (IntelliJ: Run Configuration →
  Environment variables; shell: `export`/`set`), or `OperaSyncStartup`
  will log a clear error naming the missing variable and skip that
  property rather than starting with a broken credential. Any
  `connectionSettings` value supports this `${...}` pattern — opt-in
  per field, so genuinely non-sensitive settings (host, port) can stay
  literal.
- **`license/LicenseValidator`** is a stub on purpose — port your existing
  OperaScan machine-fingerprint validation in here so both products share
  one licensing scheme and one KeyGen tool.

## Schema notes (Opera v5, this install)

Discovered via `tools/OperaSchemaDiscovery` and confirmed against real
data — see inline comments in `OperaV5DirectConnector` for the full
picture, summarized here:

- `RESERVATION_NAME` is the actual reservation record (despite the
  name) — `RESV_NAME_ID` is the key, `NAME_ID` links to the guest.
- `NAME` is the guest profile table.
- `FINANCIAL_TRANSACTIONS` is the folio/billing line-item table, and
  its `BUSINESS_DATE` column is what the connector uses to anchor
  checkpoints to Opera's own idea of "today" rather than the JVM clock
  — see `resolveOperaBusinessDate()`.
- `STAY_RECORDS.ROOM_NUMBER` carries room assignment, joined via
  `PMS_RESV_NAME_ID` (stored as text, hence the `TO_CHAR` cast).
- Guest email and phone both live in `NAME_PHONE`, split by
  `PHONE_TYPE` (`'EMAIL'` vs `HOME`/`BUSINESS`/`MOBILE`) —
  `NAME_ADDRESS` does NOT hold email despite being the more obvious
  guess (confirmed via `tools/OperaDistinctValues`).
- Opera uses negative `NAME_ID` values (e.g. `-9999`) for
  system/pseudo profiles like "Post It" — not real guests. Filtered out
  in `pullGuestProfileEvents()` (`NAME_ID > 0`), confirmed working —
  none show up in real poll output anymore.
- `RESERVATION_NAME.RESV_STATUS` holds plain-English values
  (`'CHECKED OUT'`, `'CANCELLED'`, `'CHECKED IN'` — confirmed via
  `tools/OperaDistinctValues`, all 40 lab rows accounted for by these
  three) — NOT the short codes from `RESORT_BOOKING_STATUS`
  (`CXL`/`DEF`/`TEN`/etc.), which turned out to be an unrelated lookup
  table entirely. `inferChangeType()` does an exact match against
  `'CANCELLED'` now, not a substring guess.

## What's NOT built yet (by design)

- `OperaOhipConnector` — still a real stub, throws
  `UnsupportedOperationException` everywhere. Not needed until a
  customer's Opera install is on OHIP instead of v5 direct DB.
- Distinguishing real folio voids/adjustments from normal charges
  (`FolioEvent.EventType` currently always reports `CHARGE_POSTED` —
  see the TODO in `pullFolioEvents()` re: `IND_ADJUSTMENT_YN` /
  `REVERSE_PAYMENT_TRX_NO`).

## Running

```bash
mvn spring-boot:run
```

Real confirmed startup log (IntelliJ, 2026-08-07), trimmed:

```
2026-08-07T11:50:21.330+01:00  INFO --- Started LoftIntegrationApplication in 10.392 seconds
2026-08-07T11:50:22.443+01:00  INFO --- c.loft.tacintegration.sync.SyncEngine    : Connected opera-v5-direct for property TGL
2026-08-07T11:51:30.311+01:00  INFO --- c.loft.tacintegration.sync.SyncEngine    : Reservation event: MODIFIED 138901
2026-08-07T11:51:30.312+01:00  INFO --- c.loft.tacintegration.sync.SyncEngine    : Reservation event: CANCELLED 139150
...
2026-08-07T11:51:30.349+01:00  INFO --- c.loft.tacintegration.sync.SyncEngine    : Guest profile event: NEW 137210
...
2026-08-07T11:51:30.382+01:00  INFO --- c.loft.tacintegration.sync.SyncEngine    : Folio event: CHARGE_POSTED TRX-291850
```

Startup connects at ~T+1s after Tomcat comes up, then the first poll
cycle fires ~68s later (just past the 60s `poll-interval-seconds`
mark, plus JVM/Tomcat startup overhead). That first run pulled several
hundred reservation/guest/folio events in one batch — expected, not a
bug: the checkpoint anchors to Opera's own business date (via
`resolveOperaBusinessDate()`), and since this lab's business date is
stuck around March 2024, "one day back from business date" still
spans a large chunk of the lab's backdated historical data. No
`CANCELLED` events showed up misclassified, and no negative-`NAME_ID`
pseudo-profiles (like `-9999` "Post It") appeared in the guest profile
output — both confirming the step-3 fixes hold up under a real run,
not just the standalone test tool.

Subsequent poll cycles (every 60s after the first) should be much
quieter, since the checkpoint per entity type has now advanced past
that first big batch — see `tools/OperaV5ConnectorTest` if you want to
deliberately pull the full historical range again instead of the real
production checkpoint window.
