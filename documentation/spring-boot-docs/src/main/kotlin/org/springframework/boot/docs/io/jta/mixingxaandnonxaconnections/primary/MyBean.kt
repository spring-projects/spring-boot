/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.jta.mixingxaandnonxaconnections.primary

import jakarta.jms.ConnectionFactory

@Suppress("UNUSED_PARAMETER")
class MyBean(connectionFactory: ConnectionFactory?)

