import gzx.devops.util.FileUtils;
import org.junit.Test;


public class TestJdkTest {

    @Test
    public void t() {
        String path = "C:\\Program Files\\Java\\jdk1.8.0_211";
        System.out.println(FileUtils.isJdkPath(path));
        //


        String version = FileUtils.getJdkVersion(path);
        System.out.println(version);

    }
}
