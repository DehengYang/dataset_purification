import os
import re
import yaml

# refer to: https://blog.csdn.net/abc15766228491/article/details/78800816
# for ordered dict
import collections

from utils import read_file_to_str, read_file, write_to_yaml, write_line_to_file, read_file_no_strip, write_chunks_to_file
from .info import Chunk, Action
from .similarity import *

# get path of current py file and directory
cur_file  = os.path.abspath(__file__)
cur_dir = cur_file.rsplit("/", 1)[0] 

# patch_info_log_dir = os.path.join(cur_dir, "results", "patchInfo")
# patch_info_lines_log = os.path.join(patch_info_log_dir, "reduced_lines_patches.txt")
# patch_info_chunks_log = os.path.join(patch_info_log_dir, "reduced_chunks_patches.txt")

# write_line_to_file(patch_info_lines_log, "", append=False)

"""
this is to parse the result data of purify tool.

For example: /mnt/purify/buggylocs/Defects4J/Defects4J_Chart_2/purify/log.txt
"""

# get path of current py file and directory
cur_file  = os.path.abspath(__file__)
cur_dir = cur_file.rsplit("/", 1)[0] 

def parse(bug_result_dir, bug_id, parse_result_dir, patch_info_lines_log): # '/mnt/purify/buggylocs/Defects4J/Defects4J_Chart_2/purify'
    print(bug_id)

    # read data
    log_path = os.path.join(bug_result_dir, "log.txt")
    # log_lines = read_file(log_path)
    log_str = read_file_to_str(log_path)

    # init data
    src_folder_dict = {}
    time_cost_dict = {}
    failing_test_dict = {}
    # purify_dict = {}
    # covered_line_dict = {}
    # not_covered_line_list = []
    # mutant_result_dict = {}
    patch_dict = {}
    # chunk_dict = {}
    # repair_actions_ori_dict = {}
    # repair_actions_purify_dict = {}
    fail_coverage_info_dict = {"buggy" : {}, "fixed" : {}}
    reduced_lines_dict = {}
    reduced_chunks_dict = {}
    mutant_dict = {}
    repair_actions_dict = {} #collections.OrderedDict()
    sibling_pair_list = []

    # data required before dict
    repair_actions_ori_dict, repair_actions_purify_dict = get_repair_actions_info(repair_actions_dict, log_str)
    # find_copy_paste_bugs(repair_actions_ori_dict, bug_id, sibling_pair_list)
    find_copy_paste_bugs(repair_actions_ori_dict, bug_id, sibling_pair_list)
    write_chunk_and_actions(log_str, bug_id)

    # data dict
    data_dict = collections.OrderedDict({
        '1 bug_id' : bug_id,
        '2 src folder' : src_folder_dict,
        '3 test (cases)' : failing_test_dict,
        '4 time cost of purification' : time_cost_dict,
        '5 stmts covered by failing cases' : fail_coverage_info_dict,
        '6 reduced lines' : reduced_lines_dict,
        '7 reduced chunks' : reduced_chunks_dict,
        # 'purify' : purify_dict,
        # 'covered lines by failing cases' : covered_line_dict,
        # 'uncovered lines by failing cases' : not_covered_line_list,
        # 'mutant test result': mutant_result_dict,
        '8 mutants by delta debugging': mutant_dict,
        '9 sibling repair actions' : sibling_pair_list,
        '10 patch' : patch_dict,
        # 'chunks' : chunk_dict,
        '11 repair actions':repair_actions_dict,
        '12 ori_repair_actions' : repair_actions_ori_dict,
        '13 purify_repair_actions' : repair_actions_purify_dict
        }
    )

    # 
    get_src_folder_info(src_folder_dict, log_str)

    get_time_cost(time_cost_dict, log_str)

    get_failing_test_cov_info(bug_result_dir, failing_test_dict, fail_coverage_info_dict, "buggy", log_str)
    log_str_for_fixed_cov_chunk = log_str[log_str.find("[time cost] of coverage analysis on buggy version: ") : ]
    get_failing_test_cov_info(bug_result_dir, failing_test_dict, fail_coverage_info_dict, "fixed", log_str_for_fixed_cov_chunk)

    get_reduced_lines(reduced_lines_dict, log_str, bug_result_dir, bug_id, patch_info_lines_log)
    
    get_reduced_chunks(reduced_chunks_dict, log_str, bug_result_dir)

    get_patch_dict(patch_dict, log_str)

    get_mutant_result(mutant_dict, log_str)

    ################ not used
    # m = myFindAll(log_str, re.compile('Chunk line: (.*) is covered by (.*) test cases:'))
    # for i in m:
    #     covered_line_dict[i[0]] = int(i[1])
    # m = myFindAll(log_str, re.compile('chunk line: (.*) is not covered by any failing test.'))
    # for i in m:
    #     not_covered_line_list.append(i[0])

    

    yml_path = os.path.join(parse_result_dir, bug_id + ".yml")
    write_to_yaml(yml_path, data_dict)

    # yml_path = os.path.join(parse_result_dir, bug_id + ".yml")

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
            if len(chunk.lines) != 0:
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
    repair_actions_dict['classes_cnt'] = file_class_list[0] #len(file_class_list)
    repair_actions_dict['files_cnt'] = file_class_list[0] #len(file_class_list)
    # number of actions for fileClass (7): [org/jfree/data/general/DatasetUtilities]
    file_class_name_list = re.findall(re.compile(r"number of actions for fileClass \((.*)\): \[(.*)\]"), text)
    # its corresponding actionSets:
    actions_text_list = re.findall(re.compile(r"its corresponding actionSets:(.*?)its corresponding actionSets end\.", re.DOTALL), text)

    if len(file_class_name_list) != len(actions_text_list):
        raise Exception("len(file_class_name_list) != len(action_text_list)")

    m = re.findall(re.compile(r"Chunk: (.*?)replaceRange: Pair", re.DOTALL), text)
    methods = []
    if len(m) != 0:
        for method_text in m:
            m2 = re.findall(re.compile("method: (.*)"), method_text)
            if len(m2) != 0:
                methods.append(m2[0] )


    action_cnt = 0
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
            # action_cnt += 1
        repair_actions_dict[file_class_name] = actions
    return methods, action_cnt
    

