# SPEC: Persistencia local y alta de perros

**Estado:** Borrador <!-- Borrador | En revisión | Aprobada -->

> Marcas usadas en este borrador: **[PENDIENTE]** es una decisión sin tomar, con
> su referencia a «Decisiones pendientes» (D3, D6…). **[PROPUESTA]** es algo que
> el agente sugiere y que **no** está confirmado todavía. El resto son hechos
> comprobados en el código y contra el API, o decisiones ya confirmadas.

<!-- PARA LA PERSONA
Copia esta plantilla como SPEC.md en una carpeta de la funcionalidad.
Pide al agente que la complete contigo usando MOBILE_GUIDELINES.md.
SPEC.md define qué debe cumplirse; PLAN.md desarrolla cómo implementarlo;
TASKS.md organiza los pasos de ejecución.
-->

<!-- PARA EL AGENTE
- Lee las instrucciones del proyecto y MOBILE_GUIDELINES.md. Inspecciona el
  repositorio para comprobar el comportamiento actual. Si falta la guía, pide su ubicación.
- Completa esta spec con la persona: investiga lo comprobable y consulta las
  decisiones pendientes. Haz pocas preguntas por vez y actualiza las respuestas.
- No inventes requisitos ni exclusiones. Distingue propuestas de decisiones
  confirmadas y marca como PENDIENTE lo que aún no esté resuelto.
- Aplica las consideraciones mobile relevantes sin ampliar el alcance automáticamente.
- No incluyas diseño de clases, tablas, componentes, archivos o algoritmos:
  esos detalles pertenecen a PLAN.md. Sí registra restricciones explícitas del pedido.
- Mantén el documento breve y proporcional a la funcionalidad. Conserva los comentarios.
- Un documento completo no está aprobado automáticamente. Solicita aprobación
  antes de marcarlo como Aprobada. No implementes durante esta etapa.
-->

## Qué construimos y para quién

<!-- Qué necesidad resolvemos, quién tiene esa necesidad y qué podrá hacer.
Describe el objetivo en lenguaje de producto. -->

Quien usa la app solo puede consultar un catálogo de perros que vive en un
servidor remoto: no puede registrar los suyos y, sin conexión, no ve nada.

Queremos que pueda **dar de alta sus propios perros desde la app**, que esos
perros **sigan estando disponibles al volver a abrirla** y que **el catálogo ya
descargado pueda consultarse sin conexión**.

A partir de esta funcionalidad, lo que la app muestra sale de lo guardado en el
dispositivo; la red sirve para mantenerlo al día, no para poder usar la app.

## Situación actual

<!-- Comportamiento actual relevante, limitación que queremos resolver y
comportamientos existentes que deben conservarse. No describas la arquitectura. -->

Comprobado en el código y contra el API:

- La pantalla principal lista los perros de un JSON estático alojado en GitHub
  (hoy 10 perros, con identificadores del 1 al 10) y muestra de cada uno nombre,
  raza, edad, descripción e imagen.
- Al pulsar un perro se abre su ficha, que **se pide al servidor de forma
  individual por identificador** y añade tres datos que la lista no trae: peso,
  origen y temperamento.
- El buscador filtra sobre los perros ya descargados, sin volver a consultar la
  red, por nombre y raza, ignorando mayúsculas.
- **No se guarda nada en el dispositivo:** cada apertura de la app y cada entrada
  a una ficha necesitan conexión. Sin red, la lista muestra el mensaje de error
  técnico que devuelve el sistema, sin opción de reintentar; la ficha se comporta
  igual.
- Cuando la búsqueda no encuentra coincidencias se muestra «No hay resultados».
  Ese mismo estado aparecería si el catálogo llegara vacío: hoy no se distinguen.
- Pedir la ficha de un identificador que no existe en el servidor devuelve un
  error 404, que la app presenta como error.
- No existe ninguna forma de crear, editar ni borrar perros.

Comportamientos que deben conservarse: el listado, la búsqueda por nombre y raza,
la navegación a la ficha y el retorno atrás.

## Dentro del alcance

<!-- Requisitos concretos, con identificadores estables para vincularlos a
criterios, decisiones del plan y tareas. -->

- **RF-01:** Quien usa la app puede dar de alta un perro indicando sus datos.
- **RF-02:** Los perros dados de alta se conservan en el dispositivo y siguen
  disponibles tras cerrar la app, reabrirla o reiniciar el dispositivo.
- **RF-03:** El catálogo muestra en una sola lista los perros remotos y los dados
  de alta, sin duplicados. **[PENDIENTE]** orden y distinción visual (D7).
