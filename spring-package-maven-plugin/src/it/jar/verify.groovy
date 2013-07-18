import java.io.*;
import org.springframework.maven.packaging.*;

Verify.verifyJar(
	new File( basedir, "target/jar-0.0.1.BUILD-SNAPSHOT.jar" ), "some.random.Main"
);

