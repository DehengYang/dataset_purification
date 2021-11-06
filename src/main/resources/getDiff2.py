import os
import re
import sys

"""
this is a script written based on getDiff2 for dataset purification!
2020, Oct 8.
"""

def get_buggyloc(diffPath):
    """
this program is based on: https://github.com/DehengYang/buggylocs-for-4-benchmarks/blob/bc432ff4a7b481adc86512d67662c1c030c81f17/script/checkout.py#L95
    """
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
#    del_flag = 0 #label deletions
#    add_flag = 0 # label additions
#    modify_flag = 0 # if an addtion is based on the deletion, then it is a modification.
    add_cnt_2 = 0
    del_cnt_2 = 0
    
    
    #for line in diff_info:
    for index in range(len(diff_info)):
        line = diff_info[index] 
        
        # to extract buggy_file_name
        # if re.search('---',line): # exposed by Lang 5
        if re.match('--- ',line) and line.find(".java") >= 0:
            if file_name != '' and (len(loc_del)+len(loc_add) != 0): 
                #add loc info before new file_name
                # e.g., two chunks are in one file, so in this way we need to use the if-block
                # to add chunk_locs.
                chunk_lists.append([file_name, loc_del, loc_add])
                printInfo(file_name, loc_del, loc_add, buggy_locs_per_chunk)
               
            
            # reinitialize
            loc_del = []
            loc_add = []
            buggy_locs_per_chunk = []
            add_cnt = 0
            del_cnt = 0
            add_cnt_2 = 0 # this is to catch buggy_locs (when two chunks are in one file.)
            del_cnt_2 = 0 # simlilar to add_cnt_2

            file_name = get_bug_class(line)
            debug_info("file_name: {}".format(file_name))
            continue
        
        if re.match('--- ',line) and line.find(".java") < 0: # fix a bug exposed by Bears webfirmframework-wff
            file_name = ""

        if file_name == "":  # fix a bug exposed by Bears webfirmframework-wff
            continue

        # to extract start_line
        if re.match('@@ ',line):
            # get each chunk info 
            if file_name != '' and (len(loc_del)+len(loc_add) != 0): 
                # must include changes/buggy-lines
                chunk_lists.append([file_name, loc_del, loc_add])
                printInfo(file_name, loc_del, loc_add, buggy_locs_per_chunk)
            
            # reinitialize
            loc_del = []
            loc_add = []
            buggy_locs_per_chunk = []
            add_cnt = 0
            del_cnt = 0
            
            # this is a bit complicated here. I have to consider both del and add start.
            # which is exposed by Mockito 19
            # e.g., @@ -1 +1 @@
            # @@ -1,4 +1,4 @@
            # @@ -1 +1,2 @@
            debug_info("current line: {}, file_name:{}".format(line, file_name))
            del_info = line.split('-')[1].split(" ")[0] # 1,4 or 1 
            if "," in del_info:
                loc_start_del = int(del_info.split(",")[0])
            else:
                loc_start_del = int(del_info)

            add_info = line.split('+')[1].split(" ")[0]
            if "," in add_info:
                loc_start_add = int(add_info.split(",")[0])
            else:
                loc_start_add = int(add_info)

            # if "," in line: 
            #     loc_start_del = int(line.split(',')[0].split('-')[1]) 
            #     debug_info("current line: {}, file_name:{}".format(line, file_name))
            #     loc_start_add = int(line.split(',')[1].split('+')[1]) 
            # else:# bug fix.   @@ -1 +1 @@
            #     loc_start_del = int(line.split(' ')[1].split('-')[1])
            #     debug_info("current line: {}, file_name:{}".format(line, file_name))
            #     loc_start_add = int(line.split(' ')[2].split('+')[1])
            continue
        
        # mock 19 expose the problem of '- ', as somtimes the ' ' maybe a 'tab'
#        if re.match('-',line):
#            if len(line) == 1 or line[1] != '-':
#                loc_del.append(loc_start_del - add_cnt)
#                # just now I made a copy-paste bug, by copying del_cnt here without changing to add_cnt
#                # closure 35 expose this problem.
#                del_cnt += 1
                
        if is_del(line):
            cur_del_line = loc_start_del - add_cnt
            loc_del.append(file_name + ":" + str(cur_del_line) + "==line==" + line)
            del_cnt += 1
            del_cnt_2 += 1
            
            # add del_loc
            buggy_locs.append(file_name + ':' + str(cur_del_line))
            buggy_locs_per_chunk.append(file_name + ':' + str(cur_del_line))
                
#        if re.match('\+',line): 
#        #mockito 19 expose the problem of '\+ ',it should be replaced by '\+'
#            # print(line) 
#            if len(line) == 1 or line[1] != '+': 
#            # filter string like '+++ b/src/org/mockito/internal/'
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
                pass  # this is to filter comments changes or empty line # fix a bug exposed by Bears Bears_2018swecapstone-h2ms_356638992-356666847
            
            else: # this is a fault omission
            # find previous line and next line.
            # e.g., chart 2
#                prev_loc = cur_add_line - add_cnt_2
#                next_loc = cur_add_line - add_cnt_2 + 1
#                buggy_locs.append([file_name, prev_loc])
#                buggy_locs.append([file_name, next_loc])
                
                pre_flag = 0
                pre_index = 1
                # trace backward to find previous valid line
                while (not re.match('@@ ',diff_info[index - pre_index])):
#                    print('line info:',diff_info[index - pre_index])
                    
                    cur_line = diff_info[index - pre_index].strip()
