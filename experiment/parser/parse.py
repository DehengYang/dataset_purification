
import sys
import os
import shutil
import re

from utils import *

sys.path.append('.\\tools')
from tools import purify

# get path of current py file and directory
cur_file  = os.path.abspath(__file__)
cur_dir = cur_file.rsplit("/", 1)[0] 

tool = "Purify"
tool_dir_name = "purify"
benchmark = "Defects4J"

bug_id_dir = os.path.join(cur_dir, "../runner/sampled_bugs/")
project_result_dir = "/mnt/purify/buggylocs/"
project_log_dir = "/mnt/purify/repairResults/Defects4J/"

parse_result_dir = os.path.join(cur_dir, "results")
remove_dir_all(parse_result_dir)
repair_action_log_dir = os.path.join(cur_dir, "results", "actions")
if not os.path.exists(repair_action_log_dir):
    os.makedirs(repair_action_log_dir)
patch_info_log_dir = os.path.join(cur_dir, "results", "patchInfo")
if not os.path.exists(patch_info_log_dir):
    os.makedirs(patch_info_log_dir)

def parse():
    # read bug ids
    bug_ids = read_file(os.path.join(bug_id_dir, "{}.txt".format(benchmark)))

    for bug_id in bug_ids:
        # debug
        # if bug_id != "Time-1":
        #     continue

        bug_result_dir = get_result_dir(bug_id, project_result_dir, benchmark, tool_dir_name)

        # if still_needing_run(bug_id, bug_result_dir):
        #     print(bug_id)
        #     continue

        patch_info_lines_log = os.path.join(patch_info_log_dir, "reduced_lines_patches.txt")
        purify.parse(bug_result_dir, bug_id, parse_result_dir, patch_info_lines_log)

def still_needing_run(bug_id, bug_result_dir):
    [proj_name, id_name] = bug_id.split("-")
    if bug_result_dir is None:
        return True

    proj_log_file_path = os.path.join(project_log_dir, proj_name, id_name, tool, "0", "repair.log")
    purify_diff_path = os.path.join(bug_result_dir, "purifyPatch.diff")
    purify_log_path = os.path.join(bug_result_dir, "log.txt")
    
    if not os.path.exists(proj_log_file_path):
        return True
    if not os.path.exists(purify_diff_path):
        return True
    log_str = read_file_to_str(purify_log_path)
    m = re.findall(re.compile(r"\[time cost\] of main: "), log_str)
    if len(m) == 0:
        return True
    proj_log_str = read_file_to_str(proj_log_file_path)
    m = re.findall(re.compile(r"Node: apr"), proj_log_str)
    if len(m) == 0:
        return True

    return False

def get_result_dir(bug_id, project_result_dir, benchmark, tool_dir_name):
    """
    This func is to return matched directory.  e.g., /mnt/benchmarks/buggylocs/Defects4J/Defects4J_Math_12
    
    bug fix: Math_9 -> /mnt/benchmarks/buggylocs/Defects4J/Defects4J_Math_94/MY_APR/oriFL.log does not exists. Return now.
    """

    bug_name = bug_id.strip().replace("-", ".").replace("_", ".") # this is to match bug-id in bug-results-dir
    all_fl_dir = os.path.join(project_result_dir, benchmark)

    for dirname in os.listdir(all_fl_dir):
        if not os.path.isdir(all_fl_dir + "/" + dirname):
            continue
        
        bug_name_for_match = bug_name + "."
        dirname_for_match = dirname.replace("_", ".").replace("-", ".") + "."  # adding "." is for avoid wrong matching
        if bug_name_for_match in dirname_for_match:
            return os.path.join(all_fl_dir, dirname, tool_dir_name)  # dirname
    return None

if __name__ == "__main__":
    parse()