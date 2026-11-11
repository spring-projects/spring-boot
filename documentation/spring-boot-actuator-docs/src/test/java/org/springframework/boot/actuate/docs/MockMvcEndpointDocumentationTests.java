/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.docs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.context.WebApplicationContext;

/**
 * Abstract base class for tests that generate endpoint documentation using Spring REST
 * Docs and {@link MockMvcTester}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest
public abstract class MockMvcEndpointDocumentationTests extends AbstractEndpointDocumentationTests {

	protected MockMvcTester mvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@BeforeEach
	void setup(RestDocumentationContextProvider restDocumentation) {
		this.mvc = MockMvcTester.from(this.applicationContext,
				(builder) -> builder
					.apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation).uris())
					.build());
	}

	protected WebApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