def get_time_cost(time_cost_dict, log_str):
    # [time cost] of init: 
    m = myFindAll(log_str, re.compile(r'\[time cost\] of init: (.*)'))
    # time_cost_dict['init'] = float(m[0][0])
    m = myFindAll(log_str, re.compile(r'\[time cost\] of coverage analysis on buggy version: (.*)'))
    time_cost_dict['coverage on buggy version'] = float(m[0][0])
    m = myFindAll(log_str, re.compile(r'\[time cost\] of coverage analysis on fixed version: (.*)'))
    time_cost_dict['coverage on fixed version'] = float(m[0][0])
    time_cost_simplify = float(re.findall(re.compile(r'\[time cost\] of simplifyChunks: (.*)'), log_str)[0])
    time_cost_dict['purification via coverage'] = time_cost_simplify
    get_human_patch_chunks_time_cost = float(re.findall(re.compile(r'\[time cost\] of getting human-patch chunks: (.*)'), log_str)[0])
    # time_cost_dict['get_human_patch_chunks'] = get_human_patch_chunks_time_cost
    main_time_cost = float(re.findall(re.compile(r'\[time cost\] of main: (.*)'), log_str)[0])
    # time_cost_dict['project execution (Main method)'] = main_time_cost
    # time_cost_dict['purification_via_cov_and_dd'] = float(re.findall(re.compile(r'\[time cost\] of purification via coverage and dd: (.*)'), log_str)[0])
    time_cost_dict['purification via delta debugging'] = float(re.findall(re.compile(r'\[time cost\] of dd: (.*)'), log_str)[0])
    time_cost_dict['repair actions extraction'] = float(re.findall(re.compile(r'\[time cost\] of comparing repair actions: (.*)'), log_str)[0])

