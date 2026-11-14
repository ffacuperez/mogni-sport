import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';

import { Cargando } from '../../shared/components/ui';
import ComparativaEjercicio from '../components/ComparativaEjercicio';
import EncabezadoDetalle from '../components/EncabezadoDetalle';
import ErrorConsulta from '../components/ErrorConsulta';
import { useProgreso } from '../services/ProgresoContext';
import { textoFechaCompleta, tituloSesion } from '../services/progresoService';

/** Detalle de una sesión registrada: lo indicado y lo realizado en cada ejercicio. */
export default function DetalleSesion() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { consultas, rutaBase } = useProgreso();
  const [sesion, setSesion] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarSesion(id)
      .then((respuesta) => { if (vigente) setSesion(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas, id]);

  // Si se entró directo a la URL no hay a dónde volver dentro de la app
  const volver = () => (location.key === 'default' ? navigate(`${rutaBase}/sesiones`) : navigate(-1));

  if (error) return <ErrorConsulta error={error} />;

  if (!sesion) return <Cargando texto="Cargando la sesión" />;

  return (
    <div className="animate-fade-in">
      <EncabezadoDetalle
        titulo={`${tituloSesion(sesion.nombreSesion, sesion.numeroSesion)} · Semana ${sesion.semana}`}
        subtitulo={`${textoFechaCompleta(sesion.fecha)} · ${sesion.plan.nombre}`}
        onVolver={volver}
      />
      <div className="flex flex-col gap-3">
        {sesion.ejercicios.map((ejercicio, i) => <ComparativaEjercicio key={i} ejercicio={ejercicio} />)}
      </div>
    </div>
  );
}
