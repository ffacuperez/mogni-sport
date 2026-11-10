package mogni.system_backend.alumno.importacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/** La planilla de alumnos: columnas, conversiones, errores juntos y el último día de pago. */
class PlanillaAlumnosTest {

    private static final String ENCABEZADO = "Día\tNombre\tApellido\tDNI\tEmail\tSexo\tMembresía\t¿Al día?";
    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);

    @Test
    void lee_las_filas_saltea_el_encabezado_y_convierte_los_datos() {
        var resultado = PlanillaAlumnos.leer(List.of(ENCABEZADO,
                "2\tAnabel\tAndrada\t28.969.295\tMaria.Andrada@Gmail.com \tF\t3x\tSí",
                "",
                "3\tHugo Rodrigo\tSerra\t26305315\thugo@gmail.com\tM\tx5\tSi"));

        assertEquals(List.of(), resultado.errores());
        assertEquals(2, resultado.filas().size());
        PlanillaAlumnos.Fila anabel = resultado.filas().get(0);
        assertEquals(2, anabel.diaDePago());
        assertEquals("28969295", anabel.dni());
        assertEquals("maria.andrada@gmail.com", anabel.email());
        assertEquals("Femenino", anabel.sexo());
        assertEquals(3, anabel.diasPorSemana());
        assertEquals("Hugo Rodrigo", resultado.filas().get(1).nombre());
        assertEquals("Masculino", resultado.filas().get(1).sexo());
        assertEquals(5, resultado.filas().get(1).diasPorSemana());
    }

    @Test
    void junta_todos_los_errores_con_su_linea() {
        var resultado = PlanillaAlumnos.leer(List.of(ENCABEZADO,
                "32\tAna\t\t123\tsin-arroba\tX\tlibre\tNo",
                "2\tAna\tPérez\t123",
                "2\tAna\tPérez\t555\tana@mail.com\tF\t3x\tSí",
                "4\tLuz\tGómez\t555\tana@mail.com\tF\t3x\tSí"));

        List<String> errores = resultado.errores();
        assertTrue(errores.contains("Línea 2: el día de pago \"32\" tiene que ser un número del 1 al 31"), errores.toString());
        assertTrue(errores.contains("Línea 2: falta el apellido"));
        assertTrue(errores.contains("Línea 2: el email \"sin-arroba\" no es válido"));
        assertTrue(errores.contains("Línea 2: el sexo \"X\" tiene que ser F, M u Otro"));
        assertTrue(errores.contains("Línea 2: la membresía \"libre\" tiene que decir los días por semana, como 3x"));
        assertTrue(errores.contains("Línea 2: solo se importan alumnos al día: a los que deben, cargalos a mano"));
        assertTrue(errores.contains("Línea 3: tiene 4 columnas y tienen que ser 8"));
        assertTrue(errores.contains("Línea 5: el DNI 555 está repetido en la planilla"));
        assertTrue(errores.contains("Línea 5: el email ana@mail.com está repetido en la planilla"));
        assertEquals(1, resultado.filas().size());
    }

    @Test
    void tambien_acepta_punto_y_coma() {
        var resultado = PlanillaAlumnos.leer(List.of("7;Regina;Tinnirello;40815444;regi@gmail.com;F;3x;Sí"));

        assertEquals(List.of(), resultado.errores());
        assertEquals("Tinnirello", resultado.filas().get(0).apellido());
    }

    @Test
    void el_ultimo_dia_de_pago_es_de_este_mes_si_ya_paso_y_si_no_del_anterior() {
        assertEquals(LocalDate.of(2026, 11, 2), PlanillaAlumnos.ultimoDiaDePago(2, HOY));
        assertEquals(LocalDate.of(2026, 11, 9), PlanillaAlumnos.ultimoDiaDePago(9, HOY));
        assertEquals(LocalDate.of(2026, 10, 10), PlanillaAlumnos.ultimoDiaDePago(10, HOY));
        assertEquals(LocalDate.of(2026, 10, 29), PlanillaAlumnos.ultimoDiaDePago(29, HOY));
    }

    @Test
    void si_el_mes_no_tiene_ese_dia_busca_el_anterior_que_si() {
        // Noviembre no tiene 31: pagó el 31 de octubre
        assertEquals(LocalDate.of(2026, 10, 31), PlanillaAlumnos.ultimoDiaDePago(31, LocalDate.of(2026, 11, 30)));
        // El 3 de marzo, el último 30 fue en enero (febrero no tiene 30)
        assertEquals(LocalDate.of(2026, 1, 30), PlanillaAlumnos.ultimoDiaDePago(30, LocalDate.of(2026, 3, 3)));
    }
}
