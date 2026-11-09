import { estadoVisible } from '../../cuota/services/cuotasService';
import { request } from '../../shared/services/httpClient';

const BASE_PATH = '/alumnos';

const ESTADOS_INSCRIPCION = { ACTIVA: 'Activa', CANCELADA: 'Cancelada', PROGRAMADA: 'Programada' };

// Inscripción activa o, si no tiene, la última (cancelada). null si nunca tuvo.
const inscripcionFromApi = (i) => (i ? {
  membresiaId: i.membresiaId,
  membresia: i.membresiaNombre,
  estado: ESTADOS_INSCRIPCION[i.estado] ?? i.estado,
  fechaInicio: i.fechaInicio ?? '',
  fechaFin: i.fechaFin ?? '',
} : null);

/** Texto de la membresía para mostrar: el nombre, con el estado si la inscripción no está activa. */
export function describirMembresia(alumno) {
  const inscripcion = alumno.inscripcion;
  if (!inscripcion) return 'Sin membresía';
  return inscripcion.estado === 'Activa' ? inscripcion.membresia : `${inscripcion.membresia} (${inscripcion.estado})`;
}

// Situación de cuota del alumno activo; null si está inactivo. Sin cuotas (datos viejos): situacion y vencimiento en null
const cuotaFromApi = (c) => (c ? {
  situacion: c.situacion,
  estado: estadoVisible(c),
  vencimiento: c.fechaVencimiento,
  // Precio actual de la membresía que va a renovar (la programada si tiene): lo que va a pagar
  precio: c.precioMembresia != null ? Number(c.precioMembresia) : null,
  diasEnMora: c.diasEnMora,
  puedeRenovar: c.puedeRenovar,
  renovableDesde: c.renovableDesde,
} : null);

// Backend -> forma que usan las pantallas
const fromApi = (a) => ({
  id: a.id,
  nombre: a.nombre,
  apellido: a.apellido,
  edad: a.edad,
  telefono: a.telefono ?? '',
  sexo: a.sexo ?? '',
  dni: a.dni,
  email: a.email,
  fechaNacimiento: a.fechaNacimiento ?? '',
  // Fecha de alta = inicio de la inscripción activa
  fechaIngreso: a.fechaInicioInscripcion ?? '',
  objetivo: a.objetivo ?? '',
  antecedentes: a.cuestionarioSalud ?? '',
  membresiaId: a.membresiaId ?? '',
  membresia: a.membresiaNombre ?? 'Sin membresía',
  estado: a.activo ? 'Activo' : 'Inactivo',
  // Plan de entrenamiento ACTIVO ({ id, nombre, metodo, mesociclo, semanaActual, cantidadSemanas }) o null si no tiene
  planActivo: a.planActivo ?? null,
  cuota: cuotaFromApi(a.cuotaActual),
  fechaBaja: a.fechaBaja ?? '',
  inscripcion: inscripcionFromApi(a.inscripcionActual),
  // Solo para inactivos: si al reactivarlo hay que cobrar la primera cuota o todavía tiene días pagos
  reactivacionRequierePago: a.reactivacionRequierePago ?? null,
  vencimientoUltimaCuota: a.vencimientoUltimaCuota ?? '',
  // Cambio de membresía ya pagado para el mes siguiente; null si no tiene
  membresiaProgramada: a.membresiaProgramada ?? null,
  programadaDesde: a.programadaDesde ?? null,
});

// Formulario -> body que espera el backend (Registrar/ActualizarAlumnoRequest)
const toApi = (form) => ({
  nombre: form.nombre,
  apellido: form.apellido,
  dni: form.dni,
  email: form.email,
  fechaNacimiento: form.fechaNacimiento,
  telefono: form.telefono || null,
  sexo: form.sexo || null,
  // Al editar, null = no cambiar la membresía (ya no existe "sin membresía")
  membresiaId: form.membresiaId ? Number(form.membresiaId) : null,
  objetivo: form.objetivo || null,
  cuestionarioSalud: form.antecedentes || null,
  // MOGNI-73: medio de pago de la primera cuota (solo en el alta)
  medioDePago: form.medioDePago || null,
});

/** Sin parámetro el backend devuelve solo activos; { activo: false } trae los dados de baja. */
export async function listarAlumnos({ activo } = {}) {
  const path = activo === undefined ? BASE_PATH : `${BASE_PATH}?activo=${activo}`;
  const data = await request(path);
  return data.map(fromApi);
}

/** Datos del alumno logueado (solo para cuentas con rol ALUMNO). */
export async function obtenerMiPerfil() {
  return fromApi(await request(`${BASE_PATH}/me`));
}

export async function obtenerAlumno(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`));
}

export async function crearAlumno(form) {
  return fromApi(await request(BASE_PATH, { method: 'POST', body: toApi(form) }));
}

export async function actualizarAlumno(id, form) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'PUT', body: toApi(form) }));
}

/** Baja lógica: el alumno queda inactivo, no se borra. */
export async function darDeBajaAlumno(id) {
  return fromApi(await request(`${BASE_PATH}/${id}`, { method: 'DELETE' }));
}

/**
 * MOGNI-246: si el alumno todavía tiene días pagos se reactiva sin datos; si no, hay que mandar la membresía y el
 * medio de pago de la primera cuota (como en un alta).
 */
export async function reactivarAlumno(id, { membresiaId, medioDePago } = {}) {
  const body = membresiaId ? { membresiaId: Number(membresiaId), medioDePago } : undefined;
  return fromApi(await request(`${BASE_PATH}/${id}/reactivar`, { method: 'PUT', body }));
}

/** Vista previa del cambio de membresía (reglas M1 a M9): si se permite, desde cuándo y cuánto hay que cobrar. */
export async function previsualizarCambioMembresia(id, membresiaId) {
  const v = await request(`${BASE_PATH}/${id}/cambio-membresia?membresiaId=${membresiaId}`);
  const numero = (valor) => (valor != null ? Number(valor) : null);
  return {
    ...v,
    precioPagado: numero(v.precioPagado),
    precioNuevo: numero(v.precioNuevo),
    diferenciaACobrar: numero(v.diferenciaACobrar),
  };
}

/**
 * Cambio de membresía (endpoint propio, no toca los datos personales). medioDePago solo hace falta si se cobra una
 * diferencia; si la nueva no es más cara, el backend usa el medio del pago anterior.
 */
export async function cambiarMembresia(id, { membresiaId, medioDePago }) {
  return fromApi(await request(`${BASE_PATH}/${id}/membresia`, {
    method: 'PUT',
    body: { membresiaId: Number(membresiaId), medioDePago: medioDePago || null },
  }));
}
