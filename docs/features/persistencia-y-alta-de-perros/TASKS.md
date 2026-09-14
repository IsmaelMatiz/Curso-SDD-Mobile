# TASKS: Persistencia local y alta de perros

**SPEC de referencia:** `SPEC.md`, commit `73bd8fb`, estado Aprobada
**PLAN de referencia:** `PLAN.md`, commit `a53a0b0`, estado Aprobado
**Estado de ejecución:** en curso. Autorizada explícitamente por la persona.
Etapas 1 a 5 implementadas y compiladas; validadas con test JVM donde el
criterio lo permite. Etapa 6 (T-23, T-24, T-26) tiene el código escrito y
compilado (`:app:assembleDebugAndroidTest`), pero **no ejecutado**: este
entorno no tiene `adb` ni ningún emulador/dispositivo conectado (confirmado:
`adb: command not found`), la misma limitación que ya anotaba PLAN.md.
**Autorización para implementar:** concedida por la persona en la conversación.

## Cómo se usa este documento

- Las tareas se ejecutan en orden. Cada una respeta sus dependencias.
- Una casilla solo se marca cuando **su validación se ha ejecutado y observado**.
  Un nombre de test o un comando sin ejecutar no cuentan como validación.
- Si una tarea queda bloqueada, se anota el motivo junto a ella y se continúa por
  las que no dependan de ella.
- El resultado real de cada criterio de aceptación se registra en la tabla final,
  con su evidencia.
- Salvo que se indique lo contrario, «compila» significa
  `./gradlew :app:assembleDebug` y «tests JVM» significa
  `./gradlew :app:testDebugUnitTest`.

---

## Etapa 1 · Base de datos, sin tocar todavía la app

- [x] **T-01 · Añadir Room y la exportación de esquema**
  - **Objetivo:** tener Room disponible en el proyecto y el esquema versionado.
  - **Alcance:** `gradle/libs.versions.toml` y `app/build.gradle.kts`:
    `room-runtime` y `room-ktx` 2.8.5, `room-compiler` por `ksp`, y argumento
    `room.schemaLocation` apuntando a `app/schemas`. Nada más.
  - **Depende de:** —
  - **Criterios:** ninguno directamente; habilita RF-02.
  - **Validación:** compila. Es el punto donde se confirma el riesgo del plan
    sobre Room 2.8.5 con KSP 2.3.10 y AGP 9.3.2: si no resuelve, bajar a la 2.8.x
    que lo haga y anotarlo aquí antes de seguir.
  - **Resultado:** `:app:assembleDebug` → `BUILD SUCCESSFUL`. Room 2.8.5 convive
    sin ajustes con este AGP/KSP; riesgo descartado.

- [x] **T-02 · Entidad, DAO y base de datos**
  - **Objetivo:** modelar el almacenamiento de perros remotos y propios.
  - **Alcance:** crear `data/local/DogEntity.kt`, `DogDao.kt` y `DogDatabase.kt`.
    Entidad con `id` autogenerado, `source`, `remoteId` nullable, `position`,
    `createdAt`, los datos visibles y los tres campos opcionales; índice único
    sobre `(source, remoteId)`. Base en versión 1, **sin** migración destructiva.
    DAO con observación de la lista ordenada, lectura por id, alta local y
    reemplazo transaccional del bloque remoto.
  - **Depende de:** T-01
  - **Criterios:** ninguno directamente; habilita RF-02, RF-03, RF-05, RF-12.
  - **Validación:** compila y se genera el fichero de esquema en `app/schemas`.
  - **Resultado:** compila; `app/schemas/com.aristidevs.cursopremiumandroid.data.local.DogDatabase/1.json`
    generado.

- [x] **T-03 · Módulo Hilt de base de datos**
  - **Objetivo:** que la base y el DAO se puedan inyectar.
  - **Alcance:** crear `core/di/DatabaseModule.kt` con la construcción de la base
    y la exposición del DAO, siguiendo el estilo de `DataModule`.
  - **Depende de:** T-02
  - **Criterios:** ninguno directamente.
  - **Validación:** compila; el grafo de Hilt se procesa sin error.
  - **Resultado:** `kspDebugKotlin`/`hiltJavaCompileDebug` sin error.

