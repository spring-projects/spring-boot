/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.jarmode;

import java.util.Arrays;

/**
 * {@link JarMode} for testing.
 *
 * @author Phillip Webb
 */
class TestJarMode implements JarMode {

	@Override
	public boolean accepts(String mode) {
		return "test".equals(mode);
	}

	@Override
	public void run(String mode, String[] args) {
		System.out.println("running in " + mode + " jar mode " + Arrays.asList(args));
		if (args.length > 0 && "error".equals(args[0])) {
			throw new JarModeErrorException("error message");
		}
		if (args.length > 0 && "fail".equals(args[0])) {
			throw new IllegalStateException("bad");
		}
	}

}
