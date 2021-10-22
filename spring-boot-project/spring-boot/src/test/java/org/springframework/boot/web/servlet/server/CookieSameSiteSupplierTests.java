/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.Cookie.SameSite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link CookieSameSiteSupplier}.
 *
 * @author Phillip Webb
 */
class CookieSameSiteSupplierTests {

	@Test
	void whenHasNameWhenNameIsNullThrowsException() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasName((String) null))
				.withMessage("Name must not be empty");
	}

	@Test
	void whenHasNameWhenNameIsEmptyThrowsException() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasName(""))
				.withMessage("Name must not be empty");
	}

	@Test
	void whenHasNameWhenNameMatchesCallsGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThat(supplier.whenHasName("test").getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
	}

	@Test
	void whenHasNameWhenNameDoesNotMatchDoesNotCallGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
		assertThat(supplier.whenHasName("test").getSameSite(new Cookie("tset", "x"))).isNull();
	}

	@Test
	void whenHasSuppliedNameWhenNameIsNullThrowsException() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasName((Supplier<String>) null))
				.withMessage("NameSupplier must not be empty");
	}

	@Test
	void whenHasSuppliedNameWhenNameMatchesCallsGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThat(supplier.whenHasName(() -> "test").getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
	}

	@Test
	void whenHasSuppliedNameWhenNameDoesNotMatchDoesNotCallGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
		assertThat(supplier.whenHasName(() -> "test").getSameSite(new Cookie("tset", "x"))).isNull();
	}

	@Test
	void whenHasNameMatchingRegexWhenRegexIsNullThrowsException() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasNameMatching((String) null))
				.withMessage("Regex must not be empty");
	}

	@Test
	void whenHasNameMatchingRegexWhenRegexIsEmptyThrowsException() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasNameMatching(""))
				.withMessage("Regex must not be empty");
	}

	@Test
	void whenHasNameMatchingRegexWhenNameMatchesCallsGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThat(supplier.whenHasNameMatching("te.*").getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
	}

	@Test
	void whenHasNameMatchingRegexWhenNameDoesNotMatchDoesNotCallGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
		assertThat(supplier.whenHasNameMatching("te.*").getSameSite(new Cookie("tset", "x"))).isNull();
	}

	@Test
	void whenHasNameMatchingPatternWhenPatternIsNullThrowsException() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasNameMatching((Pattern) null))
				.withMessage("Pattern must not be null");
	}

	@Test
	void whenHasNameMatchingPatternWhenNameMatchesCallsGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThat(supplier.whenHasNameMatching(Pattern.compile("te.*")).getSameSite(new Cookie("test", "x")))
				.isEqualTo(SameSite.LAX);
	}

	@Test
	void whenHasNameMatchingPatternWhenNameDoesNotMatchDoesNotCallGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
		assertThat(supplier.whenHasNameMatching(Pattern.compile("te.*")).getSameSite(new Cookie("tset", "x"))).isNull();
	}

	@Test
	void whenWhenPredicateIsNullThrowsException() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.when(null))
				.withMessage("Predicate must not be null");
	}

	@Test
	void whenWhenPredicateMatchesCallsGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
		assertThat(supplier.when((cookie) -> cookie.getName().equals("test")).getSameSite(new Cookie("test", "x")))
				.isEqualTo(SameSite.LAX);
	}

	@Test
	void whenWhenPredicateDoesNotMatchDoesNotCallGetSameSite() {
		CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
		assertThat(supplier.when((cookie) -> cookie.getName().equals("test")).getSameSite(new Cookie("tset", "x")))
				.isNull();
	}

	@Test
	void ofNoneSuppliesNone() {
		assertThat(CookieSameSiteSupplier.ofNone().getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.NONE);
	}

	@Test
	void ofLaxSuppliesLax() {
		assertThat(CookieSameSiteSupplier.ofLax().getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
	}

	@Test
	void ofStrictSuppliesStrict() {
		assertThat(CookieSameSiteSupplier.ofStrict().getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.STRICT);
	}

	@Test
	void ofWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> CookieSameSiteSupplier.of(null))
				.withMessage("SameSite must not be null");
	}

	@Test
	void ofSuppliesValue() {
		assertThat(CookieSameSiteSupplier.of(SameSite.STRICT).getSameSite(new Cookie("test", "x")))
				.isEqualTo(SameSite.STRICT);
	}

}
