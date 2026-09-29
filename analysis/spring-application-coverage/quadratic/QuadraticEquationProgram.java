/**
 * Runnable command-line entry point for the three equivalent implementations.
 *
 * Usage: java QuadraticEquationProgram <V1|V2|V3> <a> <b> <c>
 */
public final class QuadraticEquationProgram {

	private QuadraticEquationProgram() {
	}

	public static void main(String[] args) {
		if (args.length != 4) {
			throw new IllegalArgumentException("Usage: QuadraticEquationProgram <V1|V2|V3> <a> <b> <c>");
		}
		double a = Double.parseDouble(args[1]);
		double b = Double.parseDouble(args[2]);
		double c = Double.parseDouble(args[3]);
		double[] roots = switch (args[0]) {
			case "V1" -> roots(QuadraticEquationV1.solve(a, b, c));
			case "V2" -> roots(QuadraticEquationV2.solve(a, b, c));
			case "V3" -> roots(QuadraticEquationV3.solve(a, b, c));
			default -> throw new IllegalArgumentException("Unknown implementation: " + args[0]);
		};
		System.out.printf("x1=%s, x2=%s%n", roots[0], roots[1]);
	}

	private static double[] roots(QuadraticEquationV1.Roots roots) {
		return new double[] { roots.first(), roots.second() };
	}

	private static double[] roots(QuadraticEquationV2.Roots roots) {
		return new double[] { roots.first(), roots.second() };
	}

	private static double[] roots(QuadraticEquationV3.Roots roots) {
		return new double[] { roots.first(), roots.second() };
	}

}
