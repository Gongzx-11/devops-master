
public class TryFinally {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            try {
                if (i == 0) {
                    System.out.println("continue");
                    continue;
                }
            } finally {
                System.out.println(i + "finally");
            }

        }
    }
}
