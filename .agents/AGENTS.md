# VisionSecure Project Guidelines

These rules were established to preserve the specific UI/UX patterns and backend integrity of the VisionSecure project. Read and adhere to them carefully before making modifications.

## 1. Backend & Logic Integrity
*   **Do not alter core logic:** The existing logic for Firebase database structuring, CSV uploading, `exam_registry`, and password hashing must remain completely untouched unless the user explicitly requests an architectural change. 
*   **Real-time Data:** When building dashboards or registries (like Live Exam Pre-check), always use real-time `SnapshotListeners` instead of one-time `.get()` fetches to ensure the UI acts like a live stack.

## 2. Navigation Architecture
*   **Admin Dashboard:** The Admin portal uses a `DrawerLayout` (Sidebar) architecture for all major navigation (Uploads, Databases, Analytics). 
*   **Bottom Navigation Restriction:** A Bottom Navigation Bar is ONLY permitted on the main top-level dashboard (e.g., `AdminDashboardActivity`). **Do NOT** include bottom navigation bars inside sub-screens (like Analytics, Live Exam, or Student Database).
*   **Deep Linking:** Use Intent extras (e.g., `SELECTED_TAB`) to allow the Sidebar to deep-link directly into specific tabs of sub-screens.

## 3. UI/UX & Theming
*   **Brand Consistency:** Use the custom `@drawable/visionsecure_logo` in top app bars instead of generic lock/secure icons.
*   **Dark/Light Mode Compatibility:** Never hardcode solid colors that break contrast in Dark Mode (e.g., do not use light pink backgrounds with white text). If highlighting errors or warnings on cards, prefer using thick colored strokes (e.g., `setStrokeWidth` and `setStrokeColor`) over altering the base background color, ensuring text remains readable in all themes.
*   **Crash Prevention:** Always wrap asynchronous database callbacks in `try-catch` blocks and perform null/bounds checks when manipulating views that might be destroyed or delayed (like `NavigationView` headers). Use `ContextCompat.getColor()` for safe backward compatibility.
