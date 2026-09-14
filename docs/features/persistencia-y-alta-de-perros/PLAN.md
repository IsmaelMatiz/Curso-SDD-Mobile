# PLAN: Persistencia local y alta de perros

**SPEC de referencia:** `docs/features/persistencia-y-alta-de-perros/SPEC.md`
**Versión de la spec revisada:** commit `73bd8fb`, estado Aprobada
**Estado:** Borrador <!-- Borrador | En revisión | Aprobado -->

<!-- PARA LA PERSONA
Copia esta plantilla como PLAN.md junto a la SPEC.md aprobada.
Este documento define la solución técnica. Una vez revisado, el agente puede
derivar TASKS.md con tareas, dependencias y comprobaciones.
-->

<!-- PARA EL AGENTE
- Lee la SPEC.md aprobada, las instrucciones del proyecto y MOBILE_GUIDELINES.md.
  Si falta un documento necesario o la spec no está aprobada, indícalo antes de avanzar.
- Inspecciona el repositorio. Referencia rutas verificadas y distingue las nuevas propuestas.
- Propón una solución proporcional al alcance y coherente con el proyecto.
  Reutiliza lo existente y justifica nuevas dependencias o cambios de arquitectura.
- Distingue hechos, decisiones confirmadas y propuestas. Consulta las decisiones
  no resueltas; haz pocas preguntas por vez y actualiza el plan con las respuestas.
- Referencia los requisitos y criterios por su ID, sin copiar toda la spec.
- Si una decisión cambia el comportamiento o alcance, vuelve a la spec y solicita
  confirmación. No resuelvas una duda de producto mediante una suposición técnica.
- Conserva estos comentarios. No implementes durante la planificación.
- Solicita aprobación antes de marcar el plan como Aprobado. La autorización
  para implementar debe ser explícita; no se deduce del estado de los documentos.
-->

## Contexto técnico verificado

<!-- Qué existe hoy y cómo participa en la funcionalidad. -->

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| Servicio de red | `app/src/main/java/com/aristidevs/cursopremiumandroid/data/api/DogApiServices.kt` | Retrofit con `getDogs()` y `getDogDetail(id)` suspend. Se reutiliza sin cambios; pasa a ser el origen del refresco, no de la pantalla |
| DTOs de respuesta | `data/api/response/DogResponse.kt`, `DogDetailResponse.kt` | Contratos de red `@Serializable`. Se reutilizan sin cambios |
| Mapper de red | `data/mapper/DogMapper.kt` | Hoy convierte respuesta a dominio y antepone `BASE_URL` a la imagen. Pasa a convertir respuesta a entidad de base de datos |
| Repositorio | `data/DogRepositoryImpl.kt` | Hoy delega todo en la API. Se reescribe como local-first |
| Contrato de dominio | `domain/DogRepository.kt` | Interfaz propiedad del dominio. Cambia su firma |
| Modelos de dominio | `domain/model/Dog.kt`, `domain/model/DogDetailModel.kt` | Modelos planos. Cambian el tipo de `id` y la opcionalidad de tres campos |
| Casos de uso | `domain/usecase/GetDogsUseCase.kt`, `GetDogDetailUseCase.kt` | `suspend operator fun invoke`. Se modifican y se añaden dos nuevos |
| Lista | `presentation/list/DogViewModel.kt`, `DogScreen.kt` | `DogsUiState` como data class, caché `allDogs` en memoria y filtrado por nombre y raza. Se modifica conservando la representación de estado |
| Ficha | `presentation/detail/DogDetailViewModel.kt`, `DetailScreen.kt` | `DogDetailUiState` sellado Loading/Success/Error. Se modifica conservando el modelo sellado |
| Navegación | `core/navigation/Routes.kt`, `AppNavigation.kt` | Navigation3 con `NavKey` `@Serializable` y manipulación directa del back stack. Se añade una ruta |
| Inyección | `core/di/DataModule.kt`, `core/di/DogApiConfig.kt` | Módulo `object` con `@Provides` para Json, Retrofit, API y repositorio. Se amplía |
| Tema | `ui/theme/Color.kt` | `BackgroundApp`, `BackgroundComponent`, `PrimaryButton`, `SecondaryText`, `ControlColor`. Se reutilizan; el formulario y la etiqueta no introducen hex sueltos |
| Entrada de la app | `MainActivity.kt`, `CursoPremiumApp.kt` | `@AndroidEntryPoint` y `@HiltAndroidApp`. Sin cambios |
| Textos | `app/src/main/res/values/strings.xml` | Hoy contiene solo `app_name`. Se amplía con los textos nuevos |
| Build | `app/build.gradle.kts`, `gradle/libs.versions.toml` | Catálogo de versiones, Hilt por KSP, `minSdk 26`, `compileSdk 37`. Se amplían |
| Tests | `app/src/test/`, `app/src/androidTest/` | Solo `ExampleUnitTest` y `ExampleInstrumentedTest` generados. No hay ningún test de producción |

