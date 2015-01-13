#!/usr/bin/env bash

set -eu


# Parse input
while getopts ":q:a:dr" opt; do
  case $opt in
    q)
      INPUT_ECLIPSE_QUALIFIER=$OPTARG
      ;;
    a)
      INPUT_MAVEN_EXTRA_ARGS=$OPTARG
      ;;
    d)
      INPUT_MAVEN_DEPLOY="-d"
      ;;
    r)
      INPUT_MAVEN_RELEASE=",release"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 2
      ;;
  esac
done


# Set build vars
MAVEN_EXTRA_ARGS=${INPUT_MAVEN_EXTRA_ARGS:-""}
MAVEN_DEPLOY=${INPUT_MAVEN_DEPLOY:-""}
MAVEN_RELEASE=${INPUT_MAVEN_RELEASE:-""}
MAVEN_ARGS="--no-snapshot-updates --activate-profiles=!add-metaborg-repositories$MAVEN_RELEASE $MAVEN_EXTRA_ARGS"

ECLIPSE_QUALIFIER=${INPUT_ECLIPSE_QUALIFIER:-$(./latest-timestamp.sh)}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


# Clean up local repository.
rm -rf ~/.m2/repository/org/metaborg
rm -rf ~/.m2/repository/.cache/tycho/org.spoofax*
rm -rf ~/.m2/repository/.cache/tycho/org.strategoxt*
rm -rf ~/.m2/repository/.cache/tycho/org.metaborg*


# Run the Maven builds.
echo "Using Eclipse qualifier '$ECLIPSE_QUALIFIER'."

./strategoxt/strategoxt/build.sh
./spoofax-deploy/org.metaborg.maven.build.java/build.sh -q $ECLIPSE_QUALIFIER -a "$MAVEN_ARGS" $MAVEN_DEPLOY
./spoofax-deploy/org.metaborg.maven.build.spoofax.eclipse/build.sh -q $ECLIPSE_QUALIFIER -a "$MAVEN_ARGS" $MAVEN_DEPLOY
./spoofax-deploy/org.metaborg.maven.build.parentpoms/build.sh -a "$MAVEN_ARGS" $MAVEN_DEPLOY
./spoofax-deploy/org.metaborg.maven.build.spoofax.libs/build.sh -a "$MAVEN_ARGS"
./spoofax-deploy/org.metaborg.maven.build.spoofax.testrunner/build.sh -a "$MAVEN_ARGS"


# Echo locations of build products.
ECLIPSE_UPDATE_SITE="$DIR/spoofax-deploy/org.strategoxt.imp.updatesite/target/site"
SUNSHINE_JAR_ARRAY=("$DIR/spoofax-sunshine/org.spoofax.sunshine/target/org.metaborg.sunshine-"*"-shaded.jar")
BENCHMARK_JAR_ARRAY=("$DIR/spoofax-benchmark/org.metaborg.spoofax.benchmark.cmd/target/org.metaborg.spoofax.benchmark.cmd-"*".jar")
TESTRUNNER_JAR_ARRAY=("$DIR/spt/org.metaborg.spoofax.testrunner.cmd/target/org.metaborg.spoofax.testrunner.cmd-"*".jar")
LIBRARIES_JAR_ARRAY=("$DIR/spoofax-deploy/org.metaborg.maven.build.spoofax.libs/target/org.metaborg.maven.build.spoofax.libs-"*".jar")

echo "Build products"
echo "Eclipse update site: $ECLIPSE_UPDATE_SITE"
echo "Sunshine JAR: ${SUNSHINE_JAR_ARRAY[0]}"
echo "Benchmark JAR: ${BENCHMARK_JAR_ARRAY[0]}"
echo "Test runner JAR: ${TESTRUNNER_JAR_ARRAY[0]}"
echo "Libraries JAR: ${LIBRARIES_JAR_ARRAY[0]}"
