package mogni.system_backend.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.HorarioTurno;

class FranjaHorariaTest {

    private static LocalTime h(String hora) {
        return LocalTime.parse(hora);
    }

    // HU-252 prueba 1: de 8 a 12 son 4 turnos de 1 hora
    @Test
    void deOchoADoceSonCuatroTurnos() {
        FranjaHoraria franja = new FranjaHoraria(DayOfWeek.MONDAY, h("08:00"), h("12:00"));

        assertEquals(4, franja.cantidadTurnos());
        assertEquals(List.of(
                new HorarioTurno(h("08:00"), h("09:00")),
                new HorarioTurno(h("09:00"), h("10:00")),
                new HorarioTurno(h("10:00"), h("11:00")),
                new HorarioTurno(h("11:00"), h("12:00"))), franja.turnosCalculados());
    }

    // HU-252 prueba 3: las franjas pueden empezar y terminar en horarios que no son en punto
    @Test
    void deDieciseisYMediaADiecinueveYMediaSonTresTurnos() {
        FranjaHoraria franja = new FranjaHoraria(DayOfWeek.MONDAY, h("16:30"), h("19:30"));

        assertEquals(3, franja.cantidadTurnos());
        assertEquals(List.of(
                new HorarioTurno(h("16:30"), h("17:30")),
                new HorarioTurno(h("17:30"), h("18:30")),
                new HorarioTurno(h("18:30"), h("19:30"))), franja.turnosCalculados());
    }

    @Test
    void unaFranjaQueTerminaALas23NoDaLaVueltaALaMedianoche() {
        FranjaHoraria franja = new FranjaHoraria(DayOfWeek.FRIDAY, h("21:00"), h("23:00"));

        assertEquals(List.of(
                new HorarioTurno(h("21:00"), h("22:00")),
                new HorarioTurno(h("22:00"), h("23:00"))), franja.turnosCalculados());
    }

    @Test
    void superposicion() {
        FranjaHoraria franja = new FranjaHoraria(DayOfWeek.MONDAY, h("08:00"), h("12:00"));

        assertTrue(franja.seSuperponeCon(h("11:00"), h("13:00")));
        assertTrue(franja.seSuperponeCon(h("07:00"), h("09:00")));
        assertTrue(franja.seSuperponeCon(h("09:00"), h("10:00")));
        assertTrue(franja.seSuperponeCon(h("07:00"), h("13:00")));
        // Contiguas (una termina justo cuando empieza la otra): no se superponen
        assertFalse(franja.seSuperponeCon(h("12:00"), h("16:00")));
        assertFalse(franja.seSuperponeCon(h("06:00"), h("08:00")));
    }
}
