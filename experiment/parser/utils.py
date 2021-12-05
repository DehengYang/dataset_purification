import shutil
import os
import io
import yaml
from ymlUtil import ordered_yaml_dump

# get path of current py file and directory
cur_file  = os.path.abspath(__file__)
cur_dir = cur_file.rsplit("/", 1)[0] 

def read_file(path):
    if not os.path.exists(path):
        raise Exception("{} does not exists.".format(path))
        return []
    """
    read lines from file
    """
    stripped_lines = []
    with io.open(path, encoding = 'utf-8', mode = 'r') as f:
            lines = f.readlines()
            for line in lines:
                stripped_lines.append(line.strip())
    return stripped_lines

def read_file_no_strip(path):
    if not os.path.exists(path):
        raise Exception("{} does not exists.".format(path))
        return []
    """
    read lines from file
    """
    stripped_lines = []
    with io.open(path, encoding = 'utf-8', mode = 'r') as f:
            lines = f.readlines()
            for line in lines:
                stripped_lines.append(line)
    return stripped_lines

def read_file_to_str(path):
    if not os.path.exists(path):
        return []
    """
    read lines from file
    """
    string = ""
    with io.open(path, encoding = 'utf-8', mode = 'r') as f:
            lines = f.readlines()
            for line in lines:
                string += line + "\n"
    return string

def write_line_to_file(file_path, line, line_break = True, append = True):
    if line_break:
        line = line + "\n"

    if append:
        with open(file_path,'a+') as f:
            f.write(line)
    else:
        with open(file_path,'w') as f:
            f.write(line)

def write_list_to_file(file_path, lines_list, append = True): #line_break = True, 
    # if line_break:
    #     line = line + "\n"

    if append:
        with open(file_path,'a+') as f:
            for line in lines_list:
                f.write(line + "\n")
    else:
        with open(file_path,'w') as f:
            for line in lines_list:
                f.write(line + "\n")

def write_chunks_to_file(file_path, lines, message = ""):
    with open(file_path,'a+') as f: # append
        f.write("\n========== {} ==========\n".format(message))
        for line in lines:
            string = ""
            if type(line) is list:
                for subLine in line:
                    subLine = subLine.replace("\n\n", "\n")
                    string += subLine
                string += "---chunk---\n"
            else:
                line = line.replace("\n\n", "\n")
                string = line
            f.write(string)

def write_to_yaml(yml_path, data_dict):
    # write to yml
    # with open(yml_path, 'w') as outfile:
    #     yaml.safe_dump(data_dict, outfile, default_flow_style=False)

    # new approach
    with open(yml_path, 'w') as outfile:
        ordered_yaml_dump(data_dict, outfile)

    #     yaml.add_representer(collections.OrderedDict, lambda dumper, data: dumper.represent_mapping('tag:yaml.org,2002:map', data.items()))
    #     # output = yaml.dump(data_dict)
    #     yaml.safe_dump(data_dict, outfile, default_flow_style=False)

    ############### read yml ###############
    # with open(yml_path) as f:
    #     # data = yaml.load(f, Loader=yaml.FullLoader)
    #     data = yaml.safe_load(f)
    #     patch = data['patch']['ori_patch']
    #     print(patch)
    # print()

# to be finished
def read_yaml(yml_path):
    with open(yml_path) as f:
        # data = yaml.load(f, Loader=yaml.FullLoader)
        data = yaml.safe_load(f)
        # patch = data['patch']['ori_patch']
        # print(patch)
    # print()
    return data

def list_all_files(dir_path, file_type = ""): #file_type -> postfix  e.g., .yml
    files_list = []
    for file_name in os.listdir(dir_path):
        file_path = os.path.join(dir_path, file_name)
        if os.path.isdir(file_path):
            print("file_path: {} is dir".format(file_path))
        
        if os.path.isfile(file_path):
            if file_type == "":
                files_list.append(file_path)
            else:
                if file_path[-len(file_type) : ] == file_type:
                    files_list.append(file_path)
    return files_list

def remove_dir_all(dir_path):
    for file_name in os.listdir(dir_path):
        file_path = os.path.join(dir_path, file_name)
        if os.path.isfile(file_path):
            os.remove(file_path)
        if os.path.isdir(file_path):
            shutil.rmtree(file_path)