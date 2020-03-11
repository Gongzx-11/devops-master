import org.springframework.util.AntPathMatcher;


public class TestPath {

    public static void main(String[] args) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        System.out.println(antPathMatcher.match("/s/**/sss.html", "//s/s/s/sss.html"));
    }
}
