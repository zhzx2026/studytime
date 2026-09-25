#!/usr/bin/env python3
"""zipalign 的纯 Python 实现（`zipalign -f <n> in out` 语义的最小可用版）。

沙箱里从 GitHub 镜像仓捞出来的原生 zipalign 缺 libc++.so 跑不起来，build.sh
会自动退回本脚本。做法：把每个 STORED（不压缩）条目的 local header extra 字段
垫一个自定义 TLV，让数据起点对齐到 n 字节；DEFLATED 条目本来就不需要对齐。
对齐在签名之前做（apksigner 会重写中央目录，不影响已对齐的数据偏移）。
"""
import struct
import sys
import zipfile


def pad_extra(extra: bytes, header_len: int, align: int) -> bytes:
    """让 (header_len + len(pad)) % align == 0；补一个未知 ID 的 TLV。"""
    rem = (header_len + len(extra)) % align
    if rem == 0:
        return extra
    need = align - rem
    # 再加一个 TLV：2B id + 2B size + payload；最少加 4 字节
    if need < 4:
        need += align
    payload_len = need - 4
    return extra + struct.pack("<HH", 0xD7AB, payload_len) + b"\x00" * payload_len


def main(argv):
    if len(argv) == 5 and argv[1] == "-f":
        align, src, dst = int(argv[2]), argv[3], argv[4]
    elif len(argv) == 4:
        align, src, dst = int(argv[1]), argv[2], argv[3]
    else:
        sys.stderr.write("usage: zipalign.py [-f] <align> <infile> <outfile>\n")
        return 2

    offset = 0
    with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
            ni.compress_type = info.compress_type
            ni.external_attr = info.external_attr
            ni.internal_attr = info.internal_attr
            ni.create_system = info.create_system
            ni.comment = info.comment
            fn_len = len(info.filename.encode("utf-8"))
            # 下一个 local header 的落点 = 当前文件偏移（zipfile 顺序写）
            try:
                offset = zout.fp.tell()
            except Exception:
                offset = 0
            if info.compress_type == zipfile.ZIP_STORED:
                # 一次算清：垫未知 ID 的 TLV，让 (offset + 30 + 文件名 + extra) 整除 align
                base = offset + 30 + fn_len
                ni.extra = pad_extra(info.extra, base, align)
            else:
                ni.extra = info.extra
            zout.writestr(ni, data)

    # 回读校验：所有 STORED 条目数据起点必须整除 align
    bad = 0
    with zipfile.ZipFile(dst) as z:
        for info in z.infolist():
            if info.compress_type != zipfile.ZIP_STORED:
                continue
            fn_len = len(info.filename.encode("utf-8"))
            data_off = info.header_offset + 30 + fn_len + len(info.extra)
            if data_off % align != 0:
                bad += 1
                sys.stderr.write("!! %s 未对齐（off=%d）\n" % (info.filename, data_off))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
