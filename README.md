# 💊 MedPills - Gestor Inteligente de Medicación (100% Offline)

![MedPills Banner](https://img.shields.io/badge/Android-13%2B-green?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Offline First](https://img.shields.io/badge/Privacy-100%25%20Offline-blueviolet?style=for-the-badge)

**MedPills** es una aplicación nativa para Android diseñada para gestionar de forma segura, privada e ininterrumpida la toma diaria de medicamentos. Con un enfoque **100% offline y de respeto absoluto a la privacidad**, MedPills almacena toda la información de forma local y proporciona herramientas avanzadas de recordatorios críticos, control de stock y seguridad contra sobredosis.

---

## ✨ Características Principales

### 🎨 1. Interfaz Inmersiva Premium (Material 4, Glassmorphism & SDK 37)
- **Diseño Borde a Borde Nativo (Edge-to-Edge)**: Cumpliendo con los estándares modernos de Android 15 a 17 (API 35-37), la aplicación se dibuja de borde a borde por debajo de las barras transparentes de estado y de navegación del sistema, gestionado mediante controladores de insets de ventana (`WindowInsetsCompat`) para un acabado inmersivo sin solapamientos.
- **Soporte para Gestos de Atrás Predictivos (Predictive Back)**: Declarado el soporte a nivel de sistema operativo (`enableOnBackInvokedCallback`) permitiendo transiciones suaves y predictivas al retroceder entre pantallas.
- **Selector de Fotos Privado (Photo Picker)**: Integración nativa del moderno selector de fotos seguro de Android (`PickVisualMedia` de la API 33-37) para cargar fotos de perfil personalizadas, lo que garantiza el respeto estricto a la privacidad del usuario sin necesidad de solicitar permisos globales de almacenamiento.
- **Efectos de Desenfoque en Tiempo Real**: Contenedores glassmórficos dinámicos mediante la API `RenderEffect` en Android 13+.
- **Física de Materiales Realista**: Tarjetas interactivas con estados de elevación al tacto, micro-animaciones fluidas y transiciones orgánicas.
- **Tematización Inteligente**: Soporte completo para temas Claro y Oscuro sincronizados con el sistema.

### 🔒 2. Privacidad Absoluta (Privacy-First)
- **Sin Servidores Externos**: Cero telemetría, analíticas o conexiones de red.
- **Almacenamiento Local Seguro**: Base de datos relacional robusta gestionada con SQLite y Room.
- **Copias de Seguridad SAF**: Exportación e importación manual a archivos JSON locales mediante el **Storage Access Framework (SAF)** de Android.

### 📱 3. Widget de Pantalla de Inicio Dinámico
- Visualización de las tomas del día organizadas cronológicamente.
- **Estados de Toma Soportados**:
  - `TOMADO`: Marcado con check verde, texto atenuado y etiqueta aclaratoria.
  - `OMITIDO`: Indicador rojo y texto atenuado.
  - `POSPUESTO`: Texto de advertencia naranja.
  - `PENDIENTE`: Botón interactivo para marcar como tomado directamente desde la pantalla de inicio.
- **Sincronización Instantánea**: Actualización en tiempo real del widget tras cualquier cambio de estado desde la aplicación, las notificaciones o la base de datos.

### 🚨 4. Guardia contra Sobredosis (Overdose Guard)
- Protección integrada a nivel transaccional en base de datos.
- Previene registrar la toma del mismo medicamento más de una vez dentro de una ventana de **120 minutos**.
- Muestra un diálogo de advertencia de sobredosis crítico con opción de confirmación manual forzada.

### ⏰ 5. Motor de Alarmas Exactas y Persistencia
- Utiliza alarmas exactas del sistema (`AlarmManager.setExactAndAllowWhileIdle`) para asegurar la puntualidad del recordatorio, incluso en modos de ahorro de energía (*Doze*).
- **Acciones Rápidas en Notificación**: Botón para "Marcar como Tomado" y "Posponer 15m" directamente desde la notificación.
- **Recuperación tras Reinicio**: Módulo `BootReceiver` que recalcula y vuelve a programar todas las alarmas automáticamente cuando se enciende el teléfono.

### 👥 6. Soporte Multiperfil
- Crea múltiples perfiles aislados dentro de la aplicación para gestionar la medicación de familiares de forma independiente.

---

## 🛠️ Arquitectura Técnica y Stack

La aplicación se ha desarrollado bajo estrictas guías arquitectónicas para garantizar fiabilidad y prevención de corrupción de datos:

*   **Lenguaje**: Java 21 (JVM) con características modernas.
*   **Diseño**: XML Views nativo siguiendo guías Material Design 4.
*   **Base de datos**: Room (SQLite) con migración destructiva controlada en desarrollo.
*   **Multihilo**: `ExecutorService` de hilo único desacoplado para asegurar escrituras secuenciales no concurrentes en la base de datos.
*   **Prolongación de Hilo**: Uso estricto de `BroadcastReceiver.goAsync()` para evitar que el sistema operativo mate el proceso durante escrituras asíncronas de notificaciones.
*   **Deserialización Atómica**: Validación estructural in-memory mediante Gson antes de realizar cualquier borrado/restauración de base de datos.

---

## 🚀 Requisitos y Configuración

- **SDK Mínimo**: API 33 (Android 13 - Tiramisu)
- **SDK Objetivo**: API 35 (Android 15)
- **IDE**: Android Studio Koala / Ladybug o superior
- **JDK**: Java 21 o superior

### Instalación local:
1. Clona este repositorio:
   ```bash
   git clone https://github.com/SergioMuinhos/MedPills.git
   ```
2. Abre el proyecto en Android Studio.
3. Haz clic en **File -> Sync Project with Gradle Files** para descargar dependencias y sincronizar.
4. Compila y ejecuta en tu dispositivo físico o emulador:
   ```bash
   ./gradlew assembleDebug
   ```

---

## ⚙️ Permisos Clave Declarados

La aplicación requiere y gestiona de forma interactiva los siguientes permisos nativos de Android:
*   `android.permission.SCHEDULE_EXACT_ALARM`: Para recordatorios exactos. Si se revoca, la aplicación muestra una pantalla de bloqueo redirigiendo a los Ajustes del Sistema.
*   `android.permission.RECEIVE_BOOT_COMPLETED`: Para reprogramar recordatorios tras encender el móvil.
*   `android.permission.POST_NOTIFICATIONS`: Para mostrar recordatorios visuales y sonoros en Android 13+.
*   `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Permite añadir la app a la lista de excepciones de batería para evitar retrasos de alarmas provocados por Doze.

---

## 🔒 Privacidad y Transparencia
Esta aplicación no se conecta a internet. Tus datos de salud permanecen exclusivamente en la memoria interna de tu dispositivo móvil. Las copias de seguridad se almacenan donde tú elijas a través del selector de archivos de Android.

---

## 📄 Licencia
Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.
