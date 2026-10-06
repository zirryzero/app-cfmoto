# 800NK ADV Link — Directrices de Desarrollo y Arquitectura (AI & Developer Guidelines)

> **PROPÓSITO DE ESTE DOCUMENTO:**  
> Servir como única fuente de verdad técnica y operativa para asistentes de IA y desarrolladores.  
> Toda modificación, refactorización o nueva funcionalidad DEBE respetar estrictamente las reglas, contratos y restricciones aquí definidos para evitar contradicciones y regresiones críticas.

---

## 1. Identidad, Origen y Filosofía

- **Nombre del proyecto:** 800NK ADV Link (`dev.zanderp.opencfmoto`).
- **Naturaleza:** Aplicación Android local-first, independiente y no oficial.
- **Linaje:** Adaptación y bifurcación específica de [OpenCfMoto](https://github.com/zanderp/open-cfmoto) (Alexandru Popa / headunit-revived).
- **Licencia:** **GNU AGPL-3.0-or-later**. Se deben preservar obligatoriamente todos los avisos de licencia, derechos de autor y los archivos `NOTICE` y `LICENSE`.
- **Privacidad y Seguridad:** Sin cuentas de usuario, sin publicidad, sin telemetría obligatoria. La seguridad del motociclista en marcha es prioritaria: configuración siempre con el vehículo detenido, logs con credenciales (SSID, contraseñas, seriales) ofuscados por defecto.

---

## 2. Alcance Estricto y No-Objetivos (Invariantes del Proyecto)

### ✅ Lo que este proyecto ES:
- Soporte **exclusivo y especializado** para la motocicleta **CFMOTO 800NK Advanced**.
- Receptor de proyección inalámbrica (Android Auto y duplicación de apps del teléfono) hacia la pantalla MotoPlay / Carbit.

### ❌ Lo que este proyecto NO ES (Reglas de no contradicción):
1. **NO generalizar a múltiples modelos:** No añadir selectores de otros modelos de motocicleta ni alterar la lógica para soportar otros `modelId`. El contrato espera `modelId: 37426`. QRs con otros identificadores deben rechazarse explícitamente.
2. **NO usar Wi-Fi Direct ni Hotspot del teléfono:** La moto actúa exclusivamente como punto de acceso (SoftAP Wi-Fi). El teléfono se conecta a la red Wi-Fi de la moto.
3. **NO intentar ejecutar Samsung DeX directamente:** MotoPlay no es una pantalla secundaria externa compatible con Samsung DeX. Para proyectar aplicaciones del teléfono se utiliza el modo **Apps** (`MediaProjection` + `AccessibilityService`), no DeX.
4. **NO asumir conexión a Internet en la Wi-Fi de la moto:** La red Wi-Fi de la moto no tiene salida a Internet. La app debe desviar el tráfico de datos móviles (red celular) para navegación/mapas y desvincular el socket local `127.0.0.1` del proceso para evitar errores `ENONET`.

---

## 3. Contrato de Hardware y Protocolo

| Elemento | Especificación técnica | Notas de implementación |
| :--- | :--- | :--- |
| **Motocicleta objetivo** | CFMOTO 800NK Advanced | Modelo único objetivo |
| **Unidad central (Head Unit)** | CFDL26 / EasyConn / MotoPlay | Paquete observado: `com.cfmoto.easyconnect` |
| **QR MotoPlay** | Contiene SSID, PWD, modelId (`37426`) | Leído vía cámara o imagen importada |
| **Pantalla física** | `720 x 712` píxeles | Formato táctil casi cuadrado |
| **Codificación H.264** | `720 x 704` píxeles | Altura alineada a múltiplo requerido por el encoder |
| **Margen superior por defecto** | `22 px` | Reserva el overlay de estado del tablero |
| **Área útil del tablero** | `720 x 682` píxeles | Área efectiva de interacción |
| **Flujo Android Auto base** | **Horizontal `1280 x 720` a 160 dpi** | **Crítico:** NO forzar modo vertical en AA porque desalinea el control táctil de Google Maps |

---

## 4. Arquitectura de Pipelines (Cómo funciona internamente)

### 4.1. Conexión Android Auto (Self-Mode Loopback)
El teléfono actúa simultáneamente como emisor y receptor (loopback en `127.0.0.1:5288`):
1. La app escucha en el puerto local TCP `5288` e inicializa SSL Conscrypt.
2. Dispara el inicio de Android Auto mediante escalado en `AaSelfMode.kt`:
   - **Intento 1:** `WirelessStartupActivity` (versiones antiguas de Android Auto).
   - **Intento 2:** Broadcast `WirelessStartupReceiver` (AA 16.4+).
   - **Intento 3:** `START_WIRELESS_PROJECTION` con MAC Bluetooth real de la moto.
   - **Fallback (AA 17.4+):** Marcado saliente al puerto `5277` (servidor de unidad principal de Google). Si no responde tras 20 reintentos, se indica al usuario activar *"Start head unit server"* en los ajustes de desarrollador de Android Auto. **Esto es una limitación de seguridad impuesta por Google, no un error de código que deba eliminarse.**

### 4.2. Pipeline de Video (`VideoPipeline.kt` & `AaCompositor.kt`)
```
[Android Auto (1280x720)]  ──▶  VideoDecoder (Surface)
                                        │
                                        ▼
                                  AaCompositor
                        (Letterbox aspecto + márgenes 22px)
                                        │
                                        ▼
                                MediaCodec H.264
                      (Baseline 3.1 / CBR / SPS+PPS en IDR)
                                        │
                                        ▼
                        PXC Data Socket ──▶ Tablero MotoPlay
```
- **Sincronización:** Cada vez que la moto se reconecta, el primer frame emitido **debe ser un Keyframe (IDR) con cabeceras SPS/PPS**; de lo contrario, el decodificador de la moto no se inicializa y la pantalla queda en negro.
- **Eficiencia térmica:** No sobrecodificar imágenes estáticas a altos FPS. Existe un suelo de repetición de fotogramas (`IDLE_ENCODER_REPEAT_US` ~0.9s - 2s) para no agotar la batería ni sobrecalentar el teléfono en marcha.

### 4.3. Pipeline Táctil y Controles (`AaInput.kt` / `EasyConnProber.kt`)
- La moto envía eventos táctiles vía PXC comando 32 (`cx`, `cy`).
- `AaCompositor.mapBikeTouchToSource` realiza el **mapeo inverso** desde las coordenadas del tablero a las coordenadas de Android Auto. Si el toque cae en barras negras, se descarta.
- **Mandos en el manillar:** Por defecto, los botones físicos de la moto NO controlan Android Auto para evitar interferencias en la conducción. El control por botones es opcional (opt-in).

### 4.4. Modo Apps y Espejado (`ProjectionService.kt`)
- Utiliza `MediaProjection` hacia un `VirtualDisplay` interno.
- Requiere servicio en primer plano tipo `mediaProjection` (obligatorio desde Android 14+).
- Las pulsaciones en el tablero se reenvían al sistema operativo mediante un servicio de accesibilidad dedicado (`AccessibilityService`), sin capturar texto ni datos sensibles en pantalla.

---

## 5. Reglas de Oro para Asistentes de IA (Invariants & Guardrails)

1. **No romper la relación de aspecto ni el tamaño base de AA:**
   - Mantener el modo horizontal `1280x720` para la proyección AAP. No cambiarlo a layouts verticales sin verificar el impacto en el touch de Google Maps.
2. **Respetar el aislamiento de redes (`BikeWifi.kt`):**
   - Cuando el teléfono está conectado al Wi-Fi de la moto, el tráfico loopback a `127.0.0.1` falla si el proceso está vinculado a la red de la moto. Siempre desvincular temporalmente antes de abrir sockets locales.
3. **No eliminar logs informativos ni alterar los protocolos PXC:**
   - Los intercambios de sockets PXC y la cabecera CFDL26 deben respetarse exactamente como los espera el firmware del tablero.
4. **Protección de datos en Logs (`LogBus`):**
   - Cualquier nuevo log que registre datos del QR o de conexión debe usar sanitización/redacción para evitar exponer contraseñas Wi-Fi o identificadores privados.
5. **Configuración de compilación:**
   - `compileSdk = 36`, `minSdk = 29`, `targetSdk = 36`, Java 11.
   - Soportar ABI `arm64-v8a` por defecto y `-Pabi=armeabi-v7a` para teléfonos de 32 bits.

