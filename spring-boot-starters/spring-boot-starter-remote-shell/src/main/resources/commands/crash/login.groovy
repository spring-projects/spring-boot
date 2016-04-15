import org.springframework.util.ClassUtils
import org.springframework.core.io.DefaultResourceLoader

welcome = { ->
    def environment = crash.context.attributes['spring.environment']
    
    if (!environment.getProperty("spring.main.show_banner", Boolean.class, Boolean.TRUE)) {
        return ""
    }
    
    def resource = new DefaultResourceLoader(ClassUtils.getDefaultClassLoader()).getResource(
        environment.getProperty('banner.location', String, 'classpath:banner.txt')
    )
    
    def bannerText = resource.exists() && resource.isReadable() ? resource.inputStream.text : null
    if( bannerText ){
        return "${bannerText}\n"

    } else {
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
}

prompt = { ->
    return "> ";
}
