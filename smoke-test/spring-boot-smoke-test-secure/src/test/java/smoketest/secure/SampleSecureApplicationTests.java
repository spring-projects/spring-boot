/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(classes = { SampleSecureApplication.class })
class SampleSecureApplicationTests {

	@Autowired
	private SampleService service;

	private Authentication authentication;

	@BeforeEach
	void init() {
		this.authentication = new UsernamePasswordAuthenticationToken("user", "password");
	}

	@AfterEach
	void close() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void secure() {
		assertThatExceptionOfType(AuthenticationException.class)
			.isThrownBy(() -> SampleSecureApplicationTests.this.service.secure());
	}

	@Test
	void authenticated() {
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken("user", "N/A",
					AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER")));
		assertThat(this.service.secure()).isEqualTo("Hello Security");
	}

	@Test
	void preauth() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThat(this.service.authorized()).isEqualTo("Hello World");
	}

	@Test
	void denied() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThatExceptionOfType(AccessDeniedException.class)
			.isThrownBy(() -> SampleSecureApplicationTests.this.service.denied());
	}

}
