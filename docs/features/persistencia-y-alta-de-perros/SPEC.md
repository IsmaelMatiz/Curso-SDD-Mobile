# SPEC: Persistencia local y alta de perros

**Estado:** Aprobada <!-- Borrador | En revisión | Aprobada -->

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
descargado pueda consultarse por completo sin conexión**.

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
- La ficha describe su imagen como «dog» y su botón de volver como «back», que es
  lo que anunciaría un lector de pantalla.
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
  de alta, sin duplicados. Los perros propios aparecen primero e identificados con
  una etiqueta de texto, no solo por color; los remotos conservan el orden en que
  llegan del servidor.
- **RF-04:** La búsqueda por nombre y raza cubre también los perros dados de alta.
- **RF-05:** Un perro dado de alta nunca es sustituido, sobrescrito ni eliminado
  por una actualización del catálogo remoto, aunque coincidan sus datos o su
  identificador.
- **RF-06:** Con el catálogo ya descargado alguna vez, la app se puede usar sin
  conexión: la lista muestra los perros remotos y los propios sin mensaje de
  error, y **se puede abrir la ficha de cualquiera de ellos**, incluidos peso,
  origen y temperamento de los remotos.
- **RF-07:** Un perro dado de alta tiene ficha propia, con los mismos apartados
  que la de un perro remoto. Peso, origen y temperamento son opcionales: los que
  se dejen vacíos no aparecen en la ficha.
- **RF-08:** El catálogo remoto se mantiene al día: al abrir la app se muestra de
  inmediato lo guardado en el dispositivo y la actualización ocurre por detrás,
  reflejándose en la lista cuando llega. La actualización incluye las fichas de
  todos los perros del catálogo, para que estén disponibles sin conexión. Lo
  guardado refleja lo que hay en el servidor: los perros remotos que cambian se
  actualizan y los que dejan de estar en el catálogo se retiran del dispositivo.
- **RF-09:** La imagen de un perro dado de alta se elige entre las fotos del
  dispositivo, sin que se pidan permisos adicionales, y sigue viéndose sin
  conexión.
- **RF-10:** La app explica en lenguaje comprensible cuándo no ha podido
  actualizar el catálogo y ofrece reintentar. Distingue «todavía no hay ningún
  perro» de «la búsqueda no encuentra coincidencias». Aunque el catálogo remoto no
  esté disponible, se pueden ver los perros propios y dar de alta nuevos.
- **RF-11:** Al abandonar el formulario de alta con datos escritos sin guardar, se
  pide confirmación antes de descartarlos.
- **RF-12:** El catálogo guardado en el dispositivo está siempre completo y es
  coherente: solo se sustituye por una actualización que se haya descargado
  entera. Si la actualización falla, en la lista o en cualquiera de las fichas, se
  conserva intacto lo que ya había.
- **RF-13:** Las pantallas de esta funcionalidad son utilizables con lector de
  pantalla: cada campo del formulario tiene su etiqueta, los mensajes de error se
  asocian al campo que los provoca y las imágenes tienen una descripción con
  sentido. Se corrigen además las descripciones «dog» y «back» de la ficha.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- **Editar y borrar perros.** Solo se contempla el alta. Una vez creado, un perro
  dado de alta no se puede modificar ni eliminar desde la app.
- **Enviar los perros dados de alta al servidor** o a cualquier otro servicio
  remoto.
- **Sincronización entre dispositivos** y **cuentas de usuario**.
- **Compartir o exportar** el catálogo.
- **Traducir la app.** Los textos nuevos se escriben como recursos para poder
  traducirse en el futuro, pero no se añade ningún idioma ni se migran los textos
  existentes.

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

1. En el listado, la persona pulsa el botón flotante de añadir y se abre la
   pantalla de alta.
2. Indica nombre, raza, edad y descripción, y elige una foto del dispositivo.
   Puede añadir además peso, origen y temperamento, que son opcionales.
3. Confirma el alta. Si falta un dato obligatorio o hay un valor no válido, se le
   indica qué corregir, no se guarda nada y no pierde lo escrito.
4. Al guardarse correctamente, vuelve al listado y el perro aparece al principio
   de la lista, con la etiqueta que identifica a los perros propios.
5. Puede encontrarlo con el buscador y abrir su ficha, que muestra los datos que
   rellenó y omite los apartados opcionales que dejó vacíos.
6. Si sale del formulario con datos escritos sin guardar, se le pide confirmación
   antes de descartarlos; si confirma, el alta se cancela y no se crea nada.

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- De cada perro, el listado muestra nombre, raza, edad, descripción e imagen; la
  ficha muestra además peso, origen y temperamento.
- Al dar de alta un perro son **obligatorios** nombre, raza, edad, descripción y
  foto. Peso, origen y temperamento son **opcionales** y, si se dejan vacíos, no
  se muestran en la ficha.
- La foto se elige entre las del dispositivo y queda disponible aunque no haya
  conexión.
- Validaciones: la edad admite de 0 a 30 años; nombre, raza, peso, origen y
  temperamento, hasta 50 caracteres cada uno; la descripción, hasta 300.
