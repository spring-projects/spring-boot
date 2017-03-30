package org.springframework.boot.autoconfigure.social;

import org.springframework.boot.autoconfigure.social.SocialProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.social.google")

public class GoogleProperties extends SocialProperties{

	
}