def get_src_folder_info(src_folder_dict, log_str):
    m = myFindAll(log_str, re.compile('all files cnt in /mnt/purify/repairDir/Purify_.*/(.*): (.*), all java files cnt: (.*)'))
    src_folder_dict['name of src folder'] = m[0][0]
    src_folder_dict['number of files'] = int(m[0][1])
    src_folder_dict['number of java files'] = int(m[0][2])
    m = myFindAll(log_str, re.compile('testClasses size: (.*), srcClassesFromSrcDir size: .*, srcClasses size: (.*)'))
    src_folder_dict['number of test classes'] = int(m[0][0])
    src_folder_dict['number of src classes'] = int(m[0][1])

def get_failing_test_cov_info(bug_result_dir, failing_test_dict, fail_coverage_info_dict, buggy_or_fixed_label, log_str):
    ################## coverage on buggy program ######################## 
    # refer to: https://www.codespeedy.com/re-dotall-in-python/
    chunks = myFindAll(log_str, re.compile(r'failingTestCase: .*\[time cost\] of coverage analysis on {} version: '.format(buggy_or_fixed_label), re.DOTALL))
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
    failing_test_dict['failing test cases'] = ori_failing_test_cases
    failing_test_dict['number of failing test cases'] = len(ori_failing_test_cases)
    failing_test_dict['failing tests'] = ori_failing_tests

    m = myFindAll(chunks[0], re.compile('failing test case: (.*), number of covered stmts: (.*)'))
    gz_failing_test_cases = {}
    for i in m:
        fail_coverage_info_dict[buggy_or_fixed_label][i[0]] = int(i[1])
        gz_failing_test_cases[i[0]] = int(i[1])

    # check
    # if len(ori_failing_test_cases) != len(gz_failing_test_cases):
    #     raise Exception("len(ori_failing_test_cases) != len(gz_failing_test_cases)")

    total_test_cases = read_file(os.path.join(bug_result_dir, "all_tests.txt"))
    failing_test_dict['number of all test cases'] = len(total_test_cases)

    #  ################## coverage on fixed program ######################## 
    # chunk = re.findall(re.compile(r'failingTestCase: .*\[time cost\] of coverage analysis on fixed version: ', re.DOTALL), log_str)
    # if len(chunk) > 1:
    #     raise Exception("len(chunk) > 1")
    # m = re.findall(re.compile('failing test case: (.*), number of covered stmts: (.*)'), chunk[0])
    # gz_failing_test_cases_fixed_ver = {}
    # for i in m:
    #     fail_coverage_info_dict["fixed"][i[0]] = int(i[1])
    #     gz_failing_test_cases_fixed_ver[i[0]] = int(i[1])
    # print(gz_failing_test_cases_fixed_ver)
    # if len(ori_failing_test_cases) != len(gz_failing_test_cases_fixed_ver):
    #     raise Exception("len(ori_failing_test_cases) != len(gz_failing_test_cases)")

