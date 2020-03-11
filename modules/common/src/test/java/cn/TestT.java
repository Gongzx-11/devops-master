package cn;

import java.time.Duration;


public class TestT {
    public static void main(String[] args) {
        Duration parse = Duration.parse("1H");
        System.out.println(parse.getSeconds());
    }
}
