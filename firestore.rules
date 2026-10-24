// Seed user data for Firestore.
// Usage:
// 1) npm install firebase-admin
// 2) set GOOGLE_APPLICATION_CREDENTIALS to your Firebase service account JSON
// 3) node scripts/seed_users.js

const admin = require('firebase-admin');

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
});

const db = admin.firestore();

async function seedUsers() {
  const users = [
    {
      uid: 'REPLACE_WITH_REAL_FIREBASE_UID',
      email: 'owner@se-smart-cell.com',
      displayName: 'Owner SE Smart Cell',
      customUserId: 'USER_100001',
      customPassword: 'PASS_123456',
      loginMethod: 'GOOGLE',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
  ];

  for (const user of users) {
    const userRef = db.collection('users').doc(user.uid);

    await userRef.set(
      {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        customUserId: user.customUserId,
        customPassword: user.customPassword,
        loginMethod: user.loginMethod,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt,
      },
      { merge: true }
    );

    await userRef.collection('store_settings').doc('default').set(
      {
        userId: user.uid,
        storeName: 'SE SMART CELL',
        storeTagline: 'Servis HP & Laptop Cepat & Terpercaya',
        storeAddress: 'Alamat toko Anda',
        storePhone: '081234567890',
        qrisMerchantName: 'SE SMART CELL',
        bankAccounts: 'BCA: 1234567890 a/n Owner',
        ewalletAccounts: 'DANA: 081234567890 a/n Owner',
        warrantyTerms: 'Garansi sesuai kebijakan toko.',
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    await userRef.collection('service_notes').doc('demo-note-001').set({
      userId: user.uid,
      noteNumber: 'SSC-001',
      entryDate: Date.now(),
      customerName: 'Customer Demo',
      customerPhone: '081122334455',
      deviceType: 'HP',
      brand: 'Samsung',
      model: 'A15',
      imeiOrSerial: '1234567890',
      accessories: 'Dus, kabel, casing',
      issueDescription: 'Layar tidak responsif',
      technicianCheckResult: 'LCD perlu diganti',
      repairAction: 'Ganti LCD',
      serviceFee: 150000,
      sparepartFee: 350000,
      discount: 0,
      downPayment: 0,
      estimatedCompletionDate: '2026-01-15',
      technicianName: 'Teknisi 1',
      serviceStatus: 'Sedang Dikerjakan',
      warrantyPeriod: '30 hari',
      warrantyEndDate: '2026-02-14',
      initialPaymentMethod: 'Tunai',
      notes: 'Demo data untuk testing Firestore.',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    await userRef.collection('payment_records').doc('demo-payment-001').set({
      userId: user.uid,
      noteNumber: 'SSC-001',
      amount: 150000,
      paymentMethod: 'Tunai',
      referenceNumber: 'DEMO-001',
      paymentDate: Date.now(),
      paymentType: 'DP',
      isVerified: true,
      notes: 'Demo pembayaran',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }

  console.log('Seed completed.');
}

seedUsers().catch((error) => {
  console.error('Seed failed:', error);
  process.exit(1);
});

