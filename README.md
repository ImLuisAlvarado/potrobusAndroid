# Potrobus - Sistema de Rastreo de Autobuses (Android)

Potrobus es una aplicación móvil diseñada para que los estudiantes de ITSON puedan monitorear en tiempo real la ubicación de los autobuses universitarios, visualizar paradas y optimizar sus tiempos de traslado. La aplicación utiliza tecnologías modernas de código abierto para ofrecer una experiencia fluida sin dependencias de servicios de mapas de pago.

## Características Principales

- **Seguimiento en Tiempo Real:** Visualización dinámica del movimiento del autobús sobre el mapa mediante **Socket.IO**.
- **Visualización de Rutas:** Mapa interactivo que muestra todas las paradas de la ruta y traza el trayecto dinámicamente.
- **Mapas de Código Abierto:** Integración con **MapLibre SDK** y estilos de **OpenFreeMap**, eliminando la dependencia de Google Maps API.
- **Autenticación Segura:** Sistema de Login y Registro basado en **JWT** (JSON Web Tokens).
- **Notificaciones Dinámicas:** Alertas visuales sobre el estado del servicio (llegadas, salidas, pérdida de señal GPS).
- **Lista de Paradas Inteligente:** Un `RecyclerView` que se sincroniza con la ubicación del bus para resaltar el progreso del viaje.
- **Watchdog de Conexión:** Monitoreo constante de la señal GPS para informar al usuario si los datos están desactualizados.

## Stack Tecnológico

- **Lenguaje:** Kotlin
- **Mapas:** [MapLibre Native SDK](https://maplibre.org/)
- **Comunicación en Tiempo Real:** [Socket.IO Client Java](https://github.com/socketio/socket.io-client-java)
- **Networking:** [Retrofit 2](https://square.github.io/retrofit/) & OkHttp
- **Localización:** Fused Location Provider (Google Play Services)
- **Estilo de Mapas:** OpenFreeMap (Liberty style)

## Configuración y Ejecución

### Requisitos
- Android Studio Flamingo | 2022.2.1 o superior.
- Dispositivo Android con API 24 (Android 7.0) o superior.
- Conexión a internet.

### Instalación
1. Clona el repositorio:
   ```bash
   git clone https://github.com/tu-usuario/potrobus-android.git
   ```
2. Abre el proyecto en Android Studio.
3. 1 Configura la IP del servidor en `app/src/main/java/mx/itson/potrobus/utils/Constants.kt`:
   ```kotlin
   object Constants {
       const val BASE_URL = "http://xxx.xxx.x.xxx:5500/" // Cambia por la IP de tu backend
   }
   ```
3. 2 Posiblemente sea necesario configurar **[network_exceptions]** para permitir la conexión desde el dispositivo.

4. Sincroniza el proyecto con Gradle y ejecuta la aplicación.

## Estructura del Proyecto

- `mx.itson.potrobus.activities`: Actividades principales como `MapViewActivity`, `LoginActivity` y `BusSelectionActivity`.
- `mx.itson.potrobus.entities`: Modelos de datos y lógica de negocio (Unidad, Parada, Ruta).
- `mx.itson.potrobus.adapters`: Adaptadores para la visualización de listas (Buses y Paradas).
- `mx.itson.potrobus.utils`: Clases de soporte para Retrofit, constantes y manejo de red.
- `mx.itson.potrobus.interfaces`: Definición de endpoints para la comunicación con la API REST.

## Seguridad
La comunicación con el servidor está protegida mediante JWT. El token se almacena de forma segura en `SharedPreferences` y se adjunta a las peticiones HTTP y a la conexión inicial del WebSocket.

---
*Desarrollado para la comunidad tecnológica de ITSON.*
