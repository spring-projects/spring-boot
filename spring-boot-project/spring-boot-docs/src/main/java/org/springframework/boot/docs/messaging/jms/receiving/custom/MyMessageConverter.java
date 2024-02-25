/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.messaging.jms.receiving.custom;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * MyMessageConverter class.
 */
class MyMessageConverter implements MessageConverter {

	/**
	 * Converts the given object to a JMS Message.
	 * @param object the object to be converted
	 * @param session the JMS session
	 * @return the converted JMS Message
	 * @throws JMSException if an error occurs during the conversion
	 * @throws MessageConversionException if the object cannot be converted to a JMS
	 * Message
	 */
	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		return null;
	}

	/**
	 * Converts a JMS Message to an Object.
	 * @param message the JMS Message to convert
	 * @return the converted Object, or null if conversion is not possible
	 * @throws JMSException if an error occurs while converting the message
	 * @throws MessageConversionException if the message cannot be converted to an Object
	 */
	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		return null;
	}

}
