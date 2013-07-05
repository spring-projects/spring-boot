import java.io.*;
import org.springframework.zero.maven.*;

Verify.verifyWar(
	new File( basedir, "target/executable-war-0.0.1.BUILD-SNAPSHOT.war" )
);

