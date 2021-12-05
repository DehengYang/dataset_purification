
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

save_folder = os.path.join(cur_dir, "BugsNotRun")

# init
not_run_bugs_path = os.path.join(save_folder, "bugs_need_run.txt")
not_run_bugs_detail_path = os.path.join(save_folder, "detail_info.txt")
if os.path.exists(not_run_bugs_path):
    os.remove(not_run_bugs_path)
if os.path.exists(not_run_bugs_detail_path):
    os.remove(not_run_bugs_detail_path)

def parse():
    # read bug ids
    bug_ids = read_file(os.path.join(bug_id_dir, "{}.txt".format(benchmark)))

    for bug_id in bug_ids:
        # debug
        # if bug_id != "Chart-2":
        #     continue

        [proj_name, id_name] = bug_id.split("-")
        bug_result_dir = get_result_dir(bug_id, project_result_dir, benchmark, tool_dir_name)

        # /mnt/purify/buggylocs/Defects4J/Defects4J_Time_1/purify
        # /mnt/purify/repairResults/Defects4J/Chart/1/Purify/0/repair.log

        if bug_result_dir is None:
            write_line_to_file(not_run_bugs_path, "{}".format(bug_id))
            write_line_to_file(not_run_bugs_detail_path, "{} is still not run".format(bug_id))
            continue

        proj_log_file_path = os.path.join(project_log_dir, proj_name, id_name, tool, "0", "repair.log")
        purify_diff_path = os.path.join(bug_result_dir, "purifyPatch.diff")
        purify_log_path = os.path.join(bug_result_dir, "log.txt")
        
        if not os.path.exists(proj_log_file_path):
            write_line_to_file(not_run_bugs_path, "{}".format(bug_id))
            write_line_to_file(not_run_bugs_detail_path, "{} repair.log does not exist".format(bug_id))
            continue
        if not os.path.exists(purify_diff_path):
            write_line_to_file(not_run_bugs_path, "{}".format(bug_id))
            write_line_to_file(not_run_bugs_detail_path, "{} purifyPatch.diff does not exist".format(bug_id))
            continue
        log_str = read_file_to_str(purify_log_path)
        m = re.findall(re.compile(r"\[time cost\] of main: "), log_str)
        if len(m) == 0:
            write_line_to_file(not_run_bugs_path, "{}".format(bug_id))
            write_line_to_file(not_run_bugs_detail_path, "{} log.txt does not finish".format(bug_id))
            continue
        proj_log_str = read_file_to_str(proj_log_file_path)
        m = re.findall(re.compile(r"Node: apr"), proj_log_str)
        if len(m) == 0:
            write_line_to_file(not_run_bugs_path, "{}".format(bug_id))
            write_line_to_file(not_run_bugs_detail_path, "{} repair.log does not finish".format(bug_id))
            continue

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