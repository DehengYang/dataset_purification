"""
date: 2020, Oct 22 11am

I change the purify.py into purify_ori.py, as I think original information it collects is too redundant.

I need a more concise data collector.

So I save the previous purify.py here.
Next is to revise purify.py.
"""

import os
import re
import yaml

from utils import *
from .info import Chunk, Action
"""
this is to parse the result data of purify tool.

For example: /mnt/purify/buggylocs/Defects4J/Defects4J_Chart_2/purify/log.txt
"""

def parse(bug_result_dir, bug_id, parse_result_dir): # '/mnt/purify/buggylocs/Defects4J/Defects4J_Chart_2/purify'
    # read data
    log_path = os.path.join(bug_result_dir, "log.txt")
    log_lines = read_file(log_path)
    log_str = read_file_to_str(log_path)

    # init data
    src_folder_dict = {}
    time_cost_dict = {}
    failing_test_dict = {}
    purify_dict = {}
    covered_line_dict = {}
    not_covered_line_list = []
    mutant_result_dict = {}
    patch_dict = {}
    chunk_dict = {}
    repair_actions_ori_dict = {}
    repair_actions_purify_dict = {}

    # special cases.
    # 1) the chunk line is an else or else if: 
    m = myFindAll(log_str, re.compile("the chunk line is an else or else if: (.*)"))
    if len(m) != 0:
        raise Exception("len(m) != 0")
    # 2) return new Pair<String, Boolean>("inBlockStmt", true);
    # FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());

    # 3) time 5
    # FileUtil.writeToFileWithFormat("very special cases! There are %s extra failing cases in testing all. The coverage need to be re-run!", executor.getFailingCasesInTestAll().size());

    # 4) time 6
    # if (allAlphabets.contains("elseif")){
	# 		FileUtil.writeToFile("allAlphabets.contains(\"elseif\")");
	# 	}

    # 5) time 10
    # if(lineRange == null){ // bug fix: exposed by time 10.  +    private static final long START_1972 = 2L * 365L * 86400L * 1000L;
	# 		showCurNode(filePath, lineNo, stmtInfo); // is not in method
	# 		FileUtil.writeToFileWithFormat("chunk line: %s is not in any method", pair);
	# 	}

    """ lang 8
     catch(IncompatibleClassChangeError e){ // bug fix: exposed by lang 8  -> relativePath: org/apache/commons/lang3/time/FastDateParser$TextStrategy.class, className: org.apache.commons.lang3.time.FastDateParser$TextStrategy
				FileUtil.writeToFileWithFormat("IncompatibleClassChangeError occurs! error: %s, relativePath: %s, className: %s", e.toString(), relativePath, className);
				e.printStackTrace();
			}
    """

    """
    if (isInMethod == null){ // has no method
			boolPair = new Pair<String, Boolean>("notInAnyMethod", true);
			FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
			return boolPair;
		}else{
			if (isInMethod.getRight()){ // is the first line of the method
				boolPair = new Pair<String, Boolean>("inMethodButNotInBlock", true);
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}//else, go to next code lines
		}
        time 11 and lang 23
    """

    """
    chart 21
    if (bs.getChildNodes().isEmpty()){
			FileUtil.writeToFile("bs.getChildNodes().isEmpty()");
			return true; // should be included
		}

        if(stmtInfo.getLeft().equals("BlockStmt")){
			FileUtil.writeToFileWithFormat("check if curLine: %s is before this block stmt.", pair);
			boolean isBeforeBlockStmt = isBeforeBlockStmt((BlockStmt) stmtInfo.getRight(), pair);
			if (isBeforeBlockStmt){
				boolPair = new Pair<String, Boolean>("isBeforeBlockStmt", true);
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}
		}
    """

    """
    if (! runOriginal){ // this means that the coverage analysis leads to the failure.
                FileUtil.writeToFile("Coverage analysis leads to the failure of patch fail before mutation!");
                runOriginal = DeltaDebugging.assertBeforeMut(simplifiedLinesBkForDDMin, chunksBkForDDMin);
                if (!runOriginal){
                    FileUtil.raiseException("[ddmin] original patch version does not pass!");
                }else{
                    FileUtil.writeToFile("Use backup lines and chunks for ddmin now!");
                    useBk = true;
                }
            }

            if (!pass){
                FileUtil.writeToFile("[ddmin] original patch version does not pass!");
    //			FileUtil.raiseException("[ddmin] original patch version does not pass!");
            }
    """

    # 
    m = myFindAll(log_str, re.compile('all files cnt in /mnt/purify/repairDir/Purify_.*/(.*): (.*), all java files cnt: (.*)'))
    src_folder_dict['folder_name'] = m[0][0]
    src_folder_dict['files_cnt'] = int(m[0][1])
    src_folder_dict['java_files_cnt'] = int(m[0][2])
    m = myFindAll(log_str, re.compile('testClasses size: (.*), srcClassesFromSrcDir size: .*, srcClasses size: (.*)'))
    src_folder_dict['test_classes_cnt'] = int(m[0][0])
    src_folder_dict['src_classes_cnt'] = int(m[0][1])

    # [time cost] of init: 
    m = myFindAll(log_str, re.compile(r'\[time cost\] of init: (.*)'))
    time_cost_dict['init'] = float(m[0][0])
    m = myFindAll(log_str, re.compile(r'\[time cost\] of coverage analysis on buggy version: (.*)'))
    time_cost_dict['buggy_coverage'] = float(m[0][0])
    m = myFindAll(log_str, re.compile(r'\[time cost\] of coverage analysis on fixed version: (.*)'))
    time_cost_dict['fixed_coverage'] = float(m[0][0])
    time_cost_simplify = float(re.findall(re.compile(r'\[time cost\] of simplifyChunks: (.*)'), log_str)[0])
    time_cost_dict['purification_via_cov'] = time_cost_simplify
    get_human_patch_chunks_time_cost = float(re.findall(re.compile(r'\[time cost\] of getting human-patch chunks: (.*)'), log_str)[0])
    time_cost_dict['get_human_patch_chunks'] = get_human_patch_chunks_time_cost
    main_time_cost = float(re.findall(re.compile(r'\[time cost\] of main: (.*)'), log_str)[0])
    time_cost_dict['main'] = main_time_cost
    time_cost_dict['purification_via_cov_and_dd'] = float(re.findall(re.compile(r'\[time cost\] of purification via coverage and dd: (.*)'), log_str)[0])
    time_cost_dict['purification_via_dd'] = float(re.findall(re.compile(r'\[time cost\] of dd: (.*)'), log_str)[0])
    time_cost_dict['compare_repair_actions'] = float(re.findall(re.compile(r'\[time cost\] of comparing repair actions: (.*)'), log_str)[0])


    ################## coverage on buggy program ######################## 
    """
    testClasses size: 357, srcClassesFromSrcDir size: 654, srcClasses size: 681
    ...
    failingTestCase: org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests#test2947660
    ...
    failing test case: org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests#test2947660, number of covered stmts: 642
    ...
    [time cost] of coverage analysis on buggy version: 5.1220
    """
    # refer to: https://www.codespeedy.com/re-dotall-in-python/
    chunks = myFindAll(log_str, re.compile(r'failingTestCase: .*\[time cost\] of coverage analysis on buggy version: ', re.DOTALL))
    if len(chunks) > 1:
        raise Exception("len(chunk) > 1")
    m = myFindAll(chunks[0], re.compile(r'failingTestCase: (.*)'))
    ori_failing_test_cases = []
    ori_failing_tests = []
    for i in m:
        test_class = i.split("#")[0] 
        if test_class not in ori_failing_tests:
            ori_failing_tests.append(test_class)
        ori_failing_test_cases.append(i)
    print(ori_failing_test_cases)
    failing_test_dict['expected_failed_cases'] = ori_failing_test_cases
    failing_test_dict['expected_failed_cases_cnt'] = len(ori_failing_test_cases)
    failing_test_dict['expected_failed_tests'] = ori_failing_tests

    m = myFindAll(chunks[0], re.compile('failing test case: (.*), number of covered stmts: (.*)'))
    gz_failing_test_cases = {}
    # gz_failing_tests = []
    for i in m:
        # if i[0].split("#")[0] not in gz_failing_tests:
        #     gz_failing_tests.append(i[0].split("#")[0])
        gz_failing_test_cases[i[0]] = int(i[1])
    print(gz_failing_test_cases)
    failing_test_dict['gzoltar_failed_cases'] = gz_failing_test_cases

    if len(ori_failing_test_cases) != len(gz_failing_test_cases):
        raise Exception("len(ori_failing_test_cases) != len(gz_failing_test_cases)")

    total_test_cases = read_file(os.path.join(bug_result_dir, "all_tests.txt"))
    failing_test_dict['total_test_cases_cnt'] = len(total_test_cases)
    # spectrum_size = 

    ################## coverage on fixed program ######################## 

    chunk = re.findall(re.compile(r'failingTestCase: .*\[time cost\] of coverage analysis on fixed version: ', re.DOTALL), log_str)
    if len(chunk) > 1:
        raise Exception("len(chunk) > 1")
    m = re.findall(re.compile('failing test case: (.*), number of covered stmts: (.*)'), chunk[0])
    gz_failing_test_cases_fixed_ver = {}
    for i in m:
        gz_failing_test_cases_fixed_ver[i[0]] = int(i[1])
    print(gz_failing_test_cases_fixed_ver)
    if len(ori_failing_test_cases) != len(gz_failing_test_cases_fixed_ver):
        raise Exception("len(ori_failing_test_cases) != len(gz_failing_test_cases)")

    """
original_patch, patch_line_cov_info, time_cost_simplify, simplified_patch

    Chunk line: Pair<1797,-        if (dataset != null) {> is covered by 1 test cases:

    chunk line: %s is not covered by any failing test.

    [time cost] of simplifyChunks: 

    [simplifyChunks] Number of simplified lines: 0
    """
    simplified_cnt = int(myFindAll(log_str, re.compile(r'\[simplifyChunks\] Number of simplified lines: (.*)'))[0])
    # purify_dict['reduced_lines_via_coverage'] = simplified_cnt

    m = myFindAll(log_str, re.compile(r'oriChunks lines: (.*), simplifiedChunks lines: (.*), deltaChunks lines: (.*)'))[0]
    [purify_dict['ori_patch_lines'], purify_dict['cov_patch_lines'], purify_dict['delta_patch_lines']] = [int(m[0]), int(m[1]), int(m[2])]
    if simplified_cnt != purify_dict['ori_patch_lines'] - purify_dict['cov_patch_lines']:
        raise Exception("simplified_cnt != purify_dict['ori_patch_lines'] - purify_dict['cov_patch_lines']")

    m = myFindAll(log_str, re.compile('Chunk line: (.*) is covered by (.*) test cases:'))
    for i in m:
        covered_line_dict[i[0]] = int(i[1])
    m = myFindAll(log_str, re.compile('chunk line: (.*) is not covered by any failing test.'))
    for i in m:
        not_covered_line_list.append(i[0])

    # chunks = myFindAll(log_str, re.compile(r'output: --- .*?\[compile\] delete.*?before re-compiliation', re.DOTALL))
    chunks = myFindAll(log_str, re.compile(r'output: --- .*?output end\.', re.DOTALL))
    ori_patch = ""
    beforeMutPatch = ""
    mutant_list = []
    purify_patch = ""
    for chunk in chunks:
        if re.search(r"\+\+\+.*purify/patch/", chunk):  #patch diff 
            ori_patch = getPatchFromTextChunk(chunk)
            patch_dict['ori_patch'] = ori_patch
            continue
        if re.search(r"\+\+\+.*purify/assertBeforeMut/", chunk):  #assertBeforeMut diff -> expected to be same as patch diff
            before_mut_patch = getPatchFromTextChunk(chunk)
            patch_dict['before_mut_patch'] = before_mut_patch
            continue
        if re.search(r"\+\+\+.*purify/mutant/", chunk):  #assertBeforeMut diff -> expected to be same as patch diff
            mutant = getPatchFromTextChunk(chunk)
            mutant_list.append(mutant)
            continue
        if re.search(r"\+\+\+.*purify/purified/", chunk):  
            purify_patch = getPatchFromTextChunk(chunk)
            patch_dict['purify_patch'] = purify_patch
            continue
        # print(chunk)

    # ======= Mutant 2 failed: failCompile ======
    
    m = re.findall(re.compile('======= Mutant (.*) result: (.*) ======'), log_str)
    for i in m:
        [mutant_index, mutant_result] = (i[0], i[1])
        mutant_result_dict[mutant_index] = mutant_result

    # chunks
    m = myFindAll(log_str, re.compile(r"deltaChunks start: (.*)deltaChunks end\.", re.DOTALL))
    deltaChunks = getChunks(m[0])
    chunk_dict['deltaChunks'] = deltaChunks

    m = myFindAll(log_str, re.compile(r"deltaChunks after removePurifyComment start: (.*)deltaChunks after removePurifyComment end\.", re.DOTALL))
    deltaChunksRemoveComment = getChunks(m[0])
    chunk_dict['deltaChunksRemoveComment'] = deltaChunksRemoveComment

    m = myFindAll(log_str, re.compile(r"oriChunks start: (.*)oriChunks end\.", re.DOTALL))
    oriChunks = getChunks(m[0])
    chunk_dict['oriChunks'] = oriChunks

    m = myFindAll(log_str, re.compile(r"simplifiedChunks start: (.*)simplifiedChunks end\.", re.DOTALL))
    simplifiedChunks = getChunks(m[0])
    chunk_dict['simplifiedChunks'] = simplifiedChunks

    m = myFindAll(log_str, re.compile(r"======= \[getOriAndPurifyActions\] get patchActionsMap ======.*======= \[getOriAndPurifyActions\] get patchActionsMap end ======", re.DOTALL))
    get_repair_actions(m[0], repair_actions_ori_dict)
    m = myFindAll(log_str, re.compile(r"======= \[getOriAndPurifyActions\] get purifyActionsMap ======.*======= \[getOriAndPurifyActions\] get purifyActionsMap end ======", re.DOTALL))
    get_repair_actions(m[0], repair_actions_purify_dict)

    # write to yml
    yml_path = os.path.join(parse_result_dir, bug_id + ".yml")
    data_dict = {
        'time cost of purification' : time_cost_dict,
        'src folder' : src_folder_dict,
        'test (cases)' : failing_test_dict,
        'purify' : purify_dict,
        'covered lines by failing cases' : covered_line_dict,
        'uncovered lines by failing cases' : not_covered_line_list,
        'mutant test result': mutant_result_dict,
        'patch' : patch_dict,
        'chunks' : chunk_dict,
        'bug_id' : bug_id,
        'ori_repair_actions' : repair_actions_ori_dict,
        'purify_repair_actions' : repair_actions_purify_dict
    }
    with open(yml_path, 'w') as outfile:
        yaml.safe_dump(data_dict, outfile, default_flow_style=False)

    with open(yml_path) as f:
        # data = yaml.load(f, Loader=yaml.FullLoader)
        data = yaml.safe_load(f)
        patch = data['patch']['ori_patch']
        print(patch)

    print()

    # for line_cnt in range(len(log_lines)):
    #     cur_line = log_lines[line_cnt]

