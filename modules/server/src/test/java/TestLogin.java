import cn.hutool.http.HttpUtil;


public class TestLogin {
    public static void main(String[] args) {
        while (true) {
            String r = HttpUtil.createPost("http://127.0.0.1:2122/userLogin").
                    form("userName", "admin").
                    form("userPwd", "5b67127803e84539ea43ce62657eca38a0903e93").execute().body();
            System.out.println(r);
        }
    }
}
