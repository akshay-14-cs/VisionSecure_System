import os
import io
import json
import base64
import numpy as np
from fastapi import FastAPI, Form, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime
import firebase_admin
from firebase_admin import credentials, firestore
from PIL import Image


try:
    if not firebase_admin._apps:
        
        cred_path = os.path.join(os.path.dirname(__file__), "firebase_credentials.json")
        if os.path.exists(cred_path):
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)
        else:
            
            firebase_admin.initialize_app()
    db = firestore.client()
except Exception as e:
    print("\n[!] Firebase init error:", e)
    print("[!] Please place your 'firebase_credentials.json' (Admin SDK private key) inside the backend folder.\n")
    db = None

app = FastAPI(title="VisionSecure Biometric API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


try:
    import face_recognition
    FACE_REC_AVAILABLE = True
except ImportError:
    FACE_REC_AVAILABLE = False
    print("face_recognition module not found. Using mock for demo/testing.")

def get_face_encoding(image_bytes):
    if not FACE_REC_AVAILABLE:
        raise HTTPException(status_code=503, detail="Biometric recognition service is unavailable.")
    
    try:
        
        from PIL import ImageOps
        pil_image = Image.open(io.BytesIO(image_bytes))
        pil_image = ImageOps.exif_transpose(pil_image)
        if pil_image.mode != "RGB":
            pil_image = pil_image.convert("RGB")
            
        
        
        pil_image.thumbnail((800, 800))
        
        
        image = np.array(pil_image)
        
        encodings = face_recognition.face_encodings(image)
        if len(encodings) == 0:
            return None
        return encodings[0].tolist()
    except Exception as e:
        print("Face encoding error:", e)
        return None

def compare_faces(known_encoding, face_encoding_to_check, tolerance=0.65):
    if not FACE_REC_AVAILABLE:
        raise HTTPException(status_code=503, detail="Biometric recognition service is unavailable.")
    
    try:
        match = face_recognition.compare_faces([np.array(known_encoding)], np.array(face_encoding_to_check), tolerance)[0]
        return bool(match)
    except Exception as e:
        print("Face compare error:", e)
        return False

def get_best_match(all_encodings_dict, face_encoding_to_check, tolerance=0.6):
    if not FACE_REC_AVAILABLE:
        raise HTTPException(status_code=503, detail="Biometric recognition service is unavailable.")
        
    best_match_id = None
    min_distance = tolerance
    
    try:
        check_enc = np.array(face_encoding_to_check)
        for existing_id, existing_enc in all_encodings_dict.items():
            known_enc = np.array(existing_enc)
            distance = face_recognition.face_distance([known_enc], check_enc)[0]
            if distance < min_distance:
                min_distance = distance
                best_match_id = existing_id
                
        return best_match_id
    except Exception as e:
        print("Face get_best_match error:", e)
        return None

def get_all_encodings(org_id):
    docs = db.collection("students").where("org_id", "==", org_id).where("has_face_registered", "==", True).stream()
    encodings = {}
    for doc in docs:
        data = doc.to_dict()
        if "face_encoding" in data:
            encodings[doc.id] = data["face_encoding"]
    return encodings

def log_intruder(org_id, title, details, image_base64=None):
    record = {
        "org_id": org_id,
        "title": title,
        "details": details,
        "timestamp": datetime.now().isoformat(),
        "resolved": False,
        "intruder_image_base64": image_base64
    }
    db.collection("intruder_records").add(record)
    db.collection("admin_notifications").add(record)

@app.post("/biometric/register")
async def register_face(
    orgId: str = Form(...),
    userId: str = Form(...),
    inchargeId: str = Form(...),
    faceImage: UploadFile = File(...)
):
    
    incharge_doc_id = inchargeId if inchargeId.startswith(f"{orgId}_") else f"{orgId}_{inchargeId}"
    faculty_doc = db.collection("users").document(incharge_doc_id).get()
    if not faculty_doc.exists:
        raise HTTPException(status_code=403, detail="Faculty does not exist.")
    faculty_data = faculty_doc.to_dict()
    if not faculty_data.get("is_incharge"):
        raise HTTPException(status_code=403, detail="Faculty is not authorized as Incharge.")

    
    student_doc = db.collection("students").document(f"{orgId}_{userId}").get()
    if not student_doc.exists:
        raise HTTPException(status_code=404, detail="Student does not exist.")
    student_data = student_doc.to_dict()
    
    
    if student_data.get("org_id") != orgId:
        raise HTTPException(status_code=403, detail="Organization mismatch.")
    if student_data.get("department") != faculty_data.get("department"):
        raise HTTPException(status_code=403, detail="Student does not belong to your department.")
        
    
    if not student_data.get("exam_registered"):
        raise HTTPException(status_code=403, detail="Student must complete exam registration before face registration.")
    if student_data.get("has_face_registered"):
        raise HTTPException(status_code=403, detail="Face is already registered for this student.")

    image_bytes = await faceImage.read()
    
    
    from PIL import ImageOps
    pil_img = Image.open(io.BytesIO(image_bytes))
    pil_img = ImageOps.exif_transpose(pil_img)
    if pil_img.mode != "RGB":
        pil_img = pil_img.convert("RGB")
    pil_img.thumbnail((800, 800))
    
    
    buffer = io.BytesIO()
    pil_img.save(buffer, format="JPEG", quality=75)
    compressed_bytes = buffer.getvalue()
    
    image_base64 = base64.b64encode(compressed_bytes).decode('utf-8')
    encoding = get_face_encoding(image_bytes)
    
    if encoding is None:
        raise HTTPException(status_code=400, detail="No face found in image.")
    
    
    all_encodings = get_all_encodings(orgId)
    existing_id = get_best_match(all_encodings, encoding, tolerance=0.5) 
    if existing_id:
        student_doc = db.collection("students").document(existing_id).get()
        s_data = student_doc.to_dict() or {}
        s_name = s_data.get("name", "Unknown")
        enroll_no = s_data.get("enrollment_no", existing_id.replace(f"{orgId}_", ""))
        raise HTTPException(status_code=400, detail=f"This face is already registered to {enroll_no} - {s_name}")
            
    
    doc_id = f"{orgId}_{userId}"
    db.collection("students").document(doc_id).set({
        "has_face_registered": True,
        "face_image_base64": image_base64,
        "face_encoding": encoding,
        "face_registered_by": inchargeId,
        "face_registered_at": datetime.now().isoformat(),
        "face_registered_by_role": "INCHARGE"
    }, merge=True)
    
    return {"status": "success", "message": "Face registered successfully."}

@app.post("/biometric/verify-offline")
async def verify_offline(
    orgId: str = Form(...),
    studentId: str = Form(""), 
    date: str = Form(...),
    timing: str = Form(...),
    classroom: str = Form(...),
    invigilatorId: str = Form(...),
    faceImage: UploadFile = File(...)
):
    image_bytes = await faceImage.read()
    
    from PIL import ImageOps
    pil_img = Image.open(io.BytesIO(image_bytes))
    pil_img = ImageOps.exif_transpose(pil_img)
    if pil_img.mode != "RGB":
        pil_img = pil_img.convert("RGB")
    pil_img.thumbnail((800, 800))
    
    buffer = io.BytesIO()
    pil_img.save(buffer, format="JPEG", quality=75)
    compressed_bytes = buffer.getvalue()
    
    image_base64 = base64.b64encode(compressed_bytes).decode('utf-8')
    encoding = get_face_encoding(image_bytes)
    
    if encoding is None:
        log_intruder(orgId, "Verification Failed", "No face detected in offline mode.", image_base64)
        raise HTTPException(status_code=400, detail="No face detected.")
        
    
    tt_query = db.collection("timetable") \
        .where("org_id", "==", orgId) \
        .where("date", "==", date) \
        .where("timing", "==", timing) \
        .where("hall_no", "==", classroom) \
        .stream()
    
    tt_doc = None
    tt_id = None
    for doc in tt_query:
        tt_doc = doc.to_dict()
        tt_id = doc.id
        break
        
    if not tt_doc:
        raise HTTPException(status_code=403, detail="No exam scheduled in this room at this time.")
        
    subject_code = tt_doc.get("subject_code")
    faculty_assigned = tt_doc.get("faculty_id")
    
    raw_invigilator = invigilatorId
    if raw_invigilator.startswith(f"{orgId}_"):
        raw_invigilator = raw_invigilator[len(orgId)+1:]
    elif raw_invigilator.startswith(f"{orgId}-"):
        raw_invigilator = raw_invigilator[len(orgId)+1:]
        
    if faculty_assigned != invigilatorId and faculty_assigned != raw_invigilator:
        raise HTTPException(status_code=403, detail="Supervisor is not assigned to this exam.")

    
    all_encodings = get_all_encodings(orgId)
    matched_student_id = get_best_match(all_encodings, encoding, tolerance=0.48)
            
    if not matched_student_id:
        log_intruder(orgId, "Verification Failed", f"Unrecognized face during exam in {classroom}", image_base64)
        raise HTTPException(status_code=403, detail="Face not recognized.")
        
    
    student_doc = db.collection("students").document(matched_student_id).get()
    if not student_doc.exists:
        raise HTTPException(status_code=404, detail="Matched student does not exist in DB.")
        
    student_data = student_doc.to_dict()
    s_name = student_data.get("name", "Unknown")
    enroll_no = student_data.get("enrollment_no", matched_student_id.replace(f"{orgId}_", ""))
    
    if not student_data.get("exam_registered"):
        raise HTTPException(status_code=403, detail=f"Intruder Detected: {enroll_no} ({s_name}) has not completed exam registration.")
        
    registered_exams = student_data.get("registered_exams", [])
    if subject_code not in registered_exams:
        raise HTTPException(status_code=403, detail=f"Intruder Detected: {enroll_no} ({s_name}) is not registered for this subject.")
        
    
    pure_student_id = matched_student_id.replace(f"{orgId}_", "")
    record_id = f"attendance_{orgId}_{pure_student_id}_{tt_id}"
    attendance_ref = db.collection("attendance").document(record_id)
    
    if attendance_ref.get().exists:
        raise HTTPException(status_code=400, detail="Student attendance already recorded for this exam.")
        
    attendance_ref.set({
        "org_id": orgId,
        "student_id": pure_student_id,
        "subject_code": subject_code,
        "exam_date": date,
        "exam_time": timing,
        "class": tt_doc.get("class", ""),
        "room": classroom,
        "supervisor_id": invigilatorId,
        "status": "PRESENT",
        "verification_timestamp": datetime.now().isoformat()
    })
    
    return {"status": "success", "message": f"Verification successful for {enroll_no} ({s_name})."}


@app.post("/biometric/verify-online")
async def verify_online(
    orgId: str = Form(...),
    sessionId: str = Form(...),
    faceImage: UploadFile = File(...)
):
    
    session_ref = db.collection("exam_registry").document(sessionId)
    session_doc = session_ref.get()
    
    if not session_doc.exists:
        raise HTTPException(status_code=404, detail="Session not found.")
        
    session_data = session_doc.to_dict()
    student_id = session_data.get("student_id")
    
    if not student_id:
        raise HTTPException(status_code=400, detail="Invalid session data.")
        
    
    student_doc = db.collection("students").document(f"{orgId}_{student_id}").get()
    if not student_doc.exists or not student_doc.to_dict().get("has_face_registered"):
        raise HTTPException(status_code=400, detail="Student has no registered face.")
        
    registered_encoding = student_doc.to_dict().get("face_encoding")
    
    
    image_bytes = await faceImage.read()
    encoding = get_face_encoding(image_bytes)
    
    if encoding is None:
        raise HTTPException(status_code=400, detail="No face detected.")
        
    
    if compare_faces(registered_encoding, encoding):
        
        session_ref.update({"verified": True})
        return {"status": "success", "message": "Session unlocked."}
    else:
        
        log_intruder(orgId, "Impersonation Attempt", f"Face mismatch for session {sessionId} (Student {student_id}).")
        raise HTTPException(status_code=403, detail="Face mismatch.")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
