package com.schoolsaas.common;

/** Arrondi partagé par tous les calculs de moyenne (grade/reportcard/statistics). */
public final class NumberUtils {

    private NumberUtils() {
    }

    public static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
