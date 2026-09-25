/** 主机测试小断言工具（零依赖）。 */
public class T {
    static int checks = 0;
    static int fails = 0;

    public static void ok(boolean cond, String msg) {
        checks++;
        if (!cond) {
            fails++;
            System.out.println("  FAIL: " + msg);
        }
    }

    public static void eq(Object want, Object got, String msg) {
        checks++;
        boolean same = want == null ? got == null : want.equals(got);
        if (!same) {
            fails++;
            System.out.println("  FAIL: " + msg + " — want <" + want + "> got <" + got + ">");
        }
    }

    public static void eqi(long want, long got, String msg) {
        checks++;
        if (want != got) {
            fails++;
            System.out.println("  FAIL: " + msg + " — want <" + want + "> got <" + got + ">");
        }
    }

    /** 每个 Test 的 main 收尾：不通过就非零退出 */
    public static void done(String name) {
        if (fails > 0) {
            System.out.println(name + ": " + fails + "/" + checks + " FAILED");
            System.exit(1);
        }
        System.out.println(name + ": " + checks + " checks OK");
    }
}
