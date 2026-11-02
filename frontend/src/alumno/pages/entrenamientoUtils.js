export function calcular1RM({ peso, repeticiones }) {
  const pesoNumero = Number.parseFloat(peso);
  const repeticionesNumero = Number.parseFloat(repeticiones);

  if (!Number.isFinite(pesoNumero) || pesoNumero <= 0 || !Number.isFinite(repeticionesNumero) || repeticionesNumero <= 0) {
    return null;
  }

  return Number((pesoNumero * (1 + repeticionesNumero / 30)).toFixed(2));
}

export function esSerieValida(serie) {
  const peso = Number.parseFloat(serie?.peso);
  const repeticiones = Number.parseFloat(serie?.repeticiones);
  const rir = Number.parseFloat(serie?.rirRealizado);

  return Number.isFinite(peso) && peso > 0
    && Number.isFinite(repeticiones) && repeticiones > 0
    && Number.isFinite(rir) && rir >= 0;
}

export function esEjercicioCompleto(ejercicio) {
  if (!ejercicio?.series?.length) return false;
  return ejercicio.series.every(esSerieValida);
}

export function obtenerEstadoSesion(session) {
  if (!session?.ejercicios?.length) return 'En curso';
  return session.ejercicios.every(esEjercicioCompleto) ? 'Completa' : 'En curso';
}

export function obtenerRMMaximo(ejercicio) {
  const estimados = (ejercicio?.series ?? [])
    .map((serie) => serie?.rmEstimado)
    .filter((valor) => Number.isFinite(valor));

  if (!estimados.length) return null;
  return Math.max(...estimados);
}
