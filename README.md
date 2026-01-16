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
    *   Raspunsurile JSON sunt descarcate si parsate manual (`JSONObject`),  controlul fiind complet asupra fluxului de date.

### 4. Alte functionalitati: Localizare 
*   **Implementare:** Aplicatia detecteaza automat orasul utilizatorului la pornire.
*   **Tehnic:**
    *   Utilizare `FusedLocationProviderClient` din Google Play Services.

### 5. Utilizare Fragmente 
*   **Implementare:** Intreaga structura de navigare respecta cerinta explicita de a folosi sistemul de Fragmente Android (`HomeFragment`, `FavoritesFragment`, etc.).

### Etapa 2

**1. Functionalitati de Networking Complexe (Firebase)**
*   **Implementare:** Aplicatia suporta conturi de utilizatori. Orasele favorite sunt salvate in cloud si sunt specifice fiecarui cont.
*   **Tehnic:** S-a utilizat Firebase Authentication (login/register) si Firebase Firestore (baza de date pentru favorite).

**2. Extinderea Functionalitatii (Stocare, Serializare)**
*   **Implementare:** Utilizatorii pot alege unitatile de masura (C/F, kmh/mph). Aplicatia retine ultimul oras cautat.
*   **Tehnic:** S-au folosit SharedPreferences pentru a salva local preferintele utilizatorului.

**3. Broadcast Receivers**
*   **Implementare:** Aplicatia reactioneaza la schimbarile modului avion pentru a gestiona starea conectivitatii.
*   **Tehnic:** S-a implementat un `BroadcastReceiver` care asculta evenimentul de sistem `ACTION_AIRPLANE_MODE_CHANGED`.

**4. Notificari Push**
*   **Implementare:** Aplicatia trimite notificari periodice cu prognoza meteo pentru locatia relevanta.
*   **Tehnic:** S-a folosit `WorkManager` pentru a rula un task in background ce descarca datele si afiseaza o notificare.

**5. Teste Unitare Serioase**
*   **Implementare:** S-a validat componenta de networking (`WeatherRepo`) pentru a asigura corectitudinea datelor primite.
*   **Tehnic:** S-au folosit `MockWebServer` pentru a sim



### Tehnologii Utilizate
*   **Limbaj:** Kotlin
*   **UI:** Android Fragments + Jetpack Compose
*   **Networking:** OkHttp
*   **Stocare Locala:** Room Database (pentru Favorite)
