package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		String foo = System.getProperty("foo");
		if (!"value 1".equals(foo)) {
			throw new IllegalStateException("foo system property mismatch (got [" + foo + "]");
		}
		String bar = System.getProperty("bar");
		if (!"value2".equals(bar)) {
			throw new IllegalStateException("bar system property mismatch (got [" + bar + "]");
		}
		System.out.println("I haz been run");
	}

}
