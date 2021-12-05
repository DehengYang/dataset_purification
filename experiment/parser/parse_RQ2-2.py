import sys
import os
import shutil
import re

import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import pandas as pd
from scipy import stats
import math

plt.rc('font',family='Times New Roman')

from utils import *

# get path of current py file and directory
cur_file  = os.path.abspath(__file__)
cur_dir = cur_file.rsplit("/", 1)[0] 

result_dir = os.path.join(cur_dir, "RQ2-result")


def parse_and_plt():
    df = get_yml_df()

    yml_df_path = os.path.join(result_dir, "yml_df-ori.txt")
    write_line_to_file(yml_df_path,  df.to_string(), append = False) #.round(2)


    # 
    df_id = df['bug_id']
    df.drop('bug_id', axis=1, inplace=True)
    df = df.astype(float)

    df = area(df)
    df = df.round(2)

    # df['bug_id'] = df_id

    yml_df_path = os.path.join(result_dir, "yml_df-table.txt")
    write_line_to_file(yml_df_path,  df.to_string(), append = False) #.round(2)


def area(df):
    min_col = {}
    max_col = {}
    # balance = {}
    qua2 = {}
    qua5 = {}
    qua7 = {}
    qua9 = {}
    qua95 = {}
    mean = {}
    median = {}

    for col in df:
        max_col[col]= df[col].max()
        min_col[col]= df[col].min()
        mean[col] = df[col].mean()
        median[col] = df[col].median()
        # balance[col]= max_col[col] + min_col[col]

        qua2[col], qua5[col], qua7[col], qua9[col], qua95[col] = df[col].quantile([0.25,0.5, 0.75, 0.9, 0.95], \
            interpolation='nearest')

    result = pd.DataFrame([mean, median, min_col, qua2, qua5, qua7, qua9, qua95, max_col], index=['mean', 'median', 'min', 'qua2', 'qua5', 'qua7', \
        'qua9', 'qua95', 'max'])
    return result
    

def get_yml_df():
    from yamlable import YamlAble, yaml_info
    sys.path.append('.\\tools')
    from tools.info import Action

    # read yaml
    yml_dir = os.path.join(cur_dir, "results")
    yml_file_list = list_all_files(yml_dir)
    yml_df = pd.DataFrame(columns=['bug_id', 'oriLines', 'purLines', 'covLines', 'oriChunks', 'purChunks', \
        "oriAdd", "oriDel", "purAdd", "purDel", \
            "oriFile", "oriClass", \
                "purFile", "purClass", \
                "oriActionsCnt", "oriPatternsCnt",  \
                "purActionsCnt", "purPatternsCnt", "covTC", "ddTC"
            ]) #, "purMethod" "oriMethod"

    ori_patten_file = os.path.join(result_dir, "ori_pattens.txt")
    pur_patten_file = os.path.join(result_dir, "pur_pattens.txt")
    write_line_to_file(ori_patten_file, "", append = False)
    write_line_to_file(pur_patten_file, "", append = False)

    ori_pattern_all_list = []
    pur_pattern_all_list = []

    for i in range(len(yml_file_list)):   #range(10):   #
        yml_file_path = yml_file_list[i]
        yml_dict = read_yaml(yml_file_path)

        # oriAdd, oriDel = getAddDel(yml_dict['10 patch']['original'])
        # purAdd, purDel = getAddDel(yml_dict['10 patch']['purified'])

        print(yml_file_path)

        ori_repair_actions, ori_repair_patterns, actions_list, ori_patterns_list = parse_actions(yml_dict['11 repair actions']['original'])
        pur_repair_actions, pur_repair_patterns, actions_list, pur_patterns_list = parse_actions(yml_dict['11 repair actions']['purified'])

        write_line_to_file(ori_patten_file, yml_file_path, append=True)
        write_list_to_file(ori_patten_file, ori_patterns_list, append = True)
        write_line_to_file(pur_patten_file, yml_file_path, append=True)
        write_list_to_file(pur_patten_file, pur_patterns_list, append = True)

        meta_pattern_list_ori = parse(ori_patterns_list)
        ori_pattern_all_list.extend(meta_pattern_list_ori)

        meta_pattern_list_pur = parse(pur_patterns_list)
        pur_pattern_all_list.extend(meta_pattern_list_pur)

        # continue

        covTC = float(yml_dict['4 time cost of purification']['coverage on buggy version']) + float(yml_dict['4 time cost of purification']['coverage on fixed version']) + float(yml_dict['4 time cost of purification']['purification via coverage'])
        ddTC = float(yml_dict['4 time cost of purification']['purification via delta debugging'])

        yml_df.loc[yml_df.shape[0]] = [
            yml_dict['1 bug_id'], \
            yml_dict['6 reduced lines']['ori_patch_lines'], \
            yml_dict['6 reduced lines']['delta_patch_lines'], \
                yml_dict['6 reduced lines']['cov_patch_lines'], \
            yml_dict['7 reduced chunks']['original'], \
            yml_dict['7 reduced chunks']['purifiy'], \
                yml_dict['6 reduced lines']['oriAdd'], yml_dict['6 reduced lines']['oriDel'], \
                yml_dict['6 reduced lines']['purAdd'], yml_dict['6 reduced lines']['purDel'], \
                yml_dict['12 ori_repair_actions']['files_cnt'], yml_dict['12 ori_repair_actions']['classes_cnt'],  \
                yml_dict['13 purify_repair_actions']['files_cnt'], yml_dict['13 purify_repair_actions']['classes_cnt'],  \
                    ori_repair_actions, len(set(meta_pattern_list_ori)), \
                    pur_repair_actions, len(set(meta_pattern_list_pur)), \
                    covTC, ddTC
        ]

    from itertools import groupby
    # items = [5, 1, 1, 2, 2, 1, 1, 2, 2, 3, 4, 3, 5]
    results = {value: len(list(freq)) for value, freq in groupby(sorted(ori_pattern_all_list))}
    results=sorted(results.items(),key=lambda x:x[1],reverse=True)
    printPatternFreq(results, os.path.join(result_dir, "pattern-freq-ori.txt"))
    # print(results)

    results_pur = {value: len(list(freq)) for value, freq in groupby(sorted(pur_pattern_all_list))}
    results_pur=sorted(results_pur.items(),key=lambda x:x[1],reverse=True)
    printPatternFreq(results_pur, os.path.join(result_dir, "pattern-freq-pur.txt"))

    return yml_df

