package com.abdulhafid.quadratic;

public class QuadraticEquationTest {
    public static void main(String[] args) {
        test(0, 0, 0);
        test(0, 0, 1);
        test(0, 2, -4);
        test(1, 0, 1);
        test(1, -2, 1);
        test(1, -3, 2);
    }

    private static void test(double a, double b, double c) {
        System.out.printf("(%s)x^2 + (%s)x + (%s) -> %s%n",
                a, b, c, QuadraticEquation.solve(a, b, c));
    }
}
