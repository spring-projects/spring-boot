package org.springframework.boot.autoconfigure.jms.hornetq;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.jms.client.HornetQConnectionFactory;

/**
 * Secured HornetQ implementation of a JMS ConnectionFactory.
 * 
 * @author Stéphane Lagraulet
 *
 */
public class HornetQSecuredConnectionFactory extends HornetQConnectionFactory {

	private HornetQProperties properties;

	public HornetQSecuredConnectionFactory(HornetQProperties properties, final boolean ha,
			final TransportConfiguration... initialConnectors) {
		super(ha, initialConnectors);
		this.properties = properties;
	}

	public Connection createConnection() throws JMSException {
		return createConnection(properties.getUser(), properties.getPassword());
	}

}
