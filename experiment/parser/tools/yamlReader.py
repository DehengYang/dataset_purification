import yaml



def readYamlFile():
    with open('data.yml') as f:    
        data = yaml.load(f, Loader=yaml.FullLoader)
        print(data)


"""
data_dict = {
        'src folder' : src_folder_dict,
        'time cost of purification' : time_cost_dict,
        'test (cases)' : failing_test_dict,
        'purify' : purify_dict,
        'covered lines by failing cases' : covered_line_dict,
        'uncovered lines by failing cases' : not_covered_line_list,
        'mutant test result': mutant_result_dict,
        'patch' : patch_dict,
        'chunks' : chunk_dict
    }
    
"""


if __name__ == "__main__":
    readYamlFile()