**Convenciones y patrón de referencia:** la feature Dog cableada de extremo a
extremo. Se respetan: `presentation -> domain` y `data -> domain`; dominio sin
Android ni DTOs; un `StateFlow<UiState>` por pantalla recogido con
`collectAsStateWithLifecycle()`; pantalla stateful que resuelve `hiltViewModel()`
más un `*Content` sin estado; `@Inject constructor` en las clases propias;
rutas `@Serializable` que implementan `NavKey`; Retrofit con kotlinx.serialization;
KSP para el procesado de anotaciones; y colores con nombre del tema.

**Comprobado en este entorno:** `./gradlew :app:assembleDebug :app:testDebugUnitTest`
termina con `BUILD SUCCESSFUL` sobre el código actual (JDK 21, SDK en
`/home/masterchetos/Android/Sdk`).

## Solución propuesta

<!-- Explica el enfoque y sus motivos. Describe las responsabilidades y el
recorrido de datos y eventos hasta la interfaz. Usa un diagrama si aporta claridad. -->

El cambio de fondo es **invertir el origen de la verdad**. Hoy cada pantalla
pregunta a la red y muestra lo que llega. A partir de ahora la única fuente que
alimenta la interfaz es la base de datos local, y la red pasa a ser un proceso de
actualización que rellena esa base. Es lo que hace posibles RF-06, RF-10 y RF-12
sin condicionales de conectividad repartidos por la app: la pantalla no sabe si
hay red, solo observa datos.

```
                    ┌─────────────── refreshCatalog() ───────────────┐
                    │  1 petición de lista + N de ficha, en paralelo │
                    │  Si TODO llega:  una única transacción Room    │
  DogApiServices ───┤     borra filas source=REMOTE                  │
                    │     inserta las nuevas                         │
                    │  Si algo falla: no se escribe nada             │
                    └───────────────────────┬───────────────────────┘
                                            ▼
   addDog()  ──────────────────────────►  Room (tabla dogs)
   (sin red, filas source=LOCAL)            │
                                            │ Flow
                                            ▼
                             DogRepositoryImpl ──► UseCase ──► ViewModel
                                                                  │ StateFlow
                                                                  ▼
                                                               Compose
```

Cuatro decisiones sostienen el resto:

1. **Una sola tabla para ambos orígenes.** Los perros remotos y los propios viven
   en la misma tabla, distinguidos por una columna `source`. Así la lista, la
   búsqueda y la ficha son una única consulta y no hay que unir dos fuentes en
   memoria ni deduplicar (RF-03, RF-04).
2. **El refresco es todo o nada.** Se descargan la lista y las fichas de todos los
   perros y solo entonces se sustituye el bloque remoto, dentro de una transacción.
   Si falla cualquier pieza, la base queda intacta (RF-12, CA-19).
3. **El refresco solo toca las filas remotas.** El borrado previo a insertar está
   acotado a `source = REMOTE`, de modo que ninguna actualización puede rozar un
   perro propio, ni siquiera si el servidor empieza a usar su mismo identificador
   (RF-05, CA-05).
4. **La foto se copia al almacenamiento interno de la app.** El selector del
   sistema entrega un permiso de lectura temporal sobre una URI ajena; si se
   guardara esa URI, la imagen dejaría de verse más adelante. Copiarla es lo que
   hace cierto «sigue viéndose sin conexión» y «se queda en el dispositivo»
   (RF-09, CA-09).

## Módulos y componentes afectados

<!-- Si el proyecto está modularizado, identifica los módulos afectados, sus
responsabilidades y la dirección de sus dependencias. Respeta los límites
existentes y justifica cualquier módulo o dependencia nueva. Si no está
modularizado, describe las carpetas o componentes afectados sin introducir
modularización fuera del alcance; marca la tabla de módulos como No aplica. -->

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| No aplica | — | El proyecto tiene un único módulo `:app`. No se introduce modularización, que quedaría fuera del alcance de la spec. Los límites que sí se respetan son los de paquete: `presentation -> domain`, `data -> domain` | — |