def get_reduced_lines(reduced_lines_dict, log_str, bug_result_dir, bug_id, patch_info_lines_log):
    purify_dict = {}

    simplified_cnt = int(myFindAll(log_str, re.compile(r'\[simplifyChunks\] Number of simplified lines: (.*)'))[0])
    purify_dict['reduced_lines_via_coverage'] = simplified_cnt

    m = myFindAll(log_str, re.compile(r'oriChunks lines: (.*), simplifiedChunks lines: (.*), deltaChunks lines: (.*)'))[0]
    [purify_dict['ori_patch_lines'], purify_dict['cov_patch_lines'], purify_dict['delta_patch_lines']] = [int(m[0]), int(m[1]), int(m[2])]
    
    # if simplified_cnt != purify_dict['ori_patch_lines'] - purify_dict['cov_patch_lines']:
    #     raise Exception("simplified_cnt != purify_dict['ori_patch_lines'] - purify_dict['cov_patch_lines']")

    ori_lines, ori_chunks, oriDel, oriAdd = getLinesAndChunks(os.path.join(bug_result_dir, "oriPatch.diff"))
    cov_lines, cov_chunks, covDel, covAdd = getLinesAndChunks(os.path.join(bug_result_dir, "simplifyPatch.diff"))
    pur_lines, pur_chunks, purDel, purAdd = getLinesAndChunks(os.path.join(bug_result_dir, "purifyPatch.diff"))

    reduced_lines_dict['oriDel'] = oriDel
    reduced_lines_dict['oriAdd'] = oriAdd
    reduced_lines_dict['covDel'] = covDel
    reduced_lines_dict['covAdd'] = covAdd
    reduced_lines_dict['purDel'] = purDel
    reduced_lines_dict['purAdd'] = purAdd

    if len(ori_lines) !=  purify_dict['ori_patch_lines']  or len(cov_lines) !=  purify_dict['cov_patch_lines']  or len(pur_lines) !=  purify_dict['delta_patch_lines']:
        # raise Exception
        print("len(ori_lines) !=  purify_dict['ori_patch_lines']  or len(ori_lines) !=  purify_dict['ori_patch_lines']  or len(ori_lines) !=  purify_dict['ori_patch_lines']")

    if cov_simplify_fail(log_str):
        simplified_cnt = 0
        purify_dict['cov_patch_lines'] = purify_dict['ori_patch_lines']
    
    # reduced_lines_dict["by coverage"] = purify_dict['ori_patch_lines'] - purify_dict['cov_patch_lines']
    # reduced_lines_dict["by delta debugging"] = purify_dict['cov_patch_lines'] - purify_dict['delta_patch_lines']
    # reduced_lines_dict["by all"] = purify_dict['ori_patch_lines'] - purify_dict['delta_patch_lines']
    purify_dict['ori_patch_lines'], purify_dict['cov_patch_lines'], purify_dict['delta_patch_lines'] = len(ori_lines), \
        len(cov_lines), len(pur_lines)
    reduced_lines_dict['ori_patch_lines'], reduced_lines_dict['cov_patch_lines'], reduced_lines_dict['delta_patch_lines'] = len(ori_lines), \
        len(cov_lines), len(pur_lines)
    reduced_lines_dict["by coverage"] = purify_dict['ori_patch_lines'] - purify_dict['cov_patch_lines']
    reduced_lines_dict["by delta debugging"] = purify_dict['cov_patch_lines'] - purify_dict['delta_patch_lines']
    reduced_lines_dict["by all"] = purify_dict['ori_patch_lines'] - purify_dict['delta_patch_lines']

    if reduced_lines_dict["by all"] != 0:
        write_line_to_file(patch_info_lines_log, "{} reduced_lines:{} {} {}, chunks: {} {} {}".format(bug_id, \
            len(ori_lines), len(cov_lines), len(pur_lines), len(ori_chunks), len(cov_chunks), len(pur_chunks)))
        write_chunks_to_file(patch_info_lines_log, ori_chunks, "original")
        write_chunks_to_file(patch_info_lines_log, cov_chunks, "cov")
        write_chunks_to_file(patch_info_lines_log, pur_chunks, "purified")

