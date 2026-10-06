import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router';

import Home from './home/pages/Home';
import Login from './auth/pages/Login';
import CambiarPassword from './auth/pages/CambiarPassword';
import AlumnosList from './alumno/pages/AlumnosList';
import AlumnoForm from './alumno/pages/AlumnoForm';
import AlumnoDetalle from './alumno/pages/AlumnoDetalle';
import AlumnoMembresiaForm from './alumno/pages/AlumnoMembresiaForm';
import AlumnoDashboard from './alumno/pages/AlumnoDashboard';
import MembresiasList from './membresia/pages/MembresiasList';
import MembresiaForm from './membresia/pages/MembresiaForm';
import CuotasList from './cuota/pages/CuotasList';
import RegistrarPagoModal from './cuota/pages/RegistrarPagoModal';
import ConfirmModal from './shared/components/ConfirmModal';
import Toast from './shared/components/Toast';
import { debeCambiarPassword, getRol, hasSession, logout, ROL_ALUMNO, ROLES_GESTION } from './auth/services/authService';
import { setUnauthorizedHandler } from './shared/services/httpClient';
import {
  actualizarAlumno,
  crearAlumno,
  darDeBajaAlumno,
  listarAlumnos,
  obtenerAlumno,
  reactivarAlumno,
} from './alumno/services/alumnosService';
import {
  actualizarMembresia,
  crearMembresia,
  eliminarMembresia,
  listarMembresias,
} from './membresia/services/membresiasService';
import { listarCuotas, registrarPago } from './cuota/services/cuotasService';

// --- ICONS (Main App) ---
const IconHome = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>;
const IconUsers = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>;
const IconCard = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/></svg>;
const IconWallet = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"/><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"/><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"/></svg>;
const IconLogout = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>;

function Cargando() {
  return <p className="text-slate-400 text-center mt-16">Cargando...</p>;
}

// Las pantallas con :id en la URL esperan a tener el alumno. Si no está en la lista (p. ej. se recargó
// la página en un alumno dado de baja) se pide al backend.
function ConAlumno({ alumnos, cargado, onCargar, children }) {
  const { id } = useParams();
  const alumno = alumnos.find(a => String(a.id) === id);
  useEffect(() => {
    if (cargado && !alumno) onCargar(id);
  }, [cargado, alumno, id]);
  return alumno ? children(alumno) : <Cargando />;
}

function ConMembresia({ membresias, cargado, onNoEncontrada, children }) {
  const { id } = useParams();
  const membresia = membresias.find(m => String(m.id) === id);
  useEffect(() => {
    if (cargado && !membresia) onNoEncontrada();
  }, [cargado, membresia]);
  return membresia ? children(membresia) : <Cargando />;
}