- Se permiten varios perros con el mismo nombre: es normal tener un perro propio
  que se llame igual que uno del catálogo, y la etiqueta del listado ya los
  distingue.
- Cada perro tiene identidad propia: un perro dado de alta y uno remoto nunca son
  el mismo, aunque coincidan en nombre y raza.
- En el listado, los perros propios preceden a los remotos; entre los remotos se
  respeta el orden que envía el servidor.
- Lo guardado del catálogo remoto es un reflejo del servidor: se actualiza con sus
  cambios y se retira lo que el servidor deja de ofrecer. Los perros propios no se
  ven afectados por esas actualizaciones.

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Habiendo datos guardados, la lista se muestra de inmediato y la actualización ocurre por detrás sin tapar el contenido. La indicación de carga a pantalla completa queda para el primer uso, cuando todavía no hay nada guardado. Al guardar un perro se percibe que la operación está en curso y la acción de confirmar queda inhabilitada mientras tanto |
| Sin datos | Se distingue «todavía no hay ningún perro», que invita a dar de alta el primero, de «la búsqueda no encuentra coincidencias», que se refiere al texto buscado |
| Entrada inválida | Se indica qué campo falla y por qué, no se crea ningún perro y se conserva lo escrito |
| Error o espera excesiva | Se explica en lenguaje comprensible que no se ha podido actualizar el catálogo y se ofrece reintentar, sin mostrar el mensaje técnico del sistema. Si la actualización falla a medias, se conserva el catálogo anterior completo y el aviso no interrumpe lo que la persona esté haciendo |
| Sin conexión o conexión interrumpida | Con el catálogo ya descargado, la lista y todas las fichas funcionan con normalidad. En un primer uso sin red, se avisa de que no se ha podido cargar el catálogo y se ofrece reintentar, pero la pantalla sigue siendo usable: se ven los perros propios y se puede dar de alta. Dar de alta un perro nunca requiere conexión |
| Cancelar o volver atrás | Salir del alta con datos escritos pide confirmación antes de descartarlos. Si se cancela el selector de fotos sin elegir ninguna, el formulario queda como estaba |
| Pasar a segundo plano y regresar | Lo escrito en el formulario se conserva, incluida la foto elegida, y no se repite la actualización del catálogo solo por volver a la app |
| Recrear la pantalla | Al girar el dispositivo, el formulario conserva lo escrito y la foto elegida. La app no fija la orientación; el listado y el texto de búsqueda siguen sobreviviendo al giro, como hoy |
| Reabrir después de terminarse el proceso | Los perros dados de alta y el catálogo descargado siguen presentes (RF-02, RF-06). Un formulario a medias no se recupera: se descarta, en coherencia con la confirmación al salir. El texto de búsqueda tampoco se recupera, como hoy |
| Pulsaciones repetidas | Confirmar el alta varias veces seguidas crea un solo perro |
| Accesibilidad | Los campos del formulario se anuncian con su etiqueta, los errores se asocian a su campo y las imágenes tienen descripción con sentido. La etiqueta que distingue a los perros propios es texto, de modo que también la anuncia el lector de pantalla. Se corrigen «dog» y «back» en la ficha |
| Permisos | Elegir una foto se hace con el selector del sistema y no obliga a conceder ningún permiso. Si la persona no elige ninguna foto, no se pide nada |
| Privacidad | Los perros dados de alta, imágenes incluidas, se quedan en el dispositivo: no se envían al servidor ni a ningún otro servicio |
| Rendimiento y uso de datos | Guardar las fichas de todo el catálogo supone una petición por perro además de la lista en cada actualización. Esa actualización no bloquea la interfaz ni se repite sin necesidad, y las fotos elegidas no hacen crecer el almacenamiento sin control |
| Idiomas y formatos | Los textos de las pantallas nuevas y de los mensajes de error y vacío que se rediseñan viven en recursos de texto, no escritos dentro del código. No se añade ningún idioma ni se migran los textos ya existentes |

**Puntos de la guía no aplicables y motivo:** *Trabajo en segundo plano*: no hay
tareas diferidas ni envíos pendientes, porque nada se sincroniza hacia el
servidor; la actualización del catálogo ocurre con la app abierta.
*Capacidades del dispositivo*: no se usa cámara, ubicación ni sensores.

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
- **CA-12 · RF-08:** Dado un catálogo ya guardado, cuando abro la app, entonces la
  lista aparece de inmediato con ese contenido, sin pantalla de carga, y refleja
  los cambios del servidor cuando la actualización termina.
- **CA-13 · RF-06:** Dado que el catálogo se descargó al menos una vez, cuando
  abro sin conexión la ficha de un perro remoto que nunca había visitado, entonces
  veo su peso, origen y temperamento.
- **CA-14 · RF-03:** Dado un catálogo con perros remotos y al menos un perro
  propio, cuando abro el listado, entonces los propios aparecen antes que los
  remotos y llevan una etiqueta de texto que los identifica.
