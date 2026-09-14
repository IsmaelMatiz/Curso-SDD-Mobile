# TASKS: Persistencia local y alta de perros

**SPEC de referencia:** `SPEC.md`, commit `73bd8fb`, estado Aprobada
**PLAN de referencia:** `PLAN.md`, commit `a53a0b0`, estado Aprobado
**Estado de ejecución:** sin empezar
**Autorización para implementar:** pendiente. Los documentos aprobados no la
implican; hace falta que la persona la dé de forma explícita.

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

- [ ] **T-01 · Añadir Room y la exportación de esquema**
  - **Objetivo:** tener Room disponible en el proyecto y el esquema versionado.
  - **Alcance:** `gradle/libs.versions.toml` y `app/build.gradle.kts`:
    `room-runtime` y `room-ktx` 2.8.5, `room-compiler` por `ksp`, y argumento
    `room.schemaLocation` apuntando a `app/schemas`. Nada más.
  - **Depende de:** —
  - **Criterios:** ninguno directamente; habilita RF-02.
  - **Validación:** compila. Es el punto donde se confirma el riesgo del plan
    sobre Room 2.8.5 con KSP 2.3.10 y AGP 9.3.2: si no resuelve, bajar a la 2.8.x
    que lo haga y anotarlo aquí antes de seguir.

- [ ] **T-02 · Entidad, DAO y base de datos**
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

- [ ] **T-03 · Módulo Hilt de base de datos**
  - **Objetivo:** que la base y el DAO se puedan inyectar.
  - **Alcance:** crear `core/di/DatabaseModule.kt` con la construcción de la base
    y la exposición del DAO, siguiendo el estilo de `DataModule`.
  - **Depende de:** T-02
  - **Criterios:** ninguno directamente.
  - **Validación:** compila; el grafo de Hilt se procesa sin error.

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

---

## Etapa 2 · Dominio y capa de datos

- [ ] **T-05 · Ajustar los modelos de dominio**
  - **Objetivo:** dar a los perros una identidad propia y admitir datos opcionales.
  - **Alcance:** `domain/model/Dog.kt` (`id: Long`, indicador de perro propio) y
    `domain/model/DogDetailModel.kt` (`id: Long`; `weight`, `origin` y
    `temperament` nullables). Actualizar `Routes.kt`, casos de uso y pantallas
    **solo** lo imprescindible para que siga compilando, sin cambiar aún ningún
    comportamiento visible.
  - **Depende de:** —
  - **Criterios:** ninguno directamente; prepara CA-05 y CA-08.
  - **Validación:** compila y la app sigue comportándose como antes.

- [ ] **T-06 · Modelos nuevos de dominio**
  - **Objetivo:** expresar el alta, sus errores y el resultado del refresco sin
    depender de Android.
  - **Alcance:** crear `NewDog`, `DogValidationError` (errores por campo, sin
    texto) y `CatalogRefreshResult` (correcto, sin conexión, error inesperado).
  - **Depende de:** T-05
  - **Criterios:** ninguno directamente; habilita RF-01, RF-10, RF-13.
  - **Validación:** compila.

- [ ] **T-07 · Mappers de datos**
  - **Objetivo:** convertir entre red, almacenamiento y dominio.
  - **Alcance:** modificar `data/mapper/DogMapper.kt` para producir entidades
    remotas conservando la construcción de la URL con `DogApiConfig.BASE_URL`, y
    crear `data/local/DogEntityMapper.kt` para entidad a dominio y alta a entidad.
  - **Depende de:** T-06
  - **Criterios:** ninguno directamente.
  - **Validación:** tests JVM de mapeo, incluida la URL de imagen de un perro
    remoto y la URI `file://` de uno propio.

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

- [ ] **T-09 · Tests del refresco**
  - **Objetivo:** demostrar las tres garantías de convivencia.
  - **Alcance:** tests JVM del repositorio con API y DAO falsos: que un fallo en
    cualquier ficha no escribe nada, que un perro remoto retirado del servidor
    desaparece, y que un remoto con el mismo identificador que un perro propio no
    lo altera.
  - **Depende de:** T-08
  - **Criterios:** CA-05, CA-15, CA-19.
  - **Validación:** tests JVM en verde.

- [ ] **T-10 · Alta y validación en dominio**
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

