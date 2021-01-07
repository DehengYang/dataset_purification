# 1. DEPTest

<!-- title: DEPTest --> 

****DEPTest**** is an automated dataset purification tool that leverages software testing techniques (i.e., coverage analysis and delta debugging).
It can automatically identify and filter out code changes irrelevant to but tangled with the real bug fix in the human-written patch of existing datasets or bug fixing commits.

- [1. DEPTest](#1-deptest)
  - [1.1. Introduction](#11-introduction)
  - [1.2. Environment setup](#12-environment-setup)
    - [1.2.1. Requirements](#121-requirements)
    - [1.2.2. Execution](#122-execution)
  - [1.3 Manual verification](#13-manual-verification)
  - [1.4. Scenarios to use DEPTest](#14-scenarios-to-use-deptest)
    - [1.4.1. For dataset constructors](#141-for-dataset-constructors)
    - [1.4.2. For end users who use human-written patches as the ground truth](#142-for-end-users-who-use-human-written-patches-as-the-ground-truth)
    - [1.4.3. For researchers who want to explore the real challenges of APR](#143-for-researchers-who-want-to-explore-the-real-challenges-of-apr)
  - [1.5. APR techniques that are evaluated on Defects4J](#15-apr-techniques-that-are-evaluated-on-defects4j)
  - [1.6 Minor fix](#16-minor-fix)

## 1.1. Introduction
An unsolved but important problem is the purification (i.e., filtering out the code changes irrelevant to the real bug fix) of human-written patches when constructing a dataset of real-world bugs.

Such datasets with inaccurate human-written patches will introduce noise and bias to the relevant research that uses human-written patches as the ground truth (i.e., patch correctness assessment in automated program repair (APR)). Therefore, [previous studies](https://dl.acm.org/doi/abs/10.1145/3379597.3387504) strongly suggest that such purification is mandatory. 

However, only few datasets (e.g., [Defects4J](https://link.springer.com/chapter/10.1007/978-3-030-59762-7_19)) have been purified, due to the high time cost of manual purification. Also, detailed project knowledge as
well as the understanding of the intention of all code changes
in the bug fixing commit are required, which makes the manual purification a challenging task. In this situation, an automated purification technique is desired to help alleviate the burden of manual purification and improve the accuracy of human-written patches.

With such motivation, we propose a automated technique named DEPTest for **purification of datasets that can be used by test-suite based APR**. That is, our DEPTest at present serves the scenario where the buggy project, the human-written patch, and the bug triggering tests are available. We run DEPTest on Defects4J and obtain some interesting findings. All the relevant artifacts are available in this repository.


## 1.2. Environment setup
### 1.2.1. Requirements
+ JDK 1.8
+ Maven
+ [Defects4J v1.4.0](https://github.com/rjust/defects4j/tree/v1.4.0)

### 1.2.2. Execution

```
# to get purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar
mvn clean package -DskipTests

# run an example
cd example
unzip Defects4J_Time_2.zip
cd ../src/test/resources/
./defects4j_time_2.sh

# check output (purified patch)
cd -
cat output/purify/purifiedPatch.diff
```

In this way, you can obtain the purified patch of Time 2 in Defects4J.

The DEPTest can be executed on [RepairThemAll](https://github.com/program-repair/RepairThemAll). That is, no much effort will be made if you want to run DEPTest on other two real-world datasets included in RepairThemAll (i.e, [Bugs.jar](https://github.com/bugs-dot-jar/bugs-dot-jar) and [Bears](https://github.com/bears-bugs/bears-benchmark)).

Please refer to ["Deploy DEPTest into RepairThemAll"](./Deploy) for more detail.

## 1.3 Manual verification
As indicated in our paper, we randomly selected 81 (i.e., a half as a statistical size) out of the 162 purified cases without any bias for verification. To obtain reliable results, three authors in our group independently conducted the verification and discussed on inconsistent verification results until an agreement is reached. We find that among the 81 verified cases DEPTEST produced no such patches where part of real bug fixes are eliminated. An explanation is that DEPTEST focuses on filtering out
the not covered (i.e., unrelated) statements during coverage analysis and the loosely coupled (i.e., loosely related)
code changes tangled in real bug fixes during delta debugging. As a result, the complete logic and components of the real bug
fix are maintained during the purification.

We now have made [the manual verification results available](./manual_verification) for researchers or potential users to check the validity of our verification results. 


## 1.4. Scenarios to use DEPTest

### 1.4.1. For dataset constructors
If correctly configured, DEPTest can provide detailed information (e.g., which code change lines are not exposed by bug triggering tests, and which lines can be further purified) that dataset constructors may omit and recommend the purified human-written patch for assisting constructors to make a final decision for the purification.

### 1.4.2. For end users who use human-written patches as the ground truth

For end users who need the accurate ground truth patches, DEPTest results can provide helpful information. For example, DEPTest has already been executed on [Defects4J](https://github.com/rjust/defects4j) and list all the purified patches, which can be used as a reference.

For example, when end users assess the correctness of a patch generated by APR on Math 93, this patch only corresponds to the first two chunks of the original human-written patch in Defects4J. If the end users check our purified patch on Math 93 which purifies the third chunk, they then just need to focus on comparing the first two chunks with the APR patch during assessment. Otherwise, they have to manually verify if the third chunk is relevant to the real bug fix.

### 1.4.3. For researchers who want to explore the real challenges of APR

Once the irrelevant code changes are tangled in the bug fix, the real bug fixes (ground truth) are hidden. This inhibits the exploration of the real challenges of automated program repair. In some cases, a multi-chunk and complex human-written patch might just a single-chunk bug fix tangled with some code refactorings or irrelevant bug fixes (e.g., Time 2 in Defects4J). Therefore, researcher could use DEPTest to explore the bug fixing commits in the wild (e.g., in GitHub repositories), interesting findings or insights might appear during the process.

## 1.5. APR techniques that are evaluated on Defects4J

Here we also share our literature review results (i.e., the APR techniques evaluated on Defects4J) for researchers or end users who are interested in using it. 

**We will keep updating it to facilitate relevant research, by including more fruitful information and newest APR publications and tools.** It would be also appreciated if you can cite this website when you use the information provided in this repo.

| \# | Tool                             | Year | Link                                                 |
|----|----------------------------------|------|------------------------------------------------------|
| 1  | jKali                            | 2016 | https://github/com/Spirals-Team/defects4j-repair/ |
| 2  | jMutRepair                       | 2016 | https://github/com/SpoonLabs/astor/experiments/    |
| 3  | jGenProg                         | 2016 | https://github/com/Spirals/Team/defects4j/repair/ |
| 4  | HDRepair                         | 2016 | https://github/com/xuanbachle/bugfixes              |
| 5  | DynaMoth                         | 2016 | https://github/com/SpoonLabs/nopol/                |
| 6  | deductive\-reasoning\-based APR | 2016 | -                                                   |
| 7  | Nopol                            | 2017 | https://github/com/SpoonLabs/nopol/                 |
| 8  | JAID                             | 2017 | https://bitbucket/org/maxpei/jaid/wiki/Home         |
| 9  | ACS                              | 2017 | https://github/com/Adobee/ACS                       |
| 10 | ssFix                            | 2017 | https://github/com/qixin5/ssFix                     |
| 11 | ELIXIR                           | 2017 | -                                                   |
| 12 | Cardumen                         | 2017 | https://github/com/SpoonLabs/astor-experiments/    |
| 13 | DiffTGen                         | 2017 | https://github/com/qixin5/DiffTGen                  |
| 14 | ARJA                             | 2018 | https://github/com/yyxhdy/arja                      |
| 15 | LSRepair                         | 2018 | https://github/com/AutoProRepair/LSRepair           |
| 16 | SketchFix                        | 2018 | https://github/com/SketchFix/SketchFix              |
| 17 | CapGen                           | 2018 | https://github/com/justinwm/CapGen                  |
| 18 | SOFix                            | 2018 | -                                                   |
| 19 | FixMiner                         | 2018 | https://github/com/SerVal-DTF/fixminer-core       |
| 20 | SimFix                           | 2018 | https://github/com/xgdsmileboy/SimFix               |
| 21 | 3sfix                            | 2018 | https://github/com/kth-tcs/3sFix-experiments     |
| 22 | GenProg-A                       | 2018 | https://github/com/yyxhdy/arja                      |
| 23 | RSRepair-A                      | 2018 | https://github/com/yyxhdy/arja                      |
| 24 | Kali-A                          | 2018 | https://github/com/yyxhdy/arja                      |
| 25 | findbugs-violation              | 2018 | -                                                   |
| 26 | VFix                             | 2019 | -                                                   |
| 27 | SEQUENCER                        | 2019 | -                                                   |
| 28 | GenPat                           | 2019 | https://github/com/xgdsmileboy/GenPat               |
| 29 | kPAR                             | 2019 | https://github/com/SerVal-DTF/FL-VS-APR          |
| 30 | ENCORE                           | 2019 | -                                                   |
| 31 | iFixR                            | 2019 | -                                                   |
| 32 | ConFix                           | 2019 | https://github/com/thwak/ConFix                     |
| 33 | AdqFix                           | 2019 | -                                                   |
| 34 | CODIT                            | 2019 | -                                                   |
| 35 | AVATAR                           | 2019 | https://github/com/SerVal-Repair/AVATAR            |
| 36 | SharpFix                         | 2019 | https://github/com/sharpFix18/sharpFix              |
| 37 | Tbar                             | 2019 | https://github/com/SerVal-DTF/TBar                 |
| 38 | PraPR                            | 2019 | https://github/com/prapr/prapr                      |
| 39 | HERCULES                         | 2019 | -                                                   |
| 40 | DeepRepair                       | 2019 | https://github/com/SpoonLabs/astor-experiments/    |
| 41 | DLFix                            | 2020 | -                                                   |
| 42 | ARJA-p                          | 2020 | -                                                   |
| 43 | ARJA-e                          | 2020 | https://github/com/yyxhdy/arja/tree/arja-e         |
| 44 | RESTORE                          | 2020 | -                                                   |
| 45 | CoCoNuT                          | 2020 | https://github/com/lin-tan/CoCoNut-Artifact       |
| 46 | JAID-revise                     | 2020 | -                                                   |
| 47 | MuJava                           | 2020 | -                                                   |


## 1.6 Minor fix

There is a typo in Figure 1 of our paper. As shown in the following figure, the number of correctly/plausible repaired bugs for Hercules is 49/72, rather than 46/63.

![Figure_1](./doc/Fig_1.png)


----

We will consistently develop and maintain this project to make it a better tool for the community. Also, all contributions are welcome.
