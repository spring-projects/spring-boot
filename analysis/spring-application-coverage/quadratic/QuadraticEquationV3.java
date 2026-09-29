/**
 * Version 3: classify a scaled discriminant and use a cancellation-resistant
 * formula for the two real roots.
 */
public final class QuadraticEquationV3 {

	private QuadraticEquationV3() {
	}

	public static Roots solve(double a, double b, double c) {
		if (a == 0.0) {
			if (b == 0.0) {
				throw new IllegalArgumentException("Not an equation");
			}
			return new Roots(-c / b, Double.NaN);
		}
		double scale = Math.max(Math.abs(a), Math.max(Math.abs(b), Math.abs(c)));
		double scaledA = a / scale;
		double scaledB = b / scale;
		double scaledC = c / scale;
		double discriminant = Math.fma(-4.0 * scaledA, scaledC, scaledB * scaledB);
		int sign = Double.compare(discriminant, 0.0);
		switch (sign) {
			case 1:
				double squareRoot = Math.sqrt(discriminant);
				double q = -0.5 * (scaledB + Math.copySign(squareRoot, scaledB));
				return new Roots(q / scaledA, scaledC / q);
			case 0:
				double repeated = -scaledB / (2.0 * scaledA);
				return new Roots(repeated, repeated);
			default:
				return new Roots(Double.NaN, Double.NaN);
		}
	}

	public record Roots(double first, double second) {
	}

}
