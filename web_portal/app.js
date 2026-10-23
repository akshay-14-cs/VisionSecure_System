
const firebaseConfig = {
    apiKey: "AIzaSyAT78J-rPsZ8SJf7yp-1FGaOTlPNtKnsu0",
    projectId: "vision-2406131",
};


let db;
try {
    firebase.initializeApp(firebaseConfig);
    db = firebase.firestore();
} catch (e) {
    console.error("Firebase init error:", e);
}

let currentSessionId = null;
let unsubscribe = null;

async function hashPassword(password) {
    const msgUint8 = new TextEncoder().encode(password);
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const hashHex = hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
    return hashHex;
}

async function initiateLogin() {
    const orgId = document.getElementById("orgId").value.trim();
    const studentId = document.getElementById("studentId").value.trim();
    const password = document.getElementById("password").value.trim();
    const subjectCode = document.getElementById("subjectCode").value.trim();
    const errorEl = document.getElementById("loginError");

    if (!orgId || !studentId || !password || !subjectCode) {
        errorEl.textContent = "Please fill in all fields.";
        errorEl.style.display = "block";
        return;
    }

    errorEl.style.display = "none";
    document.getElementById("btnLogin").disabled = true;
    document.getElementById("btnLogin").textContent = "Authenticating...";

    try {

        let docId = studentId;
        let pureStudentId = studentId;
        if (studentId.startsWith(orgId + "_")) {
            docId = studentId;
            pureStudentId = studentId.substring(orgId.length + 1);
        } else {
            docId = `${orgId}_${studentId}`;
        }

        const studentDoc = await db.collection("students").doc(docId).get();

        if (!studentDoc.exists) {
            throw new Error("Student ID not found in database.");
        }

        const data = studentDoc.data();


        const inputHash = await hashPassword(password);
        if (data.password_hash !== inputHash) {
            throw new Error("Incorrect password.");
        }


        const timetableQuery = await db.collection("timetable")
            .where("org_id", "==", orgId)
            .where("subject_code", "==", subjectCode)
            .get();

        if (timetableQuery.empty) {
            throw new Error("Wrong subject ID");
        }

        let isOnline = false;
        timetableQuery.forEach(doc => {
            if (doc.data().exam_mode === "online") {
                isOnline = true;
            }
        });

        if (!isOnline) {
            throw new Error("Exam is not online");
        }


        if (!data.registered_exams || !data.registered_exams.includes(subjectCode)) {
            throw new Error("Student is not registered for this subject.");
        }


        const attendanceQuery = await db.collection("attendance")
            .where("org_id", "==", orgId)
            .where("student_id", "==", pureStudentId)
            .where("subject_code", "==", subjectCode)
            .where("status", "==", "PRESENT")
            .get();

        if (!attendanceQuery.empty) {
            document.getElementById("btnLogin").textContent = "Access Granted...";

            const loginSection = document.getElementById("login-section");
            const examSection = document.getElementById("exam-section");

            loginSection.classList.remove("active");
            setTimeout(() => {
                examSection.classList.add("active");
                document.getElementById("examStudentDisplay").textContent = pureStudentId;
                document.getElementById("examSubjectDisplay").textContent = subjectCode;
            }, 500);
            return;
        }


        document.getElementById("btnLogin").textContent = "Securing Terminal...";

        const loginSection = document.getElementById("login-section");
        const qrSection = document.getElementById("qr-section");

        loginSection.classList.remove("active");
        setTimeout(() => {
            qrSection.classList.add("active");


            listenForVerification(orgId, studentId, subjectCode);

        }, 500);

    } catch (error) {
        console.error("Login Error:", error);
        errorEl.textContent = error.message || "Error connecting to secure server.";
        errorEl.style.display = "block";
        document.getElementById("btnLogin").disabled = false;
        document.getElementById("btnLogin").textContent = "Verify Credentials";


        if (window.shakeEye) {
            window.shakeEye();
        }
    }
}

function listenForVerification(orgId, studentId, subjectCode) {
    if (!db) return;

    const startTime = Date.now();

    unsubscribe = db.collection("attendance")
        .where("org_id", "==", orgId)
        .where("student_id", "==", studentId)
        .where("subject_code", "==", subjectCode)
        .where("status", "==", "PRESENT")
        .onSnapshot((snapshot) => {
            if (!snapshot.empty) {
                unlockPC(studentId, subjectCode);
            }
        }, (error) => {
            console.error("Firestore listener error:", error);
        });
}

function unlockPC(studentId, subjectCode) {
    if (unsubscribe) {
        unsubscribe();
    }

    const qrSection = document.getElementById("qr-section");
    const examSection = document.getElementById("exam-section");


    qrSection.classList.remove("active");
    setTimeout(() => {
        examSection.classList.add("active");
        document.getElementById("examStudentDisplay").textContent = studentId;
        document.getElementById("examSubjectDisplay").textContent = subjectCode;
    }, 500);
}
