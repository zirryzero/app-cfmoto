# 800NK ADV Link - Privacy and permissions

_App version: 2.0.23-pre (78)_
_Last updated: 2026-08-19_

800NK ADV Link is an unofficial, local-first Android application adapted
exclusively for the CFMOTO 800NK Advanced. It does not require its own account,
does not contain advertising, and does not sell personal data.

This project was not created from scratch. It is an adaptation of
[OpenCfMoto](https://github.com/zanderp/open-cfmoto), maintained by
[ZirryZero](https://github.com/zirryzero). The original project authorship and
attributions are preserved in [NOTICE](NOTICE).

This policy describes the behavior of the code included in this version.
Google, OpenStreetMap, and other external services apply their own policies
when used.

## Summary

- There is no account, sign-in, or advertising.
- Pairing details, settings, trips, and logs are stored primarily on the device.
- Anonymous telemetry inherited from OpenCfMoto is initially enabled. It can be
  disabled under **Settings > Privacy**.
- Automatic trip logging is initially enabled. It can be disabled under
  **Settings > Startup and recovery**.
- The microphone is used when Android Auto requests audio for Gemini or the
  Assistant. 800NK ADV Link does not store that audio.
- The application does not independently upload saved trip routes, QR
  credentials, camera images, or screen content.

## Permissions and access

| Permission or access | When it is used | Purpose |
| --- | --- | --- |
| **Camera** | When tapping **Scan 800NK QR** | Reads the MotoPlay QR displayed by the motorcycle. Camera frames are not stored or uploaded. |
| **Selected image** | When choosing a QR screenshot | Android provides only the file selected through the system picker. The application does not request general gallery access. |
| **Precise location** | When connecting to the motorcycle Wi-Fi and recording a trip | Android may require it to associate with Wi-Fi networks. The trip computer also uses GPS to store route, speed, and distance. |
| **Nearby Wi-Fi devices** | When pairing or reconnecting | Requests the 800NK SoftAP network and communicates with its head unit. It is declared `neverForLocation` on Android 13 or later. |
| **Nearby Bluetooth devices** | When starting wireless Android Auto, checking pairing, optionally synchronizing the clock, or using compatible buttons | Starts projection, detects the motorcycle connection, and processes features enabled by the user. Scanning is declared `neverForLocation`. |
| **Microphone** | When Android Auto opens its voice channel for Gemini or the Assistant | Captures PCM audio and provides it to Android Auto for the duration of the request. 800NK ADV Link does not record it to a file or send it to telemetry. |
| **Notifications** | During Android Auto, mirroring, reconnection, or other active services | Displays the foreground service and actions to stop or resume it. |
| **Screen capture** | Only after approving **Mirror** in the Android dialog | Projects the complete display or a selected application to the dashboard. Approval cannot be granted silently. |
| **Display over other apps** | Optionally, for automatic recovery | Allows the service to reactivate Android Auto in the background. A notification is used when this access is unavailable. |

The application also declares Internet and network-state access, Wi-Fi changes,
multicast for local discovery, a wake lock, and foreground services for a
connected device, location, microphone, and projection. These permissions keep
the dashboard connection active.

The `KILL_BACKGROUND_PROCESSES` permission is used only as an attempt to close
the official CFMOTO package when it occupies the required local ports. If
Android does not allow this, the application opens the app-information screen
for the user to decide. Declared package visibility for Android Auto, Google
Play Services, and the CFMOTO app permits detecting those packages; it does not
provide access to a Google account, contacts, or messages.

## Data stored on the device

| Data | Storage and use |
| --- | --- |
| **800NK pairing** | Wi-Fi SSID and password, serial number, and QR metadata in private application preferences. They are used for reconnection. |
| **Settings** | Language, resolution, margins, quality, reconnection, controls, theme, privacy, and other preferences. |
| **Trips** | GPS points, timestamps, speed, distance, and duration in private JSON files. They are created during manual recording or automatically during projection when that option is enabled. |
| **Places and internal map** | Saved places, parking position, temporarily imported GPX files, offline areas, routing graph, and cached tiles. |
| **Logs and failures** | A technical log in memory and limited copies of the latest session or failure in private files for diagnostics. |
| **Pending telemetry** | A random UUID, last-send time, and a local queue limited to 24 events. |

Credentials are stored as readable text inside the private Android sandbox;
the application does not add its own encryption layer. A rooted device, a
privileged tool, or an Android-authorized backup could access them.

The manifest permits Android backup and device transfer. Depending on device
settings and the backup provider, Android may copy private application data off
the phone. 800NK ADV Link does not control the retention of those backups. You
can disable device backups in the system settings.

## Trips and location

Automatic trip logging is **enabled by default**. When a compatible session
starts, the application may record a sequence of coordinates and speeds. Trips
that do not reach the minimum movement thresholds are not saved.

Saved routes are not sent to telemetry. They remain on the device until deleted
in the application, the application data is cleared, or the application is
uninstalled, except for possible backups managed by Android. When exporting a
trip, the user manually chooses the application or destination that receives it.

## Microphone, Android Auto, and screen content

Android Auto opens and closes the microphone channel according to the voice
action. The application passes audio samples to the local Android Auto session
and stops capture when Android Auto closes the channel. Google, Gemini, the
Assistant, and applications running inside Android Auto process data under
their own terms and policies.

Android Auto projection and Mirror temporarily encode visual content as video
and send it to the 800NK head unit over its local network. This normal build
does not save that video. The local transport to the motorcycle should not be
considered end-to-end encrypted.

## Inherited anonymous telemetry

The **Anonymous usage and crash reports** option is initially enabled. The code
sends events to the inherited endpoint configured by OpenCfMoto:

`https://opencfmoto-telemetry.hello-3d9.workers.dev/v1/ping`

800NK ADV Link sends:

- a random UUID generated by the application;
- the event type and timestamp;
- the application version and version code;
- the Android version and locale;
- limited and redacted technical text for failures or errors.

The payload deliberately excludes the user's name, email, Google account, GPS
route, destination, screen content, audio, SSID, password, and QR data. Error
redaction is a best-effort safeguard and cannot guarantee that every unexpected
detail is removed from technical text. As with any HTTPS request, the operator
and its infrastructure may observe connection data such as the IP address and
user agent.

Events created while offline are stored temporarily and retried later.
Disabling the option stops uploads and deletes the queue; the UUID remains
stored locally so it remains stable if telemetry is enabled again. This
adaptation does not control the inherited service infrastructure or retention
policy. The related code comes from
[opencfmoto-telemetry](https://github.com/zanderp/opencfmoto-telemetry).

## External services

Depending on the features used, the device may communicate with:

- **Google Android Auto, Google Maps, Gemini, and other compatible apps:**
  navigation, voice, media, and services associated with the account configured
  on the phone.
- **OpenStreetMap MAPNIK and OpenFreeMap:** download and caching of map tiles,
  styles, and cartographic resources for trip maps or the Map/GPX module.
- **Komoot Photon and Nominatim/OpenStreetMap:** internal-map searches. The
  entered query and, when available, an approximate location are included to
  rank nearby results.
- **Overpass API:** point-of-interest searches and road-data downloads for a
  geographic area.
- **Valhalla at openstreetmap.de:** route calculation; it receives the origin,
  destination, intermediate points, and route options.
- **GitHub API (`zanderp/open-cfmoto`):** an automatic update check at most once
  per day before connecting, and manual update checks. This adaptation still
  uses the original project's release repository.
- **External About, support, or donation links:** opened only when selected by
  the user.

Each provider may receive the IP address, user agent, time, and data required to
answer the request. Their policies and retention periods are independent of
800NK ADV Link.

## Logs, exports, and reports

Logs may contain the phone model, Android version, network state, touch
coordinates, technical settings, and errors. Passwords, SSIDs, serial numbers,
and known tokens are redacted by default. The **Include secrets in logs** option
disables that protection and may expose credentials; it should remain disabled
when publishing a log.

Logs, reports, trips, and settings backups are shared only after a user action
and through the Android chooser. The settings backup excludes QR credentials
and personal data defined by the application.

## Control and deletion

- Disable telemetry under **Settings > Privacy**.
- Disable automatic trip logging under **Settings > Startup and recovery**.
- Delete trips and offline areas from their corresponding screens.
- Use **Logs > Clear** to clear the log available in the application.
- Revoke permissions from Android's application-information screen.
- Clear application data or uninstall it to remove local storage, without
  affecting previous backups or data already sent to third parties.

## If a permission is denied

The application still opens, but the associated feature may be unavailable:
without the camera, a QR image can still be selected; without nearby Wi-Fi or
location, the app cannot associate with the dashboard; without Bluetooth,
wireless Android Auto startup may fail; without the microphone, voice input
will not work; without notifications, Android may prevent the foreground
service; and without screen-capture approval, Mirror will not start.

## Contact and unofficial status

For questions about this adaptation:
[powerdevelopco@gmail.com](mailto:powerdevelopco@gmail.com) or
[github.com/zirryzero](https://github.com/zirryzero).

800NK ADV Link is independent and is not affiliated with, endorsed by, or
certified by CFMOTO, Carbit, Google, Android Auto, OpenStreetMap, or the other
providers mentioned. Their names and trademarks belong to their respective
owners.

---

# 800NK ADV Link - Privacidad y permisos

_Version de la aplicacion: 2.0.23-pre (78)_
_Ultima actualizacion: 2026-08-19_

800NK ADV Link es una aplicacion Android local-first y no oficial, adaptada
exclusivamente para la CFMOTO 800NK Advanced. No requiere una cuenta propia, no
incluye publicidad y no vende datos personales.

Este proyecto no fue creado desde cero. Es una adaptacion de
[OpenCfMoto](https://github.com/zanderp/open-cfmoto), mantenida por
[ZirryZero](https://github.com/zirryzero). La autoria y las atribuciones del
proyecto original se conservan en [NOTICE](NOTICE).

Esta politica describe el comportamiento del codigo incluido en esta version.
Los servicios de Google, OpenStreetMap y otros proveedores externos aplican sus
propias politicas cuando se utilizan.

## Resumen

- No hay cuenta, inicio de sesion ni publicidad.
- El emparejamiento, la configuracion, los viajes y los registros se guardan
  principalmente en el dispositivo.
- La telemetria anonima heredada de OpenCfMoto esta activada inicialmente. Puede
  desactivarse en **Configuracion > Privacidad**.
- El registro automatico de viajes esta activado inicialmente. Puede
  desactivarse en **Configuracion > Inicio y recuperacion**.
- El microfono se usa cuando Android Auto solicita audio para Gemini o el
  Asistente. 800NK ADV Link no guarda ese audio.
- La aplicacion no sube por si sola rutas de viajes guardadas, credenciales del
  QR, imagenes de la camara ni el contenido de la pantalla.

## Permisos y accesos

| Permiso o acceso | Cuando se usa | Finalidad |
| --- | --- | --- |
| **Camara** | Al tocar **Escanear QR 800NK** | Leer el QR de MotoPlay mostrado por la motocicleta. Los fotogramas no se guardan ni se suben. |
| **Imagen seleccionada** | Al elegir una captura del QR | Android entrega solo el archivo elegido mediante el selector del sistema. La aplicacion no solicita acceso general a la galeria. |
| **Ubicacion precisa** | Al conectar el Wi-Fi de la moto y al registrar un viaje | Android puede exigirla para asociarse a redes Wi-Fi. El computador de viaje tambien usa GPS para guardar ruta, velocidad y distancia. |
| **Dispositivos Wi-Fi cercanos** | Al emparejar o reconectar | Solicitar la red SoftAP de la 800NK y comunicarse con su unidad principal. Se declara `neverForLocation` en Android 13 o posterior. |
| **Bluetooth cercano** | Al iniciar Android Auto inalambrico, consultar el emparejamiento, sincronizar opcionalmente el reloj o usar botones compatibles | Iniciar la proyeccion, detectar la conexion con la moto y procesar las funciones habilitadas por el usuario. El escaneo se declara `neverForLocation`. |
| **Microfono** | Cuando Android Auto abre el canal de voz para Gemini o el Asistente | Capturar audio PCM y entregarlo a Android Auto durante la solicitud. 800NK ADV Link no lo graba en un archivo ni lo envia a su telemetria. |
| **Notificaciones** | Durante Android Auto, espejo, reconexion o servicios activos | Mostrar el servicio en primer plano y acciones para detener o reanudar. |
| **Captura de pantalla** | Solo despues de aprobar **Espejo** en el dialogo de Android | Proyectar la pantalla completa o una aplicacion elegida al tablero. La aprobacion no puede concederse silenciosamente. |
| **Mostrar sobre otras aplicaciones** | Opcional, para recuperacion automatica | Permitir que el servicio reactive Android Auto en segundo plano. Sin este permiso se utiliza una notificacion. |

La aplicacion tambien declara acceso a Internet y al estado de red, cambios de
Wi-Fi, multicast para descubrimiento local, bloqueo de activacion y servicios
en primer plano de dispositivo conectado, ubicacion, microfono y proyeccion.
Estos permisos mantienen activa la conexion con el tablero.

La autorizacion `KILL_BACKGROUND_PROCESSES` se utiliza solo como intento de
cerrar el paquete oficial de CFMOTO cuando ocupa los puertos locales necesarios.
Si Android no lo permite, la aplicacion abre la pantalla de informacion para
que el usuario decida. La visibilidad declarada para Android Auto, Google Play
Services y la aplicacion de CFMOTO permite detectar esos paquetes; no concede
acceso a la cuenta de Google, contactos o mensajes.

## Datos almacenados en el dispositivo

| Datos | Almacenamiento y uso |
| --- | --- |
| **Emparejamiento 800NK** | SSID, contrasena Wi-Fi, numero de serie y metadatos del QR en preferencias privadas de la aplicacion. Se usan para reconectar. |
| **Configuracion** | Idioma, resolucion, margenes, calidad, reconexion, controles, tema, privacidad y otras preferencias. |
| **Viajes** | Puntos GPS, marcas de tiempo, velocidad, distancia y duracion, en archivos JSON privados. Se crean durante una grabacion manual o automaticamente durante la proyeccion cuando esa opcion esta activa. |
| **Lugares y mapa interno** | Lugares guardados, posicion de estacionamiento, GPX importados temporalmente, areas sin conexion, grafo de rutas y teselas en cache. |
| **Registros y fallos** | Registro tecnico en memoria y copias limitadas de la ultima sesion o fallo en archivos privados para diagnostico. |
| **Telemetria pendiente** | UUID aleatorio, fecha del ultimo envio y una cola local limitada a 24 eventos. |

Las credenciales se almacenan en texto legible dentro del sandbox privado de
Android; no cuentan con cifrado adicional propio. Un dispositivo rooteado, una
herramienta con privilegios o una copia de seguridad autorizada por Android
podrian acceder a ellas.

El manifiesto permite las copias de seguridad y la transferencia de datos de
Android. Dependiendo de la configuracion del dispositivo y del proveedor de
copias de seguridad, Android puede copiar datos privados de la aplicacion fuera
del telefono. 800NK ADV Link no controla la conservacion de esas copias. Puede
desactivar las copias del dispositivo desde los ajustes del sistema.

## Viajes y ubicacion

El registro automatico de viajes esta **activado de forma predeterminada**. Al
iniciar una sesion compatible, la aplicacion puede registrar una secuencia de
coordenadas y velocidades. Los viajes que no alcanzan los umbrales minimos de
movimiento no se guardan.

Las rutas guardadas no se envian a la telemetria. Permanecen en el dispositivo
hasta que se eliminan desde la aplicacion, se borran sus datos o se desinstala,
salvo las posibles copias de seguridad administradas por Android. Al exportar
un viaje, el usuario elige manualmente la aplicacion o destino que lo recibira.

## Microfono, Android Auto y pantalla

Android Auto abre y cierra el canal del microfono segun la accion de voz. La
aplicacion transmite las muestras a la sesion local de Android Auto y detiene la
captura cuando Android Auto cierra el canal. Google, Gemini, el Asistente y las
aplicaciones ejecutadas dentro de Android Auto procesan los datos conforme a
sus propias condiciones y politicas.

La proyeccion de Android Auto y el modo Espejo convierten temporalmente el
contenido visual en video para enviarlo a la unidad principal de la 800NK por
su red local. Esta version normal no guarda ese video. El transporte local con
la motocicleta no debe considerarse cifrado de extremo a extremo.

## Telemetria anonima heredada

La opcion **Uso anonimo e informes de fallos** esta activada inicialmente. El
codigo envia eventos al endpoint heredado configurado por OpenCfMoto:

`https://opencfmoto-telemetry.hello-3d9.workers.dev/v1/ping`

800NK ADV Link envia:

- un UUID aleatorio generado por la aplicacion;
- tipo y fecha del evento;
- version y codigo de version de la aplicacion;
- version de Android y configuracion regional;
- en fallos o errores, texto tecnico limitado y redactado.

No se incluyen deliberadamente el nombre, correo, cuenta de Google, ruta GPS,
destino, contenido de pantalla, audio, SSID, contrasena o QR. La redaccion de
errores es una proteccion de mejor esfuerzo y no puede garantizar que todo dato
inesperado desaparezca de un mensaje tecnico. Como en cualquier solicitud
HTTPS, el operador y su infraestructura pueden observar datos de conexion como
la direccion IP y el agente de usuario.

Los eventos sin conexion se guardan temporalmente y se intentan enviar mas
tarde. Al desactivar la opcion se detienen los envios y se elimina la cola; el
UUID permanece almacenado localmente para conservarlo si se reactiva. Esta
adaptacion no controla la infraestructura ni la politica de conservacion del
servicio heredado. El codigo relacionado procede de
[opencfmoto-telemetry](https://github.com/zanderp/opencfmoto-telemetry).

## Servicios externos

Segun las funciones utilizadas, el dispositivo puede comunicarse con:

- **Google Android Auto, Google Maps, Gemini y otras aplicaciones compatibles:**
  navegacion, voz, multimedia y servicios de la cuenta configurada en el
  telefono.
- **OpenStreetMap MAPNIK y OpenFreeMap:** descarga y cache de teselas, estilos y
  recursos cartograficos para mapas de viajes o el modulo Map/GPX.
- **Photon de Komoot y Nominatim/OpenStreetMap:** busquedas del mapa interno. La
  consulta escrita y, cuando esta disponible, una ubicacion aproximada se
  incluyen para ordenar resultados cercanos.
- **Overpass API:** busqueda de puntos de interes y descarga de datos viales de
  un area geografica.
- **Valhalla de openstreetmap.de:** calculo de rutas; recibe origen, destino,
  puntos intermedios y opciones de ruta.
- **GitHub API (`zanderp/open-cfmoto`):** consulta automatica, como maximo una vez
  al dia antes de conectar, y consulta manual de nuevas versiones. Esta
  adaptacion todavia utiliza el repositorio de versiones del proyecto original.
- **Enlaces externos de Acerca de, soporte o donacion:** solo se abren cuando el
  usuario los selecciona.

Cada proveedor puede recibir la direccion IP, el agente de usuario, la hora y
los datos necesarios para responder a la solicitud. Sus politicas y periodos de
conservacion son independientes de 800NK ADV Link.

## Registros, exportaciones y reportes

Los registros pueden contener modelo del telefono, version de Android, estado
de red, coordenadas tactiles, configuracion tecnica y errores. Por defecto se
redactan contrasenas, SSID, numeros de serie y tokens conocidos. La opcion
**Incluir secretos en registros** desactiva esa proteccion y puede exponer
credenciales; debe mantenerse apagada al publicar un registro.

Los registros, reportes, viajes y copias de configuracion solo se comparten
despues de una accion del usuario y mediante el selector de Android. La copia de
configuracion excluye credenciales del QR y datos personales definidos por la
aplicacion.

## Control y eliminacion

- Desactive la telemetria en **Configuracion > Privacidad**.
- Desactive el registro automatico en **Configuracion > Inicio y recuperacion**.
- Elimine viajes y areas sin conexion desde sus pantallas correspondientes.
- Use **Registros > Borrar** para limpiar el registro disponible en la app.
- Revoque permisos desde la informacion de la aplicacion en Android.
- Borre los datos de la aplicacion o desinstalela para eliminar el almacenamiento
  local, sin perjuicio de copias de seguridad previas o datos ya enviados a
  terceros.

## Si se deniega un permiso

La aplicacion sigue abriendo, pero la funcion asociada puede no estar disponible:
sin camara aun puede seleccionarse una imagen del QR; sin Wi-Fi cercano o
ubicacion no puede asociarse al tablero; sin Bluetooth puede fallar el inicio de
Android Auto inalambrico; sin microfono no funcionara la entrada de voz; sin
notificaciones Android puede impedir el servicio en primer plano; y sin aprobar
la captura de pantalla no se inicia Espejo.

## Contacto y caracter no oficial

Para consultas sobre esta adaptacion:
[powerdevelopco@gmail.com](mailto:powerdevelopco@gmail.com) o
[github.com/zirryzero](https://github.com/zirryzero).

800NK ADV Link es independiente y no esta afiliada, respaldada ni certificada
por CFMOTO, Carbit, Google, Android Auto, OpenStreetMap ni los demas proveedores
mencionados. Sus nombres y marcas pertenecen a sus respectivos titulares.
