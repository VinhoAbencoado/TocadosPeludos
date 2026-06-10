package com.example.tocadospeludos;

import java.util.Calendar;
import java.util.Locale;

/**
 * Formatação simples de datas para eventos (sem dependências externas).
 */
public final class DateUtils {

    private static final String[] MONTHS = {
            "jan", "fev", "mar", "abr", "mai", "jun",
            "jul", "ago", "set", "out", "nov", "dez"
    };

    private DateUtils() {
    }

    /** Ex.: "20 mai 2026". */
    public static String formatDate(long millis) {
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.setTimeInMillis(millis);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);
        return String.format(Locale.getDefault(), "%02d %s %d", day, MONTHS[month], year);
    }

    /** Epoch millis às 00:00 do dia informado (usado com o DatePicker). */
    public static long atStartOfDay(int year, int month, int dayOfMonth) {
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.set(year, month, dayOfMonth, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Epoch millis às 00:00 de hoje, para comparar e ocultar eventos passados. */
    public static long startOfToday() {
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
