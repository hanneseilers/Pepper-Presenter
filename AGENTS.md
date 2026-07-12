# Copilot agent instructions

## Primary task source

The file `todo.txt` in the repository root is the authoritative task list.

Whenever you are asked to work on this repository:

1. Read `todo.txt` before analyzing or modifying source code.
2. Identify all entries beginning with `[ ]`.
3. Process open tasks in the order in which they appear.
4. Work on only one logically related task group at a time.
5. Inspect the existing implementation before making changes.
6. Make only changes that are necessary for the selected task.
7. Validate the implementation before marking a task as complete.
8. Change `[]` to `[x]` only after the task has been implemented and validated.
9. Never mark a task complete merely because code was generated.
10. If a task cannot be completed, replace `[]` with `[!]` and append a precise explanation.
11. Do not remove task descriptions from `todo.txt`.
12. Do not modify entries already marked with `[x]`.
13. At the end, report:
    - completed tasks,
    - blocked tasks,
    - changed files,
    - validation commands and results.

## Validation

After Kotlin, Java, Android resource, manifest, or Gradle changes, run:

```bash
./gradlew assembleDebug
```

A task that affects compilable source code may only be marked as completed if:

./gradlew assembleDebug succeeds, or
an external limitation prevents the build and this limitation is documented in todo.txt.

Project compatibility

This is an Android application for the Pepper robot.

Preserve these constraints unless a task in todo.txt explicitly states otherwise:

Android Studio Flamingo 2022.2.1
Pepper Android Studio plugin compatibility
SoftBank Robotics/Aldebaran QiSDK compatibility
compileSdk 34
targetSdk 33
minSdk 23
existing Gradle, Android Gradle Plugin, Kotlin and Java versions
existing package names

Do not invent QiSDK classes, methods, callbacks or dependencies.

Use the QiSDK types actually available in the project dependencies.

Safety rules
Do not run git push.
Do not rewrite Git history.
Do not delete untracked files.
Do not modify files outside this repository.
Do not install system-wide software.
Do not expose credentials or configuration secrets.
Do not perform unrelated refactoring.

Do not hide compiler errors, disable checks, or remove functionality merely to make the build succeed.
