package mogni.system_backend.cuota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;

/** Modelo de cuota (período pagado), situación de pago y reglas de fechas y montos del rediseño de cuotas. */
class CuotaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    private static Inscripcion inscripcion() {
        Membresia membresia = new Membresia(7L, "Pro", 25000.0, 3, null);
        return new Inscripcion(5L, HOY.minusDays(40), null, EstadoInscripcion.ACTIVA, new Alumno(), membresia);
    }

    // --- SituacionPago: bordes ---

    @ParameterizedTest(name = "faltan {0} días -> {1}")
    @CsvSource({
            "6, AL_DIA",
            "30, AL_DIA",
            "5, PROXIMA_A_VENCER",
            "0, PROXIMA_A_VENCER",
            "-1, VENCIDA",
            "-2, EN_MORA",
            "-9, EN_MORA"
    })
    void situacion_segun_los_dias_hasta_el_vencimiento(int dias, SituacionPago esperada) {
        assertEquals(esperada, SituacionPago.de(HOY.plusDays(dias), HOY));
    }

    @Test
    void sin_cuota_no_hay_situacion() {
        assertNull(SituacionPago.de((Cuota) null, HOY));
    }

    @Test
    void ejemplo_del_documento_vence_31_10_vencida_el_1_11_en_mora_el_2_11_baja_el_9_11() {
        LocalDate vencimiento = LocalDate.of(2026, 10, 31);
        assertEquals(SituacionPago.PROXIMA_A_VENCER, SituacionPago.de(vencimiento, LocalDate.of(2026, 10, 31)));
        assertEquals(SituacionPago.VENCIDA, SituacionPago.de(vencimiento, LocalDate.of(2026, 11, 1)));
        assertEquals(SituacionPago.EN_MORA, SituacionPago.de(vencimiento, LocalDate.of(2026, 11, 2)));
        assertEquals(0, ReglasCuota.diasEnMora(vencimiento, LocalDate.of(2026, 11, 2)));
        assertEquals(6, ReglasCuota.diasEnMora(vencimiento, LocalDate.of(2026, 11, 8)));
        assertEquals(ReglasCuota.DIAS_MORA_BAJA_AUTOMATICA, ReglasCuota.diasEnMora(vencimiento, LocalDate.of(2026, 11, 9)));
    }

    @Test
    void la_mora_corre_igual_los_fines_de_semana() {
        // Vence el viernes 30/10: el domingo 01/11 ya está en mora, sin contar días hábiles
        LocalDate viernes = LocalDate.of(2026, 10, 30);
        assertEquals(DayOfWeek.FRIDAY, viernes.getDayOfWeek());
        assertEquals(SituacionPago.VENCIDA, SituacionPago.de(viernes, viernes.plusDays(1)));
        assertEquals(SituacionPago.EN_MORA, SituacionPago.de(viernes, viernes.plusDays(2)));
        assertEquals(1, ReglasCuota.diasEnMora(viernes, viernes.plusDays(3)));
    }

    @Test
    void en_mora_hace_3_dias_si_vencio_hace_5() {
        assertEquals(3, ReglasCuota.diasEnMora(HOY.minusDays(5), HOY));
    }

    // --- Ventana de renovación e inicio de la cuota nueva ---

    @Test
    void se_puede_renovar_desde_10_dias_antes_y_despues_del_vencimiento() {
        assertTrue(ReglasCuota.puedeRenovar(HOY.plusDays(10), HOY));
        assertFalse(ReglasCuota.puedeRenovar(HOY.plusDays(11), HOY));
        assertTrue(ReglasCuota.puedeRenovar(HOY.minusDays(1), HOY));
        assertTrue(ReglasCuota.puedeRenovar(HOY.minusDays(20), HOY));
    }

    @Test
    void la_cuota_nueva_sigue_la_cadena_si_no_esta_en_mora_y_empieza_hoy_si_esta_en_mora() {
        assertEquals(HOY.plusDays(3), ReglasCuota.inicioRenovacion(HOY.plusDays(3), HOY));
        assertEquals(HOY.minusDays(1), ReglasCuota.inicioRenovacion(HOY.minusDays(1), HOY));
        assertEquals(HOY, ReglasCuota.inicioRenovacion(HOY.minusDays(2), HOY));
    }

    // --- Recargo ---

    @Test
    void recargo_del_10_por_ciento_redondeado_a_la_decena_hacia_arriba() {
        BigDecimal precio = new BigDecimal("15420.00");
        assertEquals(new BigDecimal("16970.00"), ReglasCuota.totalConRecargo(precio));
        assertEquals(new BigDecimal("1550.00"), ReglasCuota.recargo(precio));
    }

    @Test
    void si_el_total_ya_es_multiplo_de_10_no_se_redondea() {
        BigDecimal precio = new BigDecimal("25000.00");
        assertEquals(new BigDecimal("27500.00"), ReglasCuota.totalConRecargo(precio));
        assertEquals(new BigDecimal("2500.00"), ReglasCuota.recargo(precio));
    }

    @Test
    void un_centavo_de_mas_sube_a_la_decena_siguiente() {
        // 10001 + 10% = 11001,10 -> 11010
        assertEquals(new BigDecimal("11010.00"), ReglasCuota.totalConRecargo(new BigDecimal("10001.00")));
    }

    // --- Cambio de membresía ---

    @Test
    void precio_final_del_cambio_cobra_la_diferencia_si_sube_y_no_reembolsa_si_baja() {
        assertEquals(new BigDecimal("25000.00"),
                ReglasCuota.precioFinalCambio(new BigDecimal("20000.00"), new BigDecimal("25000.00")));
        assertEquals(new BigDecimal("25000.00"),
                ReglasCuota.precioFinalCambio(new BigDecimal("25000.00"), new BigDecimal("20000.00")));
    }

    // --- Entidad ---

    @Test
    void cuota_nueva_cubre_30_dias_al_precio_actual_con_su_membresia() {
        Inscripcion inscripcion = inscripcion();

        Cuota cuota = inscripcion.agregarCuota(HOY);

        assertEquals(HOY, cuota.getFechaInicio());
        assertEquals(HOY.plusDays(30), cuota.getFechaVencimiento());
        assertEquals(new BigDecimal("25000.00"), cuota.getPrecio());
        assertSame(inscripcion.getMembresia(), cuota.getMembresia());
        assertSame(inscripcion, cuota.getInscripcion());
        assertTrue(inscripcion.getCuotas().contains(cuota));
    }

    @Test
    void la_ultima_cuota_es_la_de_vencimiento_mas_reciente() {
        Inscripcion inscripcion = inscripcion();
        Cuota primera = inscripcion.agregarCuota(HOY.minusDays(40));
        Cuota segunda = inscripcion.agregarCuota(HOY.minusDays(10));
        primera.setId(1L);
        segunda.setId(2L);

        assertSame(segunda, inscripcion.buscarUltimaCuota().orElseThrow());
    }

    @Test
    void mover_una_cuota_la_saca_de_la_inscripcion_anterior() {
        Inscripcion vieja = inscripcion();
        Inscripcion nueva = inscripcion();
        Cuota cuota = vieja.agregarCuota(HOY.minusDays(5));

        nueva.recibirCuota(cuota);

        assertTrue(vieja.getCuotas().isEmpty());
        assertSame(nueva, cuota.getInscripcion());
        assertEquals(HOY.minusDays(5), cuota.getFechaInicio());
    }
}
