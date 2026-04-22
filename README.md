# Civic Connect - The Android App

Citizen-facing Android application for the Civic Issue Reporting System. Citizens use this app to photograph local infrastructure problems (potholes, broken streetlights, water leaks, etc.), attach their GPS location, and submit reports to a central database.
It can be used to see the Updates from the Admins reflected in real Time

> **Repo Info:** This repository contains only the Android app.  
> The admin dashboard (Spring Boot + Vaadin) is maintained in a separate repo.  
> Both apps share a common **Supabase** backend.


## Idea behind the Project - 

Urban infrastructure problems — potholes, broken streetlights, burst pipes, garbage overflow — often go unreported simply because citizens have no easy way to tell the right people. The Civic Issue Reporting System solves this by putting the reporting tool directly in a citizen's pocket.

**The flow is simple:**
 
1. A citizen spots a problem in their area.
2. They open the app, take a photo, and the GPS location is captured automatically.
3. They add a title, description, and pick a category (Road, Water, Electricity, etc.).
4. They hit Submit — the report lands in the municipality admin's dashboard instantly.
5. As administrators investigate and resolve the issue, the citizen sees the status update in their submissions list: `REPORTED` → `IN_PROGRESS` → `RESOLVED`.

| Usage | Library / Tool |
|---|---|
| Language | Java 17 |
| IDE | Android Studio |
| Networking | Retrofit 2 + OkHttp + Gson |
| Camera | CameraX |
| Image loading | Glide |
| Map display | OSMDroid |
| Location | FusedLocationProviderClient (Play Services) |
| Auth | Supabase Auth (email/password → JWT) |
| UI | Material Components, RecyclerView, SwipeRefreshLayout |
| Architecture | ViewModel + LiveData (Lifecycle-aware) |
| Build | Gradle 8 |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 |

