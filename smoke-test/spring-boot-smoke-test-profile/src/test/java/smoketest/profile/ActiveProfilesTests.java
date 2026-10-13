/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.profile;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that profiles are activated in the correct order from an
 * {@link EnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = { "enableEnvironmentPostProcessor=true" }) // gh-28530
@ActiveProfiles("hello")
class ActiveProfilesTests {

	@Autowired
	private Environment environment;

	@Test
	void activeProfileShouldTakePrecedenceOverProgrammaticallySetProfile() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev", "hello");
	}

}