// --- MAIN APP COMPONENT ---
function MainApp({ onLogout }) {
  const navigate = useNavigate();
  // Cobranzas (HU-45) es solo del propietario; el backend también lo exige
  const esPropietario = getRol() === 'PROPIETARIO';
  const { pathname } = useLocation();
  const [alumnos, setAlumnos] = useState([]);
  const [membresias, setMembresias] = useState([]);
  const [cargado, setCargado] = useState(false);

  const [selectedMembresiaId, setSelectedMembresiaId] = useState(null);
  const [searchAlumno, setSearchAlumno] = useState('');
  const [mostrarInactivos, setMostrarInactivos] = useState(false);
  const [inactivosCargados, setInactivosCargados] = useState(false);

  // --- Cuotas (HU-45) ---
  const [cuotas, setCuotas] = useState([]);
  const [cargandoCuotas, setCargandoCuotas] = useState(false);
  const [selectedCuotaId, setSelectedCuotaId] = useState(null);
  const [searchCuota, setSearchCuota] = useState('');
  const [filtroCuota, setFiltroCuota] = useState('Todas');
  const [cuotaAPagar, setCuotaAPagar] = useState(null);

  const [modalConfig, setModalConfig] = useState({ isOpen: false, title: '', message: '', onConfirm: null });
  const [toastConfig, setToastConfig] = useState({ isVisible: false, message: '', isError: false });

  const displayToast = (msg, isError = false) => {
    setToastConfig({ isVisible: true, message: msg, isError });
    setTimeout(() => setToastConfig({ isVisible: false, message: '', isError: false }), 4000);
  };

  useEffect(() => {
    Promise.all([listarAlumnos(), listarMembresias()])
      .then(([alumnosData, membresiasData]) => {
        setAlumnos(alumnosData);
        setMembresias(membresiasData);
      })
      .catch((error) => displayToast(error.message, true))
      .finally(() => setCargado(true));
  }, []);

  const cargarCuotas = async () => {
    setCargandoCuotas(true);
    try {
      setCuotas(await listarCuotas());
    } catch (error) {
      displayToast(error.message, true);
    } finally {
      setCargandoCuotas(false);
    }
  };

  // Cada vez que se entra a Cuotas se trae el estado actualizado (los vencimientos cambian día a día)
  useEffect(() => {
    if (esPropietario && pathname.startsWith('/cuotas')) cargarCuotas();
  }, [pathname, esPropietario]);

  const cargarAlumno = async (id) => {
    try {
      const alumno = await obtenerAlumno(id);
      setAlumnos(prev => (prev.some(a => a.id === alumno.id) ? prev : [...prev, alumno]));
    } catch (error) {
      displayToast(error.message, true);
      navigate('/alumnos', { replace: true });
    }
  };

  const ejecutar = async (accion, mensajeExito) => {
    try {
      await accion();
      displayToast(mensajeExito);
      return true;
    } catch (error) {
      displayToast(error.message, true);
      return false;
    }
  };

  const showModal = (title, message, onConfirm) => {
    setModalConfig({
      isOpen: true,
      title,
      message,
      onConfirm: () => {
        onConfirm();
        setModalConfig({ ...modalConfig, isOpen: false });
      }
    });
  };

  // HU-246: por defecto solo activos. Los dados de baja se piden al backend la primera vez que se activa el filtro.
  const handleToggleMostrarInactivos = async () => {
    const mostrar = !mostrarInactivos;
    setMostrarInactivos(mostrar);
    if (!mostrar || inactivosCargados) return;
    try {
      const inactivos = await listarAlumnos({ activo: false });
      setAlumnos(prev => [...prev, ...inactivos.filter(i => !prev.some(a => a.id === i.id))]);
      setInactivosCargados(true);
    } catch (error) {
      displayToast(error.message, true);
    }
  };

  const handleToggleEstadoAlumno = (alumno) => {

    const isActivo = alumno.estado === 'Activo';
    const actionText = isActivo ? 'dar de baja' : 'activar';
    const newEstado = isActivo ? 'Inactivo' : 'Activo';

    showModal(
      'Confirmar Acción',
      `¿Estás seguro de que deseas ${actionText} al alumno ${alumno.nombre} ${alumno.apellido}?`,
      async () => {
        // Se queda en el detalle, que ya muestra el estado actualizado
        await ejecutar(async () => {
          const actualizado = isActivo ? await darDeBajaAlumno(alumno.id) : await reactivarAlumno(alumno.id);
          setAlumnos(prev => prev.map(a => a.id === actualizado.id ? actualizado : a));
        }, `El alumno ha sido ${newEstado === 'Activo' ? 'activado' : 'dado de baja'} correctamente.`);
      }
    );
  };

  const handleSaveAlumno = (formData, isEdit) => {
    // HU-247: si cambia el DNI se avisa que también cambia la contraseña inicial. Cancelar no guarda nada.
    const original = isEdit ? alumnos.find(a => a.id === formData.id) : null;
    const dniCambio = Boolean(original) && original.dni !== formData.dni;
    let mensaje = '¿Desea confirmar el alta del nuevo alumno?';
    if (isEdit) {
      mensaje = dniCambio
        ? 'Cambiar el DNI también cambiará la contraseña inicial del alumno si aún no la modificó. ¿Estás seguro?'
        : '¿Desea confirmar los cambios en el alumno?';
    }
    showModal(
      isEdit ? (dniCambio ? 'Confirmar cambio de DNI' : 'Confirmar Cambios') : 'Confirmar Alta',
      mensaje,
      async () => {
        const ok = await ejecutar(async () => {
          const guardado = isEdit ? await actualizarAlumno(formData.id, formData) : await crearAlumno(formData);
          setAlumnos(prev => isEdit ? prev.map(a => a.id === guardado.id ? guardado : a) : [...prev, guardado]);
        }, isEdit ? 'Los cambios fueron guardados correctamente.' : 'El alumno ha sido creado correctamente.');
        if (ok) navigate(isEdit ? `/alumnos/${formData.id}` : '/alumnos'); // al editar vuelve al detalle
      }
    );
  };

  // Cambio de membresía de un alumno: el backend cancela la inscripción activa y crea una nueva
  const handleSaveMembresiaAlumno = (formData) => {
    showModal(
      'Confirmar cambio de membresía',
      formData.membresiaId
        ? '¿Desea cambiar la membresía del alumno? Se cancelará la inscripción activa y se creará una nueva desde hoy.'
        : '¿Desea dejar al alumno sin membresía? Se cancelará la inscripción activa.',
      async () => {
        const ok = await ejecutar(async () => {
          const guardado = await actualizarAlumno(formData.id, formData);
          setAlumnos(prev => prev.map(a => a.id === guardado.id ? guardado : a));
        }, 'La membresía del alumno fue actualizada correctamente.');
        if (ok) navigate(`/alumnos/${formData.id}`);
      }
    );
  };

  const handleSaveMembresia = (formData, isEdit) => {
    showModal(
      isEdit ? 'Confirmar Cambios' : 'Confirmar Creación',
      isEdit ? '¿Desea confirmar los cambios en la membresía?' : '¿Desea confirmar la creación de la nueva membresía?',
      async () => {
        const ok = await ejecutar(async () => {
          const guardada = isEdit ? await actualizarMembresia(formData.id, formData) : await crearMembresia(formData);
          setMembresias(prev => isEdit ? prev.map(m => m.id === guardada.id ? guardada : m) : [...prev, guardada]);
          if (isEdit) {
            setAlumnos(prev => prev.map(a => ({
              ...a,
              membresia: a.membresiaId === guardada.id ? guardada.nombre : a.membresia,
              inscripcion: a.inscripcion?.membresiaId === guardada.id ? { ...a.inscripcion, membresia: guardada.nombre } : a.inscripcion,
            })));
          }
        }, isEdit ? 'Membresía modificada correctamente.' : 'Membresía creada correctamente.');
        if (ok) {
          setSelectedMembresiaId(null);
          navigate('/membresias');
        }
      }
    );
  };

  // HU-45: registra el pago en el backend (la cuota pasa a PAGADA) y refresca la lista.
  // Si falla, el modal queda abierto para corregir los datos.
  const handleRegistrarPago = async (datos) => {
    const ok = await ejecutar(() => registrarPago(datos), 'El pago fue registrado correctamente.');
    if (ok) {
      setCuotaAPagar(null);
      setSelectedCuotaId(null);
      cargarCuotas();
    }
  };

  const handleDeleteMembresia = () => {
    const membresia = membresias.find(m => m.id === selectedMembresiaId);
    if (!membresia) return;

    showModal(
      'Confirmar Eliminación',
      `¿Está seguro de que desea eliminar la membresía ${membresia.nombre}?`,
      async () => {
        const ok = await ejecutar(async () => {
          await eliminarMembresia(membresia.id);
          setMembresias(prev => prev.filter(m => m.id !== membresia.id));
        }, 'Membresía eliminada correctamente.');
        if (ok) setSelectedMembresiaId(null);
      }
    );
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-200 font-sans flex flex-col md:flex-row selection:bg-red-500/30">

      <aside className="w-full md:w-64 bg-slate-950 border-r border-slate-800 flex flex-col flex-shrink-0">
        <div className="p-6 flex justify-center items-center border-b border-slate-800/50">
          <div className="w-32 h-32 rounded-full border-2 border-red-600 flex items-center justify-center bg-slate-900 shadow-[0_0_15px_rgba(220,38,38,0.3)]">
            <div className="text-center">
              <span className="block text-red-500 font-black text-2xl tracking-tighter leading-none">MOGNI</span>
              <span className="block text-slate-300 text-xs tracking-widest mt-1">SPORT</span>
            </div>
          </div>
        </div>

        <nav className="flex-1 py-4">
          <ul className="space-y-1 text-sm font-medium">
            <li>
              <button onClick={() => navigate('/')} className="w-full flex items-center gap-3 px-6 py-3 text-slate-400 hover:text-slate-100 hover:bg-slate-900 transition-colors">
                <IconHome /> Inicio
              </button>
            </li>
            <li>
              <button
                onClick={() => navigate('/alumnos')}
                className={`w-full flex items-center gap-3 px-6 py-3 transition-colors ${pathname.startsWith('/alumnos') ? 'text-slate-100 bg-slate-900 border-l-4 border-red-600' : 'text-slate-400 hover:text-slate-100 hover:bg-slate-900 border-l-4 border-transparent'}`}
              >
                <IconUsers /> Alumnos
              </button>
            </li>
            <li>
              <button
                onClick={() => { setSelectedMembresiaId(null); navigate('/membresias'); }}
                className={`w-full flex items-center gap-3 px-6 py-3 transition-colors ${pathname.startsWith('/membresias') ? 'text-slate-100 bg-slate-900 border-l-4 border-red-600' : 'text-slate-400 hover:text-slate-100 hover:bg-slate-900 border-l-4 border-transparent'}`}
              >
                <IconCard /> Membresías
              </button>
            </li>
            {esPropietario && (
            <li>
              <button
                onClick={() => { setSelectedCuotaId(null); navigate('/cuotas'); }}
                className={`w-full flex items-center gap-3 px-6 py-3 transition-colors ${pathname.startsWith('/cuotas') ? 'text-slate-100 bg-slate-900 border-l-4 border-red-600' : 'text-slate-400 hover:text-slate-100 hover:bg-slate-900 border-l-4 border-transparent'}`}
              >
                <IconWallet /> Cuotas
              </button>
            </li>
            )}
            <li>
              <button onClick={onLogout} className="w-full flex items-center gap-3 px-6 py-3 text-slate-400 hover:text-slate-100 hover:bg-slate-900 transition-colors mt-8">
                <IconLogout /> Cerrar Sesión
              </button>
            </li>
          </ul>
        </nav>
      </aside>

      <main className="flex-1 p-4 md:p-8 overflow-y-auto bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0IiBoZWlnaHQ9IjQiPgo8cmVjdCB3aWR0aD0iNCIgaGVpZ2h0PSI0IiBmaWxsPSIjMGYxNzJhIj48L3JlY3Q+CjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiMxZTI5M2IiPjwvcmVjdD4KPC9zdmc+')]">

        <Routes>
          <Route path="alumnos" element={
            <AlumnosList
              alumnos={alumnos}
              onSelect={(id) => navigate(`/alumnos/${id}`)}
              onAdd={() => navigate('/alumnos/nuevo')}
              search={searchAlumno}
              setSearch={setSearchAlumno}
              mostrarInactivos={mostrarInactivos}
              onToggleMostrarInactivos={handleToggleMostrarInactivos}
            />
          } />

          <Route path="alumnos/nuevo" element={
            <AlumnoForm
              membresias={membresias}
              onSave={handleSaveAlumno}
              onCancel={() => navigate('/alumnos')}
              onError={(msg) => displayToast(msg, true)}
            />
          } />

          <Route path="alumnos/:id" element={
            <ConAlumno alumnos={alumnos} cargado={cargado} onCargar={cargarAlumno}>
              {(alumno) => (
                <AlumnoDetalle
                  alumno={alumno}
                  onBack={() => navigate('/alumnos')}
                  onEdit={() => navigate(`/alumnos/${alumno.id}/editar`)}
                  onEditMembresia={() => navigate(`/alumnos/${alumno.id}/membresia`)}
                  onToggleStatus={() => handleToggleEstadoAlumno(alumno)}
                />
              )}
            </ConAlumno>
          } />

          <Route path="alumnos/:id/editar" element={
            <ConAlumno alumnos={alumnos} cargado={cargado} onCargar={cargarAlumno}>
              {(alumno) => (
                <AlumnoForm
                  alumnoToEdit={alumno}
                  membresias={membresias}
                  onSave={handleSaveAlumno}
                  onCancel={() => navigate(`/alumnos/${alumno.id}`)}
                  onError={(msg) => displayToast(msg, true)}
                />
              )}
            </ConAlumno>
          } />

          <Route path="alumnos/:id/membresia" element={
            <ConAlumno alumnos={alumnos} cargado={cargado} onCargar={cargarAlumno}>
              {(alumno) => (
                <AlumnoMembresiaForm
                  alumno={alumno}
                  membresias={membresias}
                  onSave={handleSaveMembresiaAlumno}
                  onCancel={() => navigate(`/alumnos/${alumno.id}`)}
                  onError={(msg) => displayToast(msg, true)}
                />
              )}
            </ConAlumno>
          } />

          <Route path="membresias" element={
            <MembresiasList
              membresias={membresias}
              selectedId={selectedMembresiaId}
              setSelectedId={setSelectedMembresiaId}
              onAdd={() => navigate('/membresias/nueva')}
              onEdit={() => navigate(`/membresias/${selectedMembresiaId}/editar`)}
              onDelete={handleDeleteMembresia}
            />
          } />

          <Route path="membresias/nueva" element={
            <MembresiaForm
              onSave={handleSaveMembresia}
              onCancel={() => navigate('/membresias')}
              onError={(msg) => displayToast(msg, true)}
            />
          } />

          <Route path="membresias/:id/editar" element={
            <ConMembresia
              membresias={membresias}
              cargado={cargado}
              onNoEncontrada={() => {
                displayToast('No se encontró la membresía.', true);
                navigate('/membresias', { replace: true });
              }}
            >
              {(membresia) => (
                <MembresiaForm
                  membresiaToEdit={membresia}
                  onSave={handleSaveMembresia}
                  onCancel={() => navigate('/membresias')}
                  onError={(msg) => displayToast(msg, true)}
                />
              )}
            </ConMembresia>
          } />

          <Route path="cuotas" element={esPropietario ? (
            <CuotasList
              cuotas={cuotas}
              cargando={cargandoCuotas}
              selectedId={selectedCuotaId}
              setSelectedId={setSelectedCuotaId}
              onRegistrarPago={setCuotaAPagar}
              searchTerm={searchCuota}
              setSearchTerm={setSearchCuota}
              filtro={filtroCuota}
              setFiltro={setFiltroCuota}
            />
          ) : <Navigate to="/alumnos" replace />} />

          <Route path="*" element={<Navigate to="/alumnos" replace />} />
        </Routes>

      </main>

      <ConfirmModal
        isOpen={modalConfig.isOpen}
        title={modalConfig.title}
        message={modalConfig.message}
        onConfirm={modalConfig.onConfirm}
        onCancel={() => setModalConfig({ ...modalConfig, isOpen: false })}
      />

      {cuotaAPagar && (
        <RegistrarPagoModal
          cuota={cuotaAPagar}
          onConfirm={handleRegistrarPago}
          onCancel={() => setCuotaAPagar(null)}
          onError={(msg) => displayToast(msg, true)}
        />
      )}

      <Toast
        message={toastConfig.message}
        isVisible={toastConfig.isVisible}
        isError={toastConfig.isError}
      />

    </div>
  );
}

