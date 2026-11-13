import { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router';
import { CalendarDays, ChevronRight, CircleAlert, CreditCard, KeyRound, LogOut, Mail, Phone, Target } from 'lucide-react';

import { obtenerMiPerfil } from '../services/alumnosService';
import { tonoEstadoCuota } from '../../cuota/services/cuotasService';
import { desactivarPush } from '../../notificacion/services/pushService';
import { formatearFecha } from '../../shared/services/texto';
import { Badge, Cargando, Panel } from '../../shared/components/ui';
import { FOCUS } from '../../shared/components/clases';
import FormCambiarPassword from '../../auth/components/FormCambiarPassword';

function Fila({ icono: Icono, etiqueta, valor, detalle, extra }) {
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
        {detalle && <p className="mt-0.5 text-xs text-ink-soft">{detalle}</p>}
      </div>
      {extra}
    </li>
  );
}

export default function AlumnoPerfil() {
  const { onLogout } = useOutletContext();
  const [alumno, setAlumno] = useState(null);
  const [error, setError] = useState(null);
  // MOGNI-263: formulario de cambio de contraseña desplegado
  const [cambiandoPassword, setCambiandoPassword] = useState(false);

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
  const cuota = alumno.cuota;

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
            // Cambio ya pagado para el mes siguiente
            detalle={alumno.membresiaProgramada
              && `Membresía ${alumno.membresiaProgramada} a partir del ${formatearFecha(alumno.programadaDesde)}`}
            extra={inscripcion && <Badge tone={activa ? 'success' : 'error'}>{inscripcion.estado}</Badge>}
          />
          {cuota?.vigenteDesde && cuota.vencimiento && (
            <Fila
              icono={CalendarDays}
              etiqueta="Vigencia de la cuota"
              valor={`${formatearFecha(cuota.vigenteDesde)} al ${formatearFecha(cuota.vencimiento)}`}
              extra={<Badge tone={tonoEstadoCuota(cuota.estado)}>{cuota.estado}</Badge>}
            />
          )}
        </ul>
      </Panel>

      {/* MOGNI-263: cambiar la contraseña desde el perfil */}
      <Panel className="mb-6 mt-4 rounded-xl px-4">
        <button
          type="button"
          onClick={() => setCambiandoPassword(!cambiandoPassword)}
          aria-expanded={cambiandoPassword}
          aria-controls="perfil-cambiar-password"
          className={`flex w-full cursor-pointer items-center gap-3 py-3.5 text-left ${FOCUS}`}
        >
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-error-bg text-primary">
            <KeyRound className="h-5 w-5" />
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-xs font-medium text-ink-soft">Seguridad</span>
            <span className="block text-sm font-semibold text-ink">Cambiar contraseña</span>
          </span>
          <ChevronRight className={`h-5 w-5 text-ink-muted transition-transform ${cambiandoPassword ? 'rotate-90' : ''}`} />
        </button>
        {cambiandoPassword && (
          <div id="perfil-cambiar-password" className="border-t border-line-soft pb-4 pt-4">
            <FormCambiarPassword onCancelar={() => setCambiandoPassword(false)} />
          </div>
        )}
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
