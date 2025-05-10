import semver, subprocess

latest = subprocess.getoutput("git describe --tags --abbrev=0").strip() or "0.0.0"
parsed = semver.VersionInfo.parse(latest.split("-")[0])
new_ver = parsed.bump_patch()

commit_msg = subprocess.getoutput("git log -1 --pretty=%B").lower()

if "alpha" in commit_msg:
    print(f"{new_ver}-alpha")
elif "beta" in commit_msg:
    print(f"{new_ver}-beta")
else:
    print(f"{new_ver}")
