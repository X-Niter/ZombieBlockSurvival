import subprocess
from datetime import datetime

changed = subprocess.getoutput("git diff --name-only HEAD^ HEAD").splitlines()
non_plugin = [f for f in changed if not f.startswith("src/java")]

if non_plugin:
    log_entry = f"### 🔧 {datetime.now().strftime('%Y-%m-%d %H:%M')}\n"
    log_entry += "\n".join(f"- `{f}` modified." for f in non_plugin)
    log_entry += "\n\n"

    with open("CHANGELOGS/system_changelog.md", "a", encoding="utf-8") as f:
        f.write(log_entry)
    print("✅ UI/System changelog updated.")
else:
    print("🛑 No non-plugin changes detected.")
