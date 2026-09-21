import { useEffect, useState } from 'react';

import Login from './auth/pages/Login';
import AlumnosList from './alumno/pages/AlumnosList';
import AlumnoForm from './alumno/pages/AlumnoForm';
import MembresiasList from './membresia/pages/MembresiasList';
import MembresiaForm from './membresia/pages/MembresiaForm';
import ConfirmModal from './shared/components/ConfirmModal';
import Toast from './shared/components/Toast';
import { hasSession, logout } from './auth/services/authService';
import { setUnauthorizedHandler } from './shared/services/httpClient';
import {
  actualizarAlumno,
  crearAlumno,
  darDeBajaAlumno,
  listarAlumnos,
  reactivarAlumno,
} from './alumno/services/alumnosService';
import {
  actualizarMembresia,
  crearMembresia,
  eliminarMembresia,
  listarMembresias,
} from './membresia/services/membresiasService';

// --- ICONS (Main App) ---
const IconHome = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>;
const IconUsers = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>;
const IconCard = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/></svg>;
const IconLogout = () => <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>;

// --- MAIN APP COMPONENT ---
function MainApp({ onLogout }) {
  const [view, setView] = useState('alumnos_list'); 
  const [alumnos, setAlumnos] = useState([]);
  const [membresias, setMembresias] = useState([]);
  
  const [selectedAlumnoId, setSelectedAlumnoId] = useState(null);
  const [selectedMembresiaId, setSelectedMembresiaId] = useState(null);
  const [searchDNI, setSearchDNI] = useState('');

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
      .catch((error) => displayToast(error.message, true));
  }, []);

  // Ejecuta una llamada al backend mostrando el resultado en el toast. Devuelve true si salió bien.
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

  const handleToggleEstadoAlumno = () => {
    const alumno = alumnos.find(a => a.id === selectedAlumnoId);
    if (!alumno) return;

    const isActivo = alumno.estado === 'Activo';
    const actionText = isActivo ? 'dar de baja' : 'activar';
    const newEstado = isActivo ? 'Inactivo' : 'Activo';

    showModal(
      'Confirmar Acción',
      `¿Estás seguro de que deseas ${actionText} al alumno ${alumno.nombre} ${alumno.apellido}?`,
      async () => {
        const ok = await ejecutar(async () => {
          const actualizado = isActivo ? await darDeBajaAlumno(alumno.id) : await reactivarAlumno(alumno.id);
          setAlumnos(prev => prev.map(a => a.id === actualizado.id ? actualizado : a));
        }, `El alumno ha sido ${newEstado === 'Activo' ? 'activado' : 'dado de baja'} correctamente.`);
        if (ok) setSelectedAlumnoId(null);
      }
    );
  };

  const handleSaveAlumno = (formData, isEdit) => {
    showModal(
      isEdit ? 'Confirmar Cambios' : 'Confirmar Alta',
      isEdit ? '¿Desea confirmar los cambios en el alumno?' : '¿Desea confirmar el alta del nuevo alumno?',
      async () => {
        const ok = await ejecutar(async () => {
          const guardado = isEdit ? await actualizarAlumno(formData.id, formData) : await crearAlumno(formData);
          setAlumnos(prev => isEdit ? prev.map(a => a.id === guardado.id ? guardado : a) : [...prev, guardado]);
        }, isEdit ? 'Los cambios fueron guardados correctamente.' : 'El alumno ha sido creado correctamente.');
        if (ok) {
          setView('alumnos_list');
          setSelectedAlumnoId(null);
        }
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
            // Los alumnos guardan el nombre de su membresía solo para mostrarlo: lo mantenemos al día
            setAlumnos(prev => prev.map(a => a.membresiaId === guardada.id ? { ...a, membresia: guardada.nombre } : a));
          }
        }, isEdit ? 'Membresía modificada correctamente.' : 'Membresía creada correctamente.');
        if (ok) {
          setView('membresias_list');
          setSelectedMembresiaId(null);
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
              <button className="w-full flex items-center gap-3 px-6 py-3 text-slate-400 hover:text-slate-100 hover:bg-slate-900 transition-colors">
                <IconHome /> Inicio
              </button>
            </li>
            <li>
              <button 
                onClick={() => { setView('alumnos_list'); setSelectedAlumnoId(null); }}
                className={`w-full flex items-center gap-3 px-6 py-3 transition-colors ${view.startsWith('alumnos') ? 'text-slate-100 bg-slate-900 border-l-4 border-red-600' : 'text-slate-400 hover:text-slate-100 hover:bg-slate-900 border-l-4 border-transparent'}`}
              >
                <IconUsers /> Alumnos
              </button>
            </li>
            <li>
              <button 
                onClick={() => { setView('membresias_list'); setSelectedMembresiaId(null); }}
                className={`w-full flex items-center gap-3 px-6 py-3 transition-colors ${view.startsWith('membresias') ? 'text-slate-100 bg-slate-900 border-l-4 border-red-600' : 'text-slate-400 hover:text-slate-100 hover:bg-slate-900 border-l-4 border-transparent'}`}
              >
                <IconCard /> Membresías
              </button>
            </li>
            <li>
              <button onClick={onLogout} className="w-full flex items-center gap-3 px-6 py-3 text-slate-400 hover:text-slate-100 hover:bg-slate-900 transition-colors mt-8">
                <IconLogout /> Cerrar Sesión
              </button>
            </li>
          </ul>
        </nav>
      </aside>

      <main className="flex-1 p-4 md:p-8 overflow-y-auto bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0IiBoZWlnaHQ9IjQiPgo8cmVjdCB3aWR0aD0iNCIgaGVpZ2h0PSI0IiBmaWxsPSIjMGYxNzJhIj48L3JlY3Q+CjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiMxZTI5M2IiPjwvcmVjdD4KPC9zdmc+')]">
        
        {view === 'alumnos_list' && (
          <AlumnosList 
            alumnos={alumnos} 
            selectedId={selectedAlumnoId}
            setSelectedId={setSelectedAlumnoId}
            onAdd={() => setView('alumnos_form')}
            onEdit={() => setView('alumnos_form')}
            onToggleStatus={handleToggleEstadoAlumno}
            searchDNI={searchDNI}
            setSearchDNI={setSearchDNI}
          />
        )}

        {view === 'alumnos_form' && (
          <AlumnoForm
            alumnoToEdit={alumnos.find(a => a.id === selectedAlumnoId)}
            membresias={membresias}
            onSave={handleSaveAlumno}
            onCancel={() => { setView('alumnos_list'); setSelectedAlumnoId(null); }}
            onError={(msg) => displayToast(msg, true)}
          />
        )}

        {view === 'membresias_list' && (
          <MembresiasList
            membresias={membresias}
            selectedId={selectedMembresiaId}
            setSelectedId={setSelectedMembresiaId}
            onAdd={() => setView('membresias_form')}
            onEdit={() => setView('membresias_form')}
            onDelete={handleDeleteMembresia}
          />
        )}

        {view === 'membresias_form' && (
          <MembresiaForm
            membresiaToEdit={membresias.find(m => m.id === selectedMembresiaId)}
            onSave={handleSaveMembresia}
            onCancel={() => { setView('membresias_list'); setSelectedMembresiaId(null); }}
          />
        )}

      </main>

      <ConfirmModal 
        isOpen={modalConfig.isOpen}
        title={modalConfig.title}
        message={modalConfig.message}
        onConfirm={modalConfig.onConfirm}
        onCancel={() => setModalConfig({ ...modalConfig, isOpen: false })}
      />
      <Toast 
        message={toastConfig.message} 
        isVisible={toastConfig.isVisible} 
        isError={toastConfig.isError} 
      />
      
    </div>
  );
}

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(hasSession);

  const handleLogout = () => {
    logout();
    setIsAuthenticated(false);
  };

  // Si el backend rechaza el token (vencido o inválido), volvemos al login
  useEffect(() => {
    setUnauthorizedHandler(handleLogout);
  }, []);

  return (
    <>
      {!isAuthenticated ? (
        <Login onLogin={() => setIsAuthenticated(true)} />
      ) : (
        <MainApp onLogout={handleLogout} />
      )}
    </>
  );
}
