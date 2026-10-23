import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import datetime
import hashlib

def hash_password(password: str) -> str:
    return hashlib.sha256(password.encode('utf-8')).hexdigest()

# 1. Initialize Firebase Admin SDK
cred = credentials.Certificate("app/firebase-service-account.json")
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)

db = firestore.client()

print("Connected to Firestore. Initializing Separate College & Hostel Organizations...")

# 2. Create 'organizations' Collection (Separate College & Hostel)
db.collection('organizations').document('org_college_main').set({
    'name': 'Vision Tech Engineering College',
    'type': 'College',
    'verification_rules': {
        'wifi_ssid': 'VisionTech_Academic_WiFi',
        'gps_lat': 28.7041,
        'gps_long': 77.1025,
        'geofence_radius_meters': 200
    }
})

db.collection('organizations').document('org_hostel_block_a').set({
    'name': 'Vision Tech Boys Hostel - Block A',
    'type': 'Hostel',
    'verification_rules': {
        'wifi_ssid': 'VisionTech_Hostel_WiFi',
        'gps_lat': 28.7050,
        'gps_long': 77.1035,
        'geofence_radius_meters': 80
    },
    'curfew_time': '21:30'
})
print("- Created separate 'College' and 'Hostel' Organizations")

# 3. Create 'users' Collection with Role-Based Access per Organization & Hashed Passwords
# Default password for testing is 'pass123'
default_password_hash = hash_password("pass123")

users = [
    # College Staff & Students
    {'id': 'college_admin', 'role': 'admin', 'name': 'College Dean', 'org_id': 'org_college_main', 'password': 'pass123'},
    {'id': 'invigilator_1', 'role': 'invigilator', 'name': 'Prof. Alan Smith', 'org_id': 'org_college_main', 'password': 'pass123'},
    {'id': 'student_college_1', 'role': 'student', 'name': 'Rahul Sharma (Roll #101)', 'org_id': 'org_college_main', 'password': 'pass123'},
    
    # Hostel Staff & Students
    {'id': 'hostel_warden', 'role': 'incharge', 'name': 'Warden Mr. Kumar', 'org_id': 'org_hostel_block_a', 'password': 'pass123'},
    {'id': 'hostel_guard', 'role': 'security', 'name': 'Guard Vikram', 'org_id': 'org_hostel_block_a', 'password': 'pass123'},
    {'id': 'student_hostel_1', 'role': 'student', 'name': 'Rahul Sharma (Room #204)', 'org_id': 'org_hostel_block_a', 'password': 'pass123'}
]

for user in users:
    db.collection('users').document(user['id']).set({
        'name': user['name'],
        'role': user['role'],
        'org_id': user['org_id'],
        'password_hash': hash_password(user['password']),
        'face_encoding': [],
        'is_first_login': True
    })
print("- Created Users with SHA-256 Hashed Passwords (default password: 'pass123')")

# 4. Create 'sessions' (College Exams vs. Hostel Curfew Checks)
# College Exam Session
db.collection('sessions').document('exam_cs101_midterm').set({
    'org_id': 'org_college_main',
    'type': 'college_exam',
    'name': 'CS101 Algorithms Mid-Term',
    'start_time': datetime.datetime.now(),
    'end_time': datetime.datetime.now() + datetime.timedelta(hours=2),
    'room_number': 'Lab 302',
    'invigilator_id': 'invigilator_1'
})

# Hostel Curfew Check Session
db.collection('sessions').document('hostel_curfew_check_today').set({
    'org_id': 'org_hostel_block_a',
    'type': 'hostel_night_curfew',
    'name': 'Night Attendance Check',
    'start_time': datetime.datetime.now(),
    'end_time': datetime.datetime.now() + datetime.timedelta(hours=1),
    'incharge_id': 'hostel_warden'
})
print("- Created separate Sessions for College Exam Proctoring and Hostel Night Curfew")

print("\nSeparate College and Hostel setup completed successfully!")
