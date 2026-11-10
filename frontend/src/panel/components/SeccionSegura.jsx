import { Component } from 'react';
import { CircleAlert } from 'lucide-react';

/** Mensaje de error de una sección de Inicio (también lo usan las secciones cuando falla la carga). */
export function ErrorSeccion({ titulo, mensaje }) {
  return (
    <div role="alert" className="flex items-start gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
      <CircleAlert className="mt-0.5 h-5 w-5 shrink-0" />
      <span className="text-sm">
        <span className="block font-semibold">{titulo}</span>
        {mensaje}
      </span>
    </div>
  );
}

/**
 * Aísla una sección de Inicio: si algo falla al dibujarla, muestra el error en su lugar y el resto de la pantalla
 * sigue andando (sin esto, un error de render desmonta toda la app y la pantalla queda en blanco).
 * ademas: lo que se sigue mostrando debajo del error (por ejemplo, otra sección que iba adentro de esta).
 */
export default class SeccionSegura extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error(`[Inicio] Falló la sección "${this.props.titulo}"`, error, info?.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <>
          <ErrorSeccion titulo={`No se pudo mostrar ${this.props.titulo}`} mensaje="Recargá la página para intentar de nuevo." />
          {this.props.ademas}
        </>
      );
    }
    return this.props.children;
  }
}