<!-- Distingue lo que se reutiliza, modifica o crea. Las rutas nuevas son propuestas.
Señala impacto sobre modelos, contratos o componentes compartidos. -->

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `data/api/DogApiServices.kt` | Reutilizar | Sin cambios | RF-08 |
| `data/api/response/*.kt` | Reutilizar | Sin cambios | RF-08 |
| `data/local/DogEntity.kt` | Crear | Fila única para perros remotos y propios: `id` autogenerado, `source`, `remoteId`, `position`, `createdAt`, datos visibles y los tres campos opcionales | RF-02, RF-03, RF-05 |
| `data/local/DogDao.kt` | Crear | Observación de la lista ordenada, lectura por id, alta local y reemplazo transaccional del bloque remoto | RF-03, RF-05, RF-12 |
| `data/local/DogDatabase.kt` | Crear | Base Room en versión 1, con esquema exportado y sin migración destructiva | RF-02 |
| `data/local/DogEntityMapper.kt` | Crear | Entidad a dominio y datos de alta a entidad | RF-01, RF-06 |
| `data/mapper/DogMapper.kt` | Modificar | Pasa de respuesta a entidad remota; conserva la construcción de la URL de imagen con `DogApiConfig.BASE_URL` | RF-08 |
| `data/image/DogImageStore.kt` | Crear | Copia la foto elegida al almacenamiento interno y devuelve su URI; borra la copia si el alta no llega a completarse | RF-09 |
| `data/DogRepositoryImpl.kt` | Modificar | Local-first: observa Room, ejecuta el refresco completo y resuelve el alta | RF-01 a RF-12 |
| `domain/DogRepository.kt` | Modificar | Contrato con observación por `Flow`, refresco con resultado y alta | RF-01, RF-06, RF-08 |
| `domain/model/Dog.kt` | Modificar | `id: Long` e indicador de perro propio para la etiqueta del listado | RF-03 |
| `domain/model/DogDetailModel.kt` | Modificar | `id: Long`; `weight`, `origin` y `temperament` pasan a nullables | RF-07 |
| `domain/model/NewDog.kt` | Crear | Datos con los que se da de alta un perro, incluida la URI de la foto elegida como texto | RF-01 |
| `domain/model/DogValidationError.kt` | Crear | Errores de validación por campo, sin texto de interfaz | RF-01, RF-13 |
| `domain/model/CatalogRefreshResult.kt` | Crear | Resultado del refresco: correcto, sin conexión o error inesperado | RF-10, RF-12 |
| `domain/usecase/GetDogsUseCase.kt` | Modificar | Devuelve `Flow<List<Dog>>` | RF-03, RF-04 |
| `domain/usecase/GetDogDetailUseCase.kt` | Modificar | Lee de la base local, ya no de la red | RF-06, RF-07 |
| `domain/usecase/RefreshCatalogUseCase.kt` | Crear | Dispara el refresco y devuelve su resultado | RF-08, RF-12 |
| `domain/usecase/AddDogUseCase.kt` | Crear | Valida los datos y, si son válidos, da de alta | RF-01 |
| `presentation/list/DogViewModel.kt` | Modificar | Combina el `Flow` de perros con el texto de búsqueda, lanza el refresco al crearse y expone los estados de carga, error y vacío | RF-03, RF-04, RF-08, RF-10 |
| `presentation/list/DogScreen.kt` | Modificar | Botón flotante, etiqueta de perro propio, vacíos diferenciados y aviso de refresco con reintento | RF-03, RF-10, RF-13 |
| `presentation/detail/DogDetailViewModel.kt` | Modificar | Lee por id local; mantiene el estado sellado | RF-06, RF-07 |
| `presentation/detail/DetailScreen.kt` | Modificar | Omite los apartados opcionales vacíos y corrige las descripciones «dog» y «back» | RF-07, RF-13 |
| `presentation/create/AddDogScreen.kt` | Crear | Formulario, selector de fotos, errores por campo y confirmación al salir | RF-01, RF-09, RF-11, RF-13 |
| `presentation/create/AddDogViewModel.kt` | Crear | Estado del formulario, validación, guardado y protección frente a envíos repetidos | RF-01, RF-11 |
| `core/navigation/Routes.kt` | Modificar | `DogDetail` pasa a `id: Long` y se añade la ruta de alta | RF-01 |
| `core/navigation/AppNavigation.kt` | Modificar | Entrada de la pantalla de alta y vuelta al listado tras guardar | RF-01 |
| `core/di/DatabaseModule.kt` | Crear | Base de datos, DAO y almacén de imágenes | RF-02, RF-09 |
| `core/di/RepositoryModule.kt` | Crear | `@Binds` del repositorio a su contrato | — |
| `core/di/DataModule.kt` | Modificar | Deja de construir el repositorio a mano; conserva Json, Retrofit y API | — |
| `app/src/main/res/values/strings.xml` | Modificar | Textos del formulario, de los estados vacíos y de los avisos de error | RF-10, RF-13 |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Modificar | Room, dependencia de test de corrutinas y exportación de esquema | — |

