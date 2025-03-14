#!/bin/bash

# Compile AltFnIvocablePersistentVector to bytecode and class files. Run from project root directory. Pathnames are relative to the root directory, not relative to the location of the `@` files.

# `@javac_destination` is a file that contains a directory path for the generated .class files.
# `@javac_classpath` is a file that contains one or more directory paths that supply imports.

# The `@` files do not shell-expand (e.g., ~ for /home/<user>/), and pathnames that contain spaces must be double-quoted.
# Include the `.jar` filename extension on classpath elements when appropriate.

javac -d @src/utilities/javac_destination -cp @src/utilities/javac_classpath src/com/sagevisuals/thingy/AltFnInvocablePersistentVector.java

exit 0