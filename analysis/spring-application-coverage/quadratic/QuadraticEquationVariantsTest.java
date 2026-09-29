import java.util.function.Function;

/**
 * Dependency-free executable tests for all quadratic variants.
 */
public final class QuadraticEquationVariantsTest {

	private QuadraticEquationVariantsTest() {
	}

	public static void main(String[] args) {
		test("V1", (coefficients) -> {
			QuadraticEquationV1.Roots roots = QuadraticEquationV1.solve(coefficients[0], coefficients[1],
					coefficients[2]);
			return new double[] { roots.first(), roots.second() };
		});
		test("V2", (coefficients) -> {
			QuadraticEquationV2.Roots roots = QuadraticEquationV2.solve(coefficients[0], coefficients[1],
					coefficients[2]);
			return new double[] { roots.first(), roots.second() };
		});
		test("V3", (coefficients) -> {
			QuadraticEquationV3.Roots roots = QuadraticEquationV3.solve(coefficients[0], coefficients[1],
					coefficients[2]);
			return new double[] { roots.first(), roots.second() };
		});
		QuadraticEquationV3.Roots cancellationRoots = QuadraticEquationV3.solve(1.0, 1.0e16, 1.0);
		assertRelative(cancellationRoots.first(), -1.0e16);
		assertRelative(cancellationRoots.second(), -1.0e-16);
		QuadraticEquationV3.Roots scaledRoots = QuadraticEquationV3.solve(1.0e200, -3.0e200, 2.0e200);
		assertRoots(new double[] { scaledRoots.first(), scaledRoots.second() }, 2, 1);
		System.out.println("All quadratic variants passed");
	}

	private static void test(String name, Function<double[], double[]> solver) {
		assertRoots(solver.apply(new double[] { 1, -3, 2 }), 2, 1);
		assertRoots(solver.apply(new double[] { 1, 2, 1 }), -1, -1);
		assertNoRealRoots(solver.apply(new double[] { 1, 0, 1 }));
		assertRoots(solver.apply(new double[] { 0, 2, -4 }), 2, Double.NaN);
		assertThrows(() -> solver.apply(new double[] { 0, 0, 1 }));
		System.out.println(name + " passed functional input-partition cases");
	}

	private static void assertRelative(double actual, double expected) {
		if (Math.abs(actual - expected) / Math.abs(expected) > 1.0e-12) {
			throw new AssertionError("Expected " + expected + " but got " + actual);
		}
	}

	private static void assertRoots(double[] actual, double expectedFirst, double expectedSecond) {
		if (Math.abs(actual[0] - expectedFirst) > 1.0e-10
				|| (!Double.isNaN(expectedSecond) && Math.abs(actual[1] - expectedSecond) > 1.0e-10)
				|| (Double.isNaN(expectedSecond) && !Double.isNaN(actual[1]))) {
			throw new AssertionError("Unexpected roots");
		}
	}

	private static void assertNoRealRoots(double[] actual) {
		if (!Double.isNaN(actual[0]) || !Double.isNaN(actual[1])) {
			throw new AssertionError("Expected no real roots");
		}
	}

	private static void assertThrows(Runnable action) {
		try {
			action.run();
			throw new AssertionError("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
			// Expected path.
		}
	}

}
