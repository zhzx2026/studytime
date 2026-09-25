#!/usr/bin/env python3
"""构建前静态体检（没 JDK/SDK 时也能跑的粗筛）：

  1. 所有 XML 语法合法、引用（@string/@color/@drawable/@mipmap/@style）都能解析；
  2. AndroidManifest 里注册的 Activity/Application 类在 src/ 里真有；
  3. Java 源码里的 R.type.name 引用在 res/ 里真有；
  4. 每个 Java 文件 package 与目录一致、括号配平；
  5. 版本号：manifest ↔ README 的 CURRENT-VERSION 标记一致。

退出码非 0 = 有问题（CI 直接红）。
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
errs = []
os.chdir(ROOT)


def err(msg):
    errs.append(msg)


# ── 收资源定义 ──────────────────────────────────────────────────────────────
defined = {"string": set(), "color": set(), "drawable": set(), "mipmap": set(),
           "style": set(), "id": set(), "layout": set(), "dimen": set(), "attr": set()}
xml_files = []
for base, _, files in os.walk("res"):
    for f in files:
        p = os.path.join(base, f)
        xml_files.append(p)
        if f.endswith(".xml"):
            try:
                tree = ET.parse(p)
            except ET.ParseError as e:
                err("XML 语法错误 %s: %s" % (p, e))
                continue
            for el in tree.iter():
                tag = el.tag.split("}")[-1]
                name = el.get("name")
                if tag in ("string", "color", "style", "dimen", "attr", "id") and name:
                    # style 的 name 可能带点（Theme.X.Y）——引用处同样带点
                    defined[tag].add(name)
        # drawable/mipmap 的定义 = 文件名（xml 与否都算）
        kind = base.split(os.sep)[-1].split("-")[0]
        if kind in ("drawable", "mipmap"):
            defined[kind].add(f.rsplit(".", 1)[0])

# ── manifest ────────────────────────────────────────────────────────────────
MANIFEST_CLASSES = []
try:
    mt = ET.parse("AndroidManifest.xml")
    mroot = mt.getroot()
    pkg = mroot.get("package", "")
    if pkg != "com.aidemo.studytime":
        err("manifest package 不对: %s" % pkg)
    app = mroot.find("application")
    for el in [app] + list(app.findall("activity")) + list(app.findall("service")) + \
             list(app.findall("provider")) + list(app.findall("receiver")):
        if el is None:
            continue
        name = el.get("{http://schemas.android.com/apk/res/android}name", "")
        if name:
            MANIFEST_CLASSES.append(name)
except ET.ParseError as e:
    err("AndroidManifest.xml 语法错误: %s" % e)
    MANIFEST_CLASSES = []
    pkg = "com.aidemo.studytime"

for cls in MANIFEST_CLASSES:
    simple = cls.split(".")[-1]
    if cls.startswith("."):
        path = "src/%s/%s.java" % (pkg.replace(".", "/"), simple)
    elif cls.startswith(pkg):
        path = "src/%s/%s.java" % (cls.replace(".", "/").rsplit("/", 1)[0], simple)
    else:
        path = None
    if path and not os.path.exists(path):
        err("manifest 注册的类没有源码: %s → 找不到 %s" % (cls, path))

# ── 引用解析 ────────────────────────────────────────────────────────────────
ref_re = re.compile(r"@(string|color|drawable|mipmap|style|id|layout|dimen)/([A-Za-z0-9_.]+)")
android_attr_re = re.compile(r"android:(theme|icon|roundIcon|label)\s*=\s*\"(@[A-Za-z]+/[A-Za-z0-9_.]+)\"")

for p in ["AndroidManifest.xml"] + xml_files:
    try:
        text = open(p, encoding="utf-8").read()
    except Exception as e:
        err("读不了 %s: %s" % (p, e))
        continue
    for m in ref_re.finditer(text):
        kind, name = m.group(1), m.group(2)
        if name not in defined[kind]:
            err("%s 引用了不存在的 @%s/%s" % (p, kind, name))

# ── Java：R 引用 / package / 拷号 ───────────────────────────────────────────
r_re = re.compile(r"\bR\.(string|color|drawable|mipmap|style|id|layout|dimen)\.([A-Za-z0-9_]+)")
java_files = []
for base, _, files in os.walk("src"):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(base, f))
for base, _, files in os.walk("test"):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(base, f))

for p in java_files:
    text = open(p, encoding="utf-8").read()
    # package 与目录
    m = re.search(r"^package\s+([a-z0-9_.]+);", text, re.M)
    if p.startswith("src/"):
        expect = os.path.dirname(p)[len("src/"):].replace(os.sep, ".")
        if not m or m.group(1) != expect:
            err("%s package 与目录不符（%s vs %s）" % (p, m and m.group(1), expect))
    # R 引用（test/ 里的不算，那边没有 R）
    if p.startswith("src/"):
        for rm in r_re.finditer(text):
            kind, name = rm.group(1), rm.group(2)
            if name not in defined[kind]:
                err("%s 引用了不存在的 R.%s.%s" % (p, kind, name))
    # 括号配平（粗筛：忽略字符串/注释里的括号不可怕，我们的源码里没有）
    stripped = re.sub(r'"(\\.|[^"\\])*"', '""', text)
    stripped = re.sub(r"'(\\.|[^'\\])*'", "''", stripped)
    stripped = re.sub(r"//.*", "", stripped)
    stripped = re.sub(r"/\*.*?\*/", "", stripped, flags=re.S)
    for a, b in [("{", "}"), ("(", ")"), ("[", "]")]:
        if stripped.count(a) != stripped.count(b):
            err("%s 括号不配平: %s%d vs %s%d" % (p, a, stripped.count(a), b, stripped.count(b)))

# ── 版本号 manifest ↔ README ────────────────────────────────────────────────
try:
    man = open("AndroidManifest.xml", encoding="utf-8").read()
    ver = re.search(r'versionName="([^"]+)"', man).group(1)
    code = re.search(r'versionCode="(\d+)"', man).group(1)
    rd = open("README.md", encoding="utf-8").read()
    mver = re.search(r"<!-- CURRENT-VERSION -->\s*\n?\s*\*\*v?([0-9.]+)", rd) or \
           re.search(r"\*\*当前版本：v?([0-9.]+)", rd)
    if mver and mver.group(1) != ver:
        err("README 版本(%s) ≠ manifest versionName(%s)" % (mver.group(1), ver))
    if not code.isdigit() or int(code) < 1:
        err("versionCode 非法: %s" % code)
except Exception as e:
    err("版本号检查失败: %s" % e)

if errs:
    print("refcheck: %d 个问题" % len(errs))
    for e in errs:
        print("  ✗", e)
    sys.exit(1)
print("refcheck OK（%d 个 java / %d 个 res xml）" % (len(java_files), len(xml_files)))