const esRolValido = (rol) => rol === ROL_ALUMNO || ROLES_GESTION.includes(rol);

// Pantalla a la que va cada rol al iniciar sesión
const rutaInicial = (rol) => {
  if (rol === ROL_ALUMNO) return '/mi-cuenta';
  if (ROLES_GESTION.includes(rol)) return '/alumnos';
  return '/login';
};

// Sin sesión manda al login (recordando a dónde quería ir); con otro rol, a su propia pantalla
function RequiereRol({ sesion, permitido, children }) {
  const location = useLocation();
  if (!sesion.isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (!permitido) return <Navigate to={rutaInicial(sesion.rol)} replace />;
  return children;
}

// El rol sale del token firmado por el backend, no de algo que elija el frontend
const leerSesion = () => {
  // Una sesión con un rol desconocido no puede ver ninguna vista: se descarta
  if (hasSession() && !esRolValido(getRol())) logout();
  return { isAuthenticated: hasSession(), rol: getRol(), debeCambiarPassword: debeCambiarPassword() };
};

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const [authSession, setAuthSession] = useState(leerSesion);

  // Rol y cambio de contraseña pendiente salen del token recién guardado
  const actualizarSesionDesdeToken = () => {
    const sesion = leerSesion();
    setAuthSession(sesion);
    return sesion;
  };

  // Después del login vuelve a la URL que se quiso abrir sin sesión, o a la pantalla inicial del rol
  const handleLogin = () => {
    const sesion = actualizarSesionDesdeToken();
    navigate(location.state?.from ?? rutaInicial(sesion.rol), { replace: true });
  };

  const handleLogout = () => {
    logout();
    setAuthSession({ isAuthenticated: false, rol: null, debeCambiarPassword: false });
    navigate('/login');
  };

  const esAlumno = authSession.rol === ROL_ALUMNO;
  const esPersonal = ROLES_GESTION.includes(authSession.rol);

  useEffect(() => {
    setUnauthorizedHandler(handleLogout);
    localStorage.removeItem('user_rol'); // clave de la versión anterior, ya no se usa
  }, []);

  // HU-248: con la contraseña inicial pendiente de cambio, esta pantalla va antes que cualquier otra
  // (incluida la Home), sea cual sea la URL; solo se sale cambiándola o cerrando sesión.
  if (authSession.isAuthenticated && authSession.debeCambiarPassword) {
    return <CambiarPassword onSuccess={actualizarSesionDesdeToken} onLogout={handleLogout} />;
  }

  return (
    <Routes>
      <Route path="/" element={
        <Home
          isAuthenticated={authSession.isAuthenticated}
          onLogin={() => navigate(authSession.isAuthenticated ? rutaInicial(authSession.rol) : '/login')}
        />
      } />

      <Route path="/login" element={
        authSession.isAuthenticated
          ? <Navigate to={rutaInicial(authSession.rol)} replace />
          : <Login onLogin={handleLogin} onBack={() => navigate('/')} />
      } />

      {/* Dashboard: solo rol ALUMNO */}
      <Route path="/mi-cuenta" element={
        <RequiereRol sesion={authSession} permitido={esAlumno}>
          <div className="min-h-screen bg-slate-950 text-slate-200 p-6 relative">
            <div className="absolute top-4 right-4 z-20">
              <button
                onClick={handleLogout}
                className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-bold transition shadow-lg cursor-pointer flex items-center gap-2"
              >
                Cerrar Sesión
              </button>
            </div>

            <div className="pt-10">
              <AlumnoDashboard />
            </div>
          </div>
        </RequiereRol>
      } />

      {/* Panel de gestión: solo PROPIETARIO, ENTRENADOR y NUTRICIONISTA. Sus rutas están en MainApp */}
      <Route path="/*" element={
        <RequiereRol sesion={authSession} permitido={esPersonal}>
          <MainApp onLogout={handleLogout} />
        </RequiereRol>
      } />
    </Routes>
  );
}
