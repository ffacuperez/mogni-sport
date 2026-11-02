/** Ícono de la pestaña Progreso, con los trazos del diseño. Toma el color del texto: rojo si está activa, gris si no. */
export default function IconoProgreso({ className }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M3 3V21H21" />
      <path d="M7 15L11 11L14 14L19 8" />
    </svg>
  );
}
