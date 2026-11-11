/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.simple;

import org.springframework.boot.ExitCodeGenerator;

public class ExitException extends RuntimeException implements ExitCodeGenerator {

	@Override
	public int getExitCode() {
		return 10;
	}

}