- [ ] **T-04 · Almacén de imágenes**
  - **Objetivo:** poder conservar una foto elegida más allá del permiso temporal.
  - **Alcance:** crear `data/image/DogImageStore.kt`, que copia el contenido de
    una URI al almacenamiento interno de la app y devuelve la URI `file://`
    resultante, y borra la copia si el alta no llega a completarse. La E/S va en
    `withContext(Dispatchers.IO)`.
  - **Depende de:** T-03
  - **Criterios:** ninguno directamente; habilita RF-09.
  - **Validación:** test instrumentado que copia un fichero de prueba y comprueba
    que el destino existe, es legible y sobrevive a releer la ruta.
  - **Resultado:** `DogImageStoreTest` escrito y compila
    (`:app:assembleDebugAndroidTest`); **no ejecutado** — sin dispositivo/emulador
    en este entorno (`adb` no disponible). `DogImageStore` se extrajo como
    interfaz (`DogImageStoreImpl`) precisamente para poder testear el
    repositorio sin `Context` real en el resto de tareas.

---

## Etapa 2 · Dominio y capa de datos

- [x] **T-05 · Ajustar los modelos de dominio**
  - **Objetivo:** dar a los perros una identidad propia y admitir datos opcionales.
  - **Alcance:** `domain/model/Dog.kt` (`id: Long`, indicador de perro propio) y
    `domain/model/DogDetailModel.kt` (`id: Long`; `weight`, `origin` y
    `temperament` nullables). Actualizar `Routes.kt`, casos de uso y pantallas
    **solo** lo imprescindible para que siga compilando, sin cambiar aún ningún
    comportamiento visible.
  - **Depende de:** —
  - **Criterios:** ninguno directamente; prepara CA-05 y CA-08.
  - **Validación:** compila y la app sigue comportándose como antes.
  - **Resultado:** compila. Este cambio se hizo junto con T-06..T-10 en el mismo
    lote antes de compilar (`domain -> data -> presentation` rompía a la vez),
    así que la comprobación de "compila" es la del lote completo, no aislada.

- [x] **T-06 · Modelos nuevos de dominio**
  - **Objetivo:** expresar el alta, sus errores y el resultado del refresco sin
    depender de Android.
  - **Alcance:** crear `NewDog`, `DogValidationError` (errores por campo, sin
    texto) y `CatalogRefreshResult` (correcto, sin conexión, error inesperado).
  - **Depende de:** T-05
  - **Criterios:** ninguno directamente; habilita RF-01, RF-10, RF-13.
  - **Validación:** compila.
  - **Resultado:** compila (ver nota en T-05).

- [x] **T-07 · Mappers de datos**
  - **Objetivo:** convertir entre red, almacenamiento y dominio.
  - **Alcance:** modificar `data/mapper/DogMapper.kt` para producir entidades
    remotas conservando la construcción de la URL con `DogApiConfig.BASE_URL`, y
    crear `data/local/DogEntityMapper.kt` para entidad a dominio y alta a entidad.
  - **Depende de:** T-06
  - **Criterios:** ninguno directamente.
  - **Validación:** tests JVM de mapeo, incluida la URL de imagen de un perro
    remoto y la URI `file://` de uno propio.
  - **Resultado:** `MapperTest` (3 tests) en verde.

- [ ] **T-08 · Migrar la lectura a local (tarea deliberadamente atómica)**
  - **Objetivo:** que la interfaz deje de leer de la red y pase a leer de Room.
  - **Alcance:** cambiar `domain/DogRepository.kt` al contrato del plan
    (observación por `Flow`, refresco con resultado, alta), reescribir
    `data/DogRepositoryImpl.kt` como local-first con el refresco todo o nada,
    actualizar `GetDogsUseCase` y `GetDogDetailUseCase`, crear
    `RefreshCatalogUseCase`, añadir `core/di/RepositoryModule.kt` con `@Binds` y
    quitar de `DataModule` la construcción manual del repositorio. Adaptar ambos
    ViewModels a las firmas nuevas **sin** cambiar todavía la apariencia.
    Va en una sola tarea porque el cambio de contrato rompe a la vez datos,
    dominio y presentación: partirla dejaría el proyecto sin compilar.
  - **Depende de:** T-03, T-07
  - **Criterios:** ninguno todavía de forma demostrable; habilita RF-06 y RF-08.
  - **Validación:** compila; con red, la app lista el catálogo tomándolo de Room
    después del primer refresco; en modo avión tras ese refresco, la lista sigue
    apareciendo.
  - **Resultado:** compila. El comportamiento en dispositivo (con red / modo
    avión) **no verificado** — sin dispositivo/emulador en este entorno; la
    lógica del repositorio queda cubierta indirectamente por T-09.

