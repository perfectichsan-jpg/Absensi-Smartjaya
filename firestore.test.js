const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_intern_01";
const BOB_UID = "bob_intern_02";
const ADMIN_UID = "admin_mentor_01";
const ADMIN_EMAIL = "perfectichsan@gmail.com";

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

function buildProfilePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    fullName: "Alice Pratama",
    email: "alice@example.com",
    division: "Engineering",
    university: "Universitas Indonesia",
    mentorName: "Budi Santoso",
    companyName: "PT Karya Nusantara",
    workModeDefault: "WFO",
    shiftStart: "08:30",
    shiftEnd: "17:30",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function buildAttendancePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    internName: "Alice Pratama",
    division: "Engineering",
    dateKey: "2026-11-26",
    workMode: "WFO",
    attendanceStatus: "ON_TIME",
    checkInTime: now,
    locationLabel: "Kantor Pusat - Jakarta",
    dailyActivities: "Setup modul autentikasi dan review PR",
    sessionState: "WORKING",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function buildLeavePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    internName: "Alice Pratama",
    division: "Engineering",
    leaveType: "IZIN_KAMPUS",
    startDate: "2026-11-28",
    endDate: "2026-11-28",
    reason: "Bimbingan skripsi dengan dosen pembimbing di kampus.",
    status: "PENDING",
    reviewerNote: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: denied across users, attendance_logs, and leave_requests", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(buildProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("attendance_logs").where("userId", "==", ALICE_UID).get());
  await assertFails(
    unauthDb.collection("attendance_logs").doc(`${ALICE_UID}_2026-11-26`).set(buildAttendancePayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("leave_requests").where("userId", "==", ALICE_UID).get());
});

test("User profile: owner create/read/partial update succeed; cross-user PII and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(buildProfilePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update allowed fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      division: "Product Engineering",
      shiftStart: "09:00",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user PII read/list rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set(
      buildProfilePayload(BOB_UID, { createdAt: new Date(Date.now() + 60000), updatedAt: new Date(Date.now() + 60000) })
    )
  );
});

test("Attendance logs: check-in, logbook edit, check-out, filtered query succeed; unfiltered query & @DocumentId id fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const logId = `${ALICE_UID}_2026-11-26`;

  // Create check-in
  await assertSucceeds(aliceDb.collection("attendance_logs").doc(logId).set(buildAttendancePayload(ALICE_UID)));

  // Reject creating with @DocumentId "id" field
  await assertFails(
    aliceDb.collection("attendance_logs").doc(`${ALICE_UID}_2026-11-27`).set(
      buildAttendancePayload(ALICE_UID, { id: "bad_id", dateKey: "2026-11-27" })
    )
  );

  // Filtered Kotlin query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("attendance_logs").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("attendance_logs").get());

  // Cross-user read & hijack fail
  await assertFails(bobDb.collection("attendance_logs").doc(logId).get());
  await assertFails(
    bobDb.collection("attendance_logs").doc(logId).update({
      dailyActivities: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Check-out transition WORKING -> COMPLETED succeeds
  await assertSucceeds(
    aliceDb.collection("attendance_logs").doc(logId).update({
      checkOutTime: new Date(Date.now() - 500),
      dailyActivities: "Menyelesaikan integrasi API dan dokumentasi",
      sessionState: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cannot revert COMPLETED back to WORKING
  await assertFails(
    aliceDb.collection("attendance_logs").doc(logId).update({
      sessionState: "WORKING",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("attendance_logs").doc(logId).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Leave requests: owner create PENDING, filtered query, admin review, terminal lock, and email spoofing guard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const verifiedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  // Owner cannot self-approve on create
  await assertFails(
    aliceDb.collection("leave_requests").doc("req_bad").set(buildLeavePayload(ALICE_UID, { status: "APPROVED" }))
  );

  // Valid PENDING create
  await assertSucceeds(aliceDb.collection("leave_requests").doc("req_1").set(buildLeavePayload(ALICE_UID)));

  // Owner filtered query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("leave_requests").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("leave_requests").get());

  // Owner cannot self-approve on update
  await assertFails(
    aliceDb.collection("leave_requests").doc("req_1").update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Spoofed admin (email_verified: false) rejected
  await assertFails(spoofedAdminDb.collection("leave_requests").get());
  await assertFails(
    spoofedAdminDb.collection("leave_requests").doc("req_1").update({
      status: "APPROVED",
      reviewerNote: "Spoofed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified admin can list and approve
  await assertSucceeds(verifiedAdminDb.collection("leave_requests").get());
  await assertSucceeds(
    verifiedAdminDb.collection("leave_requests").doc("req_1").update({
      status: "APPROVED",
      reviewerNote: "Disetujui, selamat bimbingan.",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: owner cannot edit or delete APPROVED leave request
  await assertFails(
    aliceDb.collection("leave_requests").doc("req_1").update({
      reason: "Diubah setelah disetujui",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(aliceDb.collection("leave_requests").doc("req_1").delete());
});
