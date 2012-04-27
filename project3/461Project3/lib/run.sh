#!/bin/bash

JARDIR="."
JARS="${JARDIR}/OSConsoleApps.jar:${JARDIR}/OS.jar:${JARDIR}/timingframing.jar:${JARDIR}/org.json.jar:${JARDIR}/util.jar:${JARDIR}/commons-cli-1.2.jar:${JARDIR}/sqlite4java-282/sqlite4java.jar"

COMMAND="$1"
shift
if [ ${COMMAND} = "client" ]; then
   java -cp ${JARS} edu.uw.cs.cse461.sp12.timingframing.ConsoleClient $*
elif [ ${COMMAND} = "server" ]; then
   java -cp ${JARS} edu.uw.cs.cse461.sp12.timingframing.Server $*
elif [ ${COMMAND} = "os" ]; then
    CONFIG=../OS/$1.config.ini
    shift 1
    java -cp ${JARS} edu.uw.cs.cse461.sp12.OS.OS -f ${CONFIG} $* 2>/dev/null
elif [ ${COMMAND} = "ddns" ]; then
    java -cp ${JARS} edu.uw.cs.cse461.sp12.OS.DDNSService $* 2>/dev/null
elif [ ${COMMAND} = "nm" ]; then
    java -cp ${JARS} edu.uw.cs.cse461.sp12.OS.NameManager $* 2>/dev/null
elif [ ${COMMAND} = "consoleddns" ]; then
    CONFIG=../OS/$1.config.ini
    shift 1
    java -cp ${JARS} edu.uw.cs.cse461.sp12.OSConsoleApps.DDNS.AppManager -f ${CONFIG} $*  2>/dev/null
elif [ ${COMMAND} = "console" ]; then
    CONFIG=../OS/$1.config.ini
    shift 1
    java -cp ${JARS} edu.uw.cs.cse461.sp12.OSConsoleApps.AppManager -f ${CONFIG} $*  2>/dev/null
fi
   
