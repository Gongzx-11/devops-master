import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;

import java.util.List;


public class RunFile {
    public static void main(String[] args) {
//        https://www.jianshu.com/p/3ae2f024c291
//        List<String> list = FileUtil.listFileNames("D:\\SystemDocument\\Documents\\Tencent Files\\1593503371\\Image\\Group");
//        List<File> list1 = FileUtil.loopFiles("D:\\SystemDocument\\Documents\\Tencent Files\\1593503371\\Image\\Group");
//        System.out.println(list.size());
//        System.out.println(list1.size());

//        System.out.println(FileUtil.readableFileSize(FileUtil.size(new File("D:\\SystemDocument\\Documents\\Tencent Files\\1593503371\\Image"))));
//        System.out.println(list);
//        String msg = exec("/test.sh");
//        System.out.println(msg);
        List<String> list = StrSpliter.splitTrim(":::18000", StrUtil.COLON, true);
        System.out.println(list);
    }
}
