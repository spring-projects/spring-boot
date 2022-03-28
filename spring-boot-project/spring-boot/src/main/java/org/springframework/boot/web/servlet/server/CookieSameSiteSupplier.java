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

package org.springframework.boot.web.servlet.server;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Strategy interface that can be used with {@link ConfigurableServletWebServerFactory}
 * implementations in order to supply custom {@link SameSite} values for specific
 * {@link Cookie cookies}.
 * <p>
 * Basic CookieSameSiteSupplier implementations can be constructed using the {@code of...}
 * factory methods, typically combined with name matching. For example: <pre class="code">
 * CookieSameSiteSupplier.ofLax().whenHasName("mycookie");
 * </pre>
 *
 * @author Phillip Webb
 * @since 2.6.0
 * @see ConfigurableServletWebServerFactory#addCookieSameSiteSuppliers(CookieSameSiteSupplier...)
 */
@FunctionalInterface
public interface CookieSameSiteSupplier {

	/**
	 * Get the {@link SameSite} values that should be used for the given {@link Cookie}.
	 * @param cookie the cookie to check
	 * @return the {@link SameSite} value to use or {@code null} if the next supplier
	 * should be checked
	 */
	SameSite getSameSite(Cookie cookie);

	/**
	 * Limit this supplier so that it's only called if the Cookie has the given name.
	 * @param name the name to check
	 * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
	 * name matches
	 */
	default CookieSameSiteSupplier whenHasName(String name) {
		Assert.hasText(name, "Name must not be empty");
		return when((cookie) -> ObjectUtils.nullSafeEquals(cookie.getName(), name));
	}

	/**
	 * Limit this supplier so that it's only called if the Cookie has the given name.
	 * @param nameSupplier a supplier providing the name to check
	 * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
	 * name matches
	 */
	default CookieSameSiteSupplier whenHasName(Supplier<String> nameSupplier) {
		Assert.notNull(nameSupplier, "NameSupplier must not be empty");
		return when((cookie) -> ObjectUtils.nullSafeEquals(cookie.getName(), nameSupplier.get()));
	}

	/**
	 * Limit this supplier so that it's only called if the Cookie name matches the given
	 * regex.
	 * @param regex the regex pattern that must match
	 * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
	 * name matches the regex
	 */
	default CookieSameSiteSupplier whenHasNameMatching(String regex) {
		Assert.hasText(regex, "Regex must not be empty");
		return whenHasNameMatching(Pattern.compile(regex));
	}

	/**
	 * Limit this supplier so that it's only called if the Cookie name matches the given
	 * {@link Pattern}.
	 * @param pattern the regex pattern that must match
	 * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
	 * name matches the pattern
	 */
	default CookieSameSiteSupplier whenHasNameMatching(Pattern pattern) {
		Assert.notNull(pattern, "Pattern must not be null");
		return when((cookie) -> pattern.matcher(cookie.getName()).matches());
	}

	/**
	 * Limit this supplier so that it's only called if the predicate accepts the Cookie.
	 * @param predicate the predicate used to match the cookie
	 * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
	 * cookie matches the predicate
	 */
	default CookieSameSiteSupplier when(Predicate<Cookie> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		return (cookie) -> predicate.test(cookie) ? getSameSite(cookie) : null;
	}

	/**
	 * Return a new {@link CookieSameSiteSupplier} that always returns
	 * {@link SameSite#NONE}.
	 * @return the supplier instance
	 */
	static CookieSameSiteSupplier ofNone() {
		return of(SameSite.NONE);
	}

	/**
	 * Return a new {@link CookieSameSiteSupplier} that always returns
	 * {@link SameSite#LAX}.
	 * @return the supplier instance
	 */
	static CookieSameSiteSupplier ofLax() {
		return of(SameSite.LAX);
	}

	/**
	 * Return a new {@link CookieSameSiteSupplier} that always returns
	 * {@link SameSite#STRICT}.
	 * @return the supplier instance
	 */
	static CookieSameSiteSupplier ofStrict() {
		return of(SameSite.STRICT);
	}

	/**
	 * Return a new {@link CookieSameSiteSupplier} that always returns the given
	 * {@link SameSite} value.
	 * @param sameSite the value to return
	 * @return the supplier instance
	 */
	static CookieSameSiteSupplier of(SameSite sameSite) {
		Assert.notNull(sameSite, "SameSite must not be null");
		return (cookie) -> sameSite;
	}

}
