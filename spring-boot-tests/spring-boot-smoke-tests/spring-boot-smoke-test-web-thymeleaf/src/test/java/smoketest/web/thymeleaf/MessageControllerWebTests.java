/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.web.thymeleaf;

import java.util.regex.Pattern;

import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Basic Spring MVC Test for the Sample Controller.
 *
 * @author Biju Kunjummen
 * @author Doo-Hwan, Kwak
 */
@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerWebTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void testHome() {
		assertThat(this.mvc.get().uri("/")).hasStatusOk().bodyText().contains("<title>Messages");
	}

	@Test
	void testCreate() {
		assertThat(this.mvc.post().uri("/").param("text", "FOO text").param("summary", "FOO"))
			.hasStatus(HttpStatus.FOUND)
			.headers()
			.hasEntrySatisfying("Location",
					(values) -> assertThat(values).hasSize(1)
						.element(0)
						.satisfies(HamcrestCondition.matching(RegexMatcher.matches("/[0-9]+"))));
	}

	@Test
	void testCreateValidation() {
		assertThat(this.mvc.post().uri("/").param("text", "").param("summary", "")).hasStatusOk()
			.bodyText()
			.contains("is required");
	}

	private static class RegexMatcher extends TypeSafeMatcher<String> {

		private final String regex;

		RegexMatcher(String regex) {
			this.regex = regex;
		}

		@Override
		public boolean matchesSafely(String item) {
			return Pattern.compile(this.regex).matcher(item).find();
		}

		@Override
		public void describeMismatchSafely(String item, Description mismatchDescription) {
			mismatchDescription.appendText("was \"").appendText(item).appendText("\"");
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("a string that matches regex: ").appendText(this.regex);
		}

		static org.hamcrest.Matcher<java.lang.String> matches(String regex) {
			return new RegexMatcher(regex);
		}

	}

}
