import { createContext, useContext } from 'react';

import { consultasProgresoDeAlumno, crearConsultasProgreso } from './progresoService';

/**
 * De dónde salen los datos, a dónde llevan los links y cómo se le habla al que mira. Las pantallas de Mi progreso
 * (HU-270) son las mismas para el alumno y para el entrenador (MOGNI-268); solo cambia este contexto.
 */
const PARA_EL_ALUMNO = {
  consultas: crearConsultasProgreso('/mi-progreso'),
  rutaBase: '/mi-cuenta/progreso',
  textos: {
    navegacion: 'Mi rutina',
    cargandoPlan: 'Cargando tu plan',
    cargandoPlanes: 'Cargando tus planes',
    cargandoSesiones: 'Cargando tus sesiones',
    sinPlan: { titulo: 'Todavía no tenés un plan asignado', texto: 'Tu profe te va a asignar uno pronto.' },
    sinAnteriores: { titulo: 'Aún no tenés planes anteriores', texto: 'Cuando termines tu plan actual, lo vas a encontrar acá.' },
    sinSesiones: { titulo: 'Todavía no registraste ninguna sesión', texto: 'Cuando registres tu primera sesión, la vas a ver acá.' },
    accesoBloqueado: 'No podés acceder a tu rutina',
  },
};

/** MOGNI-268: el progreso del alumno, visto por el entrenador. De solo lectura: el backend no deja registrar nada. */
export function progresoParaElEntrenador(alumnoId) {
  return {
    consultas: consultasProgresoDeAlumno(alumnoId),
    rutaBase: `/alumnos/${alumnoId}/progreso`,
    textos: {
      navegacion: 'Progreso del alumno',
      cargandoPlan: 'Cargando el plan',
      cargandoPlanes: 'Cargando los planes',
      cargandoSesiones: 'Cargando las sesiones',
      sinPlan: { titulo: 'Este alumno no tiene un plan activo', texto: 'Asignale un plan a partir de una plantilla para empezar a seguir su progreso.' },
      sinAnteriores: { titulo: 'Este alumno todavía no tiene planes anteriores', texto: 'Cuando finalice su plan actual, lo vas a ver acá.' },
      sinSesiones: { titulo: 'Este alumno todavía no registró ninguna sesión', texto: 'Cuando registre su primera sesión desde la app, la vas a ver acá.' },
      accesoBloqueado: 'No podés ver el progreso de este alumno',
    },
  };
}

// Sin proveedor, es la vista del alumno: así Mi progreso funciona igual que antes
export const ProgresoContext = createContext(PARA_EL_ALUMNO);

export const useProgreso = () => useContext(ProgresoContext);
