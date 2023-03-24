/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.ansi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertyResolver} for {@link AnsiStyle}, {@link AnsiColor},
 * {@link AnsiBackground} and {@link Ansi8BitColor} elements. Supports properties of the
 * form {@code AnsiStyle.BOLD}, {@code AnsiColor.RED} or {@code AnsiBackground.GREEN}.
 * Also supports a prefix of {@code Ansi.} which is an aggregation of everything (with
 * background colors prefixed {@code BG_}).
 * <p>
 * ANSI 8-bit color codes can be used with {@code AnsiColor} and {@code AnsiBackground}.
 * For example, {@code AnsiColor.208} will render orange text.
 * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">Wikipedia</a> has a complete
 * list of the 8-bit color codes that can be used.
 *
 * @author Phillip Webb
 * @author Toshiaki Maki
 * @since 1.3.0
 */
public class AnsiPropertySource extends PropertySource<AnsiElement> {

	private static final Iterable<Mapping> MAPPINGS;

	static {
		List<Mapping> mappings = new ArrayList<>();
		mappings.add(new EnumMapping<>("AnsiStyle.", AnsiStyle.class));
		mappings.add(new EnumMapping<>("AnsiColor.", AnsiColor.class));
		mappings.add(new Ansi8BitColorMapping("AnsiColor.", Ansi8BitColor::foreground));
		mappings.add(new EnumMapping<>("AnsiBackground.", AnsiBackground.class));
		mappings.add(new Ansi8BitColorMapping("AnsiBackground.", Ansi8BitColor::background));
		mappings.add(new EnumMapping<>("Ansi.", AnsiStyle.class));
		mappings.add(new EnumMapping<>("Ansi.", AnsiColor.class));
		mappings.add(new EnumMapping<>("Ansi.BG_", AnsiBackground.class));
		MAPPINGS = Collections.unmodifiableList(mappings);
	}

	private final boolean encode;

	/**
	 * Create a new {@link AnsiPropertySource} instance.
	 * @param name the name of the property source
	 * @param encode if the output should be encoded
	 */
	public AnsiPropertySource(String name, boolean encode) {
		super(name);
		this.encode = encode;
	}

	@Override
	public Object getProperty(String name) {
		if (StringUtils.hasLength(name)) {
			for (Mapping mapping : MAPPINGS) {
				String prefix = mapping.getPrefix();
				if (name.startsWith(prefix)) {
					String postfix = name.substring(prefix.length());
					AnsiElement element = mapping.getElement(postfix);
					if (element != null) {
						return (this.encode) ? AnsiOutput.encode(element) : element;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Mapping between a name and the pseudo property source.
	 */
	private abstract static class Mapping {

		private final String prefix;

		Mapping(String prefix) {
			this.prefix = prefix;
		}

		String getPrefix() {
			return this.prefix;
		}

		abstract AnsiElement getElement(String postfix);

	}

	/**
	 * {@link Mapping} for {@link AnsiElement} enums.
	 */
	private static class EnumMapping<E extends Enum<E> & AnsiElement> extends Mapping {

		private final Set<E> enums;

		EnumMapping(String prefix, Class<E> enumType) {
			super(prefix);
			this.enums = EnumSet.allOf(enumType);
		}

		@Override
		AnsiElement getElement(String postfix) {
			for (Enum<?> candidate : this.enums) {
				if (candidate.name().equals(postfix)) {
					return (AnsiElement) candidate;
				}
			}
			return null;
		}

	}

	/**
	 * {@link Mapping} for {@link Ansi8BitColor}.
	 */
	private static class Ansi8BitColorMapping extends Mapping {

		private final IntFunction<Ansi8BitColor> factory;

		Ansi8BitColorMapping(String prefix, IntFunction<Ansi8BitColor> factory) {
			super(prefix);
			this.factory = factory;
		}

		@Override
		AnsiElement getElement(String postfix) {
			if (containsOnlyDigits(postfix)) {
				try {
					return this.factory.apply(Integer.parseInt(postfix));
				}
				catch (IllegalArgumentException ex) {
				}
			}
			return null;
		}

		private boolean containsOnlyDigits(String postfix) {
			for (int i = 0; i < postfix.length(); i++) {
				if (!Character.isDigit(postfix.charAt(i))) {
					return false;
				}
			}
			return !postfix.isEmpty();
		}

	}

}
