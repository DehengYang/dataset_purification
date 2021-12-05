###################################################################
# a = [3,1,2,3,4] + [1]
# print(a)

# for i in range(len(a)):
#     print(a[i])

###################################################################
# import re

# text = """
# some Varying TEXT\n
# \n
# DSJFKDAFJKDAFJDSAKFJADSFLKDLAFKDSAF\n
# [more of the above, ending with a newline]\n
# [yep, there is a variable number of lines here]\n
# \n
# (repeat the above a few hundred times).
# """
# reg = re.compile(r"^(.+)\n((?:\n.+)+)", re.MULTILINE)
# reg = re.compile(r"^(.+)(?:\n|\r\n?)((?:(?:\n|\r\n?).+)+)", re.MULTILINE)
# m = reg.findall(text)
# print(m)

###################################################################
# import pickle
# d = { "abc" : [1, 2, 3], "qwerty" : [4,5,6] }
# afile = open(r'd.pkl', 'wb')
# pickle.dump(d, afile)
# afile.close()

# #reload object from file
# file2 = open(r'd.pkl', 'rb')
# new_d = pickle.load(file2)
# file2.close()

#print dictionary object loaded from file
# print new_d


###################################################################
# import yaml
# from tools.info import Action, Chunk

# data = dict(
#     A = 'a',
#     B = dict(
#         C = 'c',
#         D = 'd',
#         E = 'e',
#     )
# )

# data_dict = dict()
# data_dict['dictA'] = data
# data_dict['chunk'] = Chunk(clazz = "actionasas", lines = ['aa', 'bb'])
# # data_dict['action'] = Action('afaf')

# with open('data.yml', 'w') as outfile:
#     yaml.safe_dump(data_dict, outfile, default_flow_style=False) #dump
#     print("good")

########################################################################
# string = """
# --- /mnt/purify/repairDir/Purify_Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java\t\
#     2020-10-20 04:37:04.946617616 -0400\n+++ /mnt/purify/buggylocs/Defects4J/Defects4J_Chart_1/purify/assertBeforeMut/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java\t\
#     2020-10-20 04:37:04.942617500 -0400\n@@ -1794,7 +1794,7 @@\n         }\n     \
#     \    int index = this.plot.getIndexOf(this);\n         CategoryDataset dataset\
#     \ = this.plot.getDataset(index);\n-        if (dataset != null) {\n+        if\
#     \ (dataset == null) {\n             return result;\n         }\n         int seriesCount\
#     \ = dataset.getRowCount();
#     """
# print(string)


########################################################################
import re
txt = '''<!DOCTYPE html>
<html>
<head>
<title>Title of the document</title>
</head>
<p>
111This tutorial is provided by CodeSpeedy.
Hope you like this.
</p>
<p>
222This tutorial is provided by CodeSpeedy.
Hope you like this.
</p>
<p>
333This tutorial is provided by CodeSpeedy.
Hope you like this.
</p>
</html>'''
x = re.findall("<p>.*<p>", txt, re.DOTALL) #DOTALL MULTILINE
print(x)


a = "[as]kfjaklfas"
print(a.find("[as]"))

# print("====================")
# result = re.search("<p>.*</p>", txt, re.DOTALL)
# print(result.group())
# print("====================")
# print(result.groupdict())

########################################################################
# from utils import read_file_to_str
# bugFile = "/mnt/purify/repairDir/Purify_Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java"
# string = read_file_to_str(bugFile)
# print()