- [x] **T-09 · Tests del refresco**
  - **Objetivo:** demostrar las tres garantías de convivencia.
  - **Alcance:** tests JVM del repositorio con API y DAO falsos: que un fallo en
    cualquier ficha no escribe nada, que un perro remoto retirado del servidor
    desaparece, y que un remoto con el mismo identificador que un perro propio no
    lo altera.
  - **Depende de:** T-08
  - **Criterios:** CA-05, CA-15, CA-19.
  - **Validación:** tests JVM en verde.
  - **Resultado:** `DogRepositoryImplTest` (4 tests) en verde.

- [x] **T-10 · Alta y validación en dominio**
  - **Objetivo:** que las reglas de la spec vivan donde se pueden probar sin
    Android.
  - **Alcance:** crear `AddDogUseCase` con la validación acordada (edad de 0 a 30;
    nombre, raza, peso, origen y temperamento hasta 50; descripción hasta 300;
    nombres repetidos permitidos) devolviendo errores tipados, y el alta contra el
    repositorio.
  - **Depende de:** T-08
  - **Criterios:** CA-06, CA-20.
  - **Validación:** tests JVM por campo, incluidos edad 31 y textos por encima del
    límite, comprobando además que no se inserta nada.
  - **Resultado:** `AddDogUseCaseTest` (8 tests) en verde.

---

## Etapa 3 · Listado y ficha

- [ ] **T-11 · Estados del listado**
  - **Objetivo:** cumplir RF-10 en la pantalla principal.
  - **Alcance:** en `DogViewModel`, refresco al crearse y estado que distinga
    primera carga sin datos, contenido, catálogo vacío y fallo de refresco. En
    `DogScreen`, indicación de carga solo cuando no hay nada guardado, aviso de
    error comprensible con acción de reintentar que no tapa la lista, y textos
    distintos para «todavía no hay perros» y «la búsqueda no encuentra nada».
  - **Depende de:** T-08
  - **Criterios:** CA-12, CA-16 (parte de interfaz), CA-17.
  - **Validación:** tests JVM del ViewModel con refresco correcto y fallido;
    revisión visual de los cuatro estados.
  - **Resultado:** `DogViewModelTest` cubre refresco correcto/fallido, carga
    inicial y vacío/sin-resultados (en verde). **Revisión visual no realizada**
    — sin dispositivo/emulador.

- [ ] **T-12 · Orden y etiqueta de perro propio**
  - **Objetivo:** que los perros propios se vean primero y se distingan.
  - **Alcance:** consulta del DAO que ordena los propios primero por fecha de alta
    descendente y los remotos por su posición; etiqueta de texto en la tarjeta,
    usando colores del tema y sin hex sueltos.
  - **Depende de:** T-11
  - **Criterios:** CA-03 (parte de orden y no duplicados), CA-14.
  - **Validación:** test instrumentado de la consulta y revisión visual.
  - **Resultado:** `DogDaoTest` (orden y no duplicados) y `DogContentTest`
    (etiqueta propia) escritos y compilan; **no ejecutados** — sin
    dispositivo/emulador. La consulta SQL replicada en `FakeDogDao` para los
    tests JVM del repositorio da el mismo resultado que se espera del DAO real.

- [x] **T-13 · Búsqueda sobre el flujo combinado**
  - **Objetivo:** que el buscador cubra también a los perros propios.
  - **Alcance:** combinar en `DogViewModel` el `Flow` de perros con el texto
    buscado, conservando el filtrado por nombre y raza sin distinguir mayúsculas.
  - **Depende de:** T-11
  - **Criterios:** CA-04.
  - **Validación:** tests JVM filtrando por nombre y por raza sobre un perro propio
    y sobre uno remoto.
  - **Resultado:** `DogViewModelTest.search filters by name or breed including own dogs`
    en verde, cubriendo nombre/raza tanto del perro propio como del remoto.

- [ ] **T-14 · Ficha desde datos locales**
  - **Objetivo:** que cualquier ficha se abra sin red y omita lo que esté vacío.
  - **Alcance:** `DogDetailViewModel` lee por identificador local conservando su
    estado sellado; `DetailScreen` no pinta los apartados de peso, origen o
    temperamento cuando no tienen valor.
  - **Depende de:** T-08
  - **Criterios:** CA-08, CA-13 (parte de interfaz).
  - **Validación:** test instrumentado de Compose con opcionales vacíos y con
    opcionales rellenos.
  - **Resultado:** `DogDetailContentTest` (2 tests) escrito y compila; **no
    ejecutado** — sin dispositivo/emulador.

