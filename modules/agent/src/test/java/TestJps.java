import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import gzx.devops.util.CommandUtil;
import org.junit.Test;

import java.util.List;


public class TestJps {
    @Test
    public void test() {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -v");
        List<String> list = StrSpliter.splitTrim(execSystemCommand, StrUtil.LF, true);
        for (String item : list) {
            System.out.println("******************************");
            System.out.println(item);
        }
    }
}