## Datos y contratos

<!-- Completa solo lo aplicable. Si un punto no aplica, indica el motivo. -->

- **Modelos y contratos de entrada y salida:** el contrato de dominio pasa a
  `observeDogs(): Flow<List<Dog>>`, `observeDogDetail(id: Long): Flow<DogDetailModel?>`,
  `suspend refreshCatalog(): CatalogRefreshResult` y `suspend addDog(newDog: NewDog): Long`.
  `NewDog` viaja con la URI de la foto como `String`, para que el dominio no
  conozca `android.net.Uri`; resolverla y copiarla es responsabilidad de la capa
  de datos. La validación devuelve errores tipados (`DogValidationError`), nunca
  textos, para que los mensajes vivan en recursos y el dominio siga sin depender
  de Android (RF-13 y decisión de idiomas de la spec).
- **Identificadores, relaciones y restricciones:** la tabla tiene un `id: Long`
  autogenerado que es la identidad que usan la navegación y la interfaz, más un
  `remoteId: Int?` con el identificador del servidor, nulo en los perros propios.
  Esta separación es la que cumple RF-05 y CA-05: que el servidor publique un
  perro con id 11 no puede colisionar con un perro propio, porque los ids del
  servidor no son la clave primaria. Índice único sobre `(source, remoteId)` para
  evitar remotos duplicados. No hay relaciones entre tablas: una sola tabla.
- **Origen de los datos mostrados y transformaciones:** siempre Room. La cadena es
  respuesta de red → entidad (en el refresco) y entidad → modelo de dominio (en la
  lectura). La URL de la imagen remota se sigue construyendo con
  `DogApiConfig.BASE_URL` en el mapper, como hoy; la de un perro propio es la URI
  `file://` de la copia interna. La interfaz recibe una cadena en ambos casos y
  `AsyncImage` no cambia.
- **Persistencia, consultas y actualizaciones:** la lista se observa con una
  consulta que ordena primero los perros propios, por fecha de alta descendente
  para que el recién creado aparezca arriba (paso 4 del flujo), y después los
  remotos por la posición en que llegaron del servidor (RF-03). El filtrado por
  nombre y raza se mantiene en el ViewModel, combinando el `Flow` con el texto
  buscado: conserva exactamente el comportamiento actual y cubre también a los
  propios (RF-04).
- **Convivencia entre datos locales y remotos:** el refresco ejecuta, dentro de una
  única transacción, el borrado de las filas con `source = REMOTE` y la inserción
  del bloque nuevo. De ahí salen las tres garantías de la spec: los cambios se
  reflejan, los perros retirados del servidor desaparecen (CA-15) y los propios
  quedan intactos (CA-05). La transacción solo se abre si ya se han descargado la
  lista y todas las fichas (CA-19).
- **Compatibilidad y migraciones de datos existentes:** no hay datos previos, es la
  primera versión con base de datos. Se crea en versión 1 con el esquema exportado
  a `app/schemas` y **sin** `fallbackToDestructiveMigration`: con perros propios
  que solo existen en el dispositivo, una migración destructiva significaría
  borrarlos en una futura actualización. Las migraciones posteriores serán
  explícitas.

## Estado, operaciones y errores

<!-- Cómo se implementan los comportamientos aprobados en la spec.
Referencia RF/CA y aplica las consideraciones relevantes de MOBILE_GUIDELINES.md. -->

