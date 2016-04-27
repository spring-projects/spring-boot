welcome = { ->

	def environment = crash.context.attributes['spring.environment']
	def propertyResolver = new org.springframework.boot.bind.RelaxedPropertyResolver(environment, "spring.main.");
	def beanFactory = crash.context.attributes['spring.beanfactory']

	if (!propertyResolver.getProperty("show-banner", Boolean.class, Boolean.TRUE)) {
		return ""
	}

	// Try to print using the banner interface
	if (beanFactory != null) {
		try {
			def banner = beanFactory.getBean("springBootBanner")
			def out = new java.io.ByteArrayOutputStream()
			banner.printBanner(environment, null, new java.io.PrintStream(out))
			return out.toString()
		} catch (Exception ex) {
			// Ignore
		}
	}

	// Resolve hostname
	def hostName;
	try {
		hostName = java.net.InetAddress.getLocalHost().getHostName();
	}
	catch (java.net.UnknownHostException ignore) {
		hostName = "localhost";
	}

	// Get Spring Boot version from context
	def version = crash.context.attributes.get("spring.boot.version")

	return """\
  .   ____          _            __ _ _
 /\\\\ / ___'_ __ _ _(_)_ __  __ _ \\ \\ \\ \\
( ( )\\___ | '_ | '_| | '_ \\/ _` | \\ \\ \\ \\
 \\\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::  (v$version) on $hostName
""";
}

prompt = { ->
	return "> ";
}
