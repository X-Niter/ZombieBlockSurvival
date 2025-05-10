import os
import subprocess

def run_command(command):
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    print(f"\nCommand: {command}\n{'='*40}\n{result.stdout}\n{'-'*40}\n{result.stderr}")
    return result.stdout, result.stderr

if __name__ == "__main__":
    print("🔍 Running static analysis and auto-fix...
")
    
    # Format code with Black
    print("🧹 Formatting with black...")
    run_command("black scripts")

    # Type check with mypy
    print("🔎 Type checking with mypy...")
    run_command("mypy scripts")

    # Linting with pylint
    print("📋 Linting with pylint...")
    run_command("pylint scripts")
    
    print("✅ Self-test and auto-fix process completed.")
