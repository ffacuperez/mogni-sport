import { useId, useState } from 'react';
import { Link } from 'react-router';
import { ChevronDown, ChevronRight } from 'lucide-react';

import { cx, FOCUS } from '../../shared/components/clases';
import { Badge, Button, Panel } from '../../shared/components/ui';
import { completarSesion, textoDetalleSesion, tituloSesion } from '../services/progresoService';
import EjercicioIndicado from './EjercicioIndicado';
import EjercicioRegistrable from './EjercicioRegistrable';
import RegistrarEjercicioModal from './RegistrarEjercicioModal';
import { useProgreso } from '../services/ProgresoContext';
import { ESTADOS } from '../services/estadosSesion';

/**
 * Tarjeta de una sesión: al tocarla se expande y muestra sus ejercicios; al tocarla de nuevo se contrae.
 * HU-269: si es de la semana actual del plan activo, cada ejercicio se registra (o corrige) en cualquier orden y,
 * con todos hechos, se finaliza. El backend decide si es registrable.
 */
export default function AcordeonSesion({ sesion, planId, semana, onActualizada }) {
  const { rutaBase } = useProgreso();
  const [abierta, setAbierta] = useState(false);
  const [elegido, setElegido] = useState(null);
  const [finalizando, setFinalizando] = useState(false);
  const [error, setError] = useState('');
  const panelId = useId();
  const { tono, texto, icono: Icono } = ESTADOS[sesion.estado];
  const hechos = sesion.ejercicios.filter((e) => e.completo).length;
  const total = sesion.ejercicios.length;

  const finalizar = async () => {
    setFinalizando(true);
    setError('');
    try {
      onActualizada(await completarSesion(sesion.registroId));
    } catch (err) {
      setError(err.message || 'No se pudo finalizar la sesión');
    } finally {
      setFinalizando(false);
    }
  };

  return (
    <Panel as="article">
      <button
        type="button"
        onClick={() => setAbierta((actual) => !actual)}
        aria-expanded={abierta}
        aria-controls={panelId}
        className={cx('flex min-h-11 w-full cursor-pointer items-center gap-2.5 rounded-lg py-3 pl-4 pr-3 text-left', FOCUS)}
      >
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="text-sm font-bold text-ink">{tituloSesion(sesion.nombre, sesion.numero)}</span>
          <span className="text-xs text-ink-soft">{textoDetalleSesion(sesion)}</span>
        </span>
        <Badge tone={tono} className="shrink-0 gap-1">
          {Icono && <Icono className="h-3 w-3" />}
          {sesion.estado === 'EN_CURSO' ? `${texto} · ${hechos}/${total}` : texto}
        </Badge>
        <ChevronDown className={cx('h-5 w-5 shrink-0 text-ink-muted transition-transform duration-200', abierta && 'rotate-180')} />
      </button>

      <div id={panelId} hidden={!abierta}>
        {sesion.registrable ? (
          <>
            {sesion.ejercicios.map((ejercicio) => (
              <EjercicioRegistrable key={ejercicio.orden} ejercicio={ejercicio} onElegir={() => setElegido(ejercicio)} />
            ))}
            <div className="flex flex-col gap-2 border-t border-line px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-xs text-ink-soft">{hechos} de {total} ejercicios registrados</p>
              <Button variant="primary" size="sm" onClick={finalizar} disabled={hechos < total || finalizando}>
                {finalizando ? 'Finalizando...' : 'Finalizar sesión'}
              </Button>
            </div>
            {error && <p className="px-4 pb-3 text-sm text-error" role="alert">{error}</p>}
          </>
        ) : (
          <>
            {sesion.ejercicios.map((ejercicio) => <EjercicioIndicado key={ejercicio.orden} ejercicio={ejercicio} />)}
            {sesion.estado === 'REGISTRADA' && sesion.registroId && (
              <Link
                to={`${rutaBase}/sesiones/${sesion.registroId}`}
                className={cx('flex items-center justify-end gap-0.5 border-t border-line px-4 py-3 text-xs font-semibold text-primary hover:underline', FOCUS)}
              >
                Ver lo registrado
                <ChevronRight className="h-4 w-4" />
              </Link>
            )}
          </>
        )}
      </div>

      {elegido && (
        <RegistrarEjercicioModal
          ejercicio={elegido}
          sesion={sesion}
          planId={planId}
          semana={semana}
          onActualizada={onActualizada}
          onCerrar={() => setElegido(null)}
        />
      )}
    </Panel>
  );
}
