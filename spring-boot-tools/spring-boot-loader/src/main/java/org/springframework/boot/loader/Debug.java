package org.springframework.boot.loader;

/**
 * This is a helper class for determining whether the debug mode is enabled or not.
 *
 * @author Michael Rumpf
 *
 */
public final class Debug {
	private Debug() {
		// do not allow instances
	}

	/**
	 * Returns true if debug mode is enabled and false otherwise.
	 * 
	 * @return true or false
	 */
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
