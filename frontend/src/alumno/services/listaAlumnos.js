import { tituloPlan } from '../../progreso/services/progresoService';
import { normalizar } from '../../shared/services/texto';

// Funciones puras de la lista de alumnos: las comparten la tabla y las tarjetas

export const COLUMNAS_ACTIVOS = [
  { clave: 'alumno', label: 'Alumno' },
  { clave: 'plan', label: 'Plan' },
  { clave: 'semana', label: 'Semana' },
  { clave: 'membresia', label: 'Membresía activa' },
  { clave: 'vencimiento', label: 'Vencimiento' },
  { clave: 'precio', label: 'Próx. cuota' },
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
  plan: (a) => (a.planActivo ? tituloPlan(a.planActivo) : null),
  semana: (a) => (a.planActivo ? a.planActivo.cantidadSemanas - a.planActivo.semanaActual : null),
  membresia: (a) => (a.membresiaId ? a.membresia : null),
  vencimiento: (a) => a.cuota?.vencimiento ?? null,
  precio: (a) => a.cuota?.precio ?? null,
  estado: (a) => GRAVEDAD[a.cuota?.situacion] ?? null,
  ultimaMembresia: (a) => a.inscripcion?.membresia ?? null,
  baja: (a) => a.fechaBaja || null,
  ultimaCuota: (a) => a.vencimientoUltimaCuota || null,
  reactivacion: (a) => (a.reactivacionRequierePago ? 1 : 0),
};

// "Sin plan" va siempre arriba en estas columnas; los demás datos vacíos, siempre al final
const VACIOS_ARRIBA = ['plan', 'semana'];

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
  const posicionVacio = VACIOS_ARRIBA.includes(orden.columna) ? -1 : 1;
  const sentido = orden.direccion === 'asc' ? 1 : -1;
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