- [ ] **T-12 · Orden y etiqueta de perro propio**
  - **Objetivo:** que los perros propios se vean primero y se distingan.
  - **Alcance:** consulta del DAO que ordena los propios primero por fecha de alta
    descendente y los remotos por su posición; etiqueta de texto en la tarjeta,
    usando colores del tema y sin hex sueltos.
  - **Depende de:** T-11
  - **Criterios:** CA-03 (parte de orden y no duplicados), CA-14.
  - **Validación:** test instrumentado de la consulta y revisión visual.

- [ ] **T-13 · Búsqueda sobre el flujo combinado**
  - **Objetivo:** que el buscador cubra también a los perros propios.
  - **Alcance:** combinar en `DogViewModel` el `Flow` de perros con el texto
    buscado, conservando el filtrado por nombre y raza sin distinguir mayúsculas.
  - **Depende de:** T-11
  - **Criterios:** CA-04.
  - **Validación:** tests JVM filtrando por nombre y por raza sobre un perro propio
    y sobre uno remoto.

- [ ] **T-14 · Ficha desde datos locales**
  - **Objetivo:** que cualquier ficha se abra sin red y omita lo que esté vacío.
  - **Alcance:** `DogDetailViewModel` lee por identificador local conservando su
    estado sellado; `DetailScreen` no pinta los apartados de peso, origen o
    temperamento cuando no tienen valor.
  - **Depende de:** T-08
  - **Criterios:** CA-08, CA-13 (parte de interfaz).
  - **Validación:** test instrumentado de Compose con opcionales vacíos y con
    opcionales rellenos.

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

- [ ] **T-16 · Ruta de alta y acceso desde el listado**
  - **Objetivo:** poder llegar al formulario.
  - **Alcance:** nueva ruta en `Routes.kt`, entrada en `AppNavigation.kt` y botón
    flotante en `DogScreen`, con vuelta al listado tras guardar.
  - **Depende de:** T-11
  - **Criterios:** ninguno todavía; habilita RF-01.
  - **Validación:** compila y se navega de ida y vuelta.

- [ ] **T-17 · Estado y guardado del formulario**
  - **Objetivo:** dar de alta un perro sin duplicados.
  - **Alcance:** crear `AddDogViewModel` con el estado del formulario, errores por
    campo, indicador de guardado en curso que corta envíos repetidos, y aviso de
    resultado al listado.
  - **Depende de:** T-10, T-16
  - **Criterios:** CA-01, CA-11.
  - **Validación:** tests JVM, incluido invocar guardar varias veces seguidas y
    comprobar que solo se crea un perro.

- [ ] **T-18 · Formulario en pantalla**
  - **Objetivo:** recoger los datos con una interfaz usable.
  - **Alcance:** crear `AddDogScreen` con pantalla stateful y `*Content` sin
    estado, campos obligatorios y opcionales, errores junto a su campo, teclado y
    foco razonables, y botón de confirmar inhabilitado mientras se guarda.
  - **Depende de:** T-17
  - **Criterios:** CA-06 (parte de interfaz).
  - **Validación:** revisión visual y test instrumentado de un intento fallido.

- [ ] **T-19 · Selección y copia de la foto**
  - **Objetivo:** que la foto elegida sea de verdad de la app.
  - **Alcance:** usar el selector del sistema con `PickVisualMedia`, conservar la
    URI elegida en el ViewModel y copiarla al almacenamiento interno al guardar,
    borrando la copia si el alta falla.
  - **Depende de:** T-04, T-18
  - **Criterios:** CA-09, CA-10.
  - **Validación:** comprobación manual en emulador API 26 y en uno reciente, sin
    que aparezca ningún diálogo de permiso, y revisión de la foto en modo avión.

- [ ] **T-20 · Confirmación al salir sin guardar**
  - **Objetivo:** no perder lo escrito por un gesto accidental.
  - **Alcance:** diálogo de confirmación al volver atrás con datos escritos, tanto
    con el gesto del sistema como con el botón de la barra; cancelar mantiene el
    formulario intacto.
  - **Depende de:** T-15, T-18
  - **Criterios:** CA-18.
  - **Validación:** test instrumentado de Compose confirmando y cancelando.

---

## Etapa 5 · Textos y accesibilidad

