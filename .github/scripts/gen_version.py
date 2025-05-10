import semver, subprocess, sys

# Determine if src/java was modified
changed_files = subprocess.getoutput("git diff --name-only HEAD^ HEAD").split("\n")
plugin_changes = [f for f in changed_files if f.startswith("src/java")]

if not plugin_changes:
    print("🛑 No plugin code changes. Skipping version bump.")
    sys.exit(0)

# Get commit count for sanity
commits = subprocess.getoutput("git rev-list --count HEAD")
if commits == "0":
    print("No commits detected. Exiting.")
    sys.exit(0)

# Last commit
last_commit = subprocess.getoutput("git log -1 --pretty=%B").strip().lower()

# Get last tag or fallback
try:
    latest = subprocess.getoutput("git describe --tags --abbrev=0").strip()
    if not latest or "fatal" in latest.lower():
        raise ValueError("No tags")
    parsed = semver.VersionInfo.parse(latest.split("-")[0])
    new_ver = parsed.bump_patch()
except:
    new_ver = semver.VersionInfo.parse("0.0.1")
    subprocess.run(["git", "tag", "v0.0.1"])
    subprocess.run(["git", "push", "--tags"])
    print("0.0.1")
    sys.exit(0)

# Suffix
if "alpha" in last_commit:
    print(f"{new_ver}-alpha")
elif "beta" in last_commit:
    print(f"{new_ver}-beta")
else:
    print(f"{new_ver}")
