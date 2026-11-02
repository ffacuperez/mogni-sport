import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router';
import { Calendar, ClipboardList, Clock, CreditCard, Dumbbell, Home as IconHome, LogOut, Users, Wallet } from 'lucide-react';

import Home from './home/pages/Home';
import Login from './auth/pages/Login';
import CambiarPassword from './auth/pages/CambiarPassword';
import AlumnosList from './alumno/pages/AlumnosList';
import AlumnoForm from './alumno/pages/AlumnoForm';
import AlumnoDetalle from './alumno/pages/AlumnoDetalle';
import AlumnoMembresiaForm from './alumno/pages/AlumnoMembresiaForm';
import ReactivarAlumnoModal from './alumno/pages/ReactivarAlumnoModal';
import AlumnoLayout from './alumno/pages/AlumnoLayout';
import AlumnoPerfil from './alumno/pages/AlumnoPerfil';
import MiEntrenamiento from './alumno/pages/MiEntrenamiento';
import SeccionProximamente from './alumno/pages/SeccionProximamente';
import Notificaciones from './notificacion/pages/Notificaciones';
import MembresiasList from './membresia/pages/MembresiasList';
import MembresiaForm from './membresia/pages/MembresiaForm';
import EjerciciosList from './ejercicio/pages/EjerciciosList';
import EjercicioForm from './ejercicio/pages/EjercicioForm';
import PlantillasList from './plantilla/pages/PlantillasList';
import PlantillaForm from './plantilla/pages/PlantillaForm';
import CuotasList from './cuota/pages/CuotasList';
import RegistrarPagoModal from './cuota/pages/RegistrarPagoModal';
import HistorialPagosModal from './cuota/pages/HistorialPagosModal';
import HorariosGimnasio from './turno/pages/HorariosGimnasio';
import TurnosAlumno from './turno/pages/TurnosAlumno';
import Agenda from './turno/pages/Agenda';
import ConfirmModal from './shared/components/ConfirmModal';
import Toast from './shared/components/Toast';
import { useToast } from './shared/hooks/useToast';
import { Cargando, Logo } from './shared/components/ui';
import { FOCUS } from './shared/components/clases';
import { debeCambiarPassword, getRol, hasSession, logout, ROL_ALUMNO, ROLES_GESTION } from './auth/services/authService';
import { setUnauthorizedHandler } from './shared/services/httpClient';
import {
  actualizarAlumno,
  cambiarMembresia,
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
import {
  actualizarEjercicio,
  crearEjercicio,
  eliminarEjercicio,
  listarEjercicios,
  toggleEstadoEjercicio,
} from './ejercicio/services/ejerciciosService';
import {
  formatearFecha,
  formatearMonto,
  labelMedioDePago,
  listarCuotas,
  obtenerHistorialDePagos,
  registrarPago,
} from './cuota/services/cuotasService';

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

function ConEjercicio({ ejercicios, cargado, onNoEncontrado, children }) {
  const { id } = useParams();
  const ejercicio = ejercicios.find(e => String(e.id) === id);
  useEffect(() => {
    if (cargado && !ejercicio) onNoEncontrado();
  }, [cargado, ejercicio]);
  return ejercicio ? children(ejercicio) : <Cargando />;
}

// La plantilla la carga el propio formulario a partir del :id de la URL
function PlantillaFormRuta(props) {
  const { id } = useParams();
  return <PlantillaForm key={id} plantillaId={Number(id)} {...props} />;
}

// --- MAIN APP COMPONENT ---
function MainApp({ onLogout }) {
  const navigate = useNavigate();
  // Cobranzas (HU-45) es solo del propietario; el backend también lo exige
  const esPropietario = getRol() === 'PROPIETARIO';
  // Agenda de turnos (MOGNI-257): propietario y entrenador; el backend también lo exige
  const puedeVerAgenda = esPropietario || getRol() === 'ENTRENADOR';
  // Plantillas de plan de entrenamiento (HU-265): propietario y entrenador; el backend también lo exige
  const puedeVerPlanes = esPropietario || getRol() === 'ENTRENADOR';
  const { pathname } = useLocation();
  const [alumnos, setAlumnos] = useState([]);
  const [membresias, setMembresias] = useState([]);
  const [ejercicios, setEjercicios] = useState([]);
  const [cargado, setCargado] = useState(false);

  const [selectedMembresiaId, setSelectedMembresiaId] = useState(null);
  const [selectedEjercicioIds, setSelectedEjercicioIds] = useState([]);
  const [searchAlumno, setSearchAlumno] = useState('');

  // --- Cuotas (HU-45) ---
  const [cuotas, setCuotas] = useState([]);
  const [cargandoCuotas, setCargandoCuotas] = useState(false);
  // Una fila por alumno: se selecciona por alumnoId
  const [selectedCuotaId, setSelectedCuotaId] = useState(null);
  const [searchCuota, setSearchCuota] = useState('');
  const [filtroCuota, setFiltroCuota] = useState('Todas');
  const [cuotaAPagar, setCuotaAPagar] = useState(null);

  // --- Historial de pagos (HU-48) ---
  const [alumnoHistorial, setAlumnoHistorial] = useState(null);
  const [pagosHistorial, setPagosHistorial] = useState([]);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);

  // --- Reactivación con pago (MOGNI-246) ---
  const [alumnoAReactivar, setAlumnoAReactivar] = useState(null);

  const [modalConfig, setModalConfig] = useState({ isOpen: false, title: '', message: '', danger: false, onConfirm: null });
  const [toast, mostrarToast] = useToast();

  const displayToast = (msg, isError = false) => mostrarToast(msg, isError ? 'error' : 'success');
  const cerrarModal = () => setModalConfig(prev => ({ ...prev, isOpen: false }));

  useEffect(() => {
    // 1. Cargamos prioritariamente alumnos y membresías para que la app nunca se quede en 0
    Promise.all([listarAlumnos(), listarAlumnos({ activo: false }), listarMembresias()])
      .then(([alumnosActivos, alumnosInactivos, membresiasData]) => {
        setAlumnos([...alumnosActivos, ...alumnosInactivos]);
        setMembresias(membresiasData);
      })
      .catch((error) => displayToast(error.message, true))
      .finally(() => setCargado(true));

    // 2. Intentamos cargar los ejercicios por separado de forma independiente
    listarEjercicios()
      .then((ejerciciosData) => {
        setEjercicios(ejerciciosData);
      })
      .catch((error) => {
        console.warn('El backend aún no tiene el módulo de ejercicios desplegado:', error.message);
      });
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

  // danger: acción destructiva (baja, eliminación); el botón de confirmar se muestra en rojo
  const showModal = (title, message, onConfirm, { danger = false } = {}) => {
    setModalConfig({
      isOpen: true,
      title,
      message,
      danger,
      onConfirm: () => {
        cerrarModal();
        onConfirm();
      }
    });
  };

  const reemplazarAlumno = (actualizado) => setAlumnos(prev => prev.map(a => a.id === actualizado.id ? actualizado : a));

  // MOGNI-246: la baja cancela la inscripción y los turnos; la reactivación cobra la primera cuota salvo que todavía
  // tenga días pagos (en ese caso vuelve con su misma inscripción, sin cobrar)
  const handleToggleEstadoAlumno = (alumno) => {
    const nombre = `${alumno.nombre} ${alumno.apellido}`;

    if (alumno.estado === 'Activo') {
      showModal(
        'Confirmar baja',
        `¿Estás seguro de que deseas dar de baja al alumno ${nombre}? Se cancelarán su inscripción y sus turnos reservados que todavía no empezaron. Si ya pagó el mes siguiente, no se devuelve.`,
        async () => {
          // Se queda en el detalle, que ya muestra el estado actualizado
          await ejecutar(async () => reemplazarAlumno(await darDeBajaAlumno(alumno.id)),
            'El alumno ha sido dado de baja correctamente.');
        },
        { danger: true }
      );
      return;
    }

    if (alumno.reactivacionRequierePago) {
      setAlumnoAReactivar(alumno);
      return;
    }

    showModal(
      'Confirmar reactivación',
      `${nombre} todavía tiene días pagos hasta el ${formatearFecha(alumno.vencimientoUltimaCuota)}: se reactiva con su misma membresía, sin cobrar nada. ¿Confirmás?`,
      async () => {
        await ejecutar(async () => reemplazarAlumno(await reactivarAlumno(alumno.id)),
          'El alumno ha sido activado correctamente.');
      }
    );
  };

  const handleReactivarConPago = async (datos) => {
    const ok = await ejecutar(async () => reemplazarAlumno(await reactivarAlumno(alumnoAReactivar.id, datos)),
      'El alumno fue reactivado y el pago de la primera cuota quedó registrado.');
    if (ok) setAlumnoAReactivar(null);
  };

  const handleSaveAlumno = (formData, isEdit) => {
    // HU-247: si cambia el DNI se avisa que también cambia la contraseña inicial. Cancelar no guarda nada.
    const original = isEdit ? alumnos.find(a => a.id === formData.id) : null;
    const dniCambio = Boolean(original) && original.dni !== formData.dni;
    let mensaje;
    if (isEdit) {
      mensaje = dniCambio
        ? 'Cambiar el DNI también cambiará la contraseña inicial del alumno si aún no la modificó. ¿Estás seguro?'
        : '¿Desea confirmar los cambios en el alumno?';
    } else {
      // MOGNI-73: el alta registra el pago de la primera cuota, que no se puede deshacer
      const membresia = membresias.find(m => String(m.id) === String(formData.membresiaId));
      const monto = membresia ? `${formatearMonto(membresia.precio)} ` : '';
      mensaje = `¿Desea confirmar el alta del nuevo alumno? Se registrará el pago de la primera cuota (${monto}en ${
        labelMedioDePago(formData.medioDePago).toLowerCase()}). El pago no se puede deshacer.`;
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

  // Cambio de membresía (reglas M1 al M9)
  const handleSaveMembresiaAlumno = async (alumnoId, datos) => {
    const ok = await ejecutar(async () => reemplazarAlumno(await cambiarMembresia(alumnoId, datos)),
      'La membresía del alumno fue actualizada correctamente.');
    if (ok) navigate(`/alumnos/${alumnoId}`);
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
      },
      { danger: true }
    );
  };

  // --- Manejadores de Ejercicios ---
  const handleSaveEjercicio = (formData, isEdit) => {
    showModal(
      isEdit ? 'Confirmar Cambios' : 'Confirmar Creación',
      isEdit ? '¿Desea confirmar los cambios en el ejercicio?' : '¿Desea confirmar la creación del nuevo ejercicio?',
      async () => {
        const ok = await ejecutar(async () => {
          const guardado = isEdit ? await actualizarEjercicio(formData.id, formData) : await crearEjercicio(formData);
          setEjercicios(prev => isEdit ? prev.map(e => e.id === guardado.id ? guardado : e) : [...prev, guardado]);
        }, isEdit ? 'Ejercicio modificado correctamente.' : 'Ejercicio creado correctamente.');
        if (ok) {
          setSelectedEjercicioIds([]);
          navigate('/ejercicios');
        }
      }
    );
  };

  // El backend opera de a un ejercicio: se manda una request por cada uno. Los que fallan quedan seleccionados
  // para reintentar; el resto se aplica igual.
  const procesarEjercicios = async (lista, accion, { exito, exitoPlural, verbo }) => {
    const resultados = await Promise.allSettled(lista.map(accion));
    const fallidos = lista.filter((_, i) => resultados[i].status === 'rejected');
    const primerError = resultados.find(r => r.status === 'rejected')?.reason;
    setSelectedEjercicioIds(fallidos.map(e => e.id));

    if (fallidos.length === 0) {
      displayToast(lista.length === 1 ? `El ejercicio fue ${exito} correctamente.` : `${lista.length} ejercicios fueron ${exitoPlural} correctamente.`);
    } else if (lista.length === 1) {
      displayToast(primerError.message, true);
    } else {
      displayToast(`No se pudieron ${verbo} ${fallidos.length} de ${lista.length} ejercicios: ${primerError.message}`, true);
    }
  };

  const handleCambiarEstadoEjercicios = (lista, activar) => {
    const cambiar = () => procesarEjercicios(lista, async (ejercicio) => {
      const actualizado = await toggleEstadoEjercicio(ejercicio.id);
      setEjercicios(prev => prev.map(e => e.id === actualizado.id ? actualizado : e));
    }, activar
      ? { exito: 'reactivado', exitoPlural: 'reactivados', verbo: 'reactivar' }
      : { exito: 'dado de baja', exitoPlural: 'dados de baja', verbo: 'dar de baja' });

    // Reactivar no necesita confirmación; la baja sí
    if (activar) {
      cambiar();
      return;
    }
    showModal(
      'Confirmar Baja',
      lista.length === 1
        ? `¿Estás seguro de que deseas dar de baja el ejercicio ${lista[0].nombre}?`
        : `¿Estás seguro de que deseas dar de baja ${lista.length} ejercicios?`,
      cambiar
    );
  };

  const handleDeleteEjercicios = (lista) => {
    showModal(
      'Confirmar Eliminación',
      `${lista.length === 1
        ? `¿Está seguro de que desea eliminar el ejercicio ${lista[0].nombre}?`
        : `¿Está seguro de que desea eliminar ${lista.length} ejercicios?`} Esta acción no se puede deshacer.`,
      () => procesarEjercicios(lista, async (ejercicio) => {
        await eliminarEjercicio(ejercicio.id);
        setEjercicios(prev => prev.filter(e => e.id !== ejercicio.id));
      }, { exito: 'eliminado', exitoPlural: 'eliminados', verbo: 'eliminar' }),
      { danger: true }
    );
  };

  // MOGNI-45: renovación
  const handleRegistrarPago = async (datos) => {
    const ok = await ejecutar(() => registrarPago(datos), 'El pago fue registrado correctamente.');
    if (ok) {
      setCuotaAPagar(null);
      cargarCuotas();
    }
  };

  // HU-48: historial de pagos
  const handleVerHistorial = async (cuota) => {
    if (!cuota) return;
    setAlumnoHistorial({ nombre: cuota.nombre, apellido: cuota.apellido, dni: cuota.dni });
    setCargandoHistorial(true);
    try {
      setPagosHistorial(await obtenerHistorialDePagos(cuota.alumnoId));
    } catch (error) {
      displayToast(error.message, true);
      setAlumnoHistorial(null);
    } finally {
      setCargandoHistorial(false);
    }
  };

  const rol = getRol();
  const navItems = [
    ...(puedeVerAgenda ? [{ to: '/agenda', label: 'Agenda', icon: Calendar }] : []),
    { to: '/alumnos', label: 'Alumnos', icon: Users },
    { to: '/membresias', label: 'Membresías', icon: CreditCard, onClick: () => setSelectedMembresiaId(null) },
    { to: '/ejercicios', label: 'Ejercicios', icon: Dumbbell, onClick: () => setSelectedEjercicioIds([]) },
    ...(puedeVerPlanes ? [{ to: '/planes', label: 'Planes', icon: ClipboardList }] : []),
    // Cobranzas (HU-45) es solo del propietario
    ...(esPropietario ? [{ to: '/cuotas', label: 'Cuotas', icon: Wallet, onClick: () => setSelectedCuotaId(null) }] : []),
    // Franjas horarias y cupo (HU-252): solo el propietario
    ...(esPropietario ? [{ to: '/horarios', label: 'Horarios', icon: Clock }] : []),
  ];
  const seccionActual = navItems.find(item => pathname.startsWith(item.to))?.label ?? 'Panel';
  
  // Guía 4.5: ítems de 44px, activo con borde izquierdo rojo de 3px sobre #3A3A3A
  const itemClass = (activo) => `group flex h-11 w-full shrink-0 cursor-pointer items-center gap-3 whitespace-nowrap border-l-3 px-4 text-sm font-medium text-on-dark transition-colors ${FOCUS} ${
    activo ? 'border-primary bg-sidebar-hover' : 'border-transparent hover:bg-sidebar-hover'
  }`;
  const iconClass = (activo) => `h-5 w-5 shrink-0 ${activo ? 'text-on-dark' : 'text-ink-muted'}`;

  return (
    <div className="flex min-h-screen flex-col bg-page text-ink md:flex-row">

      <aside className="z-30 flex shrink-0 flex-col bg-sidebar md:sticky md:top-0 md:h-screen md:w-60">
        <div className="flex items-center justify-between gap-3 px-4 py-4 md:py-6">
          <Logo onDark />
          {/* En celular, cerrar sesión queda junto al logo */}
          <button type="button" onClick={onLogout} aria-label="Cerrar sesión" className={`rounded-lg p-2 text-ink-muted transition-colors hover:bg-sidebar-hover hover:text-on-dark md:hidden ${FOCUS}`}>
            <LogOut className="h-5 w-5" />
          </button>
        </div>

        <nav aria-label="Secciones" className="flex-1 pb-2 md:pb-0">
          <ul className="flex overflow-x-auto md:flex-col md:overflow-visible">
            {navItems.map(({ to, label, icon: Icon, onClick }) => {
              const activo = pathname.startsWith(to);
              return (
                <li key={to}>
                  <button
                    type="button"
                    onClick={() => { onClick?.(); navigate(to); }}
                    aria-current={activo ? 'page' : undefined}
                    className={itemClass(activo)}
                  >
                    <Icon className={iconClass(activo)} />
                    {label}
                  </button>
                </li>
              );
            })}
            <li className="md:mt-4 md:border-t md:border-sidebar-hover md:pt-4">
              <button type="button" onClick={() => navigate('/')} className={itemClass(false)}>
                <IconHome className={iconClass(false)} /> Sitio web
              </button>
            </li>
          </ul>
        </nav>

        <div className="hidden border-t border-sidebar-hover py-4 md:block">
          <p className="mb-2 px-4 text-xs text-ink-muted">
            Sesión: <span className="font-medium capitalize text-on-dark">{rol?.toLowerCase()}</span>
          </p>
          <button type="button" onClick={onLogout} className={itemClass(false)}>
            <LogOut className={iconClass(false)} /> Cerrar sesión
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <div className="sticky top-0 z-20 hidden h-14 items-center border-b border-line bg-card px-8 md:flex">
          <p className="text-sm text-ink-soft">
            Panel <span className="text-ink-muted">/</span> <span className="font-semibold text-ink">{seccionActual}</span>
          </p>
        </div>

      <main className="mx-auto w-full max-w-[1200px] flex-1 p-4 md:p-8">

        <Routes>
          <Route path="alumnos" element={
            <AlumnosList
              alumnos={alumnos}
              cargando={!cargado}
              onSelect={(id) => navigate(`/alumnos/${id}`)}
              onAdd={() => navigate('/alumnos/nuevo')}
              onReactivate={handleToggleEstadoAlumno}
              search={searchAlumno}
              setSearch={setSearchAlumno}
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
                  onSave={(datos) => handleSaveMembresiaAlumno(alumno.id, datos)}
                  onCancel={() => navigate(`/alumnos/${alumno.id}`)}
                  onError={(msg) => displayToast(msg, true)}
                />
              )}
            </ConAlumno>
          } />

          <Route path="membresias" element={
            <MembresiasList
              membresias={membresias}
              cargando={!cargado}
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

        {/* Rutas de Ejercicios */}
        <Route path="ejercicios" element={
          <EjerciciosList
            ejercicios={ejercicios}
            selectedIds={selectedEjercicioIds}
            setSelectedIds={setSelectedEjercicioIds}
            onAdd={() => navigate('/ejercicios/nuevo')}
            onEdit={(id) => navigate(`/ejercicios/${id}/editar`)}
            onCambiarEstado={handleCambiarEstadoEjercicios}
            onDelete={handleDeleteEjercicios}
          />
        } />

        <Route path="ejercicios/nuevo" element={
          <EjercicioForm
            onSubmit={handleSaveEjercicio}
            onCancel={() => navigate('/ejercicios')}
          />
        } />

        <Route path="ejercicios/:id/editar" element={
          <ConEjercicio
            ejercicios={ejercicios}
            cargado={cargado}
            onNoEncontrado={() => {
              displayToast('No se encontró el ejercicio.', true);
              navigate('/ejercicios', { replace: true });
            }}
          >
            {(ejercicio) => (
              <EjercicioForm
                initialData={ejercicio}
                onSubmit={(formData) => handleSaveEjercicio(formData, true)}
                onCancel={() => navigate('/ejercicios')}
              />
            )}
          </ConEjercicio>
        } />

          {/* Plantillas de plan de entrenamiento (HU-265) */}
          <Route path="planes" element={puedeVerPlanes
            ? <PlantillasList onAdd={() => navigate('/planes/nueva')} onEdit={(id) => navigate(`/planes/${id}/editar`)} onToast={displayToast} />
            : <Navigate to="/alumnos" replace />} />
          <Route path="planes/nueva" element={puedeVerPlanes
            ? <PlantillaForm onVolver={() => navigate('/planes')} onGuardada={() => navigate('/planes')} onToast={displayToast} />
            : <Navigate to="/alumnos" replace />} />
          <Route path="planes/:id/editar" element={puedeVerPlanes
            ? <PlantillaFormRuta onVolver={() => navigate('/planes')} onGuardada={() => navigate('/planes')} onToast={displayToast} />
            : <Navigate to="/alumnos" replace />} />

          <Route path="cuotas" element={esPropietario ? (
            <CuotasList
              cuotas={cuotas}
              cargando={cargandoCuotas}
              selectedId={selectedCuotaId}
              setSelectedId={setSelectedCuotaId}
              onRegistrarPago={setCuotaAPagar}
              onVerHistorial={handleVerHistorial}
              onVerAlumno={(fila) => navigate(`/alumnos/${fila.alumnoId}`)}
              searchTerm={searchCuota}
              setSearchTerm={setSearchCuota}
              filtro={filtroCuota}
              setFiltro={setFiltroCuota}
            />
          ) : <Navigate to="/alumnos" replace />} />

          <Route path="agenda" element={puedeVerAgenda
            ? <Agenda esPropietario={esPropietario} alumnos={alumnos} alumnosCargados={cargado} />
            : <Navigate to="/alumnos" replace />} />

          <Route path="horarios" element={esPropietario
            ? <HorariosGimnasio onToast={displayToast} />
            : <Navigate to="/alumnos" replace />} />

          <Route path="*" element={<Navigate to="/alumnos" replace />} />
        </Routes>

      </main>
      </div>

      <ConfirmModal
        isOpen={modalConfig.isOpen}
        title={modalConfig.title}
        message={modalConfig.message}
        danger={modalConfig.danger}
        onConfirm={modalConfig.onConfirm}
        onCancel={cerrarModal}
      />

      {cuotaAPagar && (
        <RegistrarPagoModal
          cuota={cuotaAPagar}
          membresias={membresias}
          onConfirm={handleRegistrarPago}
          onCancel={() => setCuotaAPagar(null)}
          onError={(msg) => displayToast(msg, true)}
        />
      )}

      {alumnoAReactivar && (
        <ReactivarAlumnoModal
          alumno={alumnoAReactivar}
          membresias={membresias}
          onConfirm={handleReactivarConPago}
          onCancel={() => setAlumnoAReactivar(null)}
          onError={(msg) => displayToast(msg, true)}
        />
      )}

      {alumnoHistorial && (
        <HistorialPagosModal
          alumno={alumnoHistorial}
          pagos={pagosHistorial}
          cargando={cargandoHistorial}
          onClose={() => setAlumnoHistorial(null)}
        />
      )}

      <Toast key={toast?.key} message={toast?.message} type={toast?.type} />

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

      {/* App del alumno: solo rol ALUMNO */}
      <Route path="/mi-cuenta" element={
        <RequiereRol sesion={authSession} permitido={esAlumno}>
          <AlumnoLayout onLogout={handleLogout} />
        </RequiereRol>
      }>
        <Route index element={<Navigate to="perfil" replace />} />
        <Route path="inicio" element={<SeccionProximamente titulo="Inicio" />} />
        <Route path="turnos" element={<TurnosAlumno />} />
        <Route path="rutinas" element={<MiEntrenamiento />} />
        <Route path="perfil" element={<AlumnoPerfil />} />
        <Route path="notificaciones" element={<Notificaciones />} />
        <Route path="*" element={<Navigate to="perfil" replace />} />
      </Route>

      {/* Panel de gestión: solo PROPIETARIO, ENTRENADOR y NUTRICIONISTA. Sus rutas están en MainApp */}
      <Route path="/*" element={
        <RequiereRol sesion={authSession} permitido={esPersonal}>
          <MainApp onLogout={handleLogout} />
        </RequiereRol>
      } />
    </Routes>
  );
}