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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write user profiles, saved routes, or reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      city: "Toluca",
      createdAt: now,
      updatedAt: now,
      lastLoginAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("saved_routes").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("reports").get()
  );
});

test("Authenticated owner can register profile and update mutable fields while preserving createdAt and userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice MueveT",
      email: "alice@example.com",
      photoUrl: "https://example.com/avatar.png",
      city: "Toluca",
      createdAt: created,
      updatedAt: created,
      lastLoginAt: created,
    })
  );

  // Partial update on subsequent sign-in
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Toluca",
      city: "Toluca · Centro",
      updatedAt: new Date(Date.now() - 1000),
      lastLoginAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field (shadow update)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp skew
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lastLoginAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation blocks reading or writing another user's profile, saved routes, and reports", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      city: "Toluca",
      createdAt: created,
      updatedAt: created,
      lastLoginAt: created,
    });
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("saved_routes")
      .doc("ruta_76")
      .set({
        routeCode: "76",
        routeTitle: "Centro · Paseo Tollocan · Terminal",
        origin: "Centro",
        destination: "Terminal Toluca",
        transportType: "CAMION",
        priceMxn: 12.0,
        createdAt: created,
        updatedAt: created,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("saved_routes").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("reports").get()
  );
});

test("Authenticated owner can manage saved_routes and reports matching Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollection list works (no existing() dereference in allow list)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_routes").get()
  );

  // Create saved route
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_routes")
      .doc("ruta_76")
      .set({
        routeCode: "76",
        routeTitle: "Centro · Paseo Tollocan · Terminal",
        origin: "Centro",
        destination: "Terminal Toluca",
        transportType: "CAMION",
        priceMxn: 12.0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject extra `id` field when @DocumentId is used
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_routes")
      .doc("ruta_bad")
      .set({
        id: "ruta_bad",
        routeCode: "24",
        routeTitle: "Terminal · Galerías",
        origin: "Terminal",
        destination: "Galerías Toluca",
        transportType: "CAMION",
        priceMxn: 12.0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create transit report
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reports")
      .doc("rep_001")
      .set({
        routeId: "ruta_76",
        routeCode: "76",
        routeTitle: "Centro · Paseo Tollocan · Terminal",
        capacity: "HAY_LUGAR",
        priceMxn: 12.0,
        addedStops: ["Los Portales", "Ciudad Universitaria"],
        gpsPointsCount: 8,
        latitude: 19.2826,
        longitude: -99.6557,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Delete saved route
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_routes")
      .doc("ruta_76")
      .delete()
  );
});
