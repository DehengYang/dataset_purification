import os
import re
import sys

def get_buggyloc(diffPath):
    diff_info = []

    with open(diffPath, 'r') as f: # read diff lines
        lines = f.readlines()
        for line in lines:
           diff_info.append(line.strip("\n")) 
    
    loc_start_del = -1
    loc_start_add = -1
        
    loc_del = []
    loc_add = []
    buggy_locs_per_chunk = []
    
    add_cnt = 0
    del_cnt = 0
    file_name=''
    
    chunk_lists = []
    buggy_locs = []
    add_cnt_2 = 0
    del_cnt_2 = 0
    
    for index in range(len(diff_info)):
        line = diff_info[index] 
        
        if re.match('--- ',line) and line.find(".java") >= 0:
            if file_name != '' and (len(loc_del)+len(loc_add) != 0): 
                chunk_lists.append([file_name, loc_del, loc_add])
                printInfo(file_name, loc_del, loc_add, buggy_locs_per_chunk)

            loc_del = []
            loc_add = []
            buggy_locs_per_chunk = []
            add_cnt = 0
            del_cnt = 0
            add_cnt_2 = 0
            del_cnt_2 = 0

            file_name = get_bug_class(line)
            debug_info("file_name: {}".format(file_name))
            continue
        
        if re.match('--- ',line) and line.find(".java") < 0: 
            file_name = ""

        if file_name == "": 
            continue

        if re.match('@@ ',line):
            if file_name != '' and (len(loc_del)+len(loc_add) != 0): 
                chunk_lists.append([file_name, loc_del, loc_add])
                printInfo(file_name, loc_del, loc_add, buggy_locs_per_chunk)
            
            loc_del = []
            loc_add = []
            buggy_locs_per_chunk = []
            add_cnt = 0
            del_cnt = 0
            
            debug_info("current line: {}, file_name:{}".format(line, file_name))
            del_info = line.split('-')[1].split(" ")[0] 
            if "," in del_info:
                loc_start_del = int(del_info.split(",")[0])
            else:
                loc_start_del = int(del_info)

            add_info = line.split('+')[1].split(" ")[0]
            if "," in add_info:
                loc_start_add = int(add_info.split(",")[0])
            else:
                loc_start_add = int(add_info)

            continue

        if is_del(line):
            cur_del_line = loc_start_del - add_cnt
            loc_del.append(file_name + ":" + str(cur_del_line) + "==line==" + line)
            del_cnt += 1
            del_cnt_2 += 1
            
            buggy_locs.append(file_name + ':' + str(cur_del_line))
            buggy_locs_per_chunk.append(file_name + ':' + str(cur_del_line))
                
        if is_add(line):
            cur_add_line = loc_start_add - del_cnt
            loc_add.append(file_name + ":" + str(cur_add_line)+ "==line==" + line)
            add_cnt += 1
            add_cnt_2 += 1
            
            if is_del(diff_info[index-1]):
                pass
            elif is_add(diff_info[index-1]):
                pass
            elif is_empty_or_comment(line, index, diff_info):
                pass 
            
            else:                 
                pre_flag = 0
                pre_index = 1
                while (not re.match('@@ ',diff_info[index - pre_index])):
                    
                    cur_line = diff_info[index - pre_index].strip()
                    
                    if is_add(diff_info[index - pre_index]) or \
                       is_del(diff_info[index - pre_index]) :
                           break
                    elif(cur_line == ''):
                           pre_index += 1
                    elif re.match('//',cur_line):
                        pre_index += 1
                    elif re.match('/\*',cur_line) or re.match('\*',cur_line) or \
                        re.match('\*/',cur_line):
                        pre_index += 1
                    elif cur_line[0] == '{' or \
                         cur_line[0] == '}':
                        break
                    else:
                        prev_loc = cur_add_line - (add_cnt_2 - del_cnt_2) - pre_index + 1
                        buggy_locs.append(file_name + ':' + str(prev_loc))
                        buggy_locs_per_chunk.append(file_name + ':' + str(prev_loc))
                        pre_flag = 1
                        break
                
                #----------------------------------------------------------------    
                
                next_flag = 0        
                next_index = 1
                add_cnt_3 = 0 # for this next valid line 
                while (index + next_index < len(diff_info)):                    
                    cur_line = diff_info[index + next_index].strip()
                    
                    if is_add(diff_info[index + next_index]):
                           next_index += 1
                    elif is_del(diff_info[index + next_index]):
                        break
                    elif re.match('@@ ',cur_line):
                        break
                    
                    elif (cur_line == ''):
                        add_cnt_3 += 1
                        next_index += 1
                    elif re.match('//',cur_line):
                        add_cnt_3 += 1
                        next_index += 1
                    elif re.match('/\*',cur_line) or re.match('\*',cur_line) or \
                        re.match('\*/',cur_line):
                        add_cnt_3 += 1
                        next_index += 1
                    elif cur_line[0] == '{' or \
                         cur_line[0] == '}':
                        break
                    else:
                        add_cnt_3 += 1
                        next_loc = cur_add_line - add_cnt_2  + add_cnt_3 + del_cnt_2 
                        buggy_locs.append(file_name + ':' + str(next_loc))
                        buggy_locs_per_chunk.append(file_name + ':' + str(next_loc))
                        next_flag = 1
                        break
                
                if next_flag == 0 and pre_flag == 0:
                    print('cannot find buggy locs for the fault omission for line: {}.'.format(line))
                    buggy_locs.append(['code_missing_fail_for_finding_buggylocs'])
                    buggy_locs_per_chunk.append(['code_missing_fail_for_finding_buggylocs'])

        loc_start_del += 1
        loc_start_add += 1
    
    # add last chunk_loc
    chunk_lists.append([file_name, loc_del, loc_add])
    printInfo(file_name, loc_del, loc_add, buggy_locs_per_chunk)

    # write to file
    uniq_buggy_locs = []
    for loc in buggy_locs:
        if loc not in uniq_buggy_locs:
            uniq_buggy_locs.append(loc)

    print("\nbuggy locations cnt: {}".format(len(uniq_buggy_locs)))
    
    return uniq_buggy_locs 

