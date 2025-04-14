/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.logging.structured;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * Utility to help with writing ElasticCommonSchema pairs in their nested form.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class ElasticCommonSchemaPairs {

	private ElasticCommonSchemaPairs() {
	}

	public static Map<String, Object> nested(Map<String, String> map) {
		return nested(map::forEach);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<String, Object> nested(Consumer<BiConsumer<K, V>> nested) {
		Map<String, Object> result = new LinkedHashMap<>();
		nested.accept((name, value) -> {
			List<String> nameParts = List.of(name.toString().split("\\."));
			Map<String, Object> destination = result;
			for (int i = 0; i < nameParts.size() - 1; i++) {
				Object existing = destination.computeIfAbsent(nameParts.get(i), (key) -> new LinkedHashMap<>());
				if (!(existing instanceof Map)) {
					String common = nameParts.subList(0, i + 1).stream().collect(Collectors.joining("."));
					throw new IllegalStateException("Duplicate ECS pairs added under '%s'".formatted(common));
				}
				destination = (Map<String, Object>) existing;
			}
			Object previous = destination.put(nameParts.get(nameParts.size() - 1), value);
			Assert.state(previous == null, () -> "Duplicate ECS pairs added under '%s'".formatted(name));
		});
		return result;
	}

}
