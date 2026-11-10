package mogni.system_backend.alumno.importacion;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lee la planilla de alumnos que ya entrenan en el gimnasio (una fila por alumno, separada por tabulaciones como al
 * copiarla de Excel, o por ";"), con las columnas:
 * <pre>Día  Nombre  Apellido  DNI  Email  Sexo  Membresía  ¿Al día?</pre>
 * "Día" es el día del mes en que paga; "Membresía", la cantidad de días por semana ("3x"). Junta todos los errores en
 * vez de cortar en el primero, así se corrige la planilla de una sola vez.
 */
public final class PlanillaAlumnos {

    private static final int COLUMNAS = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@ ]+@[^@ ]+[.][^@ ]+$");

    public record Fila(int linea, int diaDePago, String nombre, String apellido, String dni, String email, String sexo,
                       int diasPorSemana) {
    }

    public record Resultado(List<Fila> filas, List<String> errores) {
    }

    private PlanillaAlumnos() {
    }

    public static Resultado leer(List<String> lineas) {
        List<Fila> filas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        Set<String> dnis = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).replace("﻿", "");
            int numero = i + 1;
            if (linea.isBlank() || esEncabezado(linea)) {
                continue;
            }
            String[] columnas = linea.split(linea.contains("\t") ? "\t" : ";", -1);
            if (columnas.length != COLUMNAS) {
                errores.add("Línea " + numero + ": tiene " + columnas.length + " columnas y tienen que ser " + COLUMNAS);
                continue;
            }
            List<String> erroresFila = new ArrayList<>();
            Integer dia = dia(columnas[0].trim(), erroresFila);
            String nombre = obligatorio(columnas[1], "el nombre", erroresFila);
            String apellido = obligatorio(columnas[2], "el apellido", erroresFila);
            String dni = columnas[3].replaceAll("[^0-9]", "");
            if (dni.isEmpty()) erroresFila.add("el DNI no tiene números");
            String email = columnas[4].trim().toLowerCase(Locale.ROOT);
            if (!EMAIL.matcher(email).matches()) erroresFila.add("el email \"" + email + "\" no es válido");
            String sexo = sexo(columnas[5].trim(), erroresFila);
            Integer diasPorSemana = diasPorSemana(columnas[6].trim(), erroresFila);
            if (!columnas[7].trim().toLowerCase(Locale.ROOT).matches("s[ií]")) {
                erroresFila.add("solo se importan alumnos al día: a los que deben, cargalos a mano");
            }
            if (!dni.isEmpty() && !dnis.add(dni)) erroresFila.add("el DNI " + dni + " está repetido en la planilla");
            if (!emails.add(email)) erroresFila.add("el email " + email + " está repetido en la planilla");

            if (erroresFila.isEmpty()) {
                filas.add(new Fila(numero, dia, nombre, apellido, dni, email, sexo, diasPorSemana));
            } else {
                erroresFila.forEach(error -> errores.add("Línea " + numero + ": " + error));
            }
        }
        return new Resultado(filas, errores);
    }

    /**
     * El último día de pago hasta hoy inclusive: el día del mes en que paga, de este mes o de uno anterior. Si un mes
     * no tiene ese día (un 31 en noviembre) se busca en el anterior, que es cuando pagó.
     */
    public static LocalDate ultimoDiaDePago(int dia, LocalDate hoy) {
        YearMonth mes = YearMonth.from(hoy);
        while (dia > mes.lengthOfMonth() || mes.atDay(dia).isAfter(hoy)) {
            mes = mes.minusMonths(1);
        }
        return mes.atDay(dia);
    }

    private static boolean esEncabezado(String linea) {
        String primera = linea.split("[\t;]", 2)[0].trim().toLowerCase(Locale.ROOT);
        return primera.equals("día") || primera.equals("dia");
    }

    private static Integer dia(String texto, List<String> errores) {
        if (texto.matches("\\d{1,2}") && Integer.parseInt(texto) >= 1 && Integer.parseInt(texto) <= 31) {
            return Integer.parseInt(texto);
        }
        errores.add("el día de pago \"" + texto + "\" tiene que ser un número del 1 al 31");
        return null;
    }

    private static String obligatorio(String texto, String campo, List<String> errores) {
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) errores.add("falta " + campo);
        return limpio;
    }

    private static String sexo(String texto, List<String> errores) {
        return switch (texto.toLowerCase(Locale.ROOT)) {
            case "f", "femenino" -> "Femenino";
            case "m", "masculino" -> "Masculino";
            case "otro" -> "Otro";
            default -> {
                errores.add("el sexo \"" + texto + "\" tiene que ser F, M u Otro");
                yield null;
            }
        };
    }

    /** "3x", "x3" o "3": los días por semana de la membresía. */
    private static Integer diasPorSemana(String texto, List<String> errores) {
        String numero = texto.replaceAll("[^0-9]", "");
        if (!numero.isEmpty() && numero.length() <= 1) {
            return Integer.parseInt(numero);
        }
        errores.add("la membresía \"" + texto + "\" tiene que decir los días por semana, como 3x");
        return null;
    }
}