def get_reduced_chunks(reduced_chunks_dict, log_str, bug_result_dir):
    chunk_dict = {}
    
    # chunks
    # m = myFindAll(log_str, re.compile(r"deltaChunks start: (.*)deltaChunks end\.", re.DOTALL))
    # deltaChunks = getChunks(m[0])
    # chunk_dict['deltaChunks'] = deltaChunks

    m = myFindAll(log_str, re.compile(r"deltaChunks after removePurifyComment start: (.*)deltaChunks after removePurifyComment end\.", re.DOTALL))
    deltaChunksRemoveComment = getChunks(m[0])
    chunk_dict['deltaChunksRemoveComment'] = deltaChunksRemoveComment

    m = myFindAll(log_str, re.compile(r"oriChunks start: (.*)oriChunks end\.", re.DOTALL))
    oriChunks = getChunks(m[0])
    chunk_dict['oriChunks'] = oriChunks

    m = myFindAll(log_str, re.compile(r"simplifiedChunks start: (.*)simplifiedChunks end\.", re.DOTALL))
    simplifiedChunks = getChunks(m[0])

    ori_lines, ori_chunks2, delCnt, addCnt = getLinesAndChunks(os.path.join(bug_result_dir, "oriPatch.diff"))
    cov_lines, cov_chunks2, delCnt, addCnt = getLinesAndChunks(os.path.join(bug_result_dir, "simplifyPatch.diff"))
    pur_lines, pur_chunks2, delCnt, addCnt = getLinesAndChunks(os.path.join(bug_result_dir, "purifyPatch.diff"))

    if len(ori_chunks2) !=  len(oriChunks)  or len(cov_chunks2) != len(simplifiedChunks)  or len(pur_chunks2) !=  len(deltaChunksRemoveComment):
        print("len(ori_chunks2) !=  len(oriChunks)  or len(cov_lines) != len(simplifiedChunks)  or len(pur_chunks) !=  len(deltaChunksRemoveComment):")
    
    reduced_chunks_dict['original'] = len(ori_chunks2)
    reduced_chunks_dict['cov'] = len(cov_chunks2)
    reduced_chunks_dict['purifiy'] = len(pur_chunks2)
    reduced_chunks_dict['by coverage'] = len(ori_chunks2) - len(cov_chunks2)
    reduced_chunks_dict['by delta debugging'] = len(cov_chunks2) - len(pur_chunks2)
    reduced_chunks_dict['by all'] = len(ori_chunks2) - len(pur_chunks2)
    # if cov_simplify_fail(log_str):
    #     reduced_chunks_dict['by coverage'] = 0
    #     reduced_chunks_dict['by delta debugging'] = len(oriChunks) - len(deltaChunksRemoveComment)
    #     reduced_chunks_dict['by all'] = len(oriChunks) - len(deltaChunksRemoveComment)
    # else:
    #     reduced_chunks_dict['by coverage'] = len(oriChunks) - len(simplifiedChunks)
    #     reduced_chunks_dict['by delta debugging'] = len(simplifiedChunks) - len(deltaChunksRemoveComment)
    #     reduced_chunks_dict['by all'] = len(oriChunks) - len(deltaChunksRemoveComment)

def get_mutant_result(mutant_dict, log_str):
    mutant_result_dict = {}

    mutant_dict['failCompile'] = 0
    mutant_dict['failFail'] = 0
    mutant_dict['failAll'] = 0
    mutant_dict['passed'] = 0

    # ======= Mutant 2 failed: failCompile ======
    m = re.findall(re.compile('======= Mutant (.*) result: (.*) ======'), log_str)
    for i in m:
        [mutant_index, mutant_result] = (i[0], i[1])
        mutant_result_dict[int(mutant_index)] = mutant_result

        if "failCompile" in mutant_result:
            mutant_dict['failCompile'] += 1    
        elif "failFail" in mutant_result:
            mutant_dict['failFail'] += 1
        elif "failAll" in mutant_result:
            mutant_dict['failAll'] += 1
        elif "passed" in mutant_result:
            mutant_dict['passed'] += 1
        else:
            raise Exception("unknown mutant result!")
    
    mutant_dict['total'] = mutant_dict['failCompile'] + mutant_dict['failFail'] + mutant_dict['failAll'] + mutant_dict['passed']


