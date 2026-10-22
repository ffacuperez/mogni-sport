# Cuotas y cobros — Pruebas manuales (rama `fix/gestionCobros`)

Una prueba por cada prueba de aceptación de la sección 19 de `Cuotas-Rediseno-Estados-y-Preguntas.md`.

## Antes de empezar

- **Datos.** `data.sql` solo se carga con el perfil `seed`, que **borra toda la base**: no usarlo contra Neon. Con la
  base de Neon, crear alumnos de prueba propios desde la app (Alta de alumno) y no tocar los de los demás.
  Si se usa una base local con `seed`, los alumnos ya vienen así:

  | Alumno (DNI) | Situación | Detalle |
  |---|---|---|
  | Lucía Gómez (30111222) | En mora hace 3 días | 2 cuotas; la segunda se pagó en mora con recargo ($25.000 + $2.500) |
  | Martín Fernández (35222333) | Próxima a vencer (3 días) | |
  | Sofía Ruiz (38333444) | Vencida (ayer) | |
  | Tomás Díaz (40444555) | Al día (20 días) | 2 cuotas encadenadas; es ONLINE (no reserva turnos) |
  | Valentina López (42555666) | Al día (alta hoy) | |

- **Mover fechas.** Casi todas las situaciones dependen de la fecha. Para llevar a un alumno de prueba a una
  situación, correr esto en la base (solo con alumnos propios), con `X` = días desde hoy hasta el vencimiento
  (negativo si ya venció) y `ID` = id de su última cuota (la de vencimiento más reciente):

  ```sql
  UPDATE cuotas SET fecha_inicio = CURRENT_DATE + X - 30, fecha_vencimiento = CURRENT_DATE + X WHERE id = ID;
  ```

  | Situación | X |
  |---|---|
  | Al día | 6 o más |
  | Próxima a vencer | 0 a 5 |
  | Vencida | -1 |
  | En mora desde hoy (día 0) | -2 |
  | En mora hace 3 días | -5 |
  | 6 días en mora (sigue activo) | -8 |
  | 7 días en mora (baja automática) | -9 |

- La puesta al día (cancelar turnos en mora y baja automática) corre al abrir Alumnos, Cuotas y la Agenda, y al
  consultar o reservar turnos.

## MOGNI-73 — Inscribir alumno

| # | Prueba | Resultado esperado |
|---|---|---|
| 1 | Alta con membresía y medio de pago | Se crea. En Cuotas aparece "Al día", período hoy al hoy + 30. El historial muestra un pago por el precio de la membresía, con fecha de hoy |
| 2 | Alta sin medio de pago | El formulario no deja guardar: "Debe seleccionar el medio de pago de la primera cuota" |
| 3 | Recién inscripto en Cuotas | Aparece "Al día" |

## MOGNI-45 — Registrar pago (renovación)

| # | Prueba | Resultado esperado |
|---|---|---|
| 4 | Renovar con X = 3 | El modal muestra período desde el vencimiento anterior (+30). Al confirmar, la fila pasa a "Al día" con el período nuevo |
| 5 | Renovar con X = 11 | "Registrar pago" queda deshabilitado y dice desde qué día se habilita |
| 6 | Renovar con X = -1 (vencida) | El período empieza en el vencimiento anterior, sin recargo |
| 7 | Renovar con X = -5 (en mora) | El período empieza hoy y se muestra el recargo |
| 8 | Pago a un alumno inactivo | No aparece en Cuotas (solo activos) |
| 9 | Confirmar sin medio de pago | "Seleccioná un medio de pago." |
| 10 | Monto | Es el precio actual de la membresía (cambiar el precio en Membresías y ver que el modal toma el nuevo) |
| 11 | Renovar dos veces seguidas | Después del primer pago, el botón queda deshabilitado (el vencimiento se corrió 30 días) |

## MOGNI-46 — Controlar vencimiento de cuota

| # | Prueba | Resultado esperado |
|---|---|---|
| 12 | X = 6 | "Al día" |
| 13 | X = 5 | "Próxima a vencer", "Vence en 5 días" |
| 14 | X = 0 | "Próxima a vencer", "Vence hoy" |
| 15 | X = -1 | "Vencida", "Venció ayer" |
| 16 | X = -2 | "En mora", "Entró en mora hoy" |
| 17 | X = -5 | "En mora", "En mora hace 3 días" |
| 18 | Renovó por adelantado | La fila muestra la cuota nueva (período siguiente) |
| 19 | Alumno inactivo | No aparece |
| 20 | Orden, filtros y contadores | Primero en mora, después vencidas, después por vencimiento; los filtros y contadores coinciden con las filas |

