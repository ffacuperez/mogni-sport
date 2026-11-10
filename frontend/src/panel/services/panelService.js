import { request } from '../../shared/services/httpClient';

// Panel del propietario: el backend calcula todo (incluida la mora, con la misma regla que la lista de Alumnos);
// acá solo se pide y se muestra. Los porcentajes llegan en null cuando no hay con qué calcularlos y se muestran "—".

const numero = (valor) => (valor == null ? null : Number(valor));

const panelFromApi = (d) => ({
  fecha: d.fecha,
  ingresos: {
    mes: Number(d.ingresos.mes),
    mesAnterior: Number(d.ingresos.mesAnterior),
    variacionPorcentaje: numero(d.ingresos.variacionPorcentaje),
    efectivo: Number(d.ingresos.efectivo),
    transferencia: Number(d.ingresos.transferencia),
    recargos: Number(d.ingresos.recargos),
  },
  ingresosPorMes: d.ingresosPorMes.map((m) => ({ anio: m.anio, mes: m.mes, total: Number(m.total) })),
  alumnos: { ...d.alumnos },
  // membresia null: alumnos activos sin inscripción activa
  activosPorMembresia: d.activosPorMembresia.map((m) => ({ membresia: m.membresia ?? null, cantidad: m.cantidad })),
  ocupacion: { ...d.ocupacion, porcentaje: numero(d.ocupacion.porcentaje) },
  asistencia: { ...d.asistencia, porcentaje: numero(d.asistencia.porcentaje) },
});

/** Todo el resumen del panel, en una sola llamada. */
export async function consultarPanel() {
  return panelFromApi(await request('/panel'));
}

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
const MESES_LARGOS = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre',
  'octubre', 'noviembre', 'diciembre'];

/** 6 -> "Jun" */
export const mesCorto = (mes) => MESES[mes - 1];

/** "2026-06-15" -> "junio de 2026" */
export function textoMes(fechaISO) {
  const [y, m] = fechaISO.split('-').map(Number);
  return `${MESES_LARGOS[m - 1]} de ${y}`;
}

/** 17.5 -> "17,5%"; null -> "—" */
export function textoPorcentaje(valor) {
  if (valor == null) return '—';
  return `${valor.toLocaleString('es-AR', { maximumFractionDigits: 1 })}%`;
}

/** Variación contra el mes anterior con signo: "+17,5%", "-25%" o "—" si el mes anterior fue $0. */
export function textoVariacion(valor) {
  if (valor == null) return '—';
  return `${valor > 0 ? '+' : ''}${textoPorcentaje(valor)}`;
}