- **Gestión del estado de interfaz y navegación:** `DogsUiState` sigue siendo data
  class, como exige la convención del proyecto, y suma lo necesario para RF-10:
  si es la primera carga y no hay nada guardado, si el último refresco falló y por
  qué. La distinción entre «todavía no hay perros» y «la búsqueda no encuentra
  nada» se deriva del estado, no de una bandera nueva: el catálogo está vacío si no
  hay perros y no hay texto buscado. La ficha conserva su estado sellado
  Loading/Success/Error. Se añade una ruta de alta a `Routes.kt` y su entrada en
  `AppNavigation.kt`, siguiendo el patrón actual de manipular el back stack.
- **Conservación y restauración del estado:** el formulario vive en el ViewModel,
  que sobrevive al giro y al paso a segundo plano (CA-22) sin necesidad de
  `SavedStateHandle`. Es justo lo que la spec decidió: un formulario a medias **no**
  se recupera tras terminarse el proceso, de modo que no hay nada que guardar en
  disco. La URI que devuelve el selector de fotos se mantiene legible mientras el
  proceso viva, que es exactamente el mismo alcance.
- **Ejecución, concurrencia y cancelación de operaciones:** el refresco pide la
  lista y luego lanza las fichas en paralelo dentro de un `coroutineScope`, de
  modo que el fallo de una cancela las hermanas y propaga el error, que es el
  comportamiento que RF-12 necesita. Todo el trabajo de pantalla cuelga de
  `viewModelScope`. Room y Retrofit ya son seguros desde el hilo principal, así que
  **no** se envuelve cada llamada en `Dispatchers.IO`; el único `withContext(IO)`
  es el de copiar la foto, que sí es E/S de ficheros. Al capturar errores se
  relanza `CancellationException` antes de tratar el resto, y no se usa
  `runCatching` alrededor de llamadas suspend precisamente porque se tragaría esa
  cancelación.
- **Errores, reintentos y prevención de duplicados:** el repositorio traduce el
  fallo de red a `CatalogRefreshResult` distinguiendo ausencia de conexión de error
  inesperado, y el ViewModel lo convierte en un aviso con acción de reintentar que
  no tapa la lista (RF-10, CA-16). Los envíos repetidos se cortan en el ViewModel
  con un indicador de guardado en curso que además inhabilita el botón (CA-11);
  como la validación y el guardado son una sola operación, no hay ventana entre
  ambas.
- **Otras consideraciones mobile aplicables y su solución:** los textos nuevos van
  a `strings.xml`; los campos del formulario llevan etiqueta y los errores se
  asocian al campo mediante semántica de Compose; la etiqueta de perro propio es
  texto, no color; se corrigen las descripciones «dog» y «back» (RF-13, CA-21). El
  selector de fotos del sistema no requiere declarar ni pedir permisos, así que el
  manifiesto no cambia más allá del `INTERNET` que ya tiene (CA-10). Las fotos se
  guardan en almacenamiento interno de la app, que se borra al desinstalar y no es
  accesible para otras apps (privacidad).

## Dependencias y configuración

<!-- Librerías, servicios, permisos o configuración afectados. Verifica compatibilidad
con el proyecto y justifica las incorporaciones. No agregues dependencias por defecto. -->

- **Room 2.8.5** (`androidx.room:room-runtime`, `androidx.room:room-ktx`, y
  `androidx.room:room-compiler` por `ksp`). Es la persistencia exigida por la spec.
  2.8.5 es la última estable publicada en el Maven de Google, comprobado para los
  tres artefactos. `room-ktx` aporta `withTransaction`, que es lo que da la
  atomicidad de RF-12. Se declara en `gradle/libs.versions.toml` como el resto y se
  procesa con KSP, que ya usa el proyecto para Hilt; no se añade kapt.
- **Exportación de esquema:** argumento `room.schemaLocation` de KSP apuntando a
  `app/schemas`. Se prefiere el argumento de KSP a añadir el plugin de Gradle de
  Room para no incorporar un plugin nuevo por algo que se resuelve con una línea.
- **`kotlinx-coroutines-test` 1.10.2**, solo en `testImplementation`. Hace falta
  para probar ViewModels y refresco sin dormir hilos. La versión **no** es la última
  publicada (1.11.0) a propósito: el proyecto ya resuelve corrutinas 1.10.2 por el
  BOM que arrastra Compose, comprobado con `:app:dependencies`, y alinear la
  dependencia de test evita arrastrar el runtime a otra versión.
