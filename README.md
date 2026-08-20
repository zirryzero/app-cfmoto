# 800NK ADV Link

Aplicación Android adaptada exclusivamente para la **CFMOTO 800NK Advanced**. Conecta el teléfono
con el tablero mediante el QR de MotoPlay y proyecta Android Auto, navegación propia, viajes y
herramientas de diagnóstico.

> Este proyecto no fue creado desde cero. Es una adaptación de
> [OpenCfMoto](https://github.com/zanderp/open-cfmoto), desarrollada y mantenida por
> [ZirryZero](https://github.com/zirryzero). La autoría original, el historial del proyecto y las
> atribuciones completas se conservan en [NOTICE](NOTICE).

## Alcance

La aplicación admite un único contrato de motocicleta:

- Modelo: **CFMOTO 800NK Advanced**
- QR MotoPlay/Carbit: `modelId=37426` cuando el identificador está presente
- Unidad central: CFDL26 / EasyConn
- Conexión: Wi-Fi SoftAP obtenida del QR
- Panel táctil: `720×712`
- Video Android Auto: vertical `720×1280` a `160 dpi`
- Margen superior predeterminado: `22 px`

No incluye perfiles alternativos, Wi-Fi Direct, modo de
hotspot del teléfono ni protocolos de tableros de otras marcas.

## Funciones

- Android Auto inalámbrico en el tablero
- Escaneo del QR de MotoPlay con cámara o imagen de la galería
- Reconexión con el último QR de la 800NK Advanced
- Ajuste de calidad, encaje, márgenes y resolución vertical
- Navegación Map/GPX y registro de viajes
- Vista del tablero, controles y asignación de botones compatibles
- Registros y reporte de problemas sin incluir credenciales por defecto

## Emparejamiento

1. Abre MotoPlay en el tablero de la 800NK Advanced y deja visible el QR de conexión.
2. En la aplicación toca **Conectar** o **Emparejar 800NK**.
3. Escanea el QR y acepta la solicitud de conexión Wi-Fi de Android.
4. La aplicación guarda solo ese emparejamiento para las siguientes conexiones.

## Compilar

Requisitos: Android Studio, Android SDK y JDK 11 o posterior.

```powershell
.\gradlew.bat assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/`.

## Origen y licencia

800NK ADV Link conserva código de OpenCfMoto y del receptor Android Auto derivado de
`headunit-revived`. Se distribuye bajo **GNU AGPL-3.0-or-later**. Consulta [LICENSE](LICENSE) y
[NOTICE](NOTICE) antes de redistribuir una versión modificada.

Es un proyecto independiente y no oficial, sin afiliación ni respaldo de CFMOTO, Carbit, Google o
Android Auto. Configura las rutas con la motocicleta detenida.
