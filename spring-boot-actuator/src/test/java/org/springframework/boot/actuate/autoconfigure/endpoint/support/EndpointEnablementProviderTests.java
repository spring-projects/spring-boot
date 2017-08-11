/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.support;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.endpoint.EndpointType;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointEnablementProvider}.
 *
 * @author Stephane Nicoll
 */
public class EndpointEnablementProviderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void cannotDetermineEnablementWithEmptyEndpoint() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Endpoint id must have a value");
		determineEnablement("   ", true);
	}

	@Test
	public void cannotDetermineEnablementOfEndpointAll() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Endpoint id 'all' is a reserved value and cannot "
				+ "be used by an endpoint");
		determineEnablement("all", true);
	}

	@Test
	public void generalEnabledByDefault() {
		validate(determineEnablement("foo", true), true,
				"endpoint 'foo' is enabled by default");
	}

	@Test
	public void generalDisabledViaSpecificProperty() {
		validate(determineEnablement("foo", true, "endpoints.foo.enabled=false"), false,
				"found property endpoints.foo.enabled");
	}

	@Test
	public void generalDisabledViaGeneralProperty() {
		validate(determineEnablement("foo", true, "endpoints.all.enabled=false"), false,
				"found property endpoints.all.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificProperty() {
		validate(
				determineEnablement("foo", true, "endpoints.all.enabled=false",
						"endpoints.foo.enabled=true"),
				true, "found property endpoints.foo.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificWebProperty() {
		validate(
				determineEnablement("foo", true, "endpoints.foo.enabled=false",
						"endpoints.foo.web.enabled=true"),
				true, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificJmxProperty() {
		validate(
				determineEnablement("foo", true, "endpoints.foo.enabled=false",
						"endpoints.foo.jmx.enabled=true"),
				true, "found property endpoints.foo.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificAnyProperty() {
		validate(determineEnablement("foo", true, "endpoints.foo.enabled=false",
				"endpoints.foo.web.enabled=false", "endpoints.foo.jmx.enabled=true"),
				true, "found property endpoints.foo.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideViaGeneralWebProperty() {
		validate(
				determineEnablement("foo", true, "endpoints.all.enabled=false",
						"endpoints.all.web.enabled=true"),
				true, "found property endpoints.all.web.enabled");
	}

	@Test
	public void generalEnabledOverrideViaGeneralJmxProperty() {
		validate(
				determineEnablement("foo", true, "endpoints.all.enabled=false",
						"endpoints.all.jmx.enabled=true"),
				true, "found property endpoints.all.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideViaGeneralAnyProperty() {
		validate(determineEnablement("foo", true, "endpoints.all.enabled=false",
				"endpoints.all.web.enabled=false", "endpoints.all.jmx.enabled=true"),
				true, "found property endpoints.all.jmx.enabled");
	}

	@Test
	public void generalDisabledEvenWithEnabledGeneralProperties() {
		validate(
				determineEnablement("foo", true, "endpoints.all.enabled=true",
						"endpoints.all.web.enabled=true",
						"endpoints.all.jmx.enabled=true", "endpoints.foo.enabled=false"),
				false, "found property endpoints.foo.enabled");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlag() {
		validate(determineEnablement("bar", false), false,
				"endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlagEvenWithGeneralProperty() {
		validate(determineEnablement("bar", false, "endpoints.all.enabled=true"), false,
				"endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlagEvenWithGeneralWebProperty() {
		validate(determineEnablement("bar", false, "endpoints.all.web.enabled=true"),
				false, "endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlagEvenWithGeneralJmxProperty() {
		validate(determineEnablement("bar", false, "endpoints.all.jmx.enabled=true"),
				false, "endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndSpecificProperty() {
		validate(determineEnablement("bar", false, "endpoints.bar.enabled=true"), true,
				"found property endpoints.bar.enabled");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndSpecificWebProperty() {
		validate(determineEnablement("bar", false, "endpoints.bar.web.enabled=true"),
				true, "found property endpoints.bar.web.enabled");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndSpecificJmxProperty() {
		validate(determineEnablement("bar", false, "endpoints.bar.jmx.enabled=true"),
				true, "found property endpoints.bar.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndAnyProperty() {
		validate(
				determineEnablement("bar", false, "endpoints.bar.web.enabled=false",
						"endpoints.bar.jmx.enabled=true"),
				true, "found property endpoints.bar.jmx.enabled");
	}

	@Test
	public void specificEnabledByDefault() {
		validate(determineEnablement("foo", true, EndpointType.WEB), true,
				"endpoint 'foo' (web) is enabled by default");
	}

	@Test
	public void specificDisabledViaEndpointProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.foo.enabled=false"),
				false, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificDisabledViaTechProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.foo.web.enabled=false"),
				false, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void specificNotDisabledViaUnrelatedTechProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.foo.jmx.enabled=false"),
				true, "endpoint 'foo' (web) is enabled by default");
	}

	@Test
	public void specificDisabledViaGeneralProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.all.enabled=false"),
				false, "found property endpoints.all.enabled");
	}

	@Test
	public void specificEnabledOverrideViaEndpointProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.all.enabled=false", "endpoints.foo.enabled=true"),
				true, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificEnabledOverrideViaTechProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.foo.enabled=false", "endpoints.foo.web.enabled=true"),
				true, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void specificEnabledOverrideHasNotEffectWithUnrelatedTechProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.foo.enabled=false", "endpoints.foo.jmx.enabled=true"),
				false, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificEnabledOverrideViaGeneralWebProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.all.enabled=false", "endpoints.all.web.enabled=true"),
				true, "found property endpoints.all.web.enabled");
	}

	@Test
	public void specificEnabledOverrideHasNoEffectWithUnrelatedTechProperty() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.all.enabled=false", "endpoints.all.jmx.enabled=true"),
				false, "found property endpoints.all.enabled");
	}

	@Test
	public void specificDisabledWithEndpointPropertyEvenWithEnabledGeneralProperties() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.all.enabled=true", "endpoints.all.web.enabled=true",
						"endpoints.all.jmx.enabled=true", "endpoints.foo.enabled=false"),
				false, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificDisabledWithTechPropertyEvenWithEnabledGeneralProperties() {
		validate(
				determineEnablement("foo", true, EndpointType.WEB,
						"endpoints.all.enabled=true", "endpoints.all.web.enabled=true",
						"endpoints.all.jmx.enabled=true", "endpoints.foo.enabled=true",
						"endpoints.foo.web.enabled=false"),
				false, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlag() {
		validate(determineEnablement("bar", false, EndpointType.WEB), false,
				"endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlagEvenWithGeneralProperty() {
		validate(
				determineEnablement("bar", false, EndpointType.WEB,
						"endpoints.all.enabled=true"),
				false, "endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlagEvenWithGeneralWebProperty() {
		validate(
				determineEnablement("bar", false, EndpointType.WEB,
						"endpoints.all.web.enabled=true"),
				false, "endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlagEvenWithGeneralJmxProperty() {
		validate(
				determineEnablement("bar", false, EndpointType.WEB,
						"endpoints.all.jmx.enabled=true"),
				false, "endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificEnabledOverrideWithAndAnnotationFlagAndEndpointProperty() {
		validate(
				determineEnablement("bar", false, EndpointType.WEB,
						"endpoints.bar.enabled=true"),
				true, "found property endpoints.bar.enabled");
	}

	@Test
	public void specificEnabledOverrideWithAndAnnotationFlagAndTechProperty() {
		validate(
				determineEnablement("bar", false, EndpointType.WEB,
						"endpoints.bar.web.enabled=true"),
				true, "found property endpoints.bar.web.enabled");
	}

	@Test
	public void specificEnabledOverrideWithAndAnnotationFlagHasNoEffectWithUnrelatedTechProperty() {
		validate(
				determineEnablement("bar", false, EndpointType.WEB,
						"endpoints.bar.jmx.enabled=true"),
				false, "endpoint 'bar' (web) is disabled by default");
	}

	private void validate(EndpointEnablement enablement, boolean enabled,
			String... messages) {
		assertThat(enablement).isNotNull();
		assertThat(enablement.isEnabled()).isEqualTo(enabled);
		if (!ObjectUtils.isEmpty(messages)) {
			assertThat(enablement.getReason()).contains(messages);
		}
	}

	private EndpointEnablement determineEnablement(String id, boolean enabledByDefault,
			String... environment) {
		return determineEnablement(id, enabledByDefault, null, environment);
	}

	private EndpointEnablement determineEnablement(String id, boolean enabledByDefault,
			EndpointType type, String... environment) {
		MockEnvironment env = new MockEnvironment();
		TestPropertyValues.of(environment).applyTo(env);
		EndpointEnablementProvider provider = new EndpointEnablementProvider(env);
		return provider.getEndpointEnablement(id, enabledByDefault, type);
	}

}
