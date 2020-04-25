package org.springframework.boot.convert;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ObjectUtils;

import java.time.Period;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

public class PeriodToStringConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Period.class, String.class));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert((Period) source, getPeriodUnit(sourceType));
	}

	private String convert(Period source, ChronoUnit unit) {

		return source.toString();
	}

	private ChronoUnit getPeriodUnit(TypeDescriptor sourceType) {
		PeriodUnit annotation = sourceType.getAnnotation(PeriodUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

}
