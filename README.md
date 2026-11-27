# MueveT

> Documentación oficial del proyecto para el hackatón universitario.

- **Equipo:** Equipo 1
- **Campus:** Toluca
- **Versión / Estado del proyecto:** Final
---

## 1. Resumen ejecutivo

**MueveT** es una aplicación desarrollada por el **Equipo 1** (Campus Toluca) que muestra un mapa de rutas de transporte público —como camiones, combis, microbuses y taxis— para ciudadanos en ciudades sin mapas ni horarios oficiales. Facilita la movilidad urbana al ofrecer precios por ruta, frecuencias, tiempos estimados de llegada y optimización de transbordos. Es innovadora porque combina inteligencia artificial y participación de los usuarios para mapear el transporte concesionado, e incorpora un "termómetro de ocupación" que muestra qué tan llena va cada unidad.

---

## 2. Boceto y concepto

### El problema
En muchas ciudades, el transporte público concesionado (combis, microbuses, camiones y taxis) carece de mapas oficiales de rutas, paradas definidas y horarios confiables. Esta falta de información dificulta la movilidad urbana cotidiana y desincentiva el uso del transporte público frente al automóvil particular.

### Propuesta de solución
**MueveT** centraliza en un mapa accesible la información del transporte público local. Al combinar inteligencia artificial con los aportes de la comunidad de usuarios, la app construye y actualiza las rutas, muestra el costo de cada trayecto, permite conocer qué tan llenas vienen las unidades, indica frecuencias y tiempos de traslado, y optimiza viajes que requieren transbordos.

