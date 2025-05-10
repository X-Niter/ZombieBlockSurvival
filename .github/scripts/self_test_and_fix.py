import os
import subprocess
from datetime import datetime

def run_command(command, label=""):
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True)
        with open("ai_diagnostics.log", "a", encoding="utf-8") as log:
            log.write(f"=== [{label or command}] @ {datetime.utcnow().isoformat()} UTC ===\n")
            log.write(f"{result.stdout}\n{result.stderr}\n\n")
        return result.returncode
    except Exception as e:
        with open("ai_diagnostics.log", "a", encoding="utf-8") as log:
            log.write(f"[ERROR running {command}] Exception: {e}\n")
        return 1

if __name__ == "__main__":
    print("🔍 Running static analysis and auto-fix...")

    os.makedirs(".github/logs", exist_ok=True)

    # Clean old log if needed
    if os.path.exists("ai_diagnostics.log"):
        os.rename("ai_diagnostics.log", f".github/logs/diagnostics_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}.log")

    print("🧹 Formatting with black...")
    run_command("black .github/scripts", "black-format")

    print("🔎 Type checking with mypy...")
    run_command("mypy .github/scripts", "mypy-check")

    print("📋 Linting with pylint...")
    run_command("pylint .github/scripts", "pylint-lint")

    print("✅ Self-test and auto-fix completed with fallback logging.")
