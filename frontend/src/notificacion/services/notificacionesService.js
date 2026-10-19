import { request } from '../../shared/services/httpClient';

// Los avisos de cuota se calculan en el backend y los de turnos quedan persistidos allí. Qué ya vio el alumno
// se guarda solo en este navegador: afecta el punto rojo y el contador, no qué notificaciones existen.
const LEIDAS_KEY = 'mogni.notificacionesLeidas';

const CATEGORIAS = {
  CUOTA_POR_VENCER: 'cuota',
  CUOTA_VENCIDA: 'cuota',
  TURNO_CANCELADO: 'turnos',
  TURNO_REACTIVADO: 'turnos',
};

// La clave incluye el tipo: si una cuota "por vencer" ya vista pasa a "vencida", vuelve a aparecer como nueva
const claveDe = (n) => `${n.id}:${n.tipo}`;

const fromApi = (n) => ({
  id: n.id,
  clave: claveDe(n),
  tipo: n.tipo,
  categoria: CATEGORIAS[n.tipo] ?? 'avisos',
  titulo: n.titulo,
  mensaje: n.mensaje,
  fecha: n.fecha,
  cuotaId: n.cuotaId,
  fechaVencimiento: n.fechaVencimiento,
  turnoId: n.turnoId,
  fechaTurno: n.fechaTurno,
});

export async function listarNotificaciones() {
  const data = await request('/notificaciones');
  return data.map(fromApi);
}

function leerLeidas() {
  try {
    return new Set(JSON.parse(localStorage.getItem(LEIDAS_KEY)) ?? []);
  } catch {
    return new Set();
  }
}

export function contarSinLeer(notificaciones) {
  const leidas = leerLeidas();
  return notificaciones.filter(n => !leidas.has(n.clave)).length;
}

export function clavesSinLeer(notificaciones) {
  const leidas = leerLeidas();
  return new Set(notificaciones.filter(n => !leidas.has(n.clave)).map(n => n.clave));
}

/** Marca todas como vistas. Solo se guardan las vigentes, así la lista no crece con cuotas ya pagadas. */
export function marcarTodasLeidas(notificaciones) {
  try {
    localStorage.setItem(LEIDAS_KEY, JSON.stringify(notificaciones.map(n => n.clave)));
  } catch {
    // sin almacenamiento (modo privado): simplemente vuelven a contar como nuevas
  }
}
