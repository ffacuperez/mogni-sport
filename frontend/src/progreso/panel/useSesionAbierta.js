import { useLocation, useNavigate, useSearchParams } from 'react-router';

/**
 * La sesión registrada que se ve en el drawer, en la URL (?sesion=). Abrirla suma una entrada al historial, así Atrás
 * la cierra; cerrarla vuelve atrás si se abrió desde la app, o saca el parámetro si se entró directo con el link.
 * Los demás parámetros (plan, fechas) se conservan.
 */
export function useSesionAbierta() {
  const [params, setParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();

  const conSesion = (id) => (actuales) => {
    const nuevos = new URLSearchParams(actuales);
    if (id == null) nuevos.delete('sesion');
    else nuevos.set('sesion', id);
    return nuevos;
  };

  return {
    sesionId: params.get('sesion'),
    abrir: (id) => setParams(conSesion(id), { state: { drawerSesion: true } }),
    cerrar: () => {
      if (location.state?.drawerSesion) navigate(-1);
      else setParams(conSesion(null), { replace: true });
    },
  };
}
