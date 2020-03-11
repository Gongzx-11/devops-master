import gzx.devops.util.SvnKitUtil;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;


public class TestSvn {
    public static void main(String[] args) throws SVNException {
        String s = SvnKitUtil.checkOut("svn://gitee.com/keepbx/Jpom-demo-case",
                "", "", new File("/test/tt"));
        System.out.println(s);
    }
}