- **CA-15 · RF-08:** Dado un perro remoto que el servidor deja de incluir en el
  catálogo, cuando la app se actualiza, entonces ese perro desaparece de la lista
  y los perros propios permanecen intactos.
- **CA-16 · RF-10:** Dado un primer uso sin conexión y sin catálogo descargado,
  cuando abro la app, entonces veo un aviso comprensible con opción de reintentar,
  sigo viendo mis perros propios si los hay y puedo dar de alta uno nuevo.
- **CA-17 · RF-10:** Dado un catálogo vacío y sin perros propios, cuando abro el
  listado, entonces el mensaje que veo es distinto del que aparece cuando una
  búsqueda no encuentra coincidencias.
- **CA-18 · RF-11:** Dado el formulario de alta con datos escritos, cuando vuelvo
  atrás, entonces se me pide confirmación y solo se descarta lo escrito si
  confirmo.
- **CA-19 · RF-12:** Dada una actualización que se interrumpe después de descargar
  la lista pero antes de completar todas las fichas, cuando vuelvo al listado,
  entonces sigo viendo el catálogo anterior completo y se me informa de que no se
  pudo actualizar.
- **CA-20 · RF-01:** Dado el formulario de alta, cuando escribo una edad fuera del
  rango de 0 a 30 años o un texto más largo del límite de su campo, entonces se me
  indica el problema y no se crea el perro.
- **CA-21 · RF-13:** Dado el lector de pantalla activo, cuando recorro el
  formulario de alta y la ficha de un perro, entonces cada campo se anuncia con su
  etiqueta, los errores se anuncian junto al campo que los provoca y ninguna
  imagen ni botón se anuncia con un texto sin sentido.
- **CA-22 · RF-01, RF-11:** Dado el formulario de alta con datos escritos, cuando
  giro el dispositivo o salgo y vuelvo a la app, entonces sigo viendo lo que había
  escrito, incluida la foto elegida.

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
| CA-06 | Abrir el alta, dejar vacío un campo obligatorio y confirmar | Se indica el campo y el motivo, no se crea ningún perro y lo escrito permanece |
| CA-07 | Abrir la app con conexión para descargar el catálogo, activar el modo avión, terminar el proceso y volver a abrirla | La lista se muestra completa, con perros remotos y propios, y sin error |
| CA-08 | Dar de alta un perro dejando vacíos peso, origen y temperamento, y abrir su ficha | La ficha muestra los datos rellenados y no incluye los apartados vacíos |
| CA-09 | Tras dar de alta un perro con foto, activar el modo avión y recorrer lista y ficha | La foto se ve en ambas pantallas |
| CA-10 | Abrir el alta y usar la acción de elegir foto | Se abre el selector del sistema sin ninguna petición de permiso previa |
| CA-11 | En el alta con datos válidos, pulsar confirmar dos o tres veces seguidas | El listado contiene un único perro nuevo |
| CA-12 | Con catálogo ya guardado, terminar el proceso y volver a abrir la app con conexión | La lista se ve desde el primer instante y se actualiza sola si el servidor ha cambiado |
| CA-13 | Descargar el catálogo con conexión sin abrir ninguna ficha, activar el modo avión y abrir la ficha de un perro remoto | Se muestran peso, origen y temperamento sin error |
| CA-14 | Con perros propios y remotos, abrir el listado y recorrerlo, también con el lector de pantalla activo | Los propios salen primero y su etiqueta se ve y se anuncia |
| CA-15 | Con un catálogo remoto que ya no incluye un perro previamente descargado, actualizar y revisar la lista | El perro remoto desaparece; los propios siguen presentes |
| CA-16 | Instalación limpia, modo avión activado desde el principio, abrir la app; después, dar de alta un perro | Aviso comprensible con reintentar, el alta funciona y el perro creado se ve en la lista |
| CA-17 | Con el catálogo vacío y sin perros propios, abrir el listado; después, buscar un texto que no coincida con nada | Los dos mensajes son distintos y describen situaciones distintas |
| CA-18 | Abrir el alta, escribir algún dato y volver atrás; repetir confirmando y cancelando | Cancelar mantiene el formulario tal cual; confirmar descarta y vuelve al listado sin crear nada |
| CA-19 | Con catálogo ya guardado, provocar que la actualización se corte tras la lista y antes de terminar las fichas; volver al listado | El catálogo anterior sigue completo y aparece el aviso de que no se pudo actualizar |
| CA-20 | Abrir el alta e introducir una edad de 31 años, y después un texto que supere el límite de su campo | En ambos casos se indica el problema y no se crea el perro |
| CA-21 | Con TalkBack activo, recorrer el formulario de alta —incluido un intento fallido de guardar— y la ficha de un perro | Cada campo y cada error se anuncian de forma comprensible; ninguna imagen o botón se anuncia como «dog» o «back» |
| CA-22 | Rellenar parte del formulario, girar el dispositivo; después mandar la app a segundo plano y volver | Lo escrito y la foto elegida siguen ahí en los dos casos |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
