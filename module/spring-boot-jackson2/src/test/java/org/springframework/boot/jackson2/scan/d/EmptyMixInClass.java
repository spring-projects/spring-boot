/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.scan.d;

import org.springframework.boot.jackson2.JsonMixin;
import org.springframework.boot.jackson2.types.Name;
import org.springframework.boot.jackson2.types.NameAndAge;

@JsonMixin(type = { Name.class, NameAndAge.class })
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public class EmptyMixInClass {

}
