#!/bin/bash

function help_text()
{
    echo "
Usage:
   bootstrap                 (default values)
or bootstrap OPTION(S) TARGET
or bootstrap TARGET OPTION(S)

Available OPTIONs:
  -h,  --help              show this text
  -sp, --skip-pull         skip git pull --rebase
  -sc, --skip-clean        keep untracked files and directories

Available TARGETs:
  SIMS, SKUMMET and DEFAULT

Examples:
  bootstrap
  bootstrap SIMS
  bootstrap -sp DEFAULT
  bootstrap --skip-clean SKUMMET
  bootstrap -sp -sc SIMS
"
}

SKIP_PULL=false
SKIP_CLEAN=false
TARGET=DEFAULT

if [ -z "$1" ]
then
    echo "Using default values"
fi

while [ "$1" != "" ]; do
    PARAM=`echo $1 | awk -F= '{print $1}'`
    case $PARAM in
        -h | --help)
            help_text
            exit
            ;;
        -sp | --skip-pull)
            SKIP_PULL=true
            ;;
        -sc | --skip-clean)
            SKIP_CLEAN=true
            ;;
        DEFAULT | SKUMMET | SIMS)
            TARGET=$PARAM
            ;;
        *)
            echo "ERROR: unknown parameter \"$PARAM\""
            help_text
            exit 1
            ;;
    esac
    shift
done

echo "Checking for Java installation";
[ -z "$JAVA_HOME" ] && echo "JAVA_HOME cannot be empty" && exit 1;
echo "[OK] " $JAVA_HOME;

if [ "$SKIP_PULL" = true ];
then
    echo "Skipping git pull"
else
    echo "rebasing"
    git pull --rebase || exit -1;
fi

if [ "$SKIP_CLEAN" = true ];
then
    echo "Skipping git clean"
else
    echo "cleanning"
    git clean -d -x --force --quiet;
fi

echo "Force-removing Sneer jars"
rm -rf ~/.m2/repository/me/sneer/;


echo "Checking for correct Leiningen installation."
if command -v lein; then
  CUR=$(lein version | awk '{print $2}' 2>&1)
  echo "
Using Leiningen version $CUR.
";
else
  echo "
You don't seem to have Leiningen on your PATH.
Please follow the instructions at http://leiningen.org/
";
  exit 1;
fi


echo "Gradlew clean ckeck install"
./gradlew clean check install;

case $TARGET in
    SIMS)
        echo "Cleaning core"
        rm -rf ~/.m2/repository/me/sneer/core;
        ;;
    SKUMMET)
        echo "Publishing Skummet jar"
        ./gradlew :core:publish || exit -1;
        ;;
    DEFAULT)
        echo "Removing Skummet artifacts"
        rm -rf skummet-artifacts/;
        ;;
    *)
        echo "ERROR: unknown target \"$PARAM\""
        exit 1
        ;;
esac

echo "Checking for Android SDK installation";
[ -z "$ANDROID_HOME" ] && echo "ANDROID_HOME cannot be empty" && exit 1;
echo "[OK] " $ANDROID_HOME;

echo "Cleaning Android build"
cd android && ./gradlew clean && cd .. || exit -1;

if command -v adb; then
    adb devices | while read line
    do
        if [ ! "$line" = "" ] && [ `echo $line | awk '{print $2}'` = "device" ]
        then
            echo "Installing on available Android devices/emulators"
            cd android && ./gradlew installDebug && cd .. || exit -1;
            break;
        fi
    done
fi

echo "Reset environment successful"
