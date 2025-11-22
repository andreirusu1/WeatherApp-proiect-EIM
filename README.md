# WeatherApp-proiect-EIM
Android Kotlin app

## Descriere Generala
Aceasta aplicatie este un instrument modern de prognoza meteo dezvoltat pentru platforma Android, utilizand limbajul **Kotlin**. Proiectul isi propune sa ofere utilizatorilor informatii meteorologice detaliate (temperatura, umiditate, vant, prognoza orara si zilnica) intr-o interfata intuitiva si fluida.



## Functionalitati Implementate & Cerinte Etapa 1

Aplicatia a fost dezvoltata avand in vedere respectarea stricta a cerintelor pentru prima etapa a proiectului, acoperind urmatoarele aspecte tehnice:

1.UI Complex cu mai multe activitati si animatii
-Arhitectura: Aplicatia nu este un simplu ecran. Foloseste o structura robusta bazata pe Fragmente (HomeFragment, ForecastFragment, FavoritesFragment, SettingsFragment) gestionate de o Activitate gazda (MainActivity).
-Animatii: Tranzitiile intre ecrane sunt realizate prin animatii personalizate de tip Slide (stanga/dreapta), implementate programatic in MainActivity. In interiorul ecranelor, folosim API-ul de animatii din Compose (AnimatedVisibility, fadeIn) pentru o experienta fluida.

2.Procese Background non-main thread
-Implementare: Nicio operatiune grea nu blocheaza interfata (Main Thread).
-Tehnic: Toate apelurile de retea din WeatherRepo.kt si procesarea datelor JSON sunt incapsulate in blocuri withContext(Dispatchers.IO). ViewModel-ul lanseaza aceste operatiuni folosind viewModelScope, respectand ciclul de viata al aplicatiei.

3.Networking (REST API)
-Sursa date: Aplicatia nu foloseste date "hardcodate".
Implementare: Se conecteaza la API-ul public Open-Meteo folosind clientul HTTP OkHttp.
Procesare: Raspunsurile JSON sunt descarcate si parsate manual (JSONObject: Request -> Response -> Parsing -> Model -> UI).

4.Alte functionalitati: Localizare
-GPS: Aplicatia interactioneaza cu senzorii telefonului.
Implementare: Foloseste FusedLocationProviderClient pentru a obtine coordonatele exacte ale utilizatorului, pe care le transforma apoi automat in prognoza meteo locala.

5.Utilizare Fragmente
-Structura: Intreaga navigatie este construita pe sistemul de Fragmente al Android-ului.

## Tehnologii si Biblioteci Utilizate
*   **Limbaj:** Kotlin
*   **UI:** Android Fragments, Jetpack Compose, Material Design 3
*   **Networking:** OkHttp 4
*   **Async:** Kotlin Coroutines, ViewModel, StateFlow
*   **Localizare:** Google Play Services Location
*   **Baza de date:** Room Database
*   **JSON Parsing:** Org.JSON