def get_patch_dict(patch_dict, log_str):
    # chunks = myFindAll(log_str, re.compile(r'output: --- .*?\[compile\] delete.*?before re-compiliation', re.DOTALL))
    chunks = myFindAll(log_str, re.compile(r'output: --- .*?output end\.', re.DOTALL))
    ori_patch = ""
    beforeMutPatch = ""
    mutant_list = []
    purify_patch = ""
    for chunk in chunks:
        if re.search(r"\+\+\+.*purify/patch/", chunk):  #patch diff 
            ori_patch = getPatchFromTextChunk(chunk)
            patch_dict['original'] = ori_patch
            continue
        if re.search(r"\+\+\+.*purify/assertBeforeMut/", chunk):  #assertBeforeMut diff -> expected to be same as patch diff
            before_mut_patch = getPatchFromTextChunk(chunk)
            # patch_dict['before_mut_patch'] = before_mut_patch
            continue
        if re.search(r"\+\+\+.*purify/mutant/", chunk):  #assertBeforeMut diff -> expected to be same as patch diff
            mutant = getPatchFromTextChunk(chunk)
            mutant_list.append(mutant)
            continue
        if re.search(r"\+\+\+.*purify/purified/", chunk):  
            purify_patch = getPatchFromTextChunk(chunk)
            patch_dict['purified'] = purify_patch
            continue
        # print(chunk)

def get_repair_actions_info(repair_actions_dict, log_str):
    repair_actions_ori_dict = {}
    repair_actions_purify_dict = {}

    m = myFindAll(log_str, re.compile(r"======= \[getOriAndPurifyActions\] get patchActionsMap ======.*======= \[getOriAndPurifyActions\] get patchActionsMap end ======", re.DOTALL))
    ori_methods, ori_action_cnt = get_repair_actions(m[0], repair_actions_ori_dict)
    # if len(ori_methods) == 0:
    #     raise Exception("len(ori_methods) == 0")
    m = myFindAll(log_str, re.compile(r"======= \[getOriAndPurifyActions\] get purifyActionsMap ======.*======= \[getOriAndPurifyActions\] get purifyActionsMap end ======", re.DOTALL))
    pur_methods, purify_action_cnt = get_repair_actions(m[0], repair_actions_purify_dict)
    # if len(pur_methods) != 0:
    #     raise Exception("len(pur_methods) != 0")
    
    m1 = myFindAll(log_str, re.compile(r"deltaChunks after removePurifyComment start: (.*)deltaChunks after removePurifyComment end\.", re.DOTALL))
    m = re.findall(re.compile(r"chunk class: (.*?)replaceRange: Pair", re.DOTALL), m1[0])
    pur_methods = []
    if len(m) != 0:
        for method_text in m:
            m2 = re.findall(re.compile("method: (.*)"), method_text)
            if len(m2) != 0:
                pur_methods.append(m2[0] )

    ori_methods = list(set(ori_methods))
    pur_methods = list(set(pur_methods))
    # repair_actions_dict['ori_methods'] = ori_methods
    # repair_actions_dict['pur_methods'] = pur_methods

    # repair_actions_dict["original cnt"] = ori_action_cnt
    # repair_actions_dict["purified cnt"] = purify_action_cnt
    # repair_actions_dict['original'] = get_actions_str_list(repair_actions_ori_dict)
    # repair_actions_dict['purified'] = get_actions_str_list(repair_actions_purify_dict)

    return repair_actions_ori_dict, repair_actions_purify_dict

def get_actions_str_list(actions_dict):
    actions_str_list = []
    for fileClass in actions_dict.keys():
        if fileClass == "biggest_depth":
            continue
        if fileClass == "repair_actions_cnt":
            continue
        if fileClass == "classes_cnt":
            continue
        if fileClass == "files_cnt":
            continue
        actions = actions_dict[fileClass]
        for action in actions:
            # line_dict = {}
            line_list = []
            # line_cnt = 0
            for line in action.lines:
                if len(line) != 0:
                    line = line.strip()
                    # line_cnt += 1
                    # line_dict[line_cnt] = line
                    line_list.append(line)
            # actions_str_list.append(line_dict)
            actions_str_list.append(line_list)
    return actions_str_list

