import re
"""
save info of each patch purification of the bug

The work that purify has done and the corresponding info I want to collect:
+ count the loc of buggy project
    + loc count info
+ coverage on buggy version via GZoltar
    + time cost
    + total executed tests
    + spectrum size
    + failing tests and test cases
+ apply human-patch
    + human-patch info
+ re-run coverage on fixed version via GZoltar
    + time cost
    + total executed tests
    + spectrum size
    + failing tests and test cases
+ simplify original chunks
    + original and simplified patch chunks
    + how each chunk line is covered by each failing test case
    + time cost
+ mutant generation based on coverage info and using delta debugging
    + the mutant diff
    + time cost of delta debugging
    + time cost of write patch, compile, test fail, test all.
    + traces. (but currently are not neededd)
    + minimal chunks
+ get repair actions of buggy version and purified buggy version.
    + repair actions info
    + time cost

Extra:
+ the total time used for each bug 
    + the bug init time by RepairThemAll
    + the time cost of purify project running the bug
"""

import yaml
from yamlable import YamlAble, yaml_info

class BugInfo():
    def __init__(self):
        [self.loc_cnt, 
        self.time_cost_cov_bug, self.exec_test_cases, self.failing_test, self.failing_test_cases, self.spectrum_size,
        self.original_patch, self.patch_line_cov_info, self.time_cost_simplify, self.simplified_patch,
        self.mutant_list, self.minimal_chunks,
        self.repair_actions_ori, self.repair_actions_purify, self.time_cost_repair_actions,
        self.time_cost_main] = [-1]*2

    def printInfo(self):
        print("time_cost_main: {}".format(self.time_cost_main))

class Mutant():
    def __init__(self):
        self.mutant_diff = ""
        [self.time_cost, self.time_cost_compile, self.time_cost_fail, self.time_cost_all] = [-1]*4
        [self.compile_pass, self.pass_fail, self.pass_all] = [False]*3
        self.passing_cases_during_fail = []
        self.failing_cases_during_fail = []
        self.passing_cases_during_all = []
        self.failing_cases_during_all = []

@yaml_info(yaml_tag_ns='myyaml')
class Chunk(YamlAble):
    def __init__(self, chunk_lines = '', clazz = "", lines = [], method_names = [], replaceRange = []):
        self.chunk_lines = chunk_lines

        self.clazz = clazz
        self.replaceRange = list(replaceRange)
        self.lines = list(lines)
        self.method_names = list(method_names)

    def update(self):
        m = re.findall(re.compile('chunk class: (.*)'), self.chunk_lines)
        self.clazz = m[0]

        m = re.findall(re.compile('replaceRange: Pair<(.*)>'), self.chunk_lines)
        self.replaceRange = m[0].split(",")

        m = re.findall(re.compile('lineNo:(.*), lineCode:(.*)'), self.chunk_lines)
        for i in m:
            self.lines.append({i[0] : i[1]})

        m = re.findall(re.compile('method:(.*)'), self.chunk_lines)
        for i in m:
            self.method_names.append(i)

    # def __str__(self):
    #     return self.name + ": " + self.url

    def __to_yaml_dict__(self):
        return {
            "clazz" : self.clazz, 
            'lines' : self.lines,
            'method_names' : self.method_names
            }

    # def __str__(self):
    #   return repr(self)
    # def __repr__(self):
    #   return {"clazz" : self.clazz}
    #   {"clazz" : self.clazz, 'lines' : self.lines}

@yaml_info(yaml_tag_ns='myyaml')
class Action(YamlAble):
    def __init__(self, lines = [], actions = []):
        self.actions = list(actions)

        # approach 1
        # self.lines = list(lines)
        self.lines = self.updateLines3(lines)

    def updateLines(self, lines):
        real_lines = []
        for line in lines:
            if len(line) == 0:
                continue
            line = line[0: line.find("@@")]
            real_lines.append(line)
        return real_lines

    def updateLines2(self, lines):
        real_lines = []
        for line in lines:
            if len(line) == 0:
                continue
            #INS VariableDeclarationStatement@@double value=intervalXYData.getXValue(series,item);
      #@TO@ ForStatement@@[int item=0];item < itemCount; [item++] @AT@ 29591 @LENGTH@
            line = line[0: line.find("@AT@")]
            line_code = line[line.find("@@") : ]
            action = line[0:line.find("@@")]
            # line_code = line_code.replace("", "x")
            line_code = re.sub('[a-zA-Z0-9]', "x", line_code)
            line_code = re.sub('x+', "x", line_code)
            real_lines.append(action + line_code)

        return real_lines

    def updateLines3(self, lines):
        real_lines = []
        for line in lines:
            if len(line) == 0:
                continue
            line = line[0: line.find("@AT@")]
            real_lines.append(line)
        return real_lines

    def get_lines_str(self):
        lines_str = ""
        for line in self.lines:
            lines_str += line + "\n"
        return lines_str

    def update(self):
        for line in self.lines:
            if "@@" in line:
                action = re.findall(re.compile("(.*?)@@"), line)[0]
                while "---" in action:
                    # action = re.findall(re.compile("---(.*?)"), action)[0]
                    action = action[len("---"):]
                self.actions.append(action)
    def __to_yaml_dict__(self):
        # return {
        #     'actions' : self.actions
        #     }
        return {
            'lines' : self.lines
            }
