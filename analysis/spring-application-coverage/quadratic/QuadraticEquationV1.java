/**
 * Version 1: direct nested decisions.
 */
public final class QuadraticEquationV1 {

	private QuadraticEquationV1() {
	}

	public static Roots solve(double a, double b, double c) {
		if (a == 0.0) {
			if (b == 0.0) {
				throw new IllegalArgumentException("Not an equation");
			}
			return new Roots(-c / b, Double.NaN);
		}
		double discriminant = b * b - 4.0 * a * c;
		if (discriminant > 0.0) {
			double root = Math.sqrt(discriminant);
			return new Roots((-b + root) / (2.0 * a), (-b - root) / (2.0 * a));
		}
		if (discriminant == 0.0) {
			double root = -b / (2.0 * a);
			return new Roots(root, root);
		}
		return new Roots(Double.NaN, Double.NaN);
	}

	public record Roots(double first, double second) {
	}

}
