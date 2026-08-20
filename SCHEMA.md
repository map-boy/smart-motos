# SmartMotos — Firestore Schema (Source of Truth)

Derived from `SmartMotos/Models/Models.swift`. Any platform (iOS, Android, web) implementing these models must match field names and types exactly. Update this file first when the schema changes, then update each platform's models to match — not the other way around.

## Collection: `users`
Doc ID = Firebase Auth UID.

| Field | Type | Notes |
|---|---|---|
| id | string | same as doc ID |
| name | string | |
| email | string | |
| phone | string | |
| role | string enum: `passenger` \| `driver` \| `admin` | |
| photoUrl | string? | optional |
| walletBalanceRwf | int | default 0 |
| serviceProvider | string | default `"MTN"` |
| vehicleType | string | default `"Bike"` |
| licenseNumber | string | default `""` |
| verificationStatus | string | default `"verified"` |
| isAvailableOnline | bool | default true; drivers only, but present on all docs |
| driverApplication | map: `{ status: string, inspectionCode: string }` | nested map in Firestore; flattened into `UserProfile.driverApplicationStatus` / `UserProfile.inspectionCode` by `SmartRepository.mapUserProfile` on read; written back via dot-notation (`driverApplication.status`) |
| latitude | double? | optional, drivers |
| longitude | double? | optional, drivers |

Rules: read = owner, admin, or any signed-in user if `role == 'driver'`. Create = owner only, must be `role: passenger`. Update = owner (role unchanged) or admin. Delete = admin only.

## Collection: `trips`
| Field | Type | Notes |
|---|---|---|
| id | string | |
| riderId | string | |
| orderType | string | |
| recipientName | string? | goods orders |
| recipientPhone | string? | goods orders |
| cargoDescription | string? | goods orders |
| riderName | string | |
| driverId | string? | |
| driverName | string? | |
| driverAvatar | string? | |
| driverRating | float | |
| vehiclePlate | string | |
| pickup | LocationPoint (address, latitude, longitude) | |
| dropoff | LocationPoint | |
| status | string enum: `REQUESTED`\|`OFFERED`\|`DRIVER_ASSIGNED`\|`DRIVER_ARRIVED`\|`IN_PROGRESS`\|`COMPLETED`\|`CANCELLED` | |
| fareRwf | int | |
| baseFareRwf | int | |
| distanceKm | double | |
| durationMins | int | |
| paymentMethod | string | |
| bargainAmountRwf | int? | |
| ratingGiven | int | |
| ratingFeedback | string? | |
| timestamp | string | |
| offeredDriverId | string? | used by dispatch.ts during matching |

Index required: `riderId` ASC + `createdAt` DESC (see `firestore.indexes.json`).
Rules: read = rider, assigned driver, or admin. Create = rider only (`riderId == auth.uid`). Update = rider or admin. Delete = admin only.

## Collection: `topupRequests`
| Field | Type | Notes |
|---|---|---|
| id | string | |
| userId | string | |
| userName | string | |
| amountRwf | int | |
| momoNumber | string | |
| status | string | `pending` \| `approved` \| `rejected` |
| timeAgo | string | display string, not a timestamp type |

Rules: read = owner or admin. Create = owner only. Update/delete = admin only (approve/reject wallet credit is admin-gated server-side via rules, not just app UI — see audit report §3).

## Collection: `roleChangeRequests`
| Field | Type | Notes |
|---|---|---|
| uid | string | requester |
| (other fields not yet in Models.swift — audit before Android build) | | |

Rules: read = owner or admin. Create = owner only. Update/delete = disabled (`false`) — immutable once created.

## Sub-model: `SupportMessage` (chat, location TBD — not yet tied to a top-level collection in Models.swift; confirm actual Firestore path before porting to Android)
| Field | Type |
|---|---|
| id | string |
| sender | string |
| text | string |
| isUser | bool |
| timeAgo | string |

## Sub-model: `DriverInfo` (denormalized read-model, not its own collection — appears to be projected from `users` where `role == driver`)
| Field | Type |
|---|---|
| id | string |
| name | string |
| phone | string |
| rating | float |
| plateNumber | string |
| avatarUrl | string |
| currentLocation | LocationPoint |
| vehicleType | string |

---
**Confirmed by direct code inspection** (not guesses — checked against `SmartRepository.swift`):

1. **Correction to an earlier audit note:** `driverApplication.status`/`driverApplicationStatus` is NOT a bug. `SmartRepository.mapUserProfile` (checked directly) correctly reads the nested Firestore map `driverApplication: { status, inspectionCode }` and flattens both `status` and `inspectionCode` into the flat Swift `UserProfile` fields on every read; writes use Firestore dot-notation (`driverApplication.status`) to update a single nested field, which is standard and correct. Android should replicate the **nested Firestore shape** (`driverApplication` map) and can flatten it into its own model the same way, or keep it nested — either is fine as long as reads/writes agree.
2. **`supportMessages` is hardcoded mock data**, not a Firestore collection — see `SmartRepository.swift` line ~44. There is no `collection("supportMessages")` call anywhere. Do not scaffold a Firestore-backed support chat on Android from this; it doesn't exist on iOS yet either.
3. **`roleChangeRequests` has security rules defined but is never read or written by the app.** Either a planned-but-unbuilt feature, or dead rules left over from an earlier design. Confirm with whoever owns product scope before deciding whether Android needs it.
4. **`DriverInfo`** is a denormalized read-model, not a separate collection — `nearbyDriversListener` queries `users` where `role == "driver"`.

