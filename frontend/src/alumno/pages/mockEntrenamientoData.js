export const mockPlanActivo = {
  id: 'plan-001',
  nombre: 'Plan Fuerza 2026',
  activo: true,
  sesiones: [
    {
      id: 'sesion-1',
      numero: 1,
      nombre: 'Full body A',
      estado: 'En curso',
      ejercicios: [
        {
          id: 'ej-1',
          nombre: 'Press de banca',
          cantidadSeries: 3,
          repeticionesObjetivo: '8-10',
          rirObjetivo: 2,
          descanso: '2 min',
          nota: '',
          estado: 'Pendiente',
          series: [
            { numero: 1, peso: '60', repeticiones: '10', rirRealizado: '2', rmEstimado: 80 },
            { numero: 2, peso: '60', repeticiones: '9', rirRealizado: '3', rmEstimado: 78 },
            { numero: 3, peso: '60', repeticiones: '8', rirRealizado: '2', rmEstimado: 76 },
          ],
        },
        {
          id: 'ej-2',
          nombre: 'Sentadilla',
          cantidadSeries: 3,
          repeticionesObjetivo: '6-8',
          rirObjetivo: 2,
          descanso: '3 min',
          nota: '',
          estado: 'Pendiente',
          series: [
            { numero: 1, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
            { numero: 2, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
            { numero: 3, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
          ],
        },
      ],
    },
    {
      id: 'sesion-2',
      numero: 2,
      nombre: 'Empuje y tracción',
      estado: 'En curso',
      ejercicios: [
        {
          id: 'ej-3',
          nombre: 'Dominadas asistidas',
          cantidadSeries: 4,
          repeticionesObjetivo: '6-10',
          rirObjetivo: 1,
          descanso: '2 min',
          nota: '',
          estado: 'Pendiente',
          series: [
            { numero: 1, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
            { numero: 2, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
            { numero: 3, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
            { numero: 4, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
          ],
        },
      ],
    },
    {
      id: 'sesion-3',
      numero: 3,
      nombre: 'Piernas y hombros',
      estado: 'En curso',
      ejercicios: [
        {
          id: 'ej-4',
          nombre: 'Peso muerto',
          cantidadSeries: 3,
          repeticionesObjetivo: '5-8',
          rirObjetivo: 2,
          descanso: '3 min',
          nota: '',
          estado: 'Pendiente',
          series: [
            { numero: 1, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
            { numero: 2, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
            { numero: 3, peso: '', repeticiones: '', rirRealizado: '', rmEstimado: null },
          ],
        },
      ],
    },
  ],
};

export const mockPlanSinActivo = {
  ...mockPlanActivo,
  activo: false,
  sesiones: [],
};