## MOGNI-48 — Historial de pagos

| # | Prueba | Resultado esperado |
|---|---|---|
| 21 | Pago con recargo | "$27.500" y debajo "(cuota $25.000 + recargo $2.500)" |
| 22 | Cada pago | Muestra período (inicio al fin) y membresía |

## MOGNI-80 — Recargo por atraso

| # | Prueba | Resultado esperado |
|---|---|---|
| 23 | Renovar vencida (X = -1) | Sin recargo |
| 24 | Renovar en mora con precio $15.420 | Recargo $1.550, total $16.970 |
| 25 | Renovar en mora con un precio cuyo total ya es múltiplo de 10 (por ejemplo $25.000) | Total $27.500, sin redondeo extra |
| 26 | Mora en fin de semana | Con vencimiento un viernes, el domingo ya figura "En mora" (cubierto por test unitario) |

## Cambiar membresía (desde el detalle del alumno, "Modificar membresía")

| # | Prueba | Resultado esperado |
|---|---|---|
| 27 | De $20.000 a $25.000 (al día) | El aviso dice "Cobrar $5.000"; después la fila muestra precio $25.000 y la membresía nueva |
| 28 | De $25.000 a $20.000 | El aviso dice "sin reembolso"; la cuota queda en $25.000 |
| 29 | Con la cuota vencida o en mora | Toast con el motivo, no se guarda |
| 30 | Habiendo pagado el mes siguiente | Toast "ya pagó el período que empieza el …" |
| 31 | Fechas de la cuota | El período de la fila no cambia |
| 32 | Cuota pagada con recargo ($20.000 + $2.000) a una de $25.000 | El historial muestra $27.000 (cuota $25.000 + recargo $2.000) |
| 33 | Historial después del cambio | Las cuotas anteriores siguen con su membresía vieja |
| 34 | Límite semanal | La próxima reserva usa el límite de la membresía nueva |
| 35 | Opción "Sin membresía" | Ya no existe |

## MOGNI-246 — Activar/Desactivar alumno

| # | Prueba | Resultado esperado |
|---|---|---|
| 36 | Dar de baja a un alumno con un turno reservado para mañana | El turno queda con un lugar libre más; el alumno no puede iniciar sesión; su inscripción figura cancelada |
| 37 | Reactivar sin días pagos, sin medio de pago | El modal no deja confirmar |
| 38 | Reactivar sin días pagos, con membresía y medio | Cuota nueva desde hoy, sin recargo; "Al día" en Cuotas |
| 39 | Reactivar a uno cuya última cuota vence dentro de 10 días | Pide solo confirmación, no cobra; sigue con el mismo vencimiento |

## MOGNI-254 / MOGNI-259 — Reservar y consultar turnos (como alumno)

| # | Prueba | Resultado esperado |
|---|---|---|
| 40 | Vencida (X = -1) | Puede reservar |
| 41 | En mora (X = -2) | Ve los turnos apagados y el aviso "Tenés la cuota en mora"; no puede elegir ninguno |
| 42 | Renovar estando en mora | En el momento puede volver a reservar |

## Nueva HU — Gestionar alumnos en mora

| # | Prueba | Resultado esperado |
|---|---|---|
| 43 | Alumno con turno reservado para mañana pasa a mora (X = -2) y se abre la Agenda | La reserva ya no aparece y el turno tiene un lugar libre más |
| 44 | Clase extra | No recibe clase extra por esa cancelación (si la reserva había usado una, el crédito vuelve) |
| 45 | Vencido (X = -1) | Conserva sus reservas |
| 46 | 6 días en mora (X = -8) | Sigue activo |
| 47 | 7 días en mora (X = -9) y se abre Alumnos | Pasa a inactivos y no puede iniciar sesión |
| 48 | En mora que renueva antes de los 7 días | No se da de baja |
| 49 | Dado de baja automáticamente | Aparece en inactivos y se reactiva como cualquier otro (prueba 38) |
