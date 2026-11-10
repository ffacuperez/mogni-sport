import { useEffect, useState } from 'react';
import { BellRing, CircleCheck, Smartphone } from 'lucide-react';

import { Button } from '../../shared/components/ui';
import { cx } from '../../shared/components/clases';
import { InstruccionesIOS } from '../../shared/components/BotonInstalarApp';
import {
  activarPush, desactivarPush, necesitaInstalarParaPush, permisoPush, soportaPush, sincronizarPush,
} from '../services/pushService';

/** Tarjeta para activar las notificaciones push en este dispositivo (arriba de la lista de notificaciones). */
export default function AvisosCelular() {
  // cargando | inactivo | activo | bloqueado | instalar | sin-soporte
  const [estado, setEstado] = useState(() => {
    if (necesitaInstalarParaPush()) return 'instalar';
    if (!soportaPush()) return 'sin-soporte';
    if (permisoPush() === 'denied') return 'bloqueado';
    return 'cargando';
  });
  const [ocupado, setOcupado] = useState(false);
  const [mensaje, setMensaje] = useState(null); // { texto, error }
  const [verInstrucciones, setVerInstrucciones] = useState(false);

  // Si este dispositivo ya estaba suscripto se reenvía al backend (por si antes había otra cuenta en el celular)
  useEffect(() => {
    if (estado !== 'cargando') return;
    sincronizarPush()
      .then(suscripcion => setEstado(suscripcion ? 'activo' : 'inactivo'))
      .catch(() => setEstado('inactivo'));
  }, []);

  const ejecutar = async (accion) => {
    setOcupado(true);
    setMensaje(null);
    try {
      await accion();
    } catch (err) {
      setMensaje({ texto: err.message, error: true });
      if (permisoPush() === 'denied') setEstado('bloqueado');
    } finally {
      setOcupado(false);
    }
  };

  const activar = () => ejecutar(async () => {
    await activarPush();
    setEstado('activo');
    setMensaje({ texto: '¡Listo! Te vamos a avisar en este dispositivo.' });
  });

  const desactivar = () => ejecutar(async () => {
    await desactivarPush();
    setEstado('inactivo');
  });

  if (estado === 'cargando' || estado === 'sin-soporte') return null;

  const activo = estado === 'activo';

  return (
    <section className={cx('mb-5 rounded-xl border p-4', activo ? 'border-line bg-card' : 'border-primary/30 bg-card shadow-card')}>
      <div className="flex gap-3">
        <span className={cx(
          'flex h-10 w-10 shrink-0 items-center justify-center rounded-full',
          activo ? 'bg-success-bg text-success' : 'bg-error-bg text-primary',
        )}>
          {activo ? <CircleCheck className="h-5 w-5" /> : estado === 'instalar' ? <Smartphone className="h-5 w-5" /> : <BellRing className="h-5 w-5" />}
        </span>

        <div className="min-w-0 flex-1">
          {estado === 'activo' && (
            <>
              <h2 className="text-sm font-bold text-ink">Avisos al celular activados</h2>
              <p className="mt-0.5 text-xs text-ink-soft">Te avisamos cuando tu cuota esté por vencer o venza.</p>
            </>
          )}
          {estado === 'inactivo' && (
            <>
              <h2 className="text-sm font-bold text-ink">Recibí avisos en tu celular</h2>
              <p className="mt-0.5 text-xs text-ink-soft">Te avisamos cuando tu cuota esté por vencer, aunque tengas la app cerrada.</p>
            </>
          )}
          {estado === 'bloqueado' && (
            <>
              <h2 className="text-sm font-bold text-ink">Las notificaciones están bloqueadas</h2>
              <p className="mt-0.5 text-xs text-ink-soft">Para recibir avisos, permití las notificaciones de Mogni System en la configuración del navegador o del celular.</p>
            </>
          )}
          {estado === 'instalar' && (
            <>
              <h2 className="text-sm font-bold text-ink">Instalá la app para recibir avisos</h2>
              <p className="mt-0.5 text-xs text-ink-soft">En iPhone los avisos solo llegan si agregás Mogni System a tu pantalla de inicio.</p>
            </>
          )}

          <div className="mt-3 flex flex-wrap gap-2">
            {estado === 'inactivo' && <Button variant="primary" size="sm" onClick={activar} disabled={ocupado}>Activar avisos</Button>}
            {estado === 'instalar' && <Button variant="primary" size="sm" onClick={() => setVerInstrucciones(true)}>Cómo instalar</Button>}
            {activo && <Button variant="ghost" size="sm" onClick={desactivar} disabled={ocupado}>Desactivar</Button>}
          </div>

          {mensaje && (
            <p role={mensaje.error ? 'alert' : 'status'} className={cx('mt-2 text-xs', mensaje.error ? 'text-error' : 'text-success')}>
              {mensaje.texto}
            </p>
          )}
        </div>
      </div>

      {verInstrucciones && <InstruccionesIOS onClose={() => setVerInstrucciones(false)} />}
    </section>
  );
}
