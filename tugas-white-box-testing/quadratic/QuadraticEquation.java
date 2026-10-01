package com.abdulhafid.quadratic;

public class QuadraticEquation {
    public static String solve(double a, double b, double c) {
        if (a == 0) {
            if (b == 0) {
                if (c == 0) return "Tak hingga banyak solusi";
                return "Tidak ada solusi";
            }
            double x = -c / b;
            return "Persamaan linear, x = " + x;
        }
        double d = b * b - 4 * a * c;
        if (d < 0) return "Tidak ada akar real";
        if (d == 0) {
            double x = -b / (2 * a);
            return "Akar kembar, x = " + x;
        }
        double x1 = (-b + Math.sqrt(d)) / (2 * a);
        double x2 = (-b - Math.sqrt(d)) / (2 * a);
        return "Dua akar real, x1 = " + x1 + ", x2 = " + x2;
    }
}
