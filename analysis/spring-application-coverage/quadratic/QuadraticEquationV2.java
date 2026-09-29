/**
 * Version 2: guard clauses make the exceptional and linear cases explicit.
 */
public final class QuadraticEquationV2 {

	private QuadraticEquationV2() {
	}

	public static Roots solve(double a, double b, double c) {
		if (a == 0.0 && b == 0.0) {
			throw new IllegalArgumentException("Not an equation");
		}
		if (a == 0.0) {
			return new Roots(-c / b, Double.NaN);
		}
		double discriminant = b * b - 4.0 * a * c;
		if (discriminant < 0.0) {
			return new Roots(Double.NaN, Double.NaN);
		}
		double root = Math.sqrt(discriminant);
		return new Roots((-b + root) / (2.0 * a), (-b - root) / (2.0 * a));
	}

	public record Roots(double first, double second) {
	}

}
