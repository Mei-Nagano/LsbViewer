"""从 CHANGELOG 中提取指定版本的 Markdown 正文，供云端发布 Release。"""

import argparse
import re
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version")
    parser.add_argument("output", type=Path)
    parser.add_argument("--changelog", type=Path, default=Path("CHANGELOG.md"))
    args = parser.parse_args()

    text = args.changelog.read_text(encoding="utf-8-sig")
    headings = list(re.finditer(r"^##[ \t]+([^\r\n]+)$", text, re.MULTILINE))
    matches = [
        (index, heading)
        for index, heading in enumerate(headings)
        if re.fullmatch(
            rf"\[{re.escape(args.version)}\](?:[ \t]+-[ \t]+\d{{4}}-\d{{2}}-\d{{2}})?[ \t]*",
            heading.group(1).rstrip("\r"),
        )
    ]
    if len(matches) != 1:
        raise SystemExit(f"CHANGELOG.md 必须包含一个且仅一个 [{args.version}] 版本条目")

    index, heading = matches[0]
    end = headings[index + 1].start() if index + 1 < len(headings) else len(text)
    notes = text[heading.end():end].strip()
    if not notes:
        raise SystemExit(f"CHANGELOG.md 中 [{args.version}] 的更新内容为空")

    args.output.write_text(notes + "\n", encoding="utf-8")
    print(f"已读取 {args.version} 更新内容：{args.output}")


if __name__ == "__main__":
    main()
