RET=0
for p in ${EXPECTED_PATTERN[*]}
do
    cat /tmp/systemProperty.out | grep "$p"
    if [ 0 != $? ]; then RET=1; fi
done

\rm -f /tmp/systemProperty*

ldd ${NEW_JAVA_HOME}/lib/libzip.so | grep libz
if [ 0 != $? ]; then RET=1; fi
echo "================= Sanity test end ======================"

exit ${RET}
