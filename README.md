# Aplicație Meteo Android - Proiect EIM (Etapa 1)

Acest document detaliază funcționalitățile implementate conform baremului oficial pentru Etapa 1, evidențiind punctajul atins pentru fiecare cerință.

---

### 1. UI Complex cu mai multe activități și animații (1p)
*   **Implementare:** Aplicația folosește o arhitectură bazată pe o Activitate principală (`MainActivity`) care gestionează 4 ecrane distincte sub formă de Fragmente: Acasă, Prognoză, Favorite și Setări.
*   **Animații:**
    *   Tranziții personalizate de tip "Slide" (stânga/dreapta) la navigarea între meniuri, gestionate programatic în `MainActivity`.
    *   Animații fluide în interfață (`AnimatedVisibility`, `fadeIn`) folosind API-ul Jetpack Compose.

### 2. Procese Background non-main thread (1p)
*   **Implementare:** Nicio operațiune intensivă nu blochează interfața grafică (`Main Thread`), asigurând o experiență fără blocaje.
*   **Tehnic:**
    *   Toate apelurile de rețea și procesarea datelor JSON se execută pe `Dispatchers.IO`.
    *   Utilizare `Kotlin Coroutines` și `viewModelScope` în `WeatherViewModel` și `WeatherRepo`.

### 3. Networking (REST API) (1.5p)
*   **Implementare:** Aplicația comunică în timp real cu API-ul public **Open-Meteo**.
*   **Tehnic:**
    *   Se utilizează clientul HTTP **OkHttp**.
    *   Răspunsurile JSON sunt descărcate și parsate manual (`JSONObject`), demonstrând controlul complet asupra fluxului de date.

### 4. Alte funcționalități: Localizare (1p)
*   **Implementare:** Aplicația detectează automat orașul utilizatorului la pornire.
*   **Tehnic:**
    *   Utilizare `FusedLocationProviderClient` din Google Play Services.
    *   Conversia coordonatelor GPS în nume de oraș (Reverse Geocoding).

### 5. Utilizare Fragmente (0.5p)
*   **Implementare:** Întreaga structură de navigare respectă cerința explicită de a folosi sistemul de Fragmente Android (`HomeFragment`, `FavoritesFragment`, etc.).

### 6. Utilizare Jetpack Compose (Cerință Extra)
*   **Implementare:** Deși structura este pe Fragmente, desenarea efectivă a interfeței grafice este realizată 100% declarativ folosind **Jetpack Compose**, tehnologia modernă de UI de la Google.

---

### Tehnologii Utilizate
*   **Limbaj:** Kotlin
*   **Arhitectură:** MVVM (Model-View-ViewModel)
*   **UI:** Android Fragments + Jetpack Compose
*   **Networking:** OkHttp 4
*   **Stocare Locală:** Room Database (pentru Favorite)
