import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { ChevronRight, History } from 'lucide-react';

import { Badge, Cargando, Panel, Row, Table } from '../../shared/components/ui';
import { textoNivel, textoObjetivo } from '../../plantilla/services/plantillasService';
import BadgeEstadoPlan from '../components/BadgeEstadoPlan';
import ErrorConsulta from '../components/ErrorConsulta';
import { useProgreso } from '../services/ProgresoContext';
import { textoRangoPlan, textoSemanas, tituloPlan } from '../services/progresoService';
import EstadoVacioPanel, { ContenidoVacio } from './EstadoVacioPanel';

const COLUMNAS = [
  { label: 'Método · Mesociclo', className: 'pl-4' },
  { label: 'Nivel' },
  { label: 'Objetivo' },
  { label: 'Inicio – fin' },
  { label: 'Duración' },
  { label: 'Sesiones' },
  { label: 'Estado' },
  { label: '', className: 'pr-4' },
];

/**
 * Vista "Historial de planes": el activo primero (resaltado, lleva a Plan actual) y los anteriores, que abren su
 * detalle. Con activo y sin anteriores, debajo de su fila va el aviso; sin ningún plan, solo el estado vacío.
 */
export default function HistorialPlanes() {
  const { consultas, rutaBase, textos } = useProgreso();
  const navigate = useNavigate();
  const [planes, setPlanes] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultas.consultarPlanes()
      .then((respuesta) => { if (vigente) setPlanes(respuesta); })
      .catch((err) => { if (vigente) setError(err); });
    return () => { vigente = false; };
  }, [consultas]);

  if (error) return <ErrorConsulta error={error} />;

  if (!planes) return <Cargando texto={textos.cargandoPlanes} />;

  const sinAnteriores = planes.anteriores.length === 0;

  if (!planes.activo && sinAnteriores) return <EstadoVacioPanel icono={History} {...textos.sinAnteriores} />;

  const filas = [planes.activo, ...planes.anteriores].filter(Boolean);

  return (
    <Panel className="p-5">
      <Table columns={COLUMNAS}>
        {filas.map((plan) => {
          const activo = plan.estado === 'ACTIVO';
          return (
            <Row
              key={plan.id}
              selected={activo}
              onActivate={() => navigate(activo ? rutaBase : `${rutaBase}/planes/${plan.id}`)}
              title={activo ? 'Ver el plan actual' : 'Ver el detalle del plan'}
            >
              <td className="py-3 pl-4 pr-3 font-semibold">{tituloPlan(plan)}</td>
              <td className="px-3">{plan.nivel && <Badge className="font-semibold">{textoNivel(plan.nivel)}</Badge>}</td>
              <td className="max-w-56 truncate px-3">{plan.objetivo ? textoObjetivo(plan.objetivo) : '—'}</td>
              <td className="px-3 tabular-nums">{textoRangoPlan(plan)}</td>
              <td className="px-3">{textoSemanas(plan.cantidadSemanas)}</td>
              <td className="px-3 font-semibold tabular-nums">{plan.sesionesRegistradas} / {plan.sesionesTotales}</td>
              <td className="px-3"><BadgeEstadoPlan estado={plan.estado} /></td>
              <td className="pl-3 pr-4 text-right">
                <span className="inline-flex items-center gap-0.5 font-semibold text-primary">
                  Ver detalle <ChevronRight className="h-4 w-4" />
                </span>
              </td>
            </Row>
          );
        })}
      </Table>
      {sinAnteriores && <ContenidoVacio icono={History} {...textos.sinAnteriores} className="px-6 pb-6 pt-12" />}
    </Panel>
  );
}
