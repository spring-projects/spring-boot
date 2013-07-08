import java.io.*;
import org.springframework.maven.packaging.*;

Verify.verifyWar(
	new File( basedir, "target/executable-war-0.0.1.BUILD-SNAPSHOT.war" )
);