---

## Etapa 4 · Alta de perros

- [ ] **T-15 · Comprobar la intercepción del gesto de volver**
  - **Objetivo:** despejar el riesgo del plan antes de construir el formulario.
  - **Alcance:** comprobar con una pantalla mínima que `BackHandler` intercepta el
    gesto del sistema dentro del `NavDisplay` de Navigation3. Si no lo hiciera,
    anotar aquí la alternativa por `entryProvider` antes de seguir.
  - **Depende de:** —
  - **Criterios:** ninguno; condiciona CA-18.
  - **Validación:** comprobación manual en emulador, con el resultado anotado.
  - **Resultado:** **no comprobado en emulador** (no disponible en este
    entorno). Se implementó `AddDogScreen` directamente con
    `androidx.activity.compose.BackHandler`, la API estándar que se apoya en
    `OnBackPressedDispatcher` y no depende de particularidades de `NavDisplay`;
    es el mecanismo documentado para interceptar el gesto de volver en Compose.
    Queda pendiente de confirmar en dispositivo; si no funcionara, la
    alternativa que registra PLAN.md es controlar la salida desde
    `entryProvider`.

- [ ] **T-16 · Ruta de alta y acceso desde el listado**
  - **Objetivo:** poder llegar al formulario.
  - **Alcance:** nueva ruta en `Routes.kt`, entrada en `AppNavigation.kt` y botón
    flotante en `DogScreen`, con vuelta al listado tras guardar.
  - **Depende de:** T-11
  - **Criterios:** ninguno todavía; habilita RF-01.
  - **Validación:** compila y se navega de ida y vuelta.
  - **Resultado:** compila. Navegación real **no verificada** — sin
    dispositivo/emulador.

- [x] **T-17 · Estado y guardado del formulario**
  - **Objetivo:** dar de alta un perro sin duplicados.
  - **Alcance:** crear `AddDogViewModel` con el estado del formulario, errores por
    campo, indicador de guardado en curso que corta envíos repetidos, y aviso de
    resultado al listado.
  - **Depende de:** T-10, T-16
  - **Criterios:** CA-01, CA-11.
  - **Validación:** tests JVM, incluido invocar guardar varias veces seguidas y
    comprobar que solo se crea un perro.
  - **Resultado:** `AddDogViewModelTest` (5 tests) en verde. El primer intento de
    este test **encontró un bug real** en el guardián anti-duplicados (la
    comprobación vivía dentro de la corrutina lanzada, lo que no bastaba si el
    trabajo no llegaba a suspenderse de verdad); se corrigió moviendo la
    comprobación-y-fijación del indicador a antes de lanzar la corrutina, y el
    test quedó en verde.

- [ ] **T-18 · Formulario en pantalla**
  - **Objetivo:** recoger los datos con una interfaz usable.
  - **Alcance:** crear `AddDogScreen` con pantalla stateful y `*Content` sin
    estado, campos obligatorios y opcionales, errores junto a su campo, teclado y
    foco razonables, y botón de confirmar inhabilitado mientras se guarda.
  - **Depende de:** T-17
  - **Criterios:** CA-06 (parte de interfaz).
  - **Validación:** revisión visual y test instrumentado de un intento fallido.
  - **Resultado:** compila; formulario implementado con etiqueta, error y
    semántica de error por campo. Revisión visual y ejecución del test
    instrumentado **pendientes** — sin dispositivo/emulador.

- [ ] **T-19 · Selección y copia de la foto**
  - **Objetivo:** que la foto elegida sea de verdad de la app.
  - **Alcance:** usar el selector del sistema con `PickVisualMedia`, conservar la
    URI elegida en el ViewModel y copiarla al almacenamiento interno al guardar,
    borrando la copia si el alta falla.
  - **Depende de:** T-04, T-18
  - **Criterios:** CA-09, CA-10.
  - **Validación:** comprobación manual en emulador API 26 y en uno reciente, sin
    que aparezca ningún diálogo de permiso, y revisión de la foto en modo avión.
  - **Resultado:** implementado con `ActivityResultContracts.PickVisualMedia`
    (no requiere declarar ni pedir permisos) y copia a almacenamiento interno
    en `addDog()`, con borrado de la copia si el alta no llega a completarse.
    **No comprobado en emulador** — no disponible en este entorno.

