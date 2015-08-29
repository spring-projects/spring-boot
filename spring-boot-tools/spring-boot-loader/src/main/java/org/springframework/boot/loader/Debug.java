package org.springframework.boot.loader;

public final class Debug {
	private Debug() {
		// do not allow instances
	}

	public static boolean isEnabled() {
		String debug = System.getProperty("debug");
		if (debug != null && !"false".equals(debug)) {
			return true;
		}
		debug = System.getProperty("DEBUG");
		if (debug != null && !"false".equals(debug)) {
			return true;
		}
		debug = System.getenv("DEBUG");
		if (debug != null && !"false".equals(debug)) {
			return true;
		}
		return false;
	}

}