def find_copy_paste_bugs(repair_actions_ori_dict, bug_id, sibling_pair_list):
    all_actions_list = []
    for fileClass in repair_actions_ori_dict.keys():
        if fileClass == "biggest_depth":
            continue
        if fileClass == "repair_actions_cnt":
            continue
        if fileClass == "classes_cnt":
            continue
        if fileClass == "files_cnt":
            continue

        all_actions_list.extend(repair_actions_ori_dict[fileClass])
    
    action_path = os.path.join(cur_dir, "../results", "actions", bug_id + '.txt')
    for action in all_actions_list:
        write_line_to_file(action_path, action.get_lines_str())

    # sibling_pair_list = []
    for i in range(len(all_actions_list)):
        for j in range(i + 1, len(all_actions_list)):
            # compare ith and jth action
            str1 = all_actions_list[i].get_lines_str()
            str2 = all_actions_list[j].get_lines_str()
            # similarity = get_similarity(str1, str2)
            # similarity = get_similarity_for_lists(all_actions_list[i].lines, all_actions_list[j].lines)
            similarity = get_similarity_for_str_2(str1, str2)
            print("str1 {}: {} \nstr2 {}: {}\n sim: {}".format(i, str1, j, str2, similarity))
            if similarity >= 0.7:
                sibling_pair_list.append("{} {} {}".format(i, j, similarity))
            print()

def write_chunk_and_actions(log_str, bug_id):
    m = myFindAll(log_str, re.compile(r"======= \[getOriAndPurifyActions\] get patchActionsMap ======.*======= \[getOriAndPurifyActions\] get patchActionsMap end ======", re.DOTALL))
    text = m[0]
    chunk_text_list = re.findall(re.compile("Chunk: (.*?)Chunk end.", re.DOTALL), text)
    action_text_list = re.findall(re.compile("The chunk's hierarchicalActionSets: (.*?)hierarchicalActionSets end.", re.DOTALL), text)

    if len(chunk_text_list) != len(action_text_list):
        raise Exception("len(chunk_text_list) != len(action_text_list)")

    action_path = os.path.join(cur_dir, "../results", "actions", bug_id + '-chunk.txt')
    for i in range(len(chunk_text_list)):
        write_line_to_file(action_path, chunk_text_list[i].replace("\n\n", "\n"))
        write_line_to_file(action_path, action_text_list[i].replace("\n\n", "\n"))


def getLinesAndChunks(diffPath):
    diff_lines = read_file_no_strip(diffPath)

    patch_lines = []
    patch_chunks = []
    isInChunk = False
    isInDiff = False
    delCnt = 0
    addCnt = 0

    chunk = []
    for line in diff_lines:
        if re.match("\+\+\+ /mnt/purify/", line) or re.match("--- /mnt/purify/", line):
            continue

        if re.match("@@ ", line):
            if len(chunk) > 0:
                patch_chunks.append(chunk)
                chunk = []
            isInDiff = True
            continue
        
        if isInDiff == False:
            continue

        if (re.match("-", line) is not None or re.match("\+", line) is not None):            
            isInChunk = True
            patch_lines.append(line)
            chunk.append(line)

            if re.match("-", line) is not None:
                delCnt += 1
            else:
                addCnt += 1
        else:
            if isInChunk:
                patch_chunks.append(chunk)
                chunk = []
            isInChunk = False

    if len(chunk) > 0:
        patch_chunks.append(chunk)
    
    return patch_lines, patch_chunks, delCnt, addCnt

    

def cov_simplify_fail(log_str):
    # further verify simplified cnt
    m = re.findall(re.compile("Use backup lines and chunks for ddmin now!"), log_str)
    if len(m) != 0:
        return True
    return False
