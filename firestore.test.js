const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const fs = require('fs');

const DATABASE_ID = 'ai-studio-android-sesmartc-f18ec6ec-88bd-4505-96f2-2dfedd93a2cc';

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-no-project',
    firestore: {
      rules: fs.readFileSync('firestore.rules', 'utf8'),
      host: '127.0.0.1',
      port: 8085,
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

test('Unauthenticated user cannot read or write service notes', async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
  const noteRef = unauthedDb.doc('users/alice/service_notes/note1');

  await assertFails(noteRef.get());
  await assertFails(
    noteRef.set({
      userId: 'alice',
      noteNumber: 'SC-001',
      customerName: 'Budi',
      customerPhone: '081234567890',
      deviceType: 'HP',
      brand: 'Samsung',
      model: 'A54',
      issueDescription: 'Layar blank',
      serviceStatus: 'Menunggu',
    })
  );
});

test('Authenticated user can write and read own service notes', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore({ databaseId: DATABASE_ID });
  const noteRef = aliceDb.doc('users/alice/service_notes/note1');

  await assertSucceeds(
    noteRef.set({
      userId: 'alice',
      noteNumber: 'SC-001',
      customerName: 'Budi',
      customerPhone: '081234567890',
      deviceType: 'HP',
      brand: 'Samsung',
      model: 'A54',
      issueDescription: 'Layar blank',
      serviceStatus: 'Menunggu',
    })
  );

  const doc = await assertSucceeds(noteRef.get());
  assert.strictEqual(doc.data().customerName, 'Budi');
});

test('Authenticated user cannot write or read another user notes', async () => {
  const bobDb = testEnv.authenticatedContext('bob').firestore({ databaseId: DATABASE_ID });
  const aliceNoteRef = bobDb.doc('users/alice/service_notes/note1');

  await assertFails(aliceNoteRef.get());
  await assertFails(
    aliceNoteRef.set({
      userId: 'alice',
      noteNumber: 'SC-001',
      customerName: 'Hacker',
      customerPhone: '081234567890',
      deviceType: 'HP',
      brand: 'Samsung',
      model: 'A54',
      issueDescription: 'Hack',
      serviceStatus: 'Menunggu',
    })
  );
});
