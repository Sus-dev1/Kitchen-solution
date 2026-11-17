const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_kitchen_123";
const BOB_UID = "bob_kitchen_456";

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

function validTimestamp() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated user: cannot read or write any Kitchen Solution collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = validTimestamp();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pantry_items")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pantry_items")
      .doc("item_1")
      .set({
        name: "Dada Ayam",
        quantity: 300,
        unit: "g",
        stepDelta: 50,
        location: "Kulkas (Fridge)",
        daysUntilExpiry: 2,
        category: "protein",
        lowThreshold: 100,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("UserProfile: owner create, partial update, and security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validTimestamp();

  // Valid create matching Kotlin UserProfile payload
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Chef Alex Wibowo",
      email: "alex@kitchensolution.id",
      dietType: "Halal / General",
      allergies: "Kacang Tanah",
      defaultServings: 2,
      seededDefaults: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dietType: "Tinggi Protein",
      defaultServings: 4,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field (Shadow Update)
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

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      defaultServings: 3,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject cross-user read and list
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("PantryItems: owner CRUD, quantity update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validTimestamp();

  const pantryCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("pantry_items");

  // Create without 'id' field (since Kotlin uses @DocumentId)
  await assertSucceeds(
    pantryCol.doc("dada_ayam").set({
      name: "Dada Ayam",
      quantity: 300,
      unit: "g",
      stepDelta: 50,
      location: "Kulkas (Fridge)",
      daysUntilExpiry: 2,
      category: "protein",
      lowThreshold: 100,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List subcollection succeeds for owner (including empty or populated)
  await assertSucceeds(pantryCol.get());

  // Update quantity
  await assertSucceeds(
    pantryCol.doc("dada_ayam").update({
      quantity: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject negative quantity
  await assertFails(
    pantryCol.doc("dada_ayam").update({
      quantity: -10,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject writing extra @DocumentId 'id' field
  await assertFails(
    pantryCol.doc("telur_ayam").set({
      id: "telur_ayam",
      name: "Telur Ayam",
      quantity: 4,
      unit: "pcs",
      stepDelta: 1,
      location: "Suhu Ruang",
      daysUntilExpiry: 12,
      category: "egg",
      lowThreshold: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject Bob reading or writing Alice's pantry
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pantry_items")
      .doc("dada_ayam")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pantry_items")
      .get()
  );

  // Delete succeeds for owner
  await assertSucceeds(pantryCol.doc("dada_ayam").delete());
});

test("ShoppingItems and JournalNotes: owner CRUD and validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validTimestamp();

  const shoppingCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("shopping_items");

  await assertSucceeds(
    shoppingCol.doc("item_daun_bawang").set({
      name: "Daun Bawang (1 batang)",
      checked: false,
      sourceTag: "Auto AI",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    shoppingCol.doc("item_daun_bawang").update({
      checked: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(shoppingCol.get());

  const journalCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("journal_notes");

  await assertSucceeds(
    journalCol.doc("note_1").set({
      recipeTitle: "Ayam Kecap Mentega Racikan V1",
      note: "Tambahkan ekstra 1/2 sdt merica hitam saat menumis bawang putih.",
      rating: 4.8,
      dateLabel: "14 Nov 2026",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(journalCol.get());

  // Invalid rating > 5.0 fails
  await assertFails(
    journalCol.doc("note_bad").set({
      recipeTitle: "Test",
      note: "Test note",
      rating: 6.0,
      dateLabel: "16 Nov 2026",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("journal_notes")
      .get()
  );
});

test("SavedRecipes & MealPlans: owner CRUD and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validTimestamp();

  const savedCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_recipes");

  await assertSucceeds(
    savedCol.doc("fav_ayam_kecap").set({
      title: "Ayam Tumis Kecap Bawang",
      description: "Resep favorit keluarga.",
      durationMinutes: 20,
      servings: 2,
      difficulty: "Mudah",
      categoryName: "NUSANTARA",
      isCustom: false,
      caloriesKcal: 360,
      proteinGrams: 30,
      carbsGrams: 18,
      fatGrams: 16,
      ingredientsSerialized: "[{\"name\":\"Dada Ayam (300g)\",\"amountLabel\":\"300 g\",\"pantryMatchKeyword\":\"Dada Ayam\",\"deductAmount\":300,\"isBasicStaple\":false}]",
      stepsSerialized: "[{\"instruction\":\"Tumis bawang putih dan ayam.\",\"durationSeconds\":180}]",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(savedCol.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_recipes").get()
  );

  const planCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("meal_plans");

  await assertSucceeds(
    planCol.doc("Senin_Makan_Siang").set({
      dayOfWeek: "Senin",
      mealSlot: "Makan Siang",
      recipeId: "ayam_tumis_kecap",
      recipeTitle: "Ayam Tumis Kecap Bawang",
      servings: 2,
      missingItemsSummary: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(planCol.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("meal_plans").get()
  );
});
