# based on: /mnt/benchmarks/buggylocs/allBugs/runAllBugs.sh

benchmarks=(Defects4J)  # QuixBugs Bugs.jar Bears

# run bugs
maxLineNo=2

tool=Purify
log=log/run_${tool}.log

[ ! -d "log/" ] && echo "create log/" && mkdir log/

#exit

[ -f $log ] && echo "delete $log for init." && rm $log 

for((i=1;i<$maxLineNo;i++))
do
	#echo $i

	for((j=0;j<${#benchmarks[*]};j++))
	do
		# refer to: https://blog.csdn.net/wuzhiwuweisun/article/details/79136308
		bug=`sed -n "${i}p" BugsNotRun/${benchmarks[j]}.txt`
		echo "$bug"
		#continue

		# refer to: https://blog.csdn.net/mct_blog/article/details/80596386
		if [ "$bug" = "" ]; then
			echo -e "[sampled_bugs/${benchmarks[j]}.txt] skip as $i bug is null.\n\n" 
			echo -e "[sampled_bugs/${benchmarks[j]}.txt] skip as $i bug is null.\n\n"  >> $log
		else
			#continue
			grepResult=`cat $log | grep "${benchmarks[j]} $bug $i"`

			# refer to: https://blog.csdn.net/qq_34924407/article/details/82922917
			# enable continue running.
			if [ "$grepResult" != "" ];then
				echo "${benchmarks[j]} $bug is already run, skip now."
				continue
			fi

			echo "now run ${benchmarks[j]} $bug"

			# run bug
			echo "${benchmarks[j]} $bug $i" >> $log
			echo "timeout 120m python2.7 /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/repair.py $tool -b ${benchmarks[j]} -i $bug" >> $log

			startTime=$(date +%s)
			echo "start time: `date '+%Y-%m-%d %H:%M:%S'`"  >> $log

			timeout 120m python2.7 /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/repair.py $tool -b ${benchmarks[j]} -i $bug 

			endTime=$(date +%s)
			echo "end time of $tool: `date '+%Y-%m-%d %H:%M:%S'`"  >> $log
			repairTime=$(($endTime-$startTime))
			echo -e "time cost: $repairTime s\n"  >> $log

	
#			mem=`free -m | grep Mem | awk '{print $3}'` 
#			echo "before killing java, the used memory is: $mem" >> $log
			# refer to: https://blog.csdn.net/pzasdq/article/details/52964454
			# https://dalewushuang.blog.csdn.net/article/details/105530551
			#ps -ef | grep "java" | grep -v "/home/apr/env/eclipse/eclipse" | grep $tool | grep -v "grep" | awk '{print $2}' | xargs -r kill -9
			#ps -ef | grep "java" | grep -vi "eclipse" | grep -v "grep" | awk '{print $2}' | xargs -r kill -9
#			mem=`free -m | grep Mem | awk '{print $3}'`
#			echo "after killing java, the used memory is: $mem" >> $log
			echo -e "\n\n" >> $log
		fi
	done
done



















