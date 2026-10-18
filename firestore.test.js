const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date(Date.now() - 1000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read playlists or artist_tracks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("playlists").get());
  await assertFails(unauthDb.collection("artist_tracks").get());
});

test("2. Authenticated user can create valid playlist and query own playlists", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  await assertSucceeds(
    aliceDb.collection("playlists").doc("pl_alice_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Rivera",
      title: "Eastside Midnight Sessions",
      description: "Indie and neo-soul gems from local warehouse shows.",
      neighborhood: "Silver Lake, LA",
      moodTag: "Late Night Indie",
      coverTheme: "amber",
      isShared: false,
      shareCode: "SONIC-101",
      trackIds: ["track_1", "track_2"],
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("playlists").where("ownerId", "==", ALICE_UID).get()
  );
});

test("3. Cross-user isolation: Bob cannot read or update Alice's private playlist", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  await aliceDb.collection("playlists").doc("pl_private").set({
    ownerId: ALICE_UID,
    ownerName: "Alice Rivera",
    title: "Private Vault",
    description: "Draft mix",
    neighborhood: "Echo Park",
    moodTag: "Chill",
    coverTheme: "violet",
    isShared: false,
    shareCode: "PRIV-999",
    trackIds: [],
    likesCount: 0,
    createdAt: ts,
    updatedAt: ts,
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("playlists").doc("pl_private").get());
  await assertFails(
    bobDb.collection("playlists").doc("pl_private").update({
      title: "Hijacked",
      updatedAt: nowTimestamp(),
    })
  );
});

test("4. Shared playlists: Friends can read and like (+1) shared playlists but cannot edit title", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  await aliceDb.collection("playlists").doc("pl_shared").set({
    ownerId: ALICE_UID,
    ownerName: "Alice Rivera",
    title: "Brooklyn Rooftop Vibes",
    description: "Share with the crew",
    neighborhood: "Williamsburg, NYC",
    moodTag: "Rooftop Grooves",
    coverTheme: "crimson",
    isShared: true,
    shareCode: "BKLYN-77",
    trackIds: ["track_1"],
    likesCount: 2,
    createdAt: ts,
    updatedAt: ts,
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("playlists").where("isShared", "==", true).get()
  );
  // Bob can like (+1)
  await assertSucceeds(
    bobDb.collection("playlists").doc("pl_shared").update({
      likesCount: 3,
      updatedAt: nowTimestamp(),
    })
  );
  // Bob cannot jump likes by +50 or change title
  await assertFails(
    bobDb.collection("playlists").doc("pl_shared").update({
      likesCount: 50,
      updatedAt: nowTimestamp(),
    })
  );
  await assertFails(
    bobDb.collection("playlists").doc("pl_shared").update({
      title: "Bob's Playlist Now",
      updatedAt: nowTimestamp(),
    })
  );
});

test("5. Shadow field injection and identity spoofing are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  // Spoofing ownerId
  await assertFails(
    aliceDb.collection("playlists").doc("pl_spoof").set({
      ownerId: BOB_UID,
      ownerName: "Alice",
      title: "Spoof",
      description: "Desc",
      neighborhood: "LA",
      moodTag: "Indie",
      coverTheme: "amber",
      isShared: true,
      shareCode: "CODE-1",
      trackIds: [],
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Shadow field injection
  await assertFails(
    aliceDb.collection("playlists").doc("pl_shadow").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      title: "Shadow",
      description: "Desc",
      neighborhood: "LA",
      moodTag: "Indie",
      coverTheme: "amber",
      isShared: true,
      shareCode: "CODE-1",
      trackIds: [],
      likesCount: 0,
      isVerifiedAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("6. Local artist tracks and per-user saved_tracks enforce strict security boundaries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  await assertSucceeds(
    aliceDb.collection("artist_tracks").doc("track_indie_1").set({
      uploaderId: ALICE_UID,
      artistName: "The Velvet Echoes",
      trackTitle: "Sunset Boulevard Drift",
      city: "Los Angeles, CA",
      genre: "Indie Rock",
      streamUrl: "soniclocal://synth/indie_sunset",
      bio: "Four-piece garage indie band from Echo Park.",
      durationSec: 185,
      isPublic: true,
      playsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice saves a track in her personal library
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_tracks").doc("track_indie_1").set({
      userId: ALICE_UID,
      trackId: "track_indie_1",
      trackTitle: "Sunset Boulevard Drift",
      artistName: "The Velvet Echoes",
      city: "Los Angeles, CA",
      genre: "Indie Rock",
      streamUrl: "soniclocal://synth/indie_sunset",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's saved tracks
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_tracks").get()
  );
});