#                    print('cur_line:',cur_line)
                    
                    if is_add(diff_info[index - pre_index]) or \
                       is_del(diff_info[index - pre_index]) :
                           break
                    elif(cur_line == ''):
                    #e.g., line= ' \n', after strip(), the line becomes ''
                           pre_index += 1
                    elif re.match('//',cur_line):
                    # consider // comment in java
                        pre_index += 1
                    elif re.match('/\*',cur_line) or re.match('\*',cur_line) or \
                        re.match('\*/',cur_line):
                    # consider /* * */ multi-line comment in java
                        pre_index += 1
                    elif cur_line[0] == '{' or \
                         cur_line[0] == '}':
                        break
                    else:
                        # here we need del_cnt_2 rather than del_cnt . e.g., chart 25
                        prev_loc = cur_add_line - (add_cnt_2 - del_cnt_2) - pre_index + 1
                        # print('prev_loc info',cur_add_line, add_cnt_2,del_cnt,pre_index,prev_loc)
                        # e.g., chart 25, add_cnt_2 = 7
                        # line 349 - 6(add_cnt_2-del_cnt) - pre_index (1) + 1
                        buggy_locs.append(file_name + ':' + str(prev_loc))
                        buggy_locs_per_chunk.append(file_name + ':' + str(prev_loc))
                        pre_flag = 1
                        break
                
                #----------------------------------------------------------------    
                
                next_flag = 0        
                next_index = 1
                add_cnt_3 = 0 # for this next valid line 
                # trace forward to find next valid line
                while (index + next_index < len(diff_info)):
#                    print(index + next_index)
#                    print('line info:',diff_info[index + next_index])
#                    print(index + next_index , len(diff_info))
                    
                    cur_line = diff_info[index + next_index].strip()
                    
                    # exposed by time 21. when is_del_loc is the next line of cur_add_loc.
                    # then break.
#                    if is_add(diff_info[index + next_index]) or \
#                       is_del(diff_info[index + next_index]):
#                           next_index += 1
                    if is_add(diff_info[index + next_index]):
                           next_index += 1
                    elif is_del(diff_info[index + next_index]):
                        break
                    elif re.match('@@ ',cur_line): # stop if meet next line. # bug fix: delete the not after elif
                        break
                    
                    elif (cur_line == ''):
                    # consider blank line.
                    #e.g., line= ' \n', after strip(), the line becomes ''
                        add_cnt_3 += 1
                        next_index += 1
                    elif re.match('//',cur_line):
                    # consider // comment in java
                        add_cnt_3 += 1
                        next_index += 1
                    elif re.match('/\*',cur_line) or re.match('\*',cur_line) or \
                        re.match('\*/',cur_line):
                    # consider /* * */ multi-line comment in java
                        add_cnt_3 += 1
                        next_index += 1
                    elif cur_line[0] == '{' or \
                         cur_line[0] == '}':
                        break
                    else:
                        add_cnt_3 += 1
                        next_loc = cur_add_line - add_cnt_2  + add_cnt_3 + del_cnt_2 
                        # note that it should add del_cnt_2 rather than del_cnt
                        # e.g., chart 25
                        # line 318 - 4 + 1 + del_cnt = 316
                        # line 468 
                        # buggy_locs.append([file_name, next_loc])
                        buggy_locs.append(file_name + ':' + str(next_loc))
                        buggy_locs_per_chunk.append(file_name + ':' + str(next_loc))
                        next_flag = 1
                        break
                
                if next_flag == 0 and pre_flag == 0:
                    print('cannot find buggy locs for the fault omission for line: {}.'.format(line))
                    buggy_locs.append(['code_missing_fail_for_finding_buggylocs'])
                    buggy_locs_per_chunk.append(['code_missing_fail_for_finding_buggylocs'])
#                    exit('error')
            
            
        # add 1 for next line number
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
    # buggy_locs = sorted(set(buggy_locs)) # fix a bug exposed by D4j Time3. need to add set to filter repeated locs

    print("\nbuggy locations cnt: {}".format(len(uniq_buggy_locs)))
    # for loc in uniq_buggy_locs:
    #     print("buggyLine:{}".format(loc))

    # return chunk_lists, set(buggy_locs)
    return uniq_buggy_locs # just need buggy locs now

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
    # first char is '-' and make sure line[1] != '-'
         return True
    else:
        return False  

def get_bug_class(line):
    # for d4j & bears
    # /java/ > /src/ & /source/
    java_index = line.find(".java")

    if (java_index < 0):
        print "line: {} does not contain .java".format(line)
        sys.exit()

    if line.find("/java/") >= 0:
        index = line.find("/java/")
        bug_class = line[(index + 6):java_index].replace("/",".") #org/jfree/chart/util/ShapeList.java
    elif line.find("/src/") >= 0:
        index = line.find("/src/")
        bug_class = line[(index + 5):java_index].replace("/",".") #.strip(".java") not used
    elif line.find("/source/") >= 0:
        index = line.find("/source/")
        bug_class = line[(index + 8):java_index].replace("/",".") 
    else:
        bug_class = line[:java_index].split("/")[-1]

    return bug_class

def is_empty_or_comment(line, index, diff_info): 
    while (is_add(diff_info[index]) and index >= 0):  # bug fix: change index-1 into index. (consider the line itself first)
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
    # print string
    pass

if __name__ == "__main__":
    get_buggyloc(sys.argv[1])