- [ ] **T-20 · Confirmación al salir sin guardar**
  - **Objetivo:** no perder lo escrito por un gesto accidental.
  - **Alcance:** diálogo de confirmación al volver atrás con datos escritos, tanto
    con el gesto del sistema como con el botón de la barra; cancelar mantiene el
    formulario intacto.
  - **Depende de:** T-15, T-18
  - **Criterios:** CA-18.
  - **Validación:** test instrumentado de Compose confirmando y cancelando.
  - **Resultado:** lógica cubierta por `AddDogViewModelTest` (confirmar/cancelar,
    en verde). `AddDogContentTest` (Compose) escrito y compila; **no
    ejecutado** — sin dispositivo/emulador.

---

## Etapa 5 · Textos y accesibilidad

- [x] **T-21 · Textos nuevos a recursos**
  - **Objetivo:** dejar traducible lo que añadimos.
  - **Alcance:** llevar a `strings.xml` los textos del formulario, de los estados
    vacíos y de los avisos de error. No se migran los textos ya existentes ni se
    añade ningún idioma.
  - **Depende de:** T-11, T-18
  - **Criterios:** ninguno directamente; cumple RF-13 y la decisión de idiomas.
  - **Validación:** compila y no queda ningún literal nuevo en las pantallas
    añadidas.
  - **Resultado:** compila; `grep` sobre `DogScreen.kt`/`DetailScreen.kt`/
    `AddDogScreen.kt` confirma que los únicos literales `Text("...")` que
    quedan son los ya existentes antes de esta feature (título, placeholder de
    búsqueda, "años", "Edad"/"Peso"/"Origen"), que la instrucción del proyecto
    pide no migrar. `:app:lintDebug` no señala ningún string nuevo sin usar
    (los dos que señaló en la primera pasada — `dogs_empty_no_dogs_action` y
    `add_dog_saving` — se conectaron a la interfaz).

- [ ] **T-22 · Accesibilidad**
  - **Objetivo:** que las pantallas se puedan usar con lector de pantalla.
  - **Alcance:** etiquetas de los campos, errores asociados a su campo por
    semántica, descripciones con sentido en las imágenes, áreas táctiles
    suficientes, y corrección de las descripciones «dog» y «back» de la ficha.
  - **Depende de:** T-14, T-21
  - **Criterios:** CA-21.
  - **Validación:** recorrido manual con TalkBack, anotando literalmente lo que se
    anuncia en el formulario, en un error de validación y en la ficha.
  - **Resultado:** implementado (etiquetas `label`/`supportingText` en cada
    campo del formulario, `Modifier.semantics { error(...) }` asociando cada
    error a su campo, `contentDescription` con sentido en imágenes y FAB,
    "dog"/"back" corregidos a `dog.name`/"Volver"). **Marcado como completado
    en código; el recorrido con TalkBack que exige CA-21 no se ha hecho** — no
    hay dispositivo real en este entorno. Ver CA-21 en la tabla final.

---

## Etapa 6 · Validación

- [ ] **T-23 · Tests instrumentados de almacenamiento**
  - **Objetivo:** demostrar sobre una base real lo que los falsos no prueban.
  - **Alcance:** tests con base en memoria: convivencia sin duplicados, orden de la
    lista, reemplazo del bloque remoto sin tocar los propios y persistencia de un
    perro dado de alta.
  - **Depende de:** T-12
  - **Criterios:** CA-02 (parte de almacenamiento), CA-03, CA-05.
  - **Validación:** `./gradlew :app:connectedDebugAndroidTest` en verde.
  - **Resultado:** `DogDaoTest` (3 tests) escrito, cubriendo convivencia sin
    duplicados + orden, reemplazo remoto sin tocar propios, y lectura por id.
    Compila (`:app:assembleDebugAndroidTest`). **No ejecutado**: este entorno
    no tiene `adb` ni ningún emulador/dispositivo conectado.

