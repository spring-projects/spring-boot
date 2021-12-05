package org.springframework.boot.autoconfigureprocessor.extractor;

import java.util.Comparator;
import java.util.List;

public class OnClassConditionValueExtractor extends NamedValuesExtractor {

	public OnClassConditionValueExtractor() {
		super("value", "name");
	}

	@Override
	public List<Object> getValues(Object annotation) {
		List<Object> values = super.getValues(annotation);
		values.sort(this::compare);
		return values;
	}

	private int compare(Object o1, Object o2) {
		return Comparator.comparing(this::isSpringClass).thenComparing(String.CASE_INSENSITIVE_ORDER)
				.compare(o1.toString(), o2.toString());
	}

	private boolean isSpringClass(String type) {
		return type.startsWith("org.springframework");
	}
}
