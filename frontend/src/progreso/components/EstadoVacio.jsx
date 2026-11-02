/** Estado vacío de Mi progreso: ícono en un círculo rosa, título y texto secundario. */
export default function EstadoVacio({ icono: Icono, titulo, texto }) {
  return (
    <div className="mt-12 flex flex-col items-center gap-3 text-center">
      <span className="flex h-24 w-24 items-center justify-center rounded-full bg-error-bg text-primary">
        <Icono className="h-10 w-10" />
      </span>
      <p className="mt-2 text-base font-bold text-ink">{titulo}</p>
      <p className="text-sm text-ink-soft">{texto}</p>
    </div>
  );
}
