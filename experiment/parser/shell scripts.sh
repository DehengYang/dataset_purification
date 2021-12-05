apr@apr:~/apr_tools/datset_purification_2020/purification/purify/experiment/parser/results$ grep -rn "reduced lines" * -A 2 | grep "by all"| grep -v "by all: 0"  | wc -l 
117
apr@apr:~/apr_tools/datset_purification_2020/purification/purify/experiment/parser/results$ grep -rn "reduced lines" * -A 2 | grep "by all"| wc -l
254
apr@apr:~/apr_tools/datset_purification_2020/purification/purify/experiment/parser/results$ 
93
apr@apr:~/apr_tools/datset_purification_2020/purification/purify/experiment/parser/results$ grep -rn "sibling" * |wc -l
254


grep -rn "reduced lines:" * -A 2 | grep "by all" | wc -l
372
grep -rn "reduced lines:" * -A 2 | grep "by all" | grep -v "by all: 0" |wc -l
161

grep -rn "reduced lines:" * -A 2 | grep "by all" | grep -v "by all: 0" | cut -f 2 -d ":"  > 1

grep -rn "sibling repair actions" * | grep -v "\[\]" |wc -l
grep -rn "sibling repair actions" * |wc -l