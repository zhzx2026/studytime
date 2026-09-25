#!/usr/bin/env python3
"""无 JDK 时的粗筛：src 里用到的 R.<type>.<name> 必须在 res/ 里有定义；manifest 里的类必须有源文件。"""
import os, re, sys, glob

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(root)
defined = set()
for f in glob.glob("res/values*/*.xml"):
    for t, n in re.findall(r'<(color|string|dimen|style|bool|integer)\s+name="([^"]+)"', open(f, encoding="utf-8").read()):
        defined.add(("style" if t == "style" else t, n.replace(".", "_")))
for d in glob.glob("res/*/"):
    kind = os.path.basename(os.path.normpath(d)).split("-")[0]
    if kind in ("values",):
        continue
    for f in os.listdir(d):
        defined.add((kind, os.path.splitext(f)[0]))

bad = []
for f in glob.glob("src/**/*.java", recursive=True):
    src = open(f, encoding="utf-8").read()
    for t, n in re.findall(r'(?<![\w.])R\.(\w+)\.(\w+)', src):
        if (t, n) not in defined:
            bad.append(f"{f}: R.{t}.{n} 未定义")

man = open("AndroidManifest.xml", encoding="utf-8").read()
pkg = re.search(r'package="([^"]+)"', man).group(1)
for n in re.findall(r'android:name="\.(\w+)"', man):
    p = os.path.join("src", *pkg.split("."), n + ".java")
    if not os.path.exists(p):
        bad.append(f"AndroidManifest.xml: .{n} 没有源文件 {p}")
for n in re.findall(r'\"@(\w+)/([\w.]+)\"', man):
    if n[0] in ("string", "color", "style", "mipmap", "drawable") and (n[0], n[1].replace(".", "_")) not in defined:
        bad.append(f"AndroidManifest.xml: @{n[0]}/{n[1]} 未定义")

if bad:
    print("\n".join(bad))
    sys.exit(1)
print(f"refcheck OK（{len(defined)} 个资源）")