- **RF-04:** La búsqueda por nombre y raza cubre también los perros dados de alta.
- **RF-05:** **[PROPUESTA]** Un perro dado de alta nunca es sustituido,
  sobrescrito ni eliminado por una actualización del catálogo remoto, aunque
  coincidan sus datos o su identificador (D15).
- **RF-06:** Con el catálogo ya descargado alguna vez, la app se puede usar sin
  conexión: la lista muestra los perros remotos y los propios sin mensaje de
  error. **[PENDIENTE]** hasta dónde llega eso en las fichas de perros remotos
  (D12) y qué se ve en un primer uso sin conexión (D13).
- **RF-07:** Un perro dado de alta tiene ficha propia, con los mismos apartados
  que la de un perro remoto. Peso, origen y temperamento son opcionales: los que
  se dejen vacíos no aparecen en la ficha.
- **RF-08:** El catálogo remoto se mantiene al día. **[PENDIENTE]** cuándo se
  vuelve a pedir (D10) y qué ocurre con los perros remotos que cambian o
  desaparecen (D14).
- **RF-09:** La imagen de un perro dado de alta se elige entre las fotos del
  dispositivo, sin que se pidan permisos adicionales, y sigue viéndose sin
  conexión.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- **Editar y borrar perros.** Solo se contempla el alta. Una vez creado, un perro
  dado de alta no se puede modificar ni eliminar desde la app.
- **[PROPUESTA]** Pendientes de confirmar como exclusiones (D15): enviar los
  perros dados de alta al servidor o a cualquier servicio remoto; sincronización
  entre dispositivos; cuentas de usuario; compartir o exportar el catálogo.

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

1. Desde el listado, la persona abre la pantalla de alta de un perro.
   **[PENDIENTE]** punto de entrada (D6).
2. Rellena los datos del perro y elige una foto del dispositivo. Puede indicar
   además peso, origen y temperamento, que son opcionales. **[PENDIENTE]** cuáles
   de los datos restantes son obligatorios (D3).
3. Confirma el alta. Si falta un dato obligatorio o hay un valor no válido, se le
   indica qué corregir, no se guarda nada y no pierde lo escrito.
4. Al guardarse correctamente, vuelve al listado y el perro aparece en él.
   **[PENDIENTE]** en qué posición (D7).
5. Puede encontrarlo con el buscador y abrir su ficha, que muestra los datos que
   rellenó y omite los apartados opcionales que dejó vacíos.
6. Si sale de la pantalla de alta sin guardar, **[PENDIENTE]** qué ocurre con lo
   escrito (D8).

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- De cada perro, el listado muestra nombre, raza, edad, descripción e imagen; la
  ficha muestra además peso, origen y temperamento.
- El alta recoge esos mismos datos. Peso, origen y temperamento son **opcionales**
  y, si se dejan vacíos, no se muestran en la ficha. **[PENDIENTE]** cuáles de los
  demás son obligatorios (D3).
- La imagen se elige entre las fotos del dispositivo y queda disponible aunque no
  haya conexión. **[PENDIENTE]** qué se muestra si la imagen es obligatoria y no
  se elige ninguna, o si se permite dejarla vacía (D3).
- **[PENDIENTE]** Validaciones: rango admitido para la edad, longitud máxima de
  los textos y si se permiten nombres repetidos (D9).
- Cada perro tiene identidad propia: un perro dado de alta y uno remoto nunca son
  el mismo, aunque coincidan en nombre y raza.