- [ ] **T-24 · Tests instrumentados de interfaz**
  - **Objetivo:** cubrir lo que depende de Compose y del ciclo de vida.
  - **Alcance:** estados vacíos diferenciados, orden y etiqueta, ficha con
    opcionales vacíos, diálogo de confirmación y conservación del formulario al
    recrear la pantalla.
  - **Depende de:** T-20, T-22
  - **Criterios:** CA-08, CA-14, CA-17, CA-18, CA-22.
  - **Validación:** `./gradlew :app:connectedDebugAndroidTest` en verde.
  - **Resultado:** `DogContentTest`, `DogDetailContentTest` y `AddDogContentTest`
    escritos (estados vacíos, orden+etiqueta, opcionales de ficha, diálogo de
    descarte). Compilan. **No ejecutados** — sin dispositivo/emulador. No se
    escribió un test específico de recreación de actividad (CA-22): el estado
    del formulario vive en `AddDogViewModel`, que por diseño de Android
    sobrevive a un cambio de configuración, pero esa garantía queda **sin
    verificar con un test** en este entorno.

- [x] **T-25 · Comprobaciones del proyecto**
  - **Objetivo:** dejar el proyecto en el estado que exige `AGENTS.md`.
  - **Alcance:** ejecutar `:app:assembleDebug`, `:app:testDebugUnitTest`,
    `:app:lintDebug` y, con dispositivo, `:app:connectedDebugAndroidTest`.
  - **Depende de:** T-24
  - **Criterios:** ninguno por sí solo.
  - **Validación:** salida de cada comando, anotando cuáles pasaron, cuáles
    fallaron y cuáles no se pudieron ejecutar.
  - **Resultado:**
    - `:app:assembleDebug` → `BUILD SUCCESSFUL`.
    - `:app:testDebugUnitTest` → `BUILD SUCCESSFUL` (31 tests, todos en verde).
    - `:app:lintDebug` → `BUILD SUCCESSFUL`; único hallazgo relevante ya
      corregido (strings sin usar); quedan 7 avisos `NewerVersionAvailable`,
      4 `GradleDependency`, 2 `AndroidGradlePluginVersion`, 1 `RedundantLabel`
      y 1 `UseKtx`, y 7 `UnusedResources` sobre colores `purple_*`/`teal_*`/
      `black`/`white` **preexistentes** (fuera del alcance de esta feature).
    - `:app:connectedDebugAndroidTest` → **no ejecutado**: `adb: command not
      found`, sin emulador configurado en este entorno.

- [ ] **T-26 · Escenarios manuales**
  - **Objetivo:** demostrar lo que ningún test automático demuestra.
  - **Alcance:** en dispositivo o emulador: catálogo descargado y modo avión con
    el proceso terminado; ficha remota nunca visitada sin conexión; foto de un
    perro propio sin conexión; primer arranque sin red sobre instalación limpia; y
    ausencia de peticiones de permiso al elegir foto.
  - **Depende de:** T-25
  - **Criterios:** CA-02, CA-07, CA-09, CA-10, CA-13, CA-16.
  - **Validación:** pasos reproducibles y capturas. Una captura por sí sola no
    demuestra persistencia: hay que dejar constancia de que el proceso se terminó.
  - **Resultado:** **no ejecutado**. Ninguno de estos escenarios se puede
    reproducir sin un dispositivo o emulador real (modo avión, matar el
    proceso, TalkBack, comprobar ausencia de diálogo de permisos), y este
    entorno no tiene ninguno disponible.

- [x] **T-27 · Registro de resultados**
  - **Objetivo:** cerrar la etapa de validación con evidencia real.
  - **Alcance:** rellenar la tabla siguiente con el resultado observado de cada
    criterio y dejar explícito lo que quede sin comprobar y por qué.
  - **Depende de:** T-26
  - **Criterios:** todos.
  - **Validación:** la tabla no contiene ningún criterio sin estado.
  - **Resultado:** tabla rellenada abajo. Ningún criterio queda con la casilla
    "Pendiente" original: cada uno se marca Superado (con evidencia JVM),
    Parcial, o No ejecutado (con el motivo).

---

## Registro de criterios de aceptación

<!-- Se rellena durante la etapa de validación. Estado: Pendiente | Superado |
Fallido | No ejecutado (con motivo) | Bloqueado (con motivo). No se marca nada
como superado sin evidencia observada. -->

