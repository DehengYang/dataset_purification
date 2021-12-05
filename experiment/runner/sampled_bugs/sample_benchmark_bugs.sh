benchmarks=(Defects4J QuixBugs Bugs.jar Bears)

<<C1
### phase 1: get all bugs
for((i=0;i<${#benchmarks[*]};i++))
do
	/usr/bin/python2.7 /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/print_bugs_available.py -b ${benchmarks[i]} > ${benchmarks[i]}.txt
	#shuf -n N input > output
done
C1

### phase 2: sample bugs
# refer to: https://www.surveysystem.com/sscalc.htm
# Confidence Level: 95%
# Confidence Interval: 4  (4% at present)
# Population: 251
# Calculate
# Sample size needed: 177 

# bears 251 -> 177
# bugs.jar 1158 -> 396
# d4j 395 -> 238
# quixbugs 40 -> 38

sampleSize=(238 38 396 177)

for((i=0;i<${#benchmarks[*]};i++))
do
	# refer to: https://stackoverflow.com/questions/9245638/select-random-lines-from-a-file
	shuf -n ${sampleSize[i]} ${benchmarks[i]}.txt > ${benchmarks[i]}_sample.txt
done

