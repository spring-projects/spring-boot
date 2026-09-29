/**
 * Version 3: classify the discriminant once, then select with a switch.
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
		double discriminant = b * b - 4.0 * a * c;
		int sign = Double.compare(discriminant, 0.0);
		switch (sign) {
			case 1:
				double root = Math.sqrt(discriminant);
				return new Roots((-b + root) / (2.0 * a), (-b - root) / (2.0 * a));
			case 0:
				double repeated = -b / (2.0 * a);
				return new Roots(repeated, repeated);
			default:
				return new Roots(Double.NaN, Double.NaN);
		}
	}

	public record Roots(double first, double second) {
	}

}
