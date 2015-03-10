package org.springframework.boot.loader.tools.layout;

import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class BaseLayout implements Layout {
	@Override
	public String getLauncherClassName() {
		return null;
	}

	@Override
	public String getLibraryDestination(String libraryName, LibraryScope scope) {
		return null;
	}

	@Override
	public String getClassesLocation() {
		return "";
	}

	@Override
	public boolean isExecutable() {
		return false;
	}
}