- **[PENDIENTE]** Orden de presentación de la lista combinada (D7).

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Mientras se refresca el catálogo o se guarda un perro, la persona percibe que la operación está en curso y la interfaz sigue respondiendo. **[PROPUESTA]** durante el guardado la acción de confirmar queda inhabilitada |
| Sin datos | **[PENDIENTE]** (D11) Distinguir «no hay ningún perro todavía» de «la búsqueda no encuentra resultados»; hoy ambos casos muestran el mismo texto |
| Entrada inválida | Se indica qué campo falla y por qué, no se crea ningún perro y se conserva lo escrito |
| Error o espera excesiva | **[PENDIENTE]** (D11) Hoy se muestra el mensaje técnico del sistema y no se puede reintentar sin salir y volver a entrar |
| Sin conexión o conexión interrumpida | Con el catálogo ya descargado, la lista muestra los perros remotos y los propios sin error. Dar de alta un perro funciona igual sin conexión, porque no implica ninguna llamada de red. **[PENDIENTE]** fichas de perros remotos (D12) y primer uso sin catálogo descargado (D13) |
| Cancelar o volver atrás | **[PENDIENTE]** (D8) Salir del alta con datos escritos sin guardar. Si se cancela el selector de fotos sin elegir ninguna, el formulario queda como estaba |
| Pasar a segundo plano y regresar | **[PROPUESTA]** Lo escrito en el formulario se conserva y no se repite la carga del catálogo solo por volver a la app |
| Recrear la pantalla | **[PROPUESTA]** Al girar el dispositivo el formulario conserva lo escrito, incluida la foto elegida. La app no fija la orientación; hoy el listado y el texto de búsqueda sobreviven al giro |
| Reabrir después de terminarse el proceso | Los perros dados de alta y el catálogo descargado siguen presentes (RF-02, RF-06). **[PENDIENTE]** si se recupera el texto de búsqueda o un formulario a medias (D8) |
| Pulsaciones repetidas | **[PROPUESTA]** Confirmar el alta varias veces seguidas no crea perros duplicados |
| Accesibilidad | **[PENDIENTE]** Etiquetas de los campos del formulario y descripción de las imágenes. Hoy las imágenes del listado usan el nombre del perro como descripción y el botón de volver se anuncia como «back» |
| Permisos | Elegir una foto se hace con el selector del sistema y no obliga a conceder ningún permiso. Si la persona no elige ninguna foto, no se pide nada |
| Privacidad | Los perros dados de alta, imágenes incluidas, se quedan en el dispositivo: no se envían al servidor ni a ningún otro servicio |
| Rendimiento y recursos | **[PROPUESTA]** Las fotos elegidas no deben hacer crecer el almacenamiento sin control ni ralentizar el listado |
| Idiomas y formatos | **[PENDIENTE]** Hoy los textos están escritos directamente en castellano dentro del código, sin recursos de traducción; decidir si el alta sigue esa práctica o usa recursos |

**Puntos de la guía no aplicables y motivo:** *Trabajo en segundo plano*: no hay
tareas diferidas ni envíos pendientes, porque nada se sincroniza hacia el
servidor. *Capacidades del dispositivo*: no se usa cámara, ubicación ni sensores.

## Restricciones del pedido

<!-- Condiciones ya impuestas: compatibilidad, límites de alcance, requisitos
de accesibilidad o rendimiento medibles, o una tecnología expresamente exigida.
Ejemplo: Usar Room puede ser una restricción; el diseño de entidades va en PLAN.md.
No conviertas una preferencia del agente en una restricción. -->

- La persistencia local debe implementarse con **Room** (exigido en el pedido).
- Debe conservarse el comportamiento actual de listado, búsqueda y ficha.
- Se mantienen las convenciones del proyecto recogidas en `AGENTS.md`
  (arquitectura por capas, Hilt, Compose, Navigation3); no se introduce otro
  stack equivalente.
- La app declara `minSdk 26`, por lo que la solución debe funcionar desde
  Android 8.0.

## Criterios de aceptación

<!-- Resultados observables que permitan decidir si se cumple cada requisito.
Incluye los casos alternativos acordados. No uses Funciona correctamente.
Repite el formato según sea necesario. -->

- **CA-01 · RF-01:** Dado el listado abierto, cuando doy de alta un perro con
  datos válidos, entonces vuelvo al listado y el perro aparece con los datos que
  introduje.
- **CA-02 · RF-02:** Dado un perro dado de alta, cuando termino el proceso de la
  app y la vuelvo a abrir, entonces el perro sigue en el listado con los mismos
  datos.
- **CA-03 · RF-03:** Dado el catálogo remoto disponible y al menos un perro dado
  de alta, cuando abro el listado, entonces veo ambos en la misma lista y ningún
  perro aparece repetido.
- **CA-04 · RF-04:** Dado un perro dado de alta, cuando escribo parte de su nombre
  o de su raza en el buscador, entonces aparece entre los resultados.
- **CA-05 · RF-05:** Dado un perro dado de alta, cuando el catálogo remoto se
  actualiza —incluido el caso de que un perro remoto tenga su mismo
  identificador—, entonces el perro dado de alta sigue presente y sin alterar.
- **CA-06 · RF-01:** Dado el formulario de alta con un dato obligatorio vacío o no
  válido, cuando intento confirmar, entonces se me indica el error, no se crea
  ningún perro y conservo lo que había escrito.
- **CA-07 · RF-06:** Dado que el catálogo se descargó al menos una vez, cuando
  abro la app sin conexión, entonces veo la lista con los perros remotos y los
  propios, sin mensaje de error.
- **CA-08 · RF-07:** Dado un perro dado de alta sin peso, origen ni temperamento,
  cuando abro su ficha, entonces veo los datos que sí rellené y no aparecen esos
  apartados vacíos.
