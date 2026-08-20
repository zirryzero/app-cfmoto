# CFMOTO 800NK Advanced

Referencia técnica de la adaptación exclusiva de 800NK ADV Link.

## Contrato fijo

| Elemento | Valor |
| --- | --- |
| Motocicleta | CFMOTO 800NK Advanced |
| QR | MotoPlay / Carbit |
| `modelId` esperado | `37426` |
| Unidad central | CFDL26 |
| Paquete observado | `com.cfmoto.easyconnect` |
| Transporte | Wi-Fi SoftAP |
| Autenticación de socket | Activada |
| Pantalla | Táctil, `720×712` |
| Video Android Auto | Vertical, `720×1280`, `160 dpi` |
| Captura H.264 alineada | `720×704` |
| Área útil predeterminada | `720×682` |
| Margen inicial | Superior `22 px` |
| Función anunciada | `128` |

La aplicación puede aceptar un QR sin `modelId` si contiene `ssid` y `pwd`, pero rechaza un QR que
declare explícitamente un identificador distinto de `37426`.

## Resolución automática

El modo inicial **Auto optimizada** mantiene la resolución estándar y estable de Android Auto en
`720×1280`. La altura física `712` se alinea a `704` para el codificador H.264 y se reserva el margen
superior de `22 px`, por lo que Android Auto reorganiza su interfaz para un área útil de `720×682`.
Así, el compositor puede proyectar el contenido útil sin un segundo recorte ni una escala innecesaria.

El usuario puede cambiar manualmente a `720×1280` o `1080×1920`, modificar el ajuste de pantalla y
configurar la proporción o los márgenes. Los cambios de proporción se aplican en la siguiente conexión.

## Flujo de conexión

1. Leer el QR de MotoPlay.
2. Guardar un único emparejamiento.
3. Solicitar a Android la conexión al SoftAP indicado por `ssid` y `pwd`.
4. Iniciar Android Auto y esperar video estable.
5. Descubrir EasyConn mediante NSD o el puerto de activación estándar.
6. Completar el intercambio PXC/CFDL26 y transmitir H.264 al tablero.

No existen rutas operativas para perfiles alternativos, Wi-Fi Direct, hotspot alojado por el teléfono
o protocolos de otras unidades centrales.

## Validación pendiente en la moto

- Confirmar el contenido exacto del QR de la unidad del propietario.
- Confirmar prefijo HUID y versión CFDL26 en `CLIENT_INFO`.
- Verificar panel efectivo `720×712`, tacto y margen superior.
- Probar reconexión después de apagar y encender la moto.
- Medir estabilidad de las opciones de calidad y resolución HD.

Los registros compartidos ocultan contraseñas y seriales salvo que el usuario active expresamente la
inclusión de datos sensibles.
