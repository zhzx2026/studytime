import com.aidemo.studytime.Todo;

/** 待办清单：增删改查、转义落盘、脏行容错、id 不复用。 */
public class TodoTest {
    public static void main(String[] a) {
        // 1. 添加 + 规范化
        Todo t = new Todo();
        Todo.Task x = t.add("  背第三章  ", 3);
        T.ok(x != null, "能添加");
        T.eq("背第三章", x.title, "首尾空白掐掉");
        T.eqi(3, x.est, "预计番茄跟着进");
        T.eqi(1, x.id, "id 从 1 开始");
        T.ok(t.add("   ", 0) == null, "空标题拒收");
        T.ok(t.add(null, 0) == null, "null 拒收");
        T.eqi(1, t.tasks.size(), "空的没进去");

        // 换行/制表符压成空格、超长截断
        Todo.Task y = t.add("ab\ncd", 0);
        T.eq("ab cd", y.title, "换行压成空格");
        StringBuilder longsb = new StringBuilder();
        for (int i = 0; i < 100; i++) longsb.append('x');
        Todo.Task z = t.add(longsb.toString(), 0);
        T.eqi(60, z.title.length(), "超长截到 60");

        // 2. 翻转 / 删除 / 清理
        t.toggle(x.id);
        T.ok(x.done, "toggle 后完成");
        T.eqi(1, t.countDone(), "done=1");
        t.toggle(x.id);
        T.ok(!x.done, "再 toggle 回来");
        t.toggle(y.id);
        t.add("第三条", 0);
        T.eqi(4, t.tasks.size(), "四条");
        int removed = t.clearDone();
        T.eqi(1, removed, "清掉 1 条已完成");
        T.eqi(3, t.tasks.size(), "剩 3 条");
        T.ok(t.remove(x.id), "按 id 删");
        T.ok(!t.remove(x.id), "再删返回 false");

        // 3. 下一个未完成
        Todo t2 = new Todo();
        Todo.Task a1 = t2.add("a", 0);
        Todo.Task a2 = t2.add("b", 0);
        a1.done = true;
        T.eqi(a2.id, t2.nextUndone().id, "跳过已完成");
        a2.done = true;
        T.ok(t2.nextUndone() == null, "全完成返回 null");

        // 4. 落盘往返：标题里的 \t \n \\ 不串行
        Todo t3 = new Todo();
        t3.add("标题带\t制表符和\\反斜杠", 2);
        t3.add("多行\n标题", 0); // add() 会把换行压成空格
        Todo.Task d = t3.add("已完成的", 1);
        t3.toggle(d.id);
        d.counted = true;
        d.donePomos = 3;
        String blob = t3.save();
        Todo t4 = new Todo();
        t4.load(blob);
        T.eqi(3, t4.tasks.size(), "往返 3 条");
        T.eq(t3.tasks.get(0).title, t4.tasks.get(0).title, "转义往返一致");
        T.eq("多行 标题", t4.tasks.get(1).title, "add 已把换行压成空格");
        T.ok(t4.tasks.get(2).done, "done 往返");
        T.ok(t4.tasks.get(2).counted, "counted 往返");
        T.eqi(3, t4.tasks.get(2).donePomos, "donePomos 往返");
        T.eqi(d.id, t4.tasks.get(2).id, "id 往返");

        // 4b. 落盘行里的 \n 转义（load 进来的标题可以含换行，存回去必须还是「一行一条」）
        Todo t7 = new Todo();
        t7.load("n=1\n8\t0\t0\t0\t第一行\\n第二行\n");
        T.eq("第一行\n第二行", t7.tasks.get(0).title, "load 还原换行");
        String blob2 = t7.save();
        T.eqi(2, blob2.split("\n").length, "标题带换行也不裂行（header+1行）");
        Todo t8 = new Todo();
        t8.load(blob2);
        T.eq("第一行\n第二行", t8.tasks.get(0).title, "再往返仍还原");

        // 5. id 不复用：继续加是新号
        Todo.Task more = t4.add("新来的", 0);
        T.ok(more.id > d.id, "新任务 id 更大");

        // 6. 脏数据：坏行丢弃、好行照收、header 也能吃
        Todo t5 = new Todo();
        t5.load("n=9\n1\t2\t0\t0\t好行\n坏数据坏数据\n2\tx\t0\t0\t坏数字\n");
        T.eqi(1, t5.tasks.size(), "只收好行");
        Todo.Task nw = t5.add("下一个", 0);
        T.eqi(9, nw.id, "nextId 吃到 header");
        Todo t6 = new Todo();
        t6.load(null);
        T.eqi(0, t6.tasks.size(), "null 载入 = 空");
        t6.load("");
        T.eqi(0, t6.tasks.size(), "空串载入 = 空");

        T.done("TodoTest");
    }
}
