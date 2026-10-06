# 800NK ADV Link - Directrices de desarrollo y arquitectura

## 1. Proposito y fuentes de verdad

Este documento define las intenciones, invariantes y criterios de aceptacion que deben respetar
asistentes de IA y desarrolladores. No sustituye el contrato ejecutable del codigo y las pruebas ni
la informacion especializada de otros documentos:

- `LICENSE` y `NOTICE` gobiernan licencia, atribucion y procedencia.
- `PRIVACY.md` gobierna permisos, almacenamiento, telemetria y servicios externos.
- `docs/800NK-ADVANCED.md` documenta el contrato de hardware y sus validaciones pendientes.
- El codigo y las pruebas representan el comportamiento ejecutable actual.
- `README.md` representa la experiencia, instalacion y soporte publicados al usuario.

Cuando una modificacion cambie comportamiento, todos los documentos y pruebas afectados deben
actualizarse en el mismo cambio. Una discrepancia no se resuelve ignorando una de las fuentes: debe
investigarse y corregirse antes de publicar.

## 2. Identidad, origen y licencia

- **Nombre:** 800NK ADV Link.
- **ID de aplicacion:** `dev.zanderp.opencfmoto`.
- **Naturaleza:** aplicacion Android local-first, independiente y no oficial.
- **Alcance:** adaptacion exclusiva para la CFMOTO 800NK Advanced.
- **Origen:** adaptacion de
  [OpenCfMoto](https://github.com/zanderp/open-cfmoto) y del linaje comunitario indicado en
  `NOTICE` y la pantalla de creditos.
- **Licencia:** GNU AGPL-3.0-or-later.

Reglas de atribucion:

- No eliminar ni reducir avisos de autoria, `LICENSE`, `NOTICE` o creditos heredados.
- No presentar la aplicacion ni el protocolo heredado como creacion original de esta adaptacion.
- Los archivos fuente nuevos deben conservar el estilo de cabecera SPDX del modulo cuando aplique.
- Un cambio procedente de otro proyecto debe identificar su origen y respetar su licencia.

## 3. Privacidad y seguridad

- No existen cuentas propias, inicio de sesion ni publicidad.
- La telemetria anonima heredada esta **activada inicialmente**, es opcional y puede desactivarse en
  **Configuracion > Privacidad**. Desactivarla debe detener los envios y limpiar su cola pendiente.
- El registro automatico de viajes esta **activado inicialmente** y puede desactivarse en ajustes.
- Credenciales del QR, configuracion, viajes y registros se guardan principalmente en el dispositivo.
  No afirmar que estan cifrados por la aplicacion: las credenciales permanecen en el sandbox privado
  y Android puede incluir datos permitidos en sus copias de seguridad.
- `LogBus` debe redactar por defecto SSID, contrasenas, seriales, HUID, UUID y datos equivalentes.
  Mostrar secretos en registros es una eleccion explicita del usuario; la telemetria nunca debe
  heredar esa excepcion.
- No guardar audio del microfono, video proyectado, texto de accesibilidad ni contenido de pantalla.
- Todo permiso, endpoint, campo de telemetria o servicio externo nuevo exige actualizar
  `PRIVACY.md`, `README.md`, el manifiesto y los textos visibles correspondientes.

La recomendacion de realizar configuraciones con la motocicleta detenida es informativa. La
aplicacion **no implementa bloqueo por velocidad o movimiento**, y no debe anadirse uno sin una
decision explicita del responsable del proyecto.

## 4. Alcance estricto y no objetivos

### El proyecto es

- Un receptor de Android Auto inalambrico para MotoPlay / Carbit de la CFMOTO 800NK Advanced.
- Un sistema de proyeccion de pantalla y aplicaciones del telefono hacia el tablero.
- Un cliente del SoftAP Wi-Fi creado por la motocicleta.

### El proyecto no es

1. **Una aplicacion multimodelo.** No anadir perfiles, selectores, protocolos ni configuraciones
   dedicados a otras motocicletas.
2. **Un cliente Wi-Fi Direct o hotspot del telefono.** No reactivar rutas heredadas donde la moto se
   conecta a un hotspot creado por el telefono.
3. **Samsung DeX.** MotoPlay no funciona como una pantalla externa compatible con DeX. El modo
   **Aplicaciones** usa `MediaProjection` y un servicio opcional de accesibilidad.
4. **Un mecanismo para evitar DRM.** Ventanas seguras o protegidas pueden verse negras. No intentar
   capturarlas, modificarlas ni eludir su proteccion.
5. **Un enrutador global para otras aplicaciones.** 800NK ADV Link puede fijar sus propias
   solicitudes de mapas/rutas a la red movil, pero no controla el trafico de procesos de terceros
   abiertos mediante el modo Aplicaciones.

## 5. Contrato de la 800NK Advanced

| Elemento | Contrato actual | Observacion |
| --- | --- | --- |
| Motocicleta | CFMOTO 800NK Advanced | Unico modelo admitido |
| `modelId` | `37426`, cuando esta presente | Otro valor explicito se rechaza |
| QR sin `modelId` | Permitido con `ssid` y `pwd` validos | Compatibilidad requerida |
| Unidad central | CFDL26 / EasyConn / MotoPlay | Confirmar variantes con registros reales |
| Paquete informado por el tablero | `com.cfmoto.easyconnect` | No confundir con la app Android oficial |
| App Android oficial competidora | `com.cfmoto.cfmotointernational` | Solo deteccion/cierre asistido |
| Transporte | SoftAP Wi-Fi generado por la moto | Sin Internet propio |
| Panel tactil | `720 x 712` | Contrato observado actual |
| Captura H.264 esperada | `720 x 704` | `712` redondeado hacia abajo a multiplo de 16 |
| Margen inicial | Superior `22 px` | Configurable por el usuario |
| Area util inicial | `720 x 682` | Solo con captura `704` y margen superior `22` |
| Android Auto inicial | `1280 x 720`, `160 dpi` | Modo horizontal optimizado |

Las dimensiones de captura no deben codificarse como universales: se derivan de la solicitud del
tablero y se alinean mediante `BikeProfile.roundCaptureDimensions`. Las opciones verticales
`720 x 1280` y `1080 x 1920` permanecen como ajustes manuales de comparacion; no deben convertirse
en el valor predeterminado sin volver a validar Google Maps y el tactil en la moto.

## 6. Arquitectura de Android Auto

### 6.1 Inicio y transporte AAP

El telefono actua como unidad principal y telefono Android Auto simultaneamente:

1. `AaReceiver` inicializa Conscrypt y escucha AAP en `127.0.0.1:5288`.
2. `AaSelfMode` intenta, de forma escalonada:
   - `WirelessStartupActivity`;
   - `WirelessStartupReceiver`;
   - `START_WIRELESS_PROJECTION` con una MAC Bluetooth real.
3. En paralelo, `AaReceiver` intenta hasta 20 veces una conexion saliente a `127.0.0.1:5277`, el
   servidor de unidad principal de Android Auto utilizado por versiones recientes.

No disparar nuevamente `AaSelfMode` mientras exista una sesion AAP activa: puede destruir la sesion.
No eliminar la ruta `5277` ni ocultar la instruccion **Start head unit server** sin demostrar en los
telefonos objetivo una alternativa automatica estable.

### 6.2 Ciclo de red

La red de la moto no tiene Internet y puede eliminar la ruta loopback cuando el proceso completo se
vincula a ella. El ciclo de Android Auto debe conservar este orden:

1. Desvincular el proceso de redes obsoletas antes de abrir `5288` o conectar a `5277`.
2. Solicitar el SoftAP de la moto, pero diferir el enlace global del proceso mientras inicia AAP.
3. Esperar video estable de Android Auto.
4. Vincular el proceso a la red de la moto cuando PXC necesite alcanzar el tablero.
5. Para una operacion loopback posterior, desvincular temporalmente y restaurar exactamente la red
   anterior al terminar.
6. Al perder el SoftAP, eliminar inmediatamente el enlace del proceso para evitar `ENONET`.

`BikeWifi` y `BikeLink` son propietarios de ese ciclo. No introducir llamadas aisladas a
`bindProcessToNetwork` en actividades o controladores. Los sockets PXC deben usar la red de la moto;
las solicitudes HTTP propias deben usar `AppHttp` para elegir una red con Internet.

### 6.3 Video

Flujo principal:

```text
Android Auto -> VideoDecoder -> AaCompositor -> MediaCodec H.264 -> PXC -> MotoPlay
```

- El modo preferido es H.264 Baseline 3.1, CBR, sin B-frames; se conserva el fallback del encoder
  para dispositivos que no acepten ese perfil.
- Al iniciar o reanudar el envio al tablero, la primera unidad de acceso debe incluir SPS/PPS y un
  IDR. Enviar primero un P-frame puede dejar el tablero negro.
- `IDLE_ENCODER_REPEAT_US` es el respaldo del encoder a `900 000 us`.
- `IDLE_REDRAW_MS` de `AaCompositor` es un mecanismo separado de redibujado a `2 000 ms`.
- No aumentar FPS, bitrate o frecuencia de repeticion sin medir temperatura, bateria, latencia y
  estabilidad del socket de medios.

### 6.4 Tacto y botones

- `EasyConnProber` recibe el tacto del tablero mediante PXC media `cmdType 32`.
- `AaCompositor.mapCanvasToSource` transforma coordenadas del canvas hacia la fuente y descarta
  toques sobre barras negras.
- `AaInput` mantiene el conjunto multitactil enviado a Android Auto.
- El tacto del tablero es el control predeterminado.
- Los botones fisicos conservan su comportamiento normal de medios, volumen y llamadas. El control
  de Android Auto mediante botones es opt-in y debe continuar desactivado inicialmente.

## 7. Arquitectura del modo Aplicaciones

Responsabilidades:

- `MainActivity`: consentimiento de captura, seleccion de modo y ciclo de conexion.
- `ProjectionService`: servicio en primer plano `mediaProjection`, notificacion y wake lock. No crea
  por si mismo el `VirtualDisplay`.
- `ProjectionHolder`: conserva temporalmente el token de `MediaProjection`.
- `VideoPipeline`: crea el `VirtualDisplay`, compone la captura y codifica H.264.
- `AppModeController`: mantiene el estado y la aplicacion seleccionada.
- `AppLauncherActivity`: lista y abre aplicaciones instaladas.
- `AppControlAccessibilityService`: convierte gestos del tablero en gestos Android y ofrece Atras,
  Inicio, Recientes y cambio de aplicacion.

Invariantes:

- Android debe mostrar y aprobar su dialogo de captura; no intentar evitarlo.
- La accesibilidad es opcional y debe permanecer con `canRetrieveWindowContent=false`.
- No solicitar contenido de ventanas, leer texto, inspeccionar contrasenas ni persistir eventos de
  accesibilidad.
- Las coordenadas deben pasar por el mismo escalado, recorte y letterbox utilizados para el video.
- El audio permanece en el telefono o en su salida Bluetooth activa; este modo no captura audio de
  aplicaciones.
- Contenido DRM o `FLAG_SECURE` puede aparecer negro y se considera comportamiento esperado.
- No afirmar ni intentar que este modo sea Samsung DeX.

## 8. Cambios de protocolo, logs y configuracion

- No modificar comandos, endianess, framing, autenticacion, cabeceras CFDL26 ni orden del handshake
  basandose solo en suposiciones. Un cambio requiere registros o capturas que lo justifiquen y una
  prueba de regresion cuando sea posible.
- Los logs diagnosticos pueden reorganizarse o reducirse, pero deben conservar senales suficientes
  para identificar fases de AAP, Wi-Fi, PXC, video, microfono y tacto.
- Ningun log nuevo puede exponer secretos sin pasar por `LogBus` y `LogRedactor`.
- Las preferencias persistentes nuevas deben tener valor predeterminado estable, migracion o
  compatibilidad hacia atras y soporte en `SettingsBackup` cuando sean portables.
- No eliminar una opcion de recuperacion o ajuste manual sin comprobar que no sea necesaria en los
  telefonos y firmware probados.

## 9. Entorno de compilacion y ABI

- **JDK para Gradle:** 17.
- **Compatibilidad de codigo Java:** source/target 11.
- **Android SDK:** `compileSdk 36.1`, `targetSdk 36`, `minSdk 29`.
- **ABI predeterminada:** `arm64-v8a`.
- **ABI de compatibilidad:** `-Pabi=armeabi-v7a` para Android de 32 bits.
- El modo slim mantiene R8 y reduccion de recursos para publicaciones cuando corresponda.

No confundir el JDK que ejecuta Gradle con el nivel Java producido por el compilador.

## 10. Firma y publicaciones

- Una publicacion estable debe usar siempre la misma clave privada de release. La clave y sus
  contrasenas no se guardan en Git.
- Un build de tipo `release` destinado a publicacion no debe recurrir silenciosamente a la clave
  debug cuando falta el keystore: debe fallar o quedar explicitamente marcado como build de prueba.
- Un APK debug solo puede publicarse como prerelease de pruebas claramente identificada. Cambiar
  despues a otra firma exige desinstalar la aplicacion y pierde la ruta normal de actualizacion.
- Incrementar `versionCode` para cada artefacto distribuido y hacer coincidir `versionName`, etiqueta
  Git, titulo de la Release y nombres de APK.
- Generar y verificar por separado ARM64 y ARMv7; no asumir la ABI por el nombre del archivo.
- Antes de subir, comprobar firma, paquete, version y ABI con `apksigner` y `aapt dump badging`.
- No subir keystores, `keystore.properties`, tokens, contrasenas ni claves API privadas.

## 11. Criterios de aceptacion

Como minimo, todo cambio de codigo debe ejecutar:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat assembleDebug -Pabi=armeabi-v7a
git diff --check
```

En Linux o macOS se usa `./gradlew`. Para una publicacion se deben construir tambien las variantes
`release` firmadas correspondientes.

Reglas de validacion:

- Agregar pruebas proporcionales al riesgo, especialmente para parseo QR, coordenadas, preferencias,
  red y maquinas de estado.
- Ejecutar lint sobre el alcance modificado. Si el lint global falla por deuda heredada, documentar
  el resultado y demostrar que el cambio no agrega errores; no ocultarlo creando una baseline sin
  revision.
- No declarar validacion fisica cuando solo se probo en JVM o emulador.
- Los cambios en AA, Wi-Fi, PXC, video, tacto, microfono o Apps requieren prueba en la 800NK cuando
  sea posible. Si no lo es, marcar expresamente la validacion como pendiente.

Lista minima para prueba en la moto cuando el cambio afecte esos flujos:

- conexion inicial y reconexion despues de apagar/encender;
- Google Maps: toque, arrastre y zoom;
- Gemini/Assistant: apertura y microfono;
- Apps: seleccion, toque, desplazamiento, navegacion y cambio de aplicacion;
- rotacion o cambio de tamano de la captura;
- Internet movil para mapas mientras el SoftAP de la moto esta conectado;
- botones con control de AA desactivado inicialmente y activado de forma voluntaria;
- Stop, perdida de Wi-Fi y cierre de `MediaProjection` sin servicios o wake locks residuales.

## 12. Documentacion, textos y revision

- `README.md` mantiene ingles primero y espanol despues.
- `PRIVACY.md` mantiene ingles primero y espanol despues.
- Una funcion visible nueva requiere textos base en ingles y espanol. Revisar todos los locales
  configurados y aceptar fallback al ingles solo como decision consciente.
- Actualizar capturas, permisos, limitaciones y pasos de instalacion cuando cambie la experiencia.
- Preservar los enlaces al codigo original, codigo de la adaptacion, autor original, responsable de
  la adaptacion, contacto y donaciones.
- No incluir refactorizaciones, archivos generados o cambios de formato ajenos a la tarea.

Antes de cerrar un cambio, confirmar:

- alcance exclusivo de la 800NK Advanced;
- ausencia de secretos y datos personales nuevos;
- comportamiento predeterminado compatible;
- pruebas y builds ejecutados;
- documentacion y privacidad actualizadas;
- version, firma, ABI y artefactos verificados si existe publicacion;
- limitaciones pendientes descritas con precision, sin presentar supuestos como hechos confirmados.
