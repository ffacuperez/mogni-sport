import { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router';
import { CircleAlert, CreditCard, LogOut, Mail, MapPin, Phone, Target } from 'lucide-react';

import { describirModalidad, obtenerMiPerfil } from '../services/alumnosService';
import { desactivarPush } from '../../notificacion/services/pushService';
import { Badge, Cargando, Panel } from '../../shared/components/ui';
import { FOCUS } from '../../shared/components/clases';

function Fila({ icono: Icono, etiqueta, valor, extra }) {
  return (
    <li className="flex items-center gap-3 border-b border-line-soft py-3.5 last:border-b-0">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
        <Icono className="h-5 w-5" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-ink-soft">{etiqueta}</p>
        {valor
          ? <p className="truncate text-sm font-semibold text-ink">{valor}</p>
          : <p className="text-sm font-medium text-ink-muted">Sin definir</p>}
      </div>
      {extra}
    </li>
  );
}

export default function AlumnoPerfil() {
  const { onLogout } = useOutletContext();
  const [alumno, setAlumno] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    obtenerMiPerfil()
      .then(setAlumno)
      .catch((err) => setError(err.message));
  }, []);

  if (error) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-lg border border-line bg-error-bg p-4 text-error">
        <CircleAlert className="h-5 w-5 shrink-0" />
        <span className="text-sm">{error}</span>
      </div>
    );
  }

  if (!alumno) return <Cargando texto="Cargando tus datos" />;

  // Al cerrar sesión el dispositivo deja de recibir las push de esta cuenta (se borra mientras el token sigue válido)
  const cerrarSesion = async () => {
    await desactivarPush().catch(() => {});
    onLogout();
  };

  const inscripcion = alumno.inscripcion;
  const activa = inscripcion?.estado === 'Activa';

  return (
    <div className="flex min-h-[calc(100vh-11rem)] flex-col animate-fade-in">
      <div className="mb-6 flex flex-col items-center text-center">
        <span className="mb-3 flex h-20 w-20 items-center justify-center rounded-full bg-error-bg text-2xl font-bold text-primary">
          {alumno.nombre?.[0]}{alumno.apellido?.[0]}
        </span>
        <h1 className="text-xl font-bold text-ink">{alumno.nombre} {alumno.apellido}</h1>
        <p className="mt-0.5 text-sm tabular-nums text-ink-soft">DNI {alumno.dni}</p>
      </div>

      <Panel className="rounded-xl px-4">
        <ul>
          <Fila icono={Mail} etiqueta="Email" valor={alumno.email} />
          <Fila icono={Phone} etiqueta="Celular" valor={alumno.telefono} />
          <Fila icono={Target} etiqueta="Objetivo" valor={alumno.objetivo} />
          <Fila
            icono={CreditCard}
            etiqueta="Membresía"
            valor={inscripcion ? inscripcion.membresia : 'Sin membresía'}
            extra={inscripcion && <Badge tone={activa ? 'success' : 'error'}>{inscripcion.estado}</Badge>}
          />
          <Fila icono={MapPin} etiqueta="Modalidad" valor={describirModalidad(alumno)} />
        </ul>
      </Panel>

      <button
        type="button"
        onClick={cerrarSesion}
        className={`mt-auto flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl border-2 border-primary bg-card px-5 py-3 text-sm font-semibold text-primary transition-colors hover:bg-error-bg ${FOCUS}`}
      >
        <LogOut className="h-5 w-5" /> Cerrar sesión
      </button>
    </div>
  );
}