def printInfo(file_name, loc_del, loc_add, buggy_locs_per_chunk):
    print("\ndel-add info in this chunk of java class {}".format(file_name))
    for loc in loc_del:
        print("delLine:{}".format(loc))
    for loc in loc_add:
        print("addLine:{}".format(loc))
    for loc in buggy_locs_per_chunk:
        print("buggyLine:{}".format(loc))

def is_add(line):
    if re.match('\+',line) and (len(line) == 1 or line[1] != '+'):
    # first char is '+' and make sure line[1] != '+'
        return True
    else:
        return False
    
def is_del(line):  
    if re.match('-',line) and (len(line) == 1 or line[1] != '-'):
         return True
    else:
        return False  

def get_bug_class(line):
    java_index = line.find(".java")

    if (java_index < 0):
        print "line: {} does not contain .java".format(line)
        sys.exit()

    if line.find("/java/") >= 0:
        index = line.find("/java/")
        bug_class = line[(index + 6):java_index].replace("/",".") 
    elif line.find("/src/") >= 0:
        index = line.find("/src/")
        bug_class = line[(index + 5):java_index].replace("/",".") 
    elif line.find("/source/") >= 0:
        index = line.find("/source/")
        bug_class = line[(index + 8):java_index].replace("/",".") 
    else:
        bug_class = line[:java_index].split("/")[-1]

    return bug_class

def is_empty_or_comment(line, index, diff_info): 
    while (is_add(diff_info[index]) and index >= 0): 
        index -= 1
        if re.match('//', line.strip()) or \
        re.match('/\*', line.strip()) or \
        re.match('\*', line.strip()) or \
        re.match('\*/', line.strip()) or \
        line.strip() == "":
            pass
        else:
            return False
    return True


def debug_info(string):
    pass

if __name__ == "__main__":
    get_buggyloc(sys.argv[1])
