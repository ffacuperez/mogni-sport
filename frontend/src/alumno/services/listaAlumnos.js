import { normalizar } from '../../shared/services/texto';

// Funciones puras de la lista de alumnos: las comparten la tabla y las tarjetas

export const COLUMNAS_ACTIVOS = [
  { clave: 'alumno', label: 'Alumno' },
  { clave: 'plan', label: 'Plan' },
  { clave: 'semana', label: 'Semana' },
  { clave: 'membresia', label: 'Membresía activa' },
  { clave: 'precio', label: 'Próx. cuota' },
  { clave: 'vencimiento', label: 'Vencimiento' },
  { clave: 'estado', label: 'Estado' },
];

export const COLUMNAS_INACTIVOS = [
  { clave: 'alumno', label: 'Alumno' },
  { clave: 'ultimaMembresia', label: 'Última membresía' },
  { clave: 'baja', label: 'Fecha de baja' },
  { clave: 'ultimaCuota', label: 'Última cuota venció' },
  { clave: 'reactivacion', label: 'Reactivación' },
];

const GRAVEDAD = { EN_MORA: 0, VENCIDA: 1, PROXIMA_A_VENCER: 2, AL_DIA: 3 };

// Por qué valor ordena cada columna; null = dato vacío
const VALORES = {
  alumno: (a) => `${a.apellido} ${a.nombre}`,
  plan: (a) => (a.planActivo ? a.planActivo.nombre : null),
  semana: (a) => (a.planActivo ? a.planActivo.cantidadSemanas - a.planActivo.semanaActual : null),
  membresia: (a) => (a.membresiaId ? a.membresia : null),
  vencimiento: (a) => a.cuota?.vencimiento ?? null,
  precio: (a) => a.cuota?.precio ?? null,
  // Por gravedad y, dentro de la misma situación, por vencimiento (el más antiguo primero). Así el orden descendente es
  // exactamente el inverso: también se invierte quién lleva más días en mora.
  estado: (a) => (GRAVEDAD[a.cuota?.situacion] == null || !a.cuota.vencimiento
    ? null
    : GRAVEDAD[a.cuota.situacion] * 1e8 + Number(a.cuota.vencimiento.replaceAll('-', ''))),
  ultimaMembresia: (a) => a.inscripcion?.membresia ?? null,
  baja: (a) => a.fechaBaja || null,
  ultimaCuota: (a) => a.vencimientoUltimaCuota || null,
  reactivacion: (a) => (a.reactivacionRequierePago ? 1 : 0),
};

// En estas columnas "Sin plan" cuenta como el valor más chico: primero en ascendente y último en descendente. Hay
// alumnos que entrenan sin plan, así que no es un dato faltante. Los demás datos vacíos van siempre al final.
const VACIOS_MENORES = ['plan', 'semana'];

const comparar = (a, b) => (typeof a === 'number' ? a - b : String(a).localeCompare(String(b), 'es'));

/** Los alumnos de la pestaña que coinciden con la búsqueda y el filtro de situación, en el mismo orden. */
export function filtrarAlumnos(alumnos, { pestana, busqueda, filtro }) {
  const termino = normalizar(busqueda.trim());
  const coincide = (a) => !termino
    || normalizar(a.nombre).includes(termino)
    || normalizar(a.apellido).includes(termino)
    || normalizar(`${a.nombre} ${a.apellido}`).includes(termino)
    || a.dni.includes(termino);
  return alumnos
    .filter((a) => (pestana === 'activos') === (a.estado === 'Activo'))
    .filter(coincide)
    .filter((a) => !filtro || a.cuota?.situacion === filtro);
}

/** Sin orden elegido devuelve la lista como vino del backend. A igual valor también se respeta ese orden. */
export function ordenarAlumnos(alumnos, orden) {
  if (!orden) return alumnos;
  const valor = VALORES[orden.columna];
  const sentido = orden.direccion === 'asc' ? 1 : -1;
  const posicionVacio = VACIOS_MENORES.includes(orden.columna) ? -sentido : 1;
  return [...alumnos].sort((x, y) => {
    const a = valor(x);
    const b = valor(y);
    if (a == null && b == null) return 0;
    if (a == null) return posicionVacio;
    if (b == null) return -posicionVacio;
    return sentido * comparar(a, b);
  });
}

/** Al tocar una columna: ascendente, descendente y vuelta al orden normal. */
export function siguienteOrden(orden, columna) {
  if (orden?.columna !== columna) return { columna, direccion: 'asc' };
  return orden.direccion === 'asc' ? { columna, direccion: 'desc' } : null;
}
