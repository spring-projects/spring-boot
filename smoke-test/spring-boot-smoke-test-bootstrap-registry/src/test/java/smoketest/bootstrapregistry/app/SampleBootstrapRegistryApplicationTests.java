/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleBootstrapRegistryApplication}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SampleBootstrapRegistryApplicationTests {

	@Test
	void testBootstrapper(CapturedOutput output) {
		SampleBootstrapRegistryApplication.main(new String[0]);
		assertThat(output).contains("svn my-data from svn / example.com[secret]")
			.contains("client smoketest.bootstrapregistry.app.MySubversionClient");
	}

}
