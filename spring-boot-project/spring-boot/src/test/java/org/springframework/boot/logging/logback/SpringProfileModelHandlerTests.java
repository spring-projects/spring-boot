/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging.logback;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringProfileModelHandler}.
 *
 * @author Andy Wilkinson
 */
class SpringProfileModelHandlerTests {

	private final Environment environment = mock(Environment.class);

	private final Context context = new ContextBase();

	private final SpringProfileModelHandler action = new SpringProfileModelHandler(this.context, this.environment);

	private final ModelInterpretationContext interpretationContext = new ModelInterpretationContext(this.context);

	@BeforeEach
	void setUp() {
		this.action.setContext(this.context);
	}

	@Test
	void environmentIsQueriedWithProfileFromModelName() throws ActionException, ModelHandlerException {
		SpringProfileModel model = new SpringProfileModel();
		model.setName("dev");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev");
	}

	@Test
	void environmentIsQueriedWithMultipleProfilesFromCommaSeparatedModelName() throws ModelHandlerException {
		SpringProfileModel model = new SpringProfileModel();
		model.setName("dev,qa");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev", "qa");
	}

	@Test
	void environmentIsQueriedWithResolvedValueWhenModelNameUsesAPlaceholder() throws ModelHandlerException {
		SpringProfileModel model = new SpringProfileModel();
		model.setName("${profile}");
		this.context.putProperty("profile", "dev");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev");
	}

	@Test
	void environmentIsQueriedWithResolvedValuesFromCommaSeparatedNameNameAttributeWithPlaceholders()
			throws ModelHandlerException {
		SpringProfileModel model = new SpringProfileModel();
		model.setName("${profile1},${profile2}");
		this.context.putProperty("profile1", "dev");
		this.context.putProperty("profile2", "qa");
		this.action.handle(this.interpretationContext, model);
		ArgumentCaptor<Profiles> profiles = ArgumentCaptor.forClass(Profiles.class);
		then(this.environment).should().acceptsProfiles(profiles.capture());
		List<String> profileNames = new ArrayList<>();
		profiles.getValue().matches((profile) -> {
			profileNames.add(profile);
			return false;
		});
		assertThat(profileNames).containsExactly("dev", "qa");
	}

}
