import { request } from '../../shared/services/httpClient';

const BASE_PATH = '/entrenamientos';

const toFrontendPlan = (payload) => {
  if (!payload) {
    return { id: null, nombre: '', activo: false, sesiones: [] };
  }

  return {
    id: payload.id ?? null,
    nombre: payload.nombre ?? '',
    activo: Boolean(payload.activo),
    sesiones: (payload.sesiones ?? []).map((sesion) => ({
      id: sesion.id,
      entrenamientoId: sesion.entrenamientoId ?? null,
      numero: sesion.numero,
      nombre: sesion.nombre ?? '',
      estado: sesion.estado ?? 'Pendiente',
      ejercicios: (sesion.ejercicios ?? []).map((ejercicio) => ({
        id: ejercicio.id ?? null,
        ejercicioPlanId: ejercicio.ejercicioPlanId ?? null,
        nombre: ejercicio.nombre,
        cantidadSeries: Number(ejercicio.cantidadSeries ?? 0),
        repeticionesObjetivo: ejercicio.repeticionesObjetivo ?? '',
        rirObjetivo: ejercicio.rirObjetivo ?? null,
        descanso: ejercicio.descanso ?? 'Sin dato',
        nota: ejercicio.nota ?? '',
        estado: ejercicio.estado ?? 'Pendiente',
        series: (ejercicio.series ?? []).map((serie) => ({
          numero: serie.numero,
          peso: serie.peso == null ? '' : String(serie.peso),
          repeticiones: serie.repeticiones == null ? '' : String(serie.repeticiones),
          rirRealizado: serie.rirRealizado == null ? '' : String(serie.rirRealizado),
          rmEstimado: serie.rmEstimado == null ? null : Number(serie.rmEstimado),
        })),
      })),
    })),
  };
};

export async function obtenerMiEntrenamiento() {
  const data = await request(`${BASE_PATH}/mi-plan`);
  return toFrontendPlan(data);
}

export async function iniciarSesionEntrenamiento(sesionId) {
  const data = await request(`${BASE_PATH}/sesiones/${sesionId}/iniciar`, { method: 'POST' });
  return toFrontendPlan(data);
}

export async function guardarEntrenamiento(entrenamientoId, payload) {
  const data = await request(`${BASE_PATH}/${entrenamientoId}`, { method: 'PUT', body: payload });
  return toFrontendPlan(data);
}

export async function finalizarEntrenamiento(entrenamientoId) {
  const data = await request(`${BASE_PATH}/${entrenamientoId}/finalizar`, { method: 'PUT' });
  return toFrontendPlan(data);
}
