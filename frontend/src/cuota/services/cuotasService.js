// El backend todavía no tiene el módulo de cuotas (HU-45, HU-46, HU-47, HU-48 están
// "Por hacer" en el Sprint Backlog). Siguiendo el DoD del equipo ("se pueden mockear
// los datos"), este archivo calcula el estado de cuota en el frontend y guarda los
// pagos en memoria durante la sesión. Cuando el backend exponga /cuotas, solo hay que
// reemplazar las funciones de este archivo por llamadas a `request(...)` igual que en
// alumnosService.js y membresiasService.js; los componentes que lo usan no cambian.

const DIAS_VIGENCIA_CUOTA = 30; // Plan de Proyecto: vencimiento a 30 días.
const DIAS_PROXIMO_A_VENCER = 5; // Criterio de aceptación HU-46.
const PORCENTAJE_RECARGO = 10; // Criterio de aceptación HU-47 (referencia a futuro).

function sumarDias(fechaISO, dias) {
  const fecha = new Date(`${fechaISO}T00:00:00`);
  fecha.setDate(fecha.getDate() + dias);
  return fecha;
}

function hoySinHora() {
  return new Date(new Date().toDateString());
}

/** El pago más reciente de un alumno, o null si todavía no registró ninguno. */
export function obtenerUltimoPago(pagos) {
  if (!pagos || pagos.length === 0) return null;
  return [...pagos].sort((a, b) => b.fecha.localeCompare(a.fecha))[0];
}

/**
 * Calcula el estado de la cuota de un alumno activo.
 * - Sin pagos: el ciclo arranca en su fecha de ingreso (HU-46: "vencimientos
 *   individuales a 30 días desde la fecha de ingreso de cada alumno").
 * - Con pagos: el ciclo arranca en la fecha del último pago registrado.
 */
export function calcularEstadoCuota(alumno, pagos) {
  const ultimoPago = obtenerUltimoPago(pagos);
  const fechaBase = ultimoPago ? ultimoPago.fecha : alumno.fechaIngreso;
  if (!fechaBase) {
    return { estado: 'Sin datos', vencimiento: null, diasRestantes: null };
  }

  const vencimiento = sumarDias(fechaBase, DIAS_VIGENCIA_CUOTA);
  const diasRestantes = Math.round((vencimiento - hoySinHora()) / 86400000);

  let estado = 'Pagada';
  if (diasRestantes < 0) estado = 'Vencida';
  else if (diasRestantes <= DIAS_PROXIMO_A_VENCER) estado = 'Próxima a vencer';

  return { estado, vencimiento: vencimiento.toISOString().split('T')[0], diasRestantes };
}

/** Clases de Tailwind para el badge de estado (mismo patrón que ya usa AlumnoDashboard). */
export function clasesEstadoCuota(estado) {
  switch (estado) {
    case 'Pagada':
      return 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40';
    case 'Próxima a vencer':
      return 'bg-amber-950/60 text-amber-400 border border-amber-800/40';
    case 'Vencida':
      return 'bg-red-950/60 text-red-400 border border-red-800/40';
    default:
      return 'bg-slate-800 text-slate-400 border border-slate-700';
  }
}

export function formatearMonto(monto) {
  return `$${Number(monto).toLocaleString('es-AR')}`;
}

export function formatearFecha(fechaISO) {
  if (!fechaISO) return '—';
  const [y, m, d] = fechaISO.split('-');
  return `${d}/${m}/${y}`;
}

/**
 * Datos de ejemplo para que la pantalla no se vea vacía mientras no exista el backend
 * de cuotas. Genera 0, 1 o 2 pagos pasados por alumno de forma determinística (según su
 * id), nunca aleatoria, para que la demo sea reproducible.
 */
export function generarPagosDeEjemplo(alumnos) {
  const seed = {};
  alumnos.forEach((alumno) => {
    const resto = alumno.id % 3;
    if (resto === 0 || !alumno.fechaIngreso) {
      seed[alumno.id] = []; // sin pagos: queda calculado desde fechaIngreso
      return;
    }
    const diasAtras = resto === 1 ? 35 : 10; // 1 => vencida; 2 => al día/próxima
    const fechaPago = new Date();
    fechaPago.setDate(fechaPago.getDate() - diasAtras);
    seed[alumno.id] = [
      {
        fecha: fechaPago.toISOString().split('T')[0],
        monto: 25000,
        medioPago: resto === 1 ? 'Efectivo' : 'Transferencia',
        recargo: false,
      },
    ];
  });
  return seed;
}

export const PORCENTAJE_RECARGO_MORA = PORCENTAJE_RECARGO;
