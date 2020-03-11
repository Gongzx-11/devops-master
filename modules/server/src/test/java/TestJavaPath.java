import gzx.devops.util.CommandUtil;
import org.junit.Test;

import java.util.Map;


public class TestJavaPath {

    @Test
    public void t() {
        String command = CommandUtil.execSystemCommand("\"C:\\Program Files\\Java\\jdk1.8.0_211\\bin\\java.exe\"  -version");
        System.out.println(command);



        command = CommandUtil.execSystemCommand("\"C:\\Program Files\\Java\\jdk-11.0.3\\bin\\java.exe\"  -version");
        System.out.println(command);


    }

    @Test
    public void t1() {
        Map<String, String> getenv = System.getenv();
        System.out.println(getenv.get("JAVA_HOME"));
    }
}
