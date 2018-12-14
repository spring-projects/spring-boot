import java.io.*;
import org.springframework.boot.maven.*;

Verify.verifyJar(
	new File( basedir, "target/jar-0.0.1.BUILD-SNAPSHOT.jar" ), "some.random.Main", "Hello world"
);