- **Sin librería de mocking.** El proyecto no tiene ninguna y no se añade: los
  dobles de prueba serán fakes escritos a mano sobre las interfaces propias
  (`DogApiServices`, `DogDao`, `DogRepository`), que además son más estables que un
  mock. Tampoco se añade Turbine ni Robolectric; los `Flow` se prueban recogiendo
  valores y lo que necesita Android va a `androidTest`.
- **Sin dependencias nuevas para la foto.** `ActivityResultContracts.PickVisualMedia`
  viene en `androidx.activity`, que el proyecto ya declara en 1.13.0.
- **Sin permisos nuevos en el manifiesto.** El selector del sistema entrega la
  imagen sin que la app pida acceso al almacenamiento, ni en Android 8 ni en las
  versiones con selector nativo.
- **Coil 3** se reutiliza tal cual: ya carga tanto URLs remotas como URIs `file://`.

## Estrategia de validación

<!-- Una fila por criterio de la spec. Selecciona el método capaz de demostrarlo:
test unitario, integración, UI o prueba manual. No todos requieren todos los métodos.
Identifica tests existentes y separa los nuevos propuestos. Incluye regresiones relevantes.
Una captura aislada no demuestra persistencia ni ausencia de peticiones de red. -->

No existe ningún test de producción: todas las filas son propuestas nuevas. Los
tests JVM cubren la lógica; los instrumentados, lo que exige un dispositivo
(consultas Room, Compose, ciclo de vida); y quedan como manuales los escenarios
que ninguna de las dos cosas demuestra por sí sola.

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Nuevo unitario de `AddDogUseCase` y del ViewModel de alta, con DAO falso | JVM | Salida de `:app:testDebugUnitTest` |
| CA-02 | Nuevo instrumentado de DAO con base real y, además, comprobación manual matando el proceso | Emulador; `adb shell am force-stop` | Salida de la tarea y pasos manuales anotados |
| CA-03 | Nuevo instrumentado de DAO: catálogo remoto más perros propios, sin repeticiones | Emulador, base en memoria | Salida de `:app:connectedDebugAndroidTest` |
| CA-04 | Nuevo unitario del ViewModel de lista, filtrando por nombre y por raza sobre un perro propio | JVM | Salida de `:app:testDebugUnitTest` |
| CA-05 | Nuevo unitario del repositorio con API falsa que devuelve un remoto con el mismo `remoteId` que un perro propio, más instrumentado de DAO | JVM y emulador | Ambas salidas |
| CA-06 | Nuevo unitario de validación por campo y de que no se inserta nada | JVM | Salida de `:app:testDebugUnitTest` |
| CA-07 | Manual: descargar con red, modo avión, matar proceso, reabrir | Dispositivo o emulador con modo avión | Pasos y captura **acompañada** de la confirmación de que el proceso se terminó; la captura sola no lo demuestra |
| CA-08 | Nuevo instrumentado de Compose sobre `DogDetailContent` con opcionales vacíos | Emulador | Salida de `:app:connectedDebugAndroidTest` |
| CA-09 | Manual en modo avión, recorriendo lista y ficha de un perro propio | Dispositivo o emulador | Capturas de ambas pantallas en modo avión |
| CA-10 | Manual sobre instalación limpia, abriendo el selector | Emulador API 26 y uno reciente | Anotación de que no aparece diálogo de permiso y revisión de permisos concedidos en ajustes |
| CA-11 | Nuevo unitario del ViewModel de alta invocando guardar varias veces | JVM | Salida de `:app:testDebugUnitTest` |
| CA-12 | Nuevo unitario del ViewModel de lista: con datos guardados no hay estado de carga inicial | JVM | Salida de `:app:testDebugUnitTest` |
| CA-13 | Manual: descargar sin abrir fichas, modo avión, abrir una ficha remota | Dispositivo o emulador | Captura de la ficha con peso, origen y temperamento en modo avión |
| CA-14 | Nuevo instrumentado de Compose sobre el listado, comprobando orden y etiqueta | Emulador | Salida de `:app:connectedDebugAndroidTest` |
| CA-15 | Nuevo unitario del repositorio con API falsa cuyo catálogo pierde un perro | JVM | Salida de `:app:testDebugUnitTest` |
| CA-16 | Nuevo unitario del ViewModel con refresco fallido, más manual en instalación limpia y modo avión | JVM y emulador | Salida de tests y pasos manuales |
| CA-17 | Nuevo instrumentado de Compose comparando los dos estados vacíos | Emulador | Salida de `:app:connectedDebugAndroidTest` |
| CA-18 | Nuevo instrumentado de Compose sobre el diálogo de confirmación, confirmando y cancelando | Emulador | Salida de `:app:connectedDebugAndroidTest` |
| CA-19 | Nuevo unitario del repositorio con API falsa que falla en una ficha: la base no cambia | JVM | Salida de `:app:testDebugUnitTest` |
| CA-20 | Nuevo unitario de validación con edad 31 y textos por encima del límite | JVM | Salida de `:app:testDebugUnitTest` |
| CA-21 | Manual con TalkBack sobre formulario, error de validación y ficha | Dispositivo real preferible | Grabación o anotación literal de lo que anuncia el lector |
| CA-22 | Nuevo instrumentado de Compose con recreación de actividad, más manual de segundo plano | Emulador | Salida de la tarea y pasos manuales |

