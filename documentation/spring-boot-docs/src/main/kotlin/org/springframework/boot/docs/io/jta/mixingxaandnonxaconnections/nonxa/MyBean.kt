/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.jta.mixingxaandnonxaconnections.nonxa

import jakarta.jms.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier

@Suppress("UNUSED_PARAMETER")
class MyBean(@Qualifier("nonXaJmsConnectionFactory") connectionFactory: ConnectionFactory?)

