/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InvocationContext}.
 *
 * @author Phillip Webb
 */
class InvocationContextTests {

	private final SecurityContext securityContext = mock(SecurityContext.class);

	private final Map<String, Object> arguments = Collections.singletonMap("test", "value");

	@Test
	void whenCreatedWithoutApiVersionThenResolveApiVersionReturnsLatestVersion() {
		InvocationContext context = new InvocationContext(this.securityContext, this.arguments);
		assertThat(context.resolveArgument(ApiVersion.class)).isEqualTo(ApiVersion.LATEST);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenSecurityContextIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new InvocationContext(null, this.arguments))
			.withMessage("'securityContext' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenArgumentsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new InvocationContext(this.securityContext, null))
			.withMessage("'arguments' must not be null");
	}

	@Test
	void resolveSecurityContextReturnsSecurityContext() {
		InvocationContext context = new InvocationContext(this.securityContext, this.arguments);
		assertThat(context.resolveArgument(SecurityContext.class)).isEqualTo(this.securityContext);
	}

	@Test
	void getArgumentsReturnsArguments() {
		InvocationContext context = new InvocationContext(this.securityContext, this.arguments);
		assertThat(context.getArguments()).isEqualTo(this.arguments);
	}

}
