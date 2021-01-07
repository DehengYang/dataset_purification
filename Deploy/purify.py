import os
import shutil
import json
import subprocess
import datetime

from config import JAVA8_HOME, TOOL_TIMEOUT, REPAIR_ROOT, JAVA7_HOME
from config import JAVA_ARGS
from config import OUTPUT_PATH
from config import WORKING_DIRECTORY
from config import Z3_PATH
from core.RepairTool import RepairTool
from core.runner.RepairTask import RepairTask
from core.utils import add_repair_tool

def to_absolute(root, folders):
    absolute_folders = []
    for folder in folders:
        if os.path.exists(os.path.join(root, folder)):
            absolute_folders.append(os.path.join(root, folder))
    return absolute_folders

class Purify(RepairTool):
    """Purify"""

    def __init__(self, name="Purify"):
        super(Purify, self).__init__(name, "purify")

    def repair(self, repair_task):
        bug = repair_task.bug
        bug_path = os.path.join(WORKING_DIRECTORY,
                                "%s_%s_%s_%s" % (self.name, bug.benchmark.name, bug.project, bug.bug_id))
        repair_task.working_directory = bug_path
        self.init_bug(bug, bug_path)
        try:
            bin_folders = to_absolute(bug_path, bug.bin_folders())
            test_bin_folders = to_absolute(bug_path, bug.test_bin_folders())
            sources = to_absolute(bug_path, bug.source_folders())
            java_version = JAVA7_HOME
            if bug.compliance_level() > 7:
                java_version = JAVA8_HOME

            failedTestCases = bug.failing_test_methods()
            failedTestsStr = ""
            for test in failedTestCases:
                failedTestsStr += test + ":"

            classpath = ":".join(bin_folders + test_bin_folders)
            if classpath != ":":
                classpath += ":" 
            classpath += bug.classpath()

            if bug.benchmark.name == "Defects4J":
                bug.compile()

            oriPatchDiffPath = os.path.join(repair_task.buggy_loc_path().replace("/mnt/purify/", "/mnt/benchmarks/"), "patchDiff.txt")
            curPatchDiffPath = os.path.join(repair_task.buggy_loc_path(), "patchDiff.txt")
            if not os.path.exists(repair_task.buggy_loc_path()):
                os.makedirs(repair_task.buggy_loc_path())
            if not os.path.exists(curPatchDiffPath):
                shutil.copy(oriPatchDiffPath, repair_task.buggy_loc_path())

            cmd = """cd %s;
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
TZ="America/New_York"; export TZ;
export PATH="%s:$PATH";
export JAVA_HOME="%s";
time java %s -cp %s %s \\
    -dataset %s \\
    -projDir %s \\
    -srcJavaDir %s \\
	-binJavaDir %s \\
	-binTestDir %s \\
    -jvmPath %s \\
	-failedTestsStr %s \\
	-gzoltarDir %s \\
    -logDir %s \\
    -patchDiffPath %s \\
	-dependencies %s;
	echo "\\n\\nNode: `hostname`\\n";
	echo "\\n\\nDate: `date`\\n";
""" % (bug_path,
       JAVA8_HOME,
       JAVA8_HOME, 
       JAVA_ARGS, self.jar, self.main,
       bug.benchmark.name,
       bug_path, 
       ":".join(sources),
       ":".join(bin_folders),
       ":".join(test_bin_folders),
       java_version,
       failedTestsStr,
       os.path.join(REPAIR_ROOT, "libs", "gzoltar1.7.3"),
       repair_task.buggy_loc_path(),
       os.path.join(repair_task.buggy_loc_path(), "patchDiff.txt"),
       classpath)

            log_path = os.path.join(repair_task.log_dir(), "repair.log")
            if not os.path.exists(os.path.dirname(log_path)):
                os.makedirs(os.path.dirname(log_path))
            log = file(log_path, 'w')
            log.write(cmd)
            log.flush()
            print "cmd: {}".format(cmd)
            subprocess.call(cmd, shell=True, stdout=log, stderr=subprocess.STDOUT)
            with open(log_path) as data_file:
                return data_file.read()
        finally:
            path_results = os.path.join(bug_path, "output.json")
            if os.path.exists(path_results):
                repair_task.status = "FINISHED"
                shutil.copy(path_results, os.path.join(repair_task.log_dir(), "detailed-result.json"))
                with open(path_results) as fd:
                    repair_task.results = json.load(fd)
                    result = {
                        "repair_begin": self.repair_begin,
                        "repair_end": datetime.datetime.now().__str__(),
                        'patches': []
                    }
                    if 'patch' in repair_task.results:
                        result['patches'] = repair_task.results["patch"]
                    with open(os.path.join(repair_task.log_dir(), "result.json"), "w") as fd2:
                        json.dump(result, fd2, indent=2)
                    if len(result['patches']) > 0:
                        repair_task.status = "PATCHED"
            else:
                repair_task.status = "ERROR"
            cmd = "rm -rf %s;" % (bug_path)
            print "cmd for rm bug_dir: {}".format(cmd)
            subprocess.call(cmd, shell=True)
        pass

def init(args):
    return Purify()

def purify_args(parser):
    pass

parser = add_repair_tool("Purify", init, 'Operate on the bug with Purify')
purify_args(parser)
