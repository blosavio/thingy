#!/bin/bash

# Run from project root directory.
# First argument is the classpath to the Clojure jar.
# Be sure to include the `.jar` filename extension.
# E.g. <path>/.m2/repository/org/clojure/clojure/1.12.0/clojure-1.12.0.jar

serialver -classpath "$1":target/classes com.sagevisuals.AltFnInvocablePersistentVector

exit 0