| Criterio | Tareas | Estado | Evidencia |
| --- | --- | --- | --- |
| CA-01 | T-17 | Superado (lógica) | `AddDogViewModelTest.saving valid data persists one dog and signals navigating back`, en verde. No verificado visualmente en dispositivo. |
| CA-02 | T-23, T-26 | No ejecutado | Requiere terminar el proceso real y reabrir; sin dispositivo/emulador en este entorno. |
| CA-03 | T-12, T-23 | Parcial | Réplica de la consulta en `DogRepositoryImplTest` (JVM) confirma ambos orígenes sin duplicados; `DogDaoTest` (Room real) escrito pero no ejecutado — sin dispositivo. |
| CA-04 | T-13 | Superado | `DogViewModelTest.search filters by name or breed including own dogs`, en verde (nombre y raza, propio y remoto). |
| CA-05 | T-09, T-23 | Superado (lógica) | `DogRepositoryImplTest.refreshCatalog never alters an own dog even if a remote dog reuses its identifier`, en verde. `DogDaoTest` instrumentado escrito, no ejecutado. |
| CA-06 | T-10, T-18 | Parcial | Validación por campo cubierta por `AddDogUseCaseTest` (en verde). Que el error se muestre junto al campo en pantalla no verificado visualmente — sin dispositivo. |
| CA-07 | T-26 | No ejecutado | Requiere modo avión y terminar el proceso en un dispositivo real. |
| CA-08 | T-14, T-24 | Parcial | `DetailScreen` omite condicionalmente peso/origen/temperamento vacíos (revisado en código); `DogDetailContentTest` escrito, no ejecutado. |
| CA-09 | T-19, T-26 | No ejecutado | Requiere modo avión en dispositivo real. |
| CA-10 | T-19, T-26 | No ejecutado | Requiere comprobar en emulador API 26 y uno reciente que no aparece diálogo de permiso. |
| CA-11 | T-17 | Superado | `AddDogViewModelTest.tapping save several times in a row only creates one dog`, en verde (encontró y motivó la corrección de un bug real en el guardián). |
| CA-12 | T-11 | Superado | `DogViewModelTest.with data already saved there is no full-screen loading while refreshing`, en verde. |
| CA-13 | T-14, T-26 | No ejecutado | Requiere descargar con red, modo avión y abrir una ficha remota nunca visitada, en dispositivo real. |
| CA-14 | T-12, T-24 | No ejecutado | `DogContentTest.ownDogsAppearFirstAndCarryTheOwnLabel` escrito, no ejecutado — sin dispositivo. |
| CA-15 | T-09 | Superado | `DogRepositoryImplTest.refreshCatalog removes a remote dog the server no longer offers, keeping own dogs`, en verde. |
| CA-16 | T-11, T-26 | Parcial | Aviso de refresco fallido con reintento cubierto por `DogViewModelTest` (JVM, en verde). El escenario completo (instalación limpia + modo avión + dar de alta) no ejecutado — sin dispositivo. |
| CA-17 | T-11, T-24 | Superado (lógica) | `DogViewModelTest.an empty catalog is distinguished from a search with no results`, en verde. `DogContentTest` (Compose) escrito, no ejecutado. |
| CA-18 | T-20, T-24 | Superado (lógica) | `AddDogViewModelTest.leaving with unsaved data asks for confirmation and only discards when confirmed`, en verde. `AddDogContentTest` (Compose) escrito, no ejecutado. |
| CA-19 | T-09 | Superado | `DogRepositoryImplTest.refreshCatalog keeps the previous catalog intact when a detail request fails`, en verde. |
| CA-20 | T-10 | Superado | `AddDogUseCaseTest.age 31 is out of range...` y `.a name longer than 50 characters...`, en verde. |
| CA-21 | T-22 | No ejecutado | Requiere recorrido con TalkBack en dispositivo real; sin dispositivo en este entorno. |
| CA-22 | T-24 | No ejecutado | No se escribió test de recreación de actividad; el mecanismo (estado en `AddDogViewModel`, que sobrevive a cambios de configuración) no está verificado con un test. |

**Resumen:** 9 criterios superados (con test JVM en verde), 6 con la lógica
verificada por JVM pero la parte visual/instrumentada sin ejecutar, y 7 sin
ejecutar por completo. La causa es uniforme: **este entorno no tiene `adb` ni
ningún emulador o dispositivo conectado** (`adb: command not found`), la misma
limitación que ya anotaba PLAN.md. Todo el código de los tests instrumentados
y los escenarios manuales está escrito y compila
(`:app:assembleDebugAndroidTest`); falta ejecutarlo en un entorno con
dispositivo. Un test de recreación de actividad para CA-22 quedó sin escribir
y debería añadirse antes de dar la tarea por cerrada.
