from utils import *

import os

import collections

# plot
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns

def plot_reduced_lines():
    # first read data
    reduced_data_path = os.path.join(cur_dir, "result_data", "reduced_lines.txt")
    reduced_data_lines = read_file(reduced_data_path)

    plt.figure(figsize = (20, 10))
    fsize = 14
    
    reduced_data_array = np.array(reduced_data_lines).astype(np.float)
    # plt.hist(reduced_data_array, bins=np.arange(reduced_data_array.min(), reduced_data_array.max()+1))
    ax = sns.histplot(data=reduced_data_array, discrete = True) #, fill = True , fill=False
    counter = dict(collections.Counter(reduced_data_array))

    print(reduced_data_array.sum())
    print(len(reduced_data_array))
    print(reduced_data_array.sum()/len(reduced_data_array))

    pos = np.arange(1, 54)
    plt.xticks(pos, pos)
    for i in range(len(pos)):
        # print(i, counter[i + 1])
        if i + 1 not in counter.keys():
            continue 
        plt.text(pos[i], counter[i + 1] + 0.5, "{}".format(counter[i + 1]),  fontsize=12, ha = 'center', va = 'center') #, bbox=props)
    plt.title("Distributions of reduced lines", fontsize = 20)
    ax.set_xlabel("number of reduced lines", fontsize = fsize)
    ax.set_ylabel("occurrences", fontsize = fsize)
    plt.show()


if __name__ == "__main__":
    plot_reduced_lines()