def getPatchFromTextChunk(chunk):
    patch = re.findall(re.compile(r'output: (.*)output end\.', re.DOTALL), chunk)[0].strip()
    patch = patch.replace("\n\n", "\n")
    print("---patch diff---\n" + patch)

    return patch

def myFindAll(log_str, reg):
    m = reg.findall(log_str)
    return m

def getChunks(text):
    lines = text.replace("\n\n", '\n').split("\n")
    chunks_list = []
    chunk_lines = ""
    chunk_flag = False
    for i in range(len(lines)):
        line = lines[i]
        if re.match("chunk class: ", line):
            chunk_flag = True
        if line == "" and chunk_flag:
            chunk = Chunk(chunk_lines = chunk_lines)
            chunk.update()
            chunks_list.append(chunk) # add a chunk
            chunk_lines = ""
            chunk_flag = False
        else:
            if chunk_flag:
                chunk_lines += line + "\n"
    return chunks_list

def get_repair_actions(text, repair_actions_dict):
    # old approach
    # Number of repair actions: 8
    # Biggest depth of repair actions: 6
    # chunk_text_list = re.findall(re.compile("Chunk: (.*)Chunk end.", re.DOTALL), text)
    # action_text_list = re.findall(re.compile("The chunk's hierarchicalActionSets: (.*)hierarchicalActionSets end.", re.DOTALL), text)
    # for i in range(len(action_text_list)):
    #     action = Action(action_text_list)
    #     action.update()
    #     repair_actions_dict['chunk {}'.format(i)] = action
    
    # new approach
    repair_actions_dict['repair_actions_cnt'] = int(re.findall(re.compile("Number of repair actions: (.*)"), text)[0])
    repair_actions_dict['biggest_depth'] = int(re.findall(re.compile("Biggest depth of repair actions: (.*)"), text)[0])
    file_class_list = re.findall(re.compile("number of fileClasses: (.*)"), text)
    # number of actions for fileClass (7): [org/jfree/data/general/DatasetUtilities]
    file_class_name_list = re.findall(re.compile(r"number of actions for fileClass \((.*)\): \[(.*)\]"), text)
    # its corresponding actionSets:
    actions_text_list = re.findall(re.compile(r"its corresponding actionSets:(.*?)its corresponding actionSets end\.", re.DOTALL), text)

    if len(file_class_name_list) != len(actions_text_list):
        raise Exception("len(file_class_name_list) != len(action_text_list)")

    for i in range(len(file_class_name_list)):
        [action_cnt, file_class_name] = file_class_name_list[i]
        action_cnt = int(action_cnt)
        actions_text = actions_text_list[i]

        action_text_list = re.findall(re.compile(r"==actionSets start==(.*?)==actionSets end==", re.DOTALL), actions_text)
        if len(action_text_list) != action_cnt:
            raise Exception("len(action_text_list) != action_cnt")
        actions = []
        for action_text in action_text_list:
            action_text_lines = action_text.split("\n")
            action = Action(action_text_lines)
            action.update()
            actions.append(action)
        repair_actions_dict[file_class_name] = actions