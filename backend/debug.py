import traceback

try:
    import face_recognition_models
    print("Models loaded successfully!")
    print(face_recognition_models.__file__)
except Exception as e:
    print("Failed to load models!")
    traceback.print_exc()

try:
    import face_recognition
    print("face_recognition loaded successfully!")
except Exception as e:
    print("Failed to load face_recognition!")
    traceback.print_exc()
