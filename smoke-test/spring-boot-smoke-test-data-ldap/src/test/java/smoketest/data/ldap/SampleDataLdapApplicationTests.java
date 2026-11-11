/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.ldap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleDataLdapApplication}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
class SampleDataLdapApplicationTests {

	@Test
	void testDefaultSettings(CapturedOutput output) {
		assertThat(output).contains("cn=Alice Smith");
	}

}
