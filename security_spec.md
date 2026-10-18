# Security Specification — SonicLocal

## 1. Data Invariants
1. **Authentication Gate**: Unauthenticated callers (`request.auth == null`) cannot read or write any collection (`/playlists`, `/artist_tracks`, `/users/{userId}/saved_tracks`).
2. **Ownership & Isolation**:
   - Private playlists (`isShared == false`) can only be read, listed, updated, or deleted by `resource.data.ownerId == request.auth.uid`.
   - Shared playlists (`isShared == true`) can be read and listed by authenticated users querying with `isShared == true`, updated fully only by `ownerId`, or have `likesCount` incremented by `+1` by an authenticated listener.
   - Local artist tracks (`/artist_tracks/{trackId}`) can be created only where `uploaderId == request.auth.uid`, read/listed when `uploaderId == request.auth.uid || isPublic == true`, and deleted only by `uploaderId`.
   - Saved tracks (`/users/{userId}/saved_tracks/{trackId}`) are strictly isolated to `request.auth.uid == userId`.
3. **Immutable Fields**: `ownerId`, `uploaderId`, `userId`, and `createdAt` cannot be mutated on `update`.
4. **Temporal & Structural Bounds**: All timestamps must satisfy `is timestamp && <= request.time`, all strings enforce `.size()` bounds, arrays enforce `.size() <= 50`, and document IDs match `^[a-zA-Z0-9_\-]+$`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("playlists").get()` -> Reject (`PERMISSION_DENIED`).
2. **Cross-User Private Playlist Read**: Alice reading Bob's playlist where `isShared == false` -> Reject.
3. **Unfiltered Collection Scan**: `aliceDb.collection("playlists").get()` without `ownerId` or `isShared` filter -> Reject.
4. **Identity Spoofing on Playlist Create**: Alice creating a playlist with `ownerId: "bob_456"` -> Reject.
5. **Shadow Field Injection**: Creating or updating a playlist with extra key `isAdmin: true` -> Reject (`hasOnly` check).
6. **Owner Hijack on Update**: Bob updating Alice's shared playlist `ownerId` or `title` -> Reject.
7. **Timestamp Tampering on Update**: Owner mutating `createdAt` during playlist update -> Reject.
8. **Future Timestamp Injection**: Writing `createdAt` in the future (`> request.time`) -> Reject.
9. **Oversized String / Resource Poisoning**: Writing `title` > 100 chars or `trackIds` > 50 items -> Reject.
10. **Invalid Enum Value**: Writing `coverTheme: "neon_hack"` or invalid `genre` -> Reject.
11. **Arbitrary Like Manipulation**: Incrementing `likesCount` by +100 in a single update by non-owner -> Reject.
12. **Cross-User Saved Track Write/Read**: Bob reading or writing `/users/alice_123/saved_tracks/track_1` -> Reject.
