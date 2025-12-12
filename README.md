# PorVida

Aplicación móvil Android para la gestión de planes de entrenamiento y servicios, con panel de cliente, login/registro seguro, cámara para leer QR, módulo del clima y consumo de APIs externas de IA.

## Integrantes

- Nombre Integrante 1 (Rol)
- Nombre Integrante 2 (Rol)
- Nombre Integrante 3 (Rol)

> Sustituye estos nombres por los nombres reales de tu equipo.

## Funcionalidades principales

- **Autenticación de usuarios**: registro y login con almacenamiento seguro de contraseñas usando BCrypt y base de datos local Room.
- **Gestión de planes**: visualización del plan contratado, fecha de vigencia y comentario del plan (`planComment`) en el dashboard del cliente.
- **Pagos**: flujo de pago con Google Pay / Play Services Wallet para activar planes.
- **Dashboard del cliente**: pantalla principal con resumen de servicios, pedidos y acceso rápido a módulos.
- **Lector de QR**: integración con CameraX y ML Kit para escanear códigos QR.
- **Módulo de clima**: consulta del pronóstico del tiempo usando una API externa e interfaz en Jetpack Compose.
- **Módulo de posts**: listado de publicaciones de ejemplo consumiendo una API REST pública.
- **Integración con modelos de IA**: consumo de las APIs de OpenAI y Gemini desde la app (por ejemplo, para respuestas inteligentes o asistentes).

## Endpoints usados

### Endpoints externos

- **Posts (JSONPlaceholder)**  
  - Base URL: `https://jsonplaceholder.typicode.com/`  
  - Uso: obtener posts de ejemplo para la pantalla de publicaciones.

- **Clima (WeatherAPI)**  
  - Base URL: `https://api.weatherapi.com/v1/`  
  - Uso: obtener pronóstico del clima y datos de calidad del aire.  
  - Autenticación: API key en `WEATHER_API_KEY` (inyectada vía `local.properties` / `gradle.properties`).

- **OpenAI**  
  - Base URL: `https://api.openai.com/v1`  
  - Uso: consumo de modelos de lenguaje para funcionalidades de IA.  
  - Autenticación: header `Authorization: Bearer <OPENAI_API_KEY>`.

- **Gemini (Google Generative Language API)**  
  - Base URL: `https://generativelanguage.googleapis.com/v1beta`  
  - Uso: generación de contenido y respuestas con modelos Gemini.  
  - Autenticación: API key en `GEMINI_API_KEY`.

### Endpoints propios (microservicios)

> Si tuvieras microservicios propios (por ejemplo, para usuarios, pagos o reportes) describe aquí sus URLs base y ejemplos de endpoints. Si el backend está en otro repositorio, enlázalo en esta sección.

- **Ejemplo (a completar)**  
  - Base URL: `https://tu-dominio.com/api/`  
  - `POST /auth/login` – login de usuarios.  
  - `POST /auth/register` – registro de usuarios.  
  - `GET /plans` – obtener planes disponibles.

## Código fuente

- **App móvil (Android)**: carpeta `app/` de este repositorio (código en Kotlin, Jetpack Compose, Room, Retrofit, CameraX, etc.).
- **Microservicios**:  
  - Si los microservicios están en este mismo repo, agrega aquí la ruta (por ejemplo `backend/`).  
  - Si están en otro repositorio, agrega el enlace, por ejemplo: `https://github.com/organizacion/porvida-backend`.

## APK firmado y ubicación del archivo .jks

- **APK release firmado**:  
  - Ruta local (proyecto Android Studio):  
    `app/build/outputs/apk/release/app-release.apk`
  - En GitHub, puedes subir el APK como _Release asset_ o a una carpeta, por ejemplo `release/app-release.apk`.

- **Keystore (.jks)**:  
  - Ruta local configurada en `gradle.properties`:  
    `C:\\Users\\AronXD\\Desktop\\porvidaAndroidStudio\\keys\\porvida-release-keystore.jks`  
  - **Nota**: por seguridad, este archivo **no debe subirse al repositorio público**. Solo se documenta su ubicación local.

## Instrucciones para ejecutar el proyecto

### Requisitos previos

- Android Studio (Giraffe o superior) con SDK Android 26+ instalado.
- JDK 11 configurado (Android Studio ya lo incluye normalmente).
- Dispositivo físico o emulador Android con API 26 o superior.

### Configuración de claves y propiedades

1. Crear archivo `local.properties` en la raíz del proyecto (no se versiona) con contenido similar:
   ```properties
   sdk.dir=C:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
   OPENAI_API_KEY=tu-openai-api-key
   GEMINI_API_KEY=tu-gemini-api-key
   WEATHER_API_KEY=tu-weather-api-key
   ```

2. Verificar que en `gradle.properties` existan las propiedades para el firmado release:
   ```properties
   RELEASE_STORE_FILE=C:\\Users\\AronXD\\Desktop\\porvidaAndroidStudio\\keys\\porvida-release-keystore.jks
   RELEASE_STORE_PASSWORD=aronn123
   RELEASE_KEY_ALIAS=porvida_key
   RELEASE_KEY_PASSWORD=aronn123
   ```

### Compilar y ejecutar (debug)

1. Abrir el proyecto en Android Studio.  
2. Sincronizar Gradle si lo pide.  
3. Seleccionar la configuración `app` y un dispositivo/emulador.  
4. Pulsar **Run** (botón ▶) para instalar y ejecutar la app en modo debug.


## Evidencia de trabajo colaborativo

- La evidencia de trabajo colaborativo se puede revisar directamente en GitHub en la sección de **Commits** del repositorio, donde se ven los commits realizados por cada integrante.  
- Para reforzar esta evidencia, cada integrante debe realizar commits con su usuario de GitHub (o configurar su nombre/correo correctamente en Git) y, si lo desean, pueden añadir una sección en este README con enlaces a los perfiles de GitHub de cada uno.

> Actualiza este README con los nombres reales de los integrantes, enlaces a microservicios (si aplican) y cualquier endpoint adicional que hayan implementado.