def printPatternFreq(results_pur, path):
    sum = getSum(results_pur)
    write_line_to_file(path, "", False)
    for key in results_pur:
        print("{} {} {}".format(key[0], key[1], key[1]/sum))
        write_line_to_file(path, "{} {} {}".format(key[0], key[1], key[1]/sum), True)

def getSum(results_pur):
    sum = 0
    for key in results_pur:
        sum += key[1]
    return sum

# def parse(patterns_list):
#     meta_pattern_list = []

#     parent_dict = {}
#     for pattern in patterns_list: #'UPD FieldDeclaration\n---UPD VariableDeclarationFragment'
#         lines = pattern.strip().split("\n")
#         for line in lines:
#             if line == "":
#                 continue

#             if re.match("---", line):
#                 level = 0
#                 while(re.match("---", line)):
#                     line = line[3:]
#                     level += 1
#                 if level in parent_dict.keys():
#                     parent_dict[level].append(line)
#                 else:
#                     parent_dict[level] = [line]

#             else:
#                 if 0 in parent_dict.keys():
#                     parent_dict[0].append(line)
#                     raise Exception("0 in parent_dict.keys():")
#                 else:
#                     parent_dict[0] = [line]
    
#         for level in parent_dict.keys():
#             if level + 1 in parent_dict.keys():
#                 # src, target, op
#                 target = parent_dict[level].split(" ")[1]
#                 for src in parent_dict[level + 1]:
#                     op, src_name = src.split(" ")
#                     meta_pattern_list.append(src_name, target, op)
#     return meta_pattern_list
#     INS IfStatement
# ---MOV IfStatement
# ---INS InfixExpression
# ------INS SimpleName
# ------INS Operator
# ------INS TypeLiteral
# ---INS ReturnStatement
# ------INS ClassInstanceCreation
# ---------INS New
# ---------INS ParameterizedType
# ------------INS SimpleType
# ------------INS SimpleType
# ---------INS NumberLiteral

def parse(patterns_list):
    meta_pattern_list = []

    for pattern in patterns_list: #'UPD FieldDeclaration\n---UPD VariableDeclarationFragment'
        lines = pattern.strip().split("\n")
        # for line in lines:
        for i in range(len(lines)-1,-1,-1):
            line = lines[i]
            cur_level = getLevel(line)

            # go to prev lines
            for j in range(i - 1,-1,-1):
                line_prev = lines[j]
                prev_level = getLevel(line_prev)
                if prev_level + 1 == cur_level:
                    #
                    target = removeSlash(line_prev).split(" ")[1]
                    op, src_name = removeSlash(line).split(" ")
                    # meta_pattern_list.append([src_name, target, op])
                    meta_pattern_list.append("{} {} {}".format(src_name, target, op))
                    break
    return meta_pattern_list

def getLevel(line):
    level = 0
    while(re.match("---", line)):
        line = line[3:]
        level += 1
    return level

def removeSlash(line):
    while(re.match("---", line)):
        line = line[3:]
    return line

def parse_actions(action_list):
    actions = len(action_list)
    actions_list = []
    patterns_list = []
    for action in action_list:
        if action == "":
            continue
        action_str = ""
        pattern_str = ""
        for line in action:
            if line == "":
                continue
            op_str = get_op(line, isAction = True)
            if op_str != "":
                action_str += op_str + '\n'

            op_str = get_op(line, isAction = False)
            if op_str != "":
                pattern_str += op_str + '\n'
        if action_str not in actions_list:
            actions_list.append(action_str)
        # else:
        #     print("in list 1")
        if pattern_str not in patterns_list:
            patterns_list.append(pattern_str)
        # else:
        #     print("in list 2")

    return len(actions_list), len(patterns_list), actions_list, patterns_list

def get_op(op_line, isAction = True):
    line = op_line
    if isAction:
        m = re.findall(re.compile("(.*?)@@"), op_line)
        if len(m) != 0:
            if re.search("@AT@", op_line):
                line = op_line[0: op_line.find("@AT@")]
    else:
        m = re.findall(re.compile("(.*?)@@"), op_line)
        if len(m) != 0:
            if re.search("@@", op_line):
                line = op_line[0: op_line.find("@@")]

    if "UPD" not in line and "MOV" not in line and "INS" not in line and "DEL" not in line:
        line = ""

    return line

if __name__ == "__main__":
    parse_and_plt()
    # statistical_analysis_manual()