/** 极简断言（主机侧测试不引 JUnit，javac + java 就能跑）。 */
public final class T {
    static int pass, fail;

    static void ok(boolean cond, String what) {
        if (cond) pass++;
        else { fail++; System.out.println("  FAIL: " + what); }
    }

    static void eq(Object got, Object want, String what) {
        boolean same = got == null ? want == null : got.equals(want);
        ok(same, what + "（got=" + got + " want=" + want + "）");
    }

    static void done(String name) {
        System.out.println(name + ": " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
