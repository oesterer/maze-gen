#!/usr/bin/env bash
set -euo pipefail

# Remove tracked files that now match .gitignore patterns.
# This uses git rm --cached so working tree copies remain; rerun with --force
# if you want them deleted from disk, too.

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

# List tracked files that match ignore rules (-c) and are ignored (-i).
files=()
while IFS= read -r line; do
  files+=("$line")
done < <(git ls-files -i -c --exclude-standard)

if [ ${#files[@]} -eq 0 ]; then
  echo "No tracked ignored files to remove."
  exit 0
fi

echo "Removing tracked ignored files:" && printf ' - %s\n' "${files[@]}"
git rm --cached --ignore-unmatch -- "${files[@]}"