**Comprobaciones de regresión:** que el listado sigue mostrando los perros
remotos con su imagen, que la búsqueda por nombre y raza mantiene el mismo
comportamiento sin distinguir mayúsculas, que la ficha de un perro remoto sigue
mostrando los mismos datos, y que la navegación de ida y vuelta entre lista y
ficha no cambia. Atención especial al cambio de `Int` a `Long` en el identificador
de la ruta de ficha, que toca navegación y ambas pantallas.

**Comandos verificados para compilar y ejecutar tests:**
`./gradlew :app:assembleDebug :app:testDebugUnitTest` se ha ejecutado en este
entorno sobre el código actual y termina con `BUILD SUCCESSFUL`. Durante la
implementación se añaden `./gradlew :app:lintDebug` y, con dispositivo conectado,
`./gradlew :app:connectedDebugAndroidTest`, que **no** se han ejecutado todavía.

**Pruebas en dispositivo, emulador o simulador:** los escenarios sin conexión
(CA-07, CA-09, CA-13, CA-16) necesitan modo avión real sobre la app instalada.
CA-02 necesita terminar el proceso, no basta con mandar la app a segundo plano.
CA-10 conviene comprobarlo en un emulador API 26, que es el mínimo declarado y
donde el selector de fotos toma el camino de compatibilidad, y en uno reciente con
selector nativo. CA-21 se comprueba mejor con TalkBack en dispositivo real.

**Limitaciones del entorno:** aquí hay JDK y SDK, y la compilación y los tests JVM
se ejecutan, pero no consta ningún emulador ni dispositivo conectado, así que
`:app:connectedDebugAndroidTest` y todo lo manual quedan pendientes de un entorno
con dispositivo. Además, CA-15 y CA-19 dependen de que el catálogo remoto cambie o
falle, cosa que no controlamos en el servidor real: se cubren con una API falsa en
los tests, y su comprobación manual queda limitada a lo que el servidor permita.

<!-- Esta sección planifica la validación. Durante la implementación, registra
en TASKS.md o en el informe de validación acordado los resultados y evidencias
reales. Distingue pruebas ejecutadas, fallidas, no ejecutadas y bloqueadas.
Compilar o tener tests en verde no sustituye revisar los criterios de la spec. -->

## Orden de implementación

<!-- Etapas y dependencias principales. El desglose ejecutable se escribe en TASKS.md.
Incluye puntos de comprobación para avanzar con cambios pequeños. -->

1. **Dependencias y base de datos vacía.** Añadir Room al catálogo y al módulo,
   configurar la exportación de esquema y crear entidad, DAO y base. *Comprobación:*
   `:app:assembleDebug` pasa y se genera el esquema. Es el punto donde se confirma
   que Room 2.8.5 convive con este KSP y este AGP; si no lo hiciera, se ajusta la
   versión antes de construir nada encima.
2. **Contrato de dominio y repositorio local-first.** Cambiar `DogRepository`, los
   modelos y los casos de uso, y reescribir `DogRepositoryImpl` con el refresco
   transaccional. Sin tocar interfaz todavía. *Comprobación:* tests JVM del
   repositorio (CA-05, CA-15, CA-19) en verde.
3. **Listado y ficha sobre datos locales.** Adaptar ambos ViewModels y ambas
   pantallas, incluidos los estados vacíos y el aviso de refresco. *Comprobación:*
   la app arranca, muestra el catálogo y funciona en modo avión (CA-07, CA-13).
