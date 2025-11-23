# Aplicatie Meteo Android - Proiect EIM (Etapa 1)

---

### 1. UI Complex cu mai multe activitati si animatii
*   **Implementare:** Aplicatia foloseste o arhitectura bazata pe o Activitate principala (`MainActivity`) care gestioneaza 4 ecrane distincte sub forma de Fragmente: Acasa, Prognoza, Favorite si Setari.
*   **Animatii:**
    *   Tranzitii personalizate de tip "Slide" (stanga/dreapta) la navigarea intre meniuri, gestionate programatic in `MainActivity`.
    *   Animatii fluide in interfata (`AnimatedVisibility`, `fadeIn`) folosind API-ul Jetpack Compose.

### 2. Procese Background non-main thread 
*   **Implementare:** Nicio operatiune intensiva nu blocheaza interfata grafica (`Main Thread`), asigurand o experienta fara blocaje.
*   **Tehnic:**
    *   Toate apelurile de retea si procesarea datelor JSON se executa pe `Dispatchers.IO`.
    *   Utilizare `Kotlin Coroutines` si `viewModelScope` in `WeatherViewModel` si `WeatherRepo`.

### 3. Networking (REST API)
*   **Implementare:** Aplicatia comunica in timp real cu API-ul public **Open-Meteo**.
*   **Tehnic:**
    *   Se utilizeaza clientul HTTP **OkHttp**.
    *   Raspunsurile JSON sunt descarcate si parsate manual (`JSONObject`), demonstrand controlul complet asupra fluxului de date.

### 4. Alte functionalitati: Localizare (1p)
*   **Implementare:** Aplicatia detecteaza automat orasul utilizatorului la pornire.
*   **Tehnic:**
    *   Utilizare `FusedLocationProviderClient` din Google Play Services.
    *   Conversia coordonatelor GPS in nume de oras (Reverse Geocoding).

### 5. Utilizare Fragmente (0.5p)
*   **Implementare:** Intreaga structura de navigare respecta cerinta explicita de a folosi sistemul de Fragmente Android (`HomeFragment`, `FavoritesFragment`, etc.).

---

### Tehnologii Utilizate
*   **Limbaj:** Kotlin
*   **UI:** Android Fragments + Jetpack Compose
*   **Networking:** OkHttp
*   **Stocare Locala:** Room Database (pentru Favorite)
