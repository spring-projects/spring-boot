package org.springframework.boot.autoconfigureprocessor.extractor;

import java.util.List;

public interface ValueExtractor {

	List<Object> getValues(Object annotation);

}
