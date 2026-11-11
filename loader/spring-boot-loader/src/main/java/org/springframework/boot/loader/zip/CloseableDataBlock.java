/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.zip;

import java.io.Closeable;

/**
 * A {@link Closeable} {@link DataBlock}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public interface CloseableDataBlock extends DataBlock, Closeable {

}