4. **Alta de perros.** Ruta, formulario, selector de fotos, copia al almacenamiento
   interno, validación y confirmación al salir. *Comprobación:* CA-01, CA-06,
   CA-11, CA-18, CA-20.
5. **Orden, etiqueta, accesibilidad y textos.** Ordenación de la lista, etiqueta de
   perro propio, descripciones y paso de los textos nuevos a recursos.
   *Comprobación:* CA-14, CA-17, CA-21.
6. **Validación final.** Ejecutar `:app:assembleDebug`, `:app:testDebugUnitTest`,
   `:app:lintDebug` y, con dispositivo, `:app:connectedDebugAndroidTest`, y recorrer
   los escenarios manuales. *Comprobación:* registrar en TASKS.md el resultado real
   de cada criterio, separando lo ejecutado de lo pendiente.

**Uso de subagentes:** no se plantea delegar. El trabajo es secuencial y casi todo
él pasa por contratos compartidos —modelos de dominio, `DogRepository`, `Routes.kt`
y los módulos de inyección—, de modo que repartirlo en paralelo produciría
conflictos sobre los mismos ficheros en lugar de ahorrar tiempo. Si en la etapa 5
se quisiera paralelizar algo, la única parte razonablemente aislada sería la
revisión de accesibilidad y textos, con un encargo acotado a `DetailScreen.kt`,
`DogScreen.kt` y `strings.xml`, sin tocar dominio ni datos.

## Riesgos y decisiones pendientes

<!-- Riesgos concretos de esta solución y cómo se resolverán, sin listas genéricas.
Escribe Ninguna en las decisiones pendientes cuando estén resueltas. -->

- **Riesgos y medidas acordadas:**
  - *Room 2.8.5 con este stack.* Se ha comprobado que la versión existe y que el
    proyecto compila hoy, pero **no** que ambas cosas convivan: el proyecto usa una
    combinación poco común (AGP 9.3.2, `compileSdk 37`, KSP 2.3.10). Se resuelve en
    la etapa 1, que es deliberadamente lo primero y lo más barato de deshacer; si
    hubiera incompatibilidad, se baja a la 2.8.x que resuelva.
  - *Interceptar el gesto de volver con Navigation3.* La confirmación de RF-11
    depende de que `BackHandler` intercepte el gesto del sistema dentro de
    `NavDisplay`. Es lo esperable, pero no está comprobado en este proyecto. Se
    verifica al principio de la etapa 4 con una pantalla mínima antes de construir
    el formulario entero; si no funcionara, la alternativa es controlar la salida
    desde el propio `entryProvider`.
  - *Selector de fotos en API 26.* En el mínimo declarado no hay selector nativo y
    se usa el camino de compatibilidad. No cambia el permiso —sigue sin hacer
    falta— pero sí la pantalla que ve la persona. Se comprueba en emulador API 26
    (CA-10).
  - *Momento de copiar la foto.* El plan copia al guardar, no al elegir, para no
    dejar ficheros huérfanos si el alta se cancela. A cambio depende de que la URI
    siga siendo legible desde que se elige hasta que se guarda, que es cierto
    mientras viva el proceso y coincide con el alcance que la spec fijó para el
    formulario. Si en pruebas apareciera algún caso de URI caducada, la alternativa
    es copiar al elegir y borrar la copia al cancelar.
  - *Refresco con una petición por perro.* Hoy son once en total y es asumible, tal
    como recoge la spec. Deja de serlo si el catálogo crece mucho; no se optimiza
    ahora, pero conviene no perderlo de vista si el curso amplía el API.
  - *Cambio de `Int` a `Long` en el identificador.* Afecta a rutas y a las dos
    pantallas existentes. El compilador señala todos los puntos, así que el riesgo
    real es bajo, pero es la regresión más probable y por eso está en las
    comprobaciones.
- **Decisiones pendientes:** Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el plan cubre los requisitos, respeta las exclusiones, reutiliza
componentes verificados y permite demostrar todos los criterios de aceptación.
Resuelve dudas y marcadores pendientes. Si la spec cambió, revisa su impacto.
Tras aprobar el plan, deriva TASKS.md con IDs, dependencias, referencias a RF/CA
y comprobaciones. No marques una tarea terminada sin realizar su validación;
si está bloqueada, registra el motivo.
-->