- [ ] **T-21 · Textos nuevos a recursos**
  - **Objetivo:** dejar traducible lo que añadimos.
  - **Alcance:** llevar a `strings.xml` los textos del formulario, de los estados
    vacíos y de los avisos de error. No se migran los textos ya existentes ni se
    añade ningún idioma.
  - **Depende de:** T-11, T-18
  - **Criterios:** ninguno directamente; cumple RF-13 y la decisión de idiomas.
  - **Validación:** compila y no queda ningún literal nuevo en las pantallas
    añadidas.

- [ ] **T-22 · Accesibilidad**
  - **Objetivo:** que las pantallas se puedan usar con lector de pantalla.
  - **Alcance:** etiquetas de los campos, errores asociados a su campo por
    semántica, descripciones con sentido en las imágenes, áreas táctiles
    suficientes, y corrección de las descripciones «dog» y «back» de la ficha.
  - **Depende de:** T-14, T-21
  - **Criterios:** CA-21.
  - **Validación:** recorrido manual con TalkBack, anotando literalmente lo que se
    anuncia en el formulario, en un error de validación y en la ficha.

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

- [ ] **T-24 · Tests instrumentados de interfaz**
  - **Objetivo:** cubrir lo que depende de Compose y del ciclo de vida.
  - **Alcance:** estados vacíos diferenciados, orden y etiqueta, ficha con
    opcionales vacíos, diálogo de confirmación y conservación del formulario al
    recrear la pantalla.
  - **Depende de:** T-20, T-22
  - **Criterios:** CA-08, CA-14, CA-17, CA-18, CA-22.
  - **Validación:** `./gradlew :app:connectedDebugAndroidTest` en verde.

- [ ] **T-25 · Comprobaciones del proyecto**
  - **Objetivo:** dejar el proyecto en el estado que exige `AGENTS.md`.
  - **Alcance:** ejecutar `:app:assembleDebug`, `:app:testDebugUnitTest`,
    `:app:lintDebug` y, con dispositivo, `:app:connectedDebugAndroidTest`.
  - **Depende de:** T-24
  - **Criterios:** ninguno por sí solo.
  - **Validación:** salida de cada comando, anotando cuáles pasaron, cuáles
    fallaron y cuáles no se pudieron ejecutar.

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

- [ ] **T-27 · Registro de resultados**
  - **Objetivo:** cerrar la etapa de validación con evidencia real.
  - **Alcance:** rellenar la tabla siguiente con el resultado observado de cada
    criterio y dejar explícito lo que quede sin comprobar y por qué.
  - **Depende de:** T-26
  - **Criterios:** todos.
  - **Validación:** la tabla no contiene ningún criterio sin estado.

---

## Registro de criterios de aceptación

<!-- Se rellena durante la etapa de validación. Estado: Pendiente | Superado |
Fallido | No ejecutado (con motivo) | Bloqueado (con motivo). No se marca nada
como superado sin evidencia observada. -->

| Criterio | Tareas | Estado | Evidencia |
| --- | --- | --- | --- |
| CA-01 | T-17 | Pendiente | — |
| CA-02 | T-23, T-26 | Pendiente | — |
| CA-03 | T-12, T-23 | Pendiente | — |
| CA-04 | T-13 | Pendiente | — |
| CA-05 | T-09, T-23 | Pendiente | — |
| CA-06 | T-10, T-18 | Pendiente | — |
| CA-07 | T-26 | Pendiente | — |
| CA-08 | T-14, T-24 | Pendiente | — |
| CA-09 | T-19, T-26 | Pendiente | — |
| CA-10 | T-19, T-26 | Pendiente | — |
| CA-11 | T-17 | Pendiente | — |
| CA-12 | T-11 | Pendiente | — |
| CA-13 | T-14, T-26 | Pendiente | — |
| CA-14 | T-12, T-24 | Pendiente | — |
| CA-15 | T-09 | Pendiente | — |
| CA-16 | T-11, T-26 | Pendiente | — |
| CA-17 | T-11, T-24 | Pendiente | — |
| CA-18 | T-20, T-24 | Pendiente | — |
| CA-19 | T-09 | Pendiente | — |
| CA-20 | T-10 | Pendiente | — |
| CA-21 | T-22 | Pendiente | — |
| CA-22 | T-24 | Pendiente | — |
