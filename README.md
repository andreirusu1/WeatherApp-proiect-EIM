# Aplicatie Meteo Android - Proiect EIM

### Etapa 1

**1. UI Complex cu mai multe activitati si animatii**
***Implementare:** Aplicatia foloseste o arhitectura bazata pe o Activitate principala (MainActivity) care gestioneaza 4 ecrane distincte sub forma de Fragmente: Acasa, Prognoza, Favorite si Setari.
*   **Animatii:** Tranzitii personalizate de tip "Slide" (stanga/dreapta) la navigarea intre meniuri si animatii fluide in interfata (AnimatedVisibility, fadeIn) folosind Jetpack Compose.

**2. Procese Background non-main thread**
*   **Implementare:** Nicio operatiune intensiva nu blocheaza interfata grafica (Main Thread), asigurand o experienta fara blocaje.
*   **Tehnic:** Toate apelurile de retea si procesarea datelor JSON se executa pe `Dispatchers.IO` folosind Kotlin Coroutines si `viewModelScope`.

**3. Networking (REST API)**
*   **Implementare:** Aplicatia comunica in timp real cu API-ul public Open-Meteo pentru a descarca datele meteo.
*   **Tehnic:** Se utilizeaza clientul HTTP OkHttp si parsare manuala a raspunsurilor JSON.

**4. Alte functionalitati: Localizare**
*   **Implementare:** Aplicatia detecteaza automat orasul utilizatorului la pornire pentru a afisa vremea locala.
*   **Tehnic:** Se utilizeaza `FusedLocationProviderClient` din Google Play Services.

**5. Utilizare Fragmente**
*   **Implementare:** Intreaga structura de navigare respecta cerinta de a folosi sistemul de Fragmente Android (HomeFragment, ForecastFragment, etc.).

---

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
*   **Arhitectura & UI:** Android Fragments, ViewModel, Jetpack Compose
*   **Networking & Baza de Date Cloud:** OkHttp, Firebase (Firestore, Authentication)
*   **Procese Background:** Kotlin Coroutines, WorkManager
*   **Stocare Locala:** SharedPreferences
*   **Servicii:** Google Play Services (Location)
