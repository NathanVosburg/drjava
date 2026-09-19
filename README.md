# DrJava — Go-button fork

A personal, class-focused fork of [DrJava](https://github.com/DrJavaAtRice/drjava)
by Nathan Vosburg. The toolbar combines Compile and Run into one **Go** button:
compile, wait for the interpreter to reset, then run if compilation succeeds.

This fork also fixes build configuration and Java/JShell compatibility issues
encountered while testing on Apple Silicon macOS with Homebrew OpenJDK 26.0.2.1.
It handles programs that exit their execution process so subsequent saves and
compilations still work. The focused integration tests cover successful runs,
edited-code reruns, compiler errors, and recovery after a program exits.
Other IDE features and platforms have not been comprehensively tested.

This is an independent fork, not an official DrJava release. Original DrJava
copyright notices and the [BSD-style license](drjava/LICENSE) are retained.

# Download and run

**[Download drjava.jar](https://github.com/NathanVosburg/drjava/releases/latest/download/drjava.jar)**

1. Install a **full JDK 26** if you do not already have one. This fork needs the
   compiler and JShell included in a JDK; Java 8 and a standalone JRE are not sufficient.
2. Download the JAR above. No Git, Ant, or source-code build is needed.
3. From the folder containing the downloaded file, run:

   ```sh
   java -jar drjava.jar
   ```

4. Open your `.java` file and click **Go**.

Double-clicking the JAR may also work if your system associates JAR files with
JDK 26. If it opens with an older Java version, use the terminal command with
JDK 26's `java` executable instead. Check the selected runtime with `java -version`.

For Macs using Homebrew Java, the command is:

```sh
"$(brew --prefix openjdk)/bin/java" -jar ~/Downloads/drjava.jar
```

Tested on Apple Silicon macOS 26.2 with OpenJDK 26.0.2.1. Other platforms are
not yet verified. See [release notes](https://github.com/NathanVosburg/drjava/releases/latest)
for the downloadable version. Releases belong to this fork and are separate
from upstream DrJava's SourceForge releases.

# Upstream background

This code base is merely a continuation of the DrJava code base formerly hosted
at Sourceforge.  We decided to shift from Subversion to Git for two reasons:

1. Git is more flexible (and complex) than Subversion.
1. Second, and more importantly, Git appears to be the preferred repository and
version control system among software developers and our upper level students
are more familiar with Git than Subversion. 

# Future Extensions

This repository also includes the unreleased pedagogic IDE
for Scala called DrScala, which we will release as soon as we can persuade the Scala developers
to fix a serious bug in the :require REPL command which we use to dynamically
add new paths to the REPL class path.  

# Distribution of binaries

We will continue to distribute new releases of DrJava via Sourceforge to
preserve our distribution interface.  The move from Subversion on Sourceforge
to Git on Github only concerns DrJava developers and others interested in the
DrJava code base.

# Local development

This checkout targets Java 10 bytecode and bundles a Mac integration library
that requires Java 9 or later. The old Java 8 instructions in `drjava/README`
do not apply to this checkout. Apache Ant 1.10+ and a full JDK are required.

On Apple Silicon macOS with Homebrew, from the repository root:

```sh
brew install ant
export JAVA_HOME="$(brew --prefix openjdk)"
export PATH="$JAVA_HOME/bin:$PATH"
ant -f drjava/build.xml jar
java -jar drjava/drjava.jar
```

The generated application is **`drjava/drjava.jar`**. The `drjava.jar` at the
repository root is a copy of this fork's published release; it is not automatically
updated by a local build.
Re-run the Ant command after editing source, then restart the application.
The build uses the dependency JARs already included in the repository.

The toolbar's **Go** button compiles all open documents (or the active
project), then runs the current document or the project's configured main
class. Compilation errors and cancelled saves prevent execution. Separate
Compile and Run commands remain available in the menus.

Focused Go integration tests (opens Swing windows and launches local Java
interpreter processes):

```sh
java -cp "$PWD/drjava/classes/test:$PWD/drjava/classes/base:$PWD/drjava/classes/lib:$PWD/drjava/lib/buildlib/junit.jar" \
  junit.textui.TestRunner edu.rice.cs.drjava.ui.GoButtonTest
```
