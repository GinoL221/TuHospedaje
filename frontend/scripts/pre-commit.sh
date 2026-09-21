#!/bin/sh

if ! command -v gitleaks >/dev/null 2>&1; then
	printf '%s\n' 'gitleaks is required to commit safely.' >&2
	exit 1
fi

gitleaks git --pre-commit --staged --redact --no-banner . || exit $?

cd frontend || exit 1
exec npm exec -- lint-staged
