# HybridMode

Monorepo: **Android app** + **Strapi 5 backend**.

| Path | Stack |
|------|--------|
| `app/`, `core/` | Kotlin Android (Compose, Clean Architecture) |
| `backend/` | Strapi 5 (Users & Permissions + Upload) |
| `config.properties` | Machine-local BASE_URL / mock flag |

```bash
# Terminal 1 - Strapi (Node 20+)
npm run backend

# Terminal 2 - Android
./gradlew :app:installDebug
```

## Architecture (DDD)

Full Domain-Driven Design with Clean dependency rule: **UI → Application → Domain ← Infrastructure**.

| Module / package | Responsibility |
|------------------|----------------|
| `:app` | Presentation |
| `:core:domain` `shared/` | `AggregateRoot` (event sourcing buffer), `ValueObject`, `DomainEvent`, `DomainEventPublisher` |
| `:core:domain` `auth/` | Auth bounded context — `User` aggregate, `Email`/`UserId`/`DisplayName` VOs, session/tokens, auth events, `AuthRepository` |
| `:core:domain` `reporting/` | Reporting bounded context — `FieldReport` aggregate + invariants, report VOs, sync domain service, report events, `FieldReportRepository` |
| `:core:domain` `application/` | Application services (use cases) that orchestrate aggregates and publish domain events |
| `:core:data` | Infrastructure / anti-corruption (Strapi DTOs ↔ domain, Room, tokens, sync) |
| `:core:common` | Shared kernel |
| `backend/` | Strapi 5 API |

## Strapi integration (stock API)

### One config file for every laptop

All runtime keys live in **`config.properties`** (gitignored). On a new machine:

```bash
cp config.properties.example config.properties
# edit STRAPI_BASE_URL to this laptop's LAN IP (real device) or 10.0.2.2 (emulator)
```

| Key | Purpose |
|-----|---------|
| `STRAPI_BASE_URL` | Debug/base Strapi URL (must end with `/`) |
| `RELEASE_BASE_URL` | Release build URL |
| `USE_MOCK_BACKEND` | `true` = in-app mock, no Strapi |
| `ACCESS_TOKEN_TTL_MS` | Client max token TTL (`300000` = 5 min) |

Gradle prints the active values on every build: `[HybridMode] config → BASE_URL=...`

### Endpoints used

| Feature | Method | Path |
|---------|--------|------|
| Login | `POST` | `/api/auth/local` (`identifier`, `password`) |
| Register | `POST` | `/api/auth/local/register` (`username`, `email`, `password`) |
| Refresh | `POST` | `/api/auth/refresh` (`refreshToken`) |
| Logout / revoke | `POST` | `/api/auth/logout` (Bearer + optional `refreshToken`) |
| Me | `GET` | `/api/users/me` |
| Upload | `POST` | `/api/upload` (`files` + `fileInfo`) |

### Auth & session policy

- Strapi runs with `jwtManagement: 'refresh'` (`backend/config/plugins.ts`): short-lived access JWT + rotatable refresh token.
- App stores both tokens; access lifetime is clamped by `ACCESS_TOKEN_TTL_MS` (default 5 min).
- **Refresh** = `POST /api/auth/refresh`; on 401 the local session is cleared.
- **Logout** = best-effort `POST /api/auth/logout` (server revoke), then always clear local tokens/user cache.
- **Offline**: cached session remains usable for local work (including queued uploads).

### Room migrations

- Schema export: `core/data/schemas/` (commit new JSON when bumping `@Database` version).
- `AppMigrations` holds real SQL upgrades (e.g. `2 → 3` adds `uploads.ownerUserId`).
- Only pre-release version `1` still uses destructive fallback.

### Strapi permissions checklist

In **Settings → Users & Permissions → Roles → Authenticated**:

- [x] `User` → `me`
- [x] `Upload` → `upload` (and find if needed)

**Public**:

- [x] `Auth` → `callback`, `connect`, `emailConfirmation`, `forgotPassword`, `register`, `resetPassword`, `sendEmailConfirmation` (as needed)
- [x] Local auth enabled for login/register

Create a user in Strapi Admin (or register from the app).

### Real device (USB / same Wi‑Fi)

1. Phone and Mac on the **same network**.
2. In `config.properties` set `STRAPI_BASE_URL=http://YOUR_LAN_IP:1337/`  
   (`ipconfig getifaddr en0` on Mac).
3. Strapi: `HOST=0.0.0.0 npm run develop`
4. Reinstall: `./gradlew :app:installDebug`

### Local demo without Strapi

In `config.properties`:

```properties
USE_MOCK_BACKEND=true
```

Demo user: `demo@hybridmode.app` / `password123`

## Image upload (two schemas)

| Schema | When | Behavior |
|--------|------|----------|
| **Online** | Connected | Save locally → `POST /api/upload` → `Synced` |
| **Offline** | No network | Queue as `PendingOffline` |

Title / description map to Strapi `fileInfo.alternativeText` / `caption`.  
`UploadSyncObserver` auto-syncs when connectivity returns.

## Build & run

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Or Run **app** from Android Studio with an emulator and Strapi on `localhost:1337`.

## Production checklist

- [x] Multi-module Clean Architecture
- [x] Strapi auth + upload DTOs/APIs
- [x] Encrypted token storage + 5-minute local TTL
- [x] Offline session grace + offline upload queue
- [x] ProGuard/R8, backup exclusion, HTTPS base config
- [ ] Set release `BASE_URL` to your Strapi host
- [ ] Keep `USE_MOCK_BACKEND = false` in release
- [ ] CI signing / Play App Signing