- **Bocetos / Wireframes de la interfaz:** [https://grid-savor-30246568.figma.site/]

### Alineación con los ODS
- **ODS 11 — Ciudades y comunidades sostenibles (Meta 11.2):** El proyecto atiende directamente la Meta 11.2, orientada a garantizar sistemas de transporte seguros, accesibles y sostenibles para todos, al reducir las barreras de información que impiden aprovechar el transporte público frente al automóvil.
- **ODS 10 — Reducción de las desigualdades:** Al incluir un listado de precios por ruta y facilitar el acceso a información sobre el transporte concesionado, la app apoya la planeación económica de los traslados. 

---

## 3. Documentación funcional

A continuación se describen las funciones principales de **MueveT**:

| # | Función | Descripción |
|---|---|---|
| 1 | **Mapeo colaborativo de rutas con IA** | Genera y mantiene actualizado el mapa de rutas de transporte público combinando inteligencia artificial con los aportes de los usuarios de la aplicación. |
| 2 | **Listado de precios por ruta** | Muestra el costo de cada ruta disponible para que el usuario conozca de antemano el impacto económico que tendrá el trayecto en su bolsillo. |
| 3 | **Termómetro de ocupación** | Muestra un indicador visual de qué tan lleno se encuentra el transporte público en la ruta consultada. |
| 4 | **Frecuencia y tiempo estimado de llegada** | Indica cada cuánto pasa el transporte dentro de la ruta del usuario y calcula el tiempo estimado para llegar al destino. |
| 5 | **Optimización de rutas y transbordos** | Calcula la mejor combinación de trayectos y tiempos cuando el usuario necesita cambiar de un transporte a otro para llegar a su destino. |

---

## 4. Documentación técnica

### Arquitectura general
**MueveT** sigue una arquitectura **MVVM (Model–View–ViewModel)** con un esquema híbrido cliente–nube (**offline-first**), donde la interfaz declarativa en **Jetpack Compose** observa un estado único inmutable (`MueveTUiState`).


### Stack tecnológico

| Capa | Tecnologías |
|---|---|
| **Frontend / App móvil** | Android nativo en **Kotlin** con **Jetpack Compose**, **Material Design 3**, **Kotlin Coroutines/Flow** y **Java 11**. |
| **Backend / Autenticación** | **Firebase Authentication** (Google Sign-In mediante **Jetpack CredentialManager** y **Google Identity `googleid`**). |
| **Base de datos local** | **Room** con **KSP** sobre SQLite (tablas `transit_routes` y `route_reports`). |
| **Base de datos en la nube** | **Cloud Firestore** (Firebase BoM `35.1.0`) con colecciones aisladas por usuario (`/users/{userId}`, `saved_routes`, `reports`). |
| **Servicios de mapas** | **OpenStreetMap** (`osmdroid 6.1.20`) como mapa global interactivo en tiempo real por defecto, con soporte opcional de **Google Maps Compose**. |
| **Inteligencia artificial** | **Gemini** integrado mediante **Firebase AI Logic SDK** (`com.google.firebase:firebase-ai`) con Firebase BoM `35.1.0`, backend `GenerativeBackend.googleAI()` y **Google Maps Grounding** (`Tool.googleMaps()`). |

### Integración de Gemini en el flujo de la app

#### Punto de integración
Gemini se invoca desde la cuarta pestaña de navegación, **Asistente** (`AssistantChatScreen` / `AppTab.ASISTENTE`). En la parte superior de la pantalla, el usuario selecciona el modo mediante chips que llaman a `MueveTViewModel.selectAiModel(tier)`:
- **Estándar** (`gemini-3.8-flash`, activo por defecto)
- **Rápido** (`gemini-3.5-flash-lite`)
- **Profundo** (`gemini-4-argon`)

La llamada a la API se dispara al tocar una tarjeta de **Sugerencias rápidas** o al enviar una consulta desde el campo de texto (`chat_send_button`), ejecutando el flujo `MueveTViewModel.sendChatMessage(prompt)` → `TransitAiAssistant.sendMessageStream(...)`.

#### Entradas y salidas
- **Entradas enviadas a Gemini:**
  - Instrucción de sistema en español enfocada en movilidad del Valle de Toluca (camiones, combis, taxis colectivos y Tren Interurbano El Insurgente).
  - Resumen textual de todas las rutas vigentes del catálogo (`routesSummary`: código, corredor, tipo de transporte, duración en minutos, tarifa en MXN y nivel de saturación, incluyendo rutas recién reportadas por usuarios).
  - Los últimos 12 mensajes del historial de conversación.
  - La pregunta en lenguaje natural del usuario.
  - Configuración de `Tool.googleMaps()` mediante `ToolConfig(retrievalConfig)` con las coordenadas GPS actuales del dispositivo (`userLat`, `userLng`, o respaldo en Toluca `19.2826, -99.6557`) e idioma `es-MX`.
- **Formato de respuesta consumido por el ViewModel:**
  - Flujo reactivo en streaming (`Flow<StreamChatEvent>`, con límite de 60 s) que emite `StreamChatEvent.PartialText(text)` para renderizar el texto progresivamente y `StreamChatEvent.Completed(fullText, mapsSources)` al finalizar.
  - `mapsSources` contiene una lista de objetos `MapsGroundingSource(title, uri, placeId)` extraídos de `groundingMetadata.groundingChunks`.
  - `MueveTViewModel` actualiza la lista `chatMessages` en `MueveTUiState` y la interfaz despliega los enlaces en la tarjeta atribuida **"Google Maps"** para abrir cada parada o punto directamente en la app de mapas.

#### Propósito específico
Apoyar el mapeo colaborativo de rutas de transporte público con los usuarios, integrando los datos actualizados por la comunidad (tarifas, saturación y rutas nuevas reportadas) con ubicaciones reales verificadas mediante **Google Maps Grounding**. Además, interviene de forma conversacional en la orientación de combinaciones de rutas, transbordos, comparación de tarifas y ubicación de paradas cercanas (por ejemplo, sugerir qué camión o combinación tomar entre zonas como Centro, Ciudad Universitaria, Paseo Tollocan, Terminal Toluca o Metepec). Por su parte, el filtrado estructurado en la pantalla **Explora** (*Más rápida*, *Más barata*, *Menos llena*) se resuelve de manera local y determinista en `MueveTViewModel`.

### Configuración e instalación para desarrolladores

#### Requisitos previos
- **Android Studio:** Versión actualizada compatible con Android Gradle Plugin (AGP) y Gradle (por ejemplo, Android Studio Panda / Meerkat o superior con soporte para AGP 9.x).
- **JDK:** Java 11 o superior (el código compila con `JavaVersion.VERSION_11` como compatibilidad de origen y destino).
- **Android SDK:**
  - `compileSdk`: 36 (`minorApiLevel = 1`)
  - `targetSdk`: 36
  - `minSdk`: 24 (Android 7.0 Nougat)
- **Dispositivo o emulador:** Teléfono Android físico o emulador con **Google Play Services** instalados (necesarios para Google Sign-In con Credential Manager y ubicación GPS con `FusedLocationProviderClient`).
- **Node.js (opcional):** Solo si se desean ejecutar localmente las pruebas de reglas de seguridad de Firestore (`firestore.test.js`) con el emulador.

#### Configuración de Firebase (`google-services.json` y `firebase_applet_config.xml`)
1. Crea o vincula un proyecto en **Firebase Console** registrado con el `applicationId`: `com.aistudio.muevet.xkrpzm`.
2. Activa los servicios requeridos por la aplicación:
   - **Firebase Authentication:** Habilita el proveedor **Google** y registra la huella SHA-1 del keystore (`debug.keystore` en desarrollo o tu keystore de release) para generar `default_web_client_id`.
   - **Cloud Firestore:** Crea la base de datos correspondiente al identificador `firestore_database_id` definido en `app/src/main/res/values/firebase_applet_config.xml` y despliega las reglas de `firestore.rules`.
   - **Firebase AI Logic:** Habilita el backend de Gemini Developer API (`GenerativeBackend.googleAI()`).
   - **Firebase App Check:** Configura el proveedor ReCAPTCHA (usando `recaptcha_site_key` en `firebase_applet_config.xml`) para compilaciones release y registra el token de depuración para entornos debug.
3. Descarga el archivo `google-services.json` desde Firebase Console y colócalo en `app/google-services.json`. *(Gradle incluye tolerancia en compilación si falta el archivo;
---

## 5. Manual de usuario

Esta guía está pensada para cualquier persona que use **MueveT** por primera vez.

### Cómo instalar y acceder a la app
1. **Descarga o acceso:** [https://drive.google.com/file/d/19JQFVoqMjrHcFWr9NYG93F02VXA1wITG/view?usp=sharing].
2. **Inicio de sesión:** Accede utilizando tu cuenta de Google.
3. **Permisos necesarios:** Activa el permiso de ubicación para obtener recomendaciones y paradas cercanas a tu posición actual (si no está disponible, el asistente usa Toluca como referencia).

### Cómo usar MueveT paso a paso
1. **Explora el mapa y filtra rutas:** Al abrir la aplicación, verás el mapa interactivo con las rutas de transporte público disponibles (camiones, combis, microbuses y taxis). En la sección **Explora**, puedes ordenar y filtrar opciones según tu prioridad: **Más rápida**, **Más barata** o **Menos llena**.
2. **Revisa el precio de tu trayecto:** Consulta el listado de precios por ruta para saber cuánto te costará el viaje antes de salir y cuidar tu presupuesto.
3. **Consulta frecuencias y tiempos:** Selecciona la ruta que necesitas para ver cada cuánto pasa el transporte y cuánto tiempo aproximado te tomará llegar a tu destino.
4. **Verifica el termómetro de ocupación:** Antes de subir, observa el indicador del "termómetro de ocupación" para saber qué tan lleno viene el transporte.
5. **Pregunta al Asistente para transbordos y paradas:** Entra a la pestaña **Asistente** para consultar en lenguaje natural qué camión o combinación tomar entre zonas (por ejemplo, Centro, Ciudad Universitaria, Paseo Tollocan, Terminal Toluca o Metepec). Puedes elegir entre los modos **Estándar**, **Rápido** o **Profundo**, tocar una de las **Sugerencias rápidas** o escribir tu pregunta. Cuando el asistente te muestre ubicaciones en la tarjeta de **Google Maps**, tócalas para abrir la parada directamente en tu app de mapas.
6. **Ayuda a mapear rutas:** Si conoces una ruta que falta o ha cambiado, puedes aportar información desde la app para que se integre al catálogo y el mapa se mantenga útil para todos.


---

## 6. Accesibilidad e impacto:

### Inclusión y accesibilidad
- **Acceso equitativo a la información:** MueveT está diseñada para ciudadanos que utilizan combis, microbuses, camiones y taxis en ciudades donde no existen mapas oficiales de rutas, paradas ni horarios, acercando herramientas de movilidad a quienes más dependen del transporte público.
- **Transparencia en el costo del transporte:** Al mostrar un listado de precios por ruta y permitir filtrar por la opción más barata, facilita que los usuarios planeen sus gastos diarios de movilidad con claridad.
- **Enfoque offline-first:** Al contar con almacenamiento local en el dispositivo, la arquitectura busca facilitar la disponibilidad de la información de rutas incluso con conectividad limitada. 

### Impacto esperado
- **Mejor experiencia de movilidad urbana:** Reducir la incertidumbre diaria sobre rutas, costos, tiempos de espera, transbordos y nivel de ocupación de las unidades.
- **Impulso al transporte sostenible:** Hacer más práctico y predecible el uso del transporte público para incentivar que más personas lo elijan frente al automóvil particular.
- **Contribución a la Meta 11.2 del ODS 11:** Apoyar el avance hacia sistemas de transporte más seguros, accesibles y sostenibles en entornos urbanos.