- **CA-09 · RF-09:** Dado un perro dado de alta con una foto elegida del
  dispositivo, cuando abro la app sin conexión, entonces su foto se sigue viendo
  en la lista y en su ficha.
- **CA-10 · RF-09:** Dado el formulario de alta, cuando elijo una foto del
  dispositivo, entonces la app no me pide ningún permiso.
- **CA-11 · RF-01:** Dado un alta con datos válidos, cuando pulso confirmar varias
  veces seguidas, entonces se crea un único perro.
- **CA-12 · RF-08:** **[PENDIENTE]** (D10, D14).

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | En dispositivo o emulador: abrir el listado, dar de alta un perro con todos los datos obligatorios y confirmar | Se vuelve al listado y el perro aparece con los datos introducidos |
| CA-02 | Tras CA-01, terminar el proceso de la app (no basta con mandarla a segundo plano) y volver a abrirla | El perro sigue en el listado con los mismos datos |
| CA-03 | Con catálogo remoto accesible y al menos un perro dado de alta, abrir el listado | Aparecen los perros remotos y los propios en una sola lista, sin repeticiones |
| CA-04 | Escribir en el buscador parte del nombre y, después, parte de la raza de un perro dado de alta | El perro aparece en ambos casos |
| CA-05 | Con un catálogo remoto que incluya un perro con el mismo identificador que uno dado de alta, abrir el listado | El perro dado de alta sigue presente y con sus datos intactos |
| CA-06 | Abrir el alta, dejar vacío un campo obligatorio o escribir un valor no válido y confirmar | Se indica el campo y el motivo, no se crea ningún perro y lo escrito permanece |
| CA-07 | Abrir la app con conexión para descargar el catálogo, activar el modo avión, terminar el proceso y volver a abrirla | La lista se muestra completa, con perros remotos y propios, y sin error |
| CA-08 | Dar de alta un perro dejando vacíos peso, origen y temperamento, y abrir su ficha | La ficha muestra los datos rellenados y no incluye los apartados vacíos |
| CA-09 | Tras dar de alta un perro con foto, activar el modo avión y recorrer lista y ficha | La foto se ve en ambas pantallas |
| CA-10 | Abrir el alta y usar la acción de elegir foto | Se abre el selector del sistema sin ninguna petición de permiso previa |
| CA-11 | En el alta con datos válidos, pulsar confirmar dos o tres veces seguidas | El listado contiene un único perro nuevo |
| CA-12 | **[PENDIENTE]** (D10, D14) | **[PENDIENTE]** |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

- **D3 · Campos obligatorios del alta.** Peso, origen y temperamento ya son
  opcionales. ¿Cuáles de nombre, raza, edad, descripción y foto son obligatorios?
- **D6 · Punto de entrada al alta.** ¿Desde dónde se abre el formulario?
- **D7 · Orden y distinción visual.** ¿En qué orden se presenta la lista combinada
  y se distingue de algún modo a los perros propios?
- **D8 · Salir del alta sin guardar.** ¿Se descarta lo escrito, se pide
  confirmación o se conserva como borrador al volver?
- **D9 · Validaciones.** Rango de edad admitido, longitudes máximas y si se
  permiten perros con el mismo nombre.
- **D10 · Cuándo se refresca el catálogo remoto.** ¿En cada apertura, con un gesto
  manual, cada cierto tiempo?
- **D11 · Estados de error y vacío.** ¿Entra en el alcance mejorar el mensaje de
  error actual, añadir reintento y distinguir «catálogo vacío» de «búsqueda sin
  resultados», o se deja como está?
- **D12 · Fichas de perros remotos sin conexión.** El peso, el origen y el
  temperamento de un perro remoto llegan en una petición aparte por perro, así que
  solo estarán disponibles sin conexión si se han guardado antes. ¿Se guardan
  todas al refrescar el catálogo, solo las de las fichas que se hayan abierto, o
  las fichas remotas no están disponibles sin conexión?
- **D13 · Primer uso sin conexión.** ¿Qué se ve al abrir la app por primera vez sin
  red y sin nada descargado todavía?
- **D14 · Perros remotos que cambian o desaparecen.** Si el catálogo remoto
  modifica los datos de un perro o deja de incluirlo, ¿se actualiza, se conserva
  lo descargado o se elimina del dispositivo?
- **D15 · Exclusiones y regla RF-05.** Confirmar las exclusiones propuestas en
  «Fuera de alcance» y la regla de que un perro propio nunca se pierde por una
  actualización remota.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
