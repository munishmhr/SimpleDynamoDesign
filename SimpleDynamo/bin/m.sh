for i in {1..30}
do
  name='executionLog_'
  ext='.txt'
  fullName=$name$i$ext
  echo $fullName
  em1='5554_'$i$ext
  em2='5556_'$i$ext
  em3='5558_'$i$ext
  em4='5560_'$i$ext
  em5='5562_'$i$ext
  adb -s emulator-5554 shell logcat >> $em1 &
  adb -s emulator-5556 shell logcat >> $em2 &
  adb -s emulator-5558 shell logcat >> $em3 &
  adb -s emulator-5560 shell logcat >> $em4 &
  adb -s emulator-5562 shell logcat >> $em5 &

    ./simpledynamo-grading.linux -n 5 /home/munish/eclipseworkspaceAndroid/SimpleDynamo/bin/SimpleDynamo.apk  >> $fullName
 
  for KILLPID in `ps | grep 'adb' | cut -d" " -f 1`; do
    kill -9 $KILLPID;
  done
done
