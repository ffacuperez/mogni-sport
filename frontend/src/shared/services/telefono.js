// MOGNI-260: celulares argentinos -> formato internacional que usa WhatsApp (549 + característica + número,
// sin el 0 de larga distancia ni el 15 del celular).

// El "15" va después de la característica: 2 dígitos en el AMBA (11), si no 3 o 4
function sinQuince(numero) {
  const posiciones = numero.startsWith('11') ? [2] : [3, 4];
  const posicion = posiciones.find(p => numero.slice(p, p + 2) === '15');
  return posicion === undefined ? numero : numero.slice(0, posicion) + numero.slice(posicion + 2);
}

/**
 * "0351 15-674-8350", "351 6748350" o "+54 9 351 674 8350" -> "5493516748350".
 * null si no se puede armar un número de 10 dígitos (vacío, o cargado sin característica).
 */
export function normalizarCelularAR(telefono) {
  let numero = String(telefono ?? '').replace(/\D/g, '');
  if (numero.startsWith('00')) numero = numero.slice(2);
  // Ninguna característica argentina empieza con 5 ni con 9: un 54 o un 9 al principio son el prefijo internacional
  if (numero.startsWith('54')) numero = numero.slice(2);
  if (numero.startsWith('9')) numero = numero.slice(1);
  if (numero.startsWith('0')) numero = numero.slice(1);
  if (numero.length === 12) numero = sinQuince(numero);
  return numero.length === 10 ? `549${numero}` : null;
}

/** Link para abrir el chat de WhatsApp (https://wa.me/549...). null si el número no es válido. */
export function linkWhatsApp(telefono) {
  const numero = normalizarCelularAR(telefono);
  return numero ? `https://wa.me/${numero}` : null;
}
