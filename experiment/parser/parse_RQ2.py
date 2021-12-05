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

from yamlable import YamlAble, yaml_info
sys.path.append('.\\tools')
from tools.info import Action

plt.rc('font',family='Times New Roman')

from utils import *

# get path of current py file and directory
cur_file  = os.path.abspath(__file__)
cur_dir = cur_file.rsplit("/", 1)[0] 

result_dir = os.path.join(cur_dir, "RQ2-result")


def parse_and_plt():
    yml_df = get_yml_df()

    
    yml_df['oriLines'] = yml_df['oriLines'].astype(float)
    yml_df['purLines'] = yml_df['purLines'].astype(float)
    yml_df['oriChunks'] = yml_df['oriChunks'].astype(float)
    yml_df['purChunks'] = yml_df['purChunks'].astype(float)
    yml_df['timeCost'] = yml_df['timeCost'].astype(float)

    # plot
    fig = plt.figure(figsize=(5, 4.5))
    plt.style.use('seaborn-darkgrid')
    palette = plt.get_cmap('Set1') # 'Set1'
    palette_cnt = 0

    # yml_df = yml_df.sort_values(by=['oriLines'])
    yml_df = yml_df.sort_values(by=['oriChunks'])
    yml_df = yml_df.reset_index(drop=True)

    # a = yml_df.index + 1

    # df = pd.melt(yml_df[['oriLines', 'purLines']])
    df = pd.melt(yml_df[['oriChunks', 'purChunks']])

    xticks = []
    for i in range(2):
        for j in yml_df.index:
            xticks.append(j + 1)

    sns.lineplot(x=xticks, y='value', hue='variable', data=df) #, ['Year'] #(df.index + 1)

    # sns.lineplot(x=yml_df.index + 1, y='oriLines', data=yml_df)
    plt.tight_layout()
    plt.savefig(os.path.join(result_dir, 'RQ2-1.pdf'))  
    plt.show()


    yml_df_path = os.path.join(result_dir, "yml_df.txt")
    # df_tmp = yml_df.drop
    write_line_to_file(yml_df_path,  yml_df.to_string(), append = False) #.round(2)

    yml_df = yml_df.drop(index=(yml_df.loc[(yml_df['oriLines'] > 3)].index))[['oriLines', 'purLines']]
    print(yml_df.shape)
    yml_df = yml_df.drop(index=(yml_df.loc[(yml_df['oriLines'] == yml_df['purLines'])].index))[['oriLines', 'purLines']]
    print(yml_df.shape)

    return

    # yml_df = yml_df[['oriLines', 'purLines']]
    # yml_df = yml_df.drop(index=(yml_df.loc[(yml_df['oriLines']==yml_df['purLines'])].index))[['oriLines', 'purLines']]
    # yml_df['oriLines'] = yml_df['oriLines'].astype(float)
    # yml_df['purLines'] = yml_df['purLines'].astype(float)
    # flierprops = dict(marker='o', markerfacecolor='w', markersize=5, markeredgewidth = 1.5) #, linestyle='none') #'0.75' linestyle='none'alpha=0
    # # sns.boxplot(x="type", y="lines", data=yml_df, order=["ori", "pur"])
    # ax = sns.boxplot(x="variable", y="value", data=pd.melt(yml_df), width=.7, flierprops=flierprops)#, color = "whitesmoke") #, orient = "h") #width=.6,   , color = palette(palette_cnt)


    # flierprops = dict(markerfacecolor='0.75', markersize=5,
    #             linestyle='none')
    # seaborn.boxplot(x="centrality", y="score", hue="model", data=data,
    #                 flierprops=flierprops)

    # Add transparency to colors
    # for patch in ax.artists:
    #     r, g, b, a = patch.get_facecolor()
    #     patch.set_facecolor((r, g, b, .3))


    fs = 10
    # ax.set_title(title, fontsize = fs)
    ax.set_xlabel("")
    # ax.set_ylabel("{}".format(r'$|\delta|$'), fontsize = fs)#, family = 'sans-serif')
    ax.set_ylabel("{}".format('Patch size by number of lines'), fontsize = fs)#, family = 'sans-serif')
    ax.set_xticklabels(['Original', 'Purified'], fontsize = fs)   # , family = 'sans-serif'
    # ax.set_yticklabels(ax.get_yticks(), size = fs)
    # ax.set_ylim([0, 60])
    # ax.set_yticklabels(ax.get_yticks(), size = fs - 2)
    plt.yticks(fontsize=fs - 2)

    plt.setp(ax.artists, edgecolor = 'k', facecolor='whitesmoke')
    plt.setp(ax.lines, color='k')
    # for p, artist in enumerate(ax.artists):
    #     # artist.set_edgecolor('blue')
    #     for q in range(p*6, p*6+6):
    #         line = ax.lines[q]
    #         line.set_color('pink')

    adjust_box_widths(fig, 0.6)
    plt.tight_layout()
    plt.savefig(os.path.join(result_dir, 'RQ1-1.pdf'))  
    plt.show()

    # analysis
    # statistical_analysis(yml_df)

# def statistical_analysis(df, repair_tools, data_type):
#     print("-----start------")
#     # Kruskal-Wallis H-test
#     from numpy.random import seed
#     from numpy.random import randn
#     from scipy.stats import kruskal, wilcoxon
#     medians = df.groupby(['tool'])[data_type].median()#.values
#     means = df.groupby(['tool'])[data_type].mean()#.values
#     print("medians--")
#     print(medians)
#     # print(df.groupby(['tool'])['flimit'])
#     print("means--")
#     print(means)
#     for tool in repair_tools:
#         tool_flimit = df[df['tool'] == tool][data_type].tolist()
#         for compare_tool in repair_tools:
#             compare_tool_flimit = df[df['tool'] == compare_tool][data_type].tolist()
#             stat, p = kruskal(tool_flimit, compare_tool_flimit)
#             effect_size = (stat - 2 + 1)/(len(tool_flimit) + len(compare_tool_flimit) - 2)
#             # print('Statistics=%.3f, p=%.3f, effect_size=%.3f for %s %s' % (stat, p, effect_size, tool, compare_tool))
#             print('{} {} p={}, effect_size={}'.format(tool, compare_tool, p, effect_size))
#     print("-----end------")


import numpy as np
from matplotlib.patches import PathPatch

def adjust_box_widths(g, fac):
    """
    Adjust the widths of a seaborn-generated boxplot.
    """

    # iterating through Axes instances
    for ax in g.axes:

        # iterating through axes artists:
        for c in ax.get_children():

            # searching for PathPatches
            if isinstance(c, PathPatch):
                # getting current width of box:
                p = c.get_path()
                verts = p.vertices
                verts_sub = verts[:-1]
                xmin = np.min(verts_sub[:, 0])
                xmax = np.max(verts_sub[:, 0])
                xmid = 0.5*(xmin+xmax)
                xhalf = 0.5*(xmax - xmin)

                # setting new width of box
                xmin_new = xmid-fac*xhalf
                xmax_new = xmid+fac*xhalf
                verts_sub[verts_sub[:, 0] == xmin, 0] = xmin_new
                verts_sub[verts_sub[:, 0] == xmax, 0] = xmax_new

                # setting new width of median line
                for l in ax.lines:
                    if np.all(l.get_xdata() == [xmin, xmax]):
                        l.set_xdata([xmin_new, xmax_new])

def statistical_analysis(df=None):
    print("-----start------")
    # Kruskal-Wallis H-test
    from numpy.random import seed
    from numpy.random import randn
    from scipy.stats import kruskal, wilcoxon, ranksums
    # medians = df.groupby(['tool'])[data_type].median()#.values
    # means = df.groupby(['tool'])[data_type].mean()#.values
    # print("medians--")
    # print(medians)
    # # print(df.groupby(['tool'])['flimit'])
    # print("means--")
    # print(means)
    tool_flimit = df['oriLines'].tolist()
    tool_flimit2 = df['purLines'].tolist()
    # stat, p = kruskal(tool_flimit2, tool_flimit)
    stat, p = wilcoxon(tool_flimit, tool_flimit2)
    effect_size = 0
    # stat, p = ranksums(tool_flimit, tool_flimit2)
    # effect_size = (stat - 2 + 1)/(len(tool_flimit) + len(tool_flimit2) - 2)

    print('stat={}, p={}, effect_size={}'.format(stat, p, effect_size))

    # stat, p = ranksums(tool_flimit, tool_flimit2)
    N = len(tool_flimit)
    M = len(tool_flimit2)
    stat = 259 #simfix #239 tbar

    a = [3.0, 2.0, 14.0, 2.0, 4.0, 2.0, 1.0, 2.0, 2.0, 2.0, 35.0, 14.0, 6.0, 8.0, 2.0, 2.0, 2.0, 8.0, 2.0, 8.0, 4.0, 4.0, 9.0, 17.0, 15.0, 2.0, 4.0, 3.0, 4.0, 11.0, 6.0, 19.0, 2.0, 3.0, 5.0, 12.0, 4.0, 8.0, 17.0, 3.0, 7.0, 3.0, 6.0, 2.0, 11.0, 54.0, 12.0, 2.0, 3.0, 3.0, 5.0, 4.0, 10.0, 4.0, 3.0, 27.0, 6.0, 6.0, 4.0, 4.0, 2.0, 6.0, 20.0, 3.0, 6.0, 10.0, 4.0, 2.0, 2.0, 3.0, 4.0, 3.0, 6.0, 2.0, 4.0, 2.0, 19.0, 21.0, 2.0, 2.0, 2.0, 3.0, 4.0, 2.0, 9.0, 8.0, 4.0, 2.0, 2.0, 5.0, 5.0, 4.0, 23.0, 4.0, 4.0, 6.0, 2.0, 7.0, 3.0, 2.0, 35.0, 9.0, 19.0, 3.0, 6.0, 33.0, 4.0, 8.0, 6.0, 8.0, 2.0, 2.0, 17.0, 2.0, 8.0, 4.0, 8.0, 26.0, 12.0, 15.0, 9.0, 7.0, 19.0, 1.0, 2.0, 9.0, 2.0, 5.0, 9.0, 2.0, 9.0, 2.0, 8.0, 5.0, 1.0, 8.0, 6.0, 4.0, 2.0, 3.0, 24.0, 2.0, 6.0, 10.0, 8.0, 9.0, 2.0, 12.0, 7.0, 17.0, 18.0, 8.0, 4.0, 2.0, 4.0, 12.0, 11.0, 16.0, 17.0, 5.0, 11.0, 2.0, 3.0, 4.0, 3.0, 2.0, 3.0, 16.0, 2.0, 2.0, 4.0, 2.0, 6.0, 3.0, 2.0, 9.0, 22.0, 2.0, 6.0, 4.0, 4.0, 9.0, 4.0, 22.0, 2.0, 16.0, 7.0, 7.0, 3.0, 4.0, 9.0, 10.0, 2.0, 6.0, 3.0, 7.0, 3.0, 11.0, 24.0, 3.0, 2.0, 21.0, 5.0, 2.0, 2.0, 6.0, 43.0, 6.0, 7.0, 7.0, 13.0, 4.0, 2.0, 12.0, 3.0, 6.0, 2.0, 3.0, 22.0, 14.0, 12.0, 7.0, 18.0, 4.0, 9.0, 1.0, 2.0, 5.0, 13.0, 23.0, 2.0, 2.0, 2.0, 4.0, 2.0, 3.0, 2.0, 21.0, 4.0, 7.0, 1.0, 16.0, 4.0, 9.0, 2.0, 10.0, 3.0, 17.0, 4.0, 2.0, 2.0, 14.0, 6.0, 19.0, 2.0, 11.0, 23.0, 6.0, 4.0, 40.0, 13.0, 2.0, 21.0, 29.0, 4.0, 14.0, 23.0, 8.0, 4.0, 2.0, 11.0, 6.0, 2.0, 1.0, 1.0, 46.0, 3.0, 6.0, 3.0, 2.0, 2.0, 4.0, 5.0, 4.0, 5.0, 6.0, 2.0, 1.0, 4.0, 10.0, 22.0, 4.0, 6.0, 12.0, 17.0, 2.0, 2.0, 3.0, 6.0, 1.0, 7.0, 4.0, 13.0, 28.0, 2.0, 10.0, 8.0, 3.0, 55.0, 48.0, 11.0, 3.0, 16.0, 2.0, 2.0, 12.0, 3.0, 5.0, 8.0, 4.0, 2.0, 7.0, 5.0, 3.0, 30.0, 2.0, 2.0, 2.0, 8.0, 2.0, 5.0, 30.0, 2.0, 30.0, 5.0, 7.0, 5.0, 4.0, 6.0, 2.0, 3.0, 8.0, 9.0, 2.0, 8.0, 2.0, 7.0, 2.0, 3.0, 2.0, 2.0, 8.0, 14.0, 9.0, 19.0, 2.0, 9.0, 2.0, 5.0, 34.0, 31.0, 8.0, 2.0, 3.0, 10.0, 10.0, 3.0, 4.0, 8.0, 3.0, 4.0, 3.0, 6.0, 3.0, 7.0, 7.0, 11.0, 20.0, 4.0, 3.0, 12.0, 12.0, 5.0, 11.0, 2.0, 2.0, 4.0, 13.0, 18.0, 2.0, 1.0, 39.0, 25.0, 23.0, 14.0]
    b = [3.0, 2.0, 11.0, 2.0, 2.0, 2.0, 1.0, 2.0, 2.0, 2.0, 19.0, 6.0, 6.0, 5.0, 1.0, 2.0, 2.0, 7.0, 2.0, 2.0, 4.0, 4.0, 9.0, 12.0, 15.0, 2.0, 3.0, 3.0, 4.0, 10.0, 6.0, 15.0, 2.0, 2.0, 5.0, 12.0, 4.0, 1.0, 17.0, 3.0, 3.0, 3.0, 4.0, 2.0, 10.0, 1.0, 10.0, 1.0, 3.0, 2.0, 5.0, 4.0, 4.0, 4.0, 3.0, 19.0, 5.0, 2.0, 2.0, 4.0, 2.0, 3.0, 10.0, 3.0, 3.0, 7.0, 3.0, 2.0, 2.0, 3.0, 2.0, 3.0, 6.0, 2.0, 4.0, 1.0, 17.0, 21.0, 2.0, 2.0, 2.0, 1.0, 4.0, 2.0, 9.0, 7.0, 3.0, 2.0, 2.0, 3.0, 5.0, 2.0, 23.0, 3.0, 4.0, 6.0, 2.0, 2.0, 1.0, 2.0, 34.0, 9.0, 3.0, 3.0, 4.0, 24.0, 3.0, 8.0, 5.0, 8.0, 2.0, 2.0, 11.0, 2.0, 6.0, 4.0, 8.0, 21.0, 3.0, 13.0, 6.0, 5.0, 18.0, 1.0, 1.0, 9.0, 2.0, 3.0, 7.0, 2.0, 9.0, 2.0, 4.0, 5.0, 1.0, 2.0, 6.0, 4.0, 2.0, 3.0, 15.0, 2.0, 6.0, 8.0, 8.0, 9.0, 2.0, 10.0, 7.0, 6.0, 7.0, 6.0, 1.0, 1.0, 2.0, 11.0, 7.0, 16.0, 11.0, 5.0, 9.0, 2.0, 3.0, 4.0, 3.0, 2.0, 3.0, 16.0, 2.0, 2.0, 4.0, 2.0, 6.0, 3.0, 2.0, 8.0, 19.0, 2.0, 6.0, 2.0, 4.0, 8.0, 3.0, 16.0, 2.0, 16.0, 7.0, 7.0, 3.0, 4.0, 9.0, 9.0, 2.0, 6.0, 3.0, 7.0, 3.0, 9.0, 24.0, 3.0, 2.0, 15.0, 3.0, 2.0, 2.0, 3.0, 23.0, 5.0, 6.0, 5.0, 13.0, 4.0, 2.0, 12.0, 3.0, 6.0, 2.0, 2.0, 9.0, 12.0, 10.0, 7.0, 10.0, 4.0, 2.0, 1.0, 2.0, 5.0, 10.0, 22.0, 2.0, 2.0, 2.0, 2.0, 2.0, 3.0, 2.0, 21.0, 4.0, 5.0, 1.0, 14.0, 4.0, 9.0, 1.0, 8.0, 3.0, 17.0, 4.0, 2.0, 2.0, 13.0, 6.0, 15.0, 2.0, 10.0, 1.0, 3.0, 4.0, 40.0, 13.0, 2.0, 21.0, 29.0, 4.0, 2.0, 23.0, 8.0, 4.0, 2.0, 6.0, 4.0, 2.0, 1.0, 1.0, 24.0, 3.0, 6.0, 3.0, 2.0, 2.0, 4.0, 5.0, 4.0, 5.0, 4.0, 2.0, 1.0, 4.0, 7.0, 16.0, 2.0, 6.0, 9.0, 16.0, 2.0, 2.0, 3.0, 6.0, 1.0, 7.0, 2.0, 10.0, 23.0, 1.0, 10.0, 7.0, 3.0, 19.0, 44.0, 9.0, 3.0, 16.0, 2.0, 2.0, 9.0, 3.0, 4.0, 7.0, 3.0, 2.0, 7.0, 4.0, 3.0, 10.0, 2.0, 2.0, 2.0, 5.0, 2.0, 5.0, 12.0, 2.0, 30.0, 5.0, 2.0, 5.0, 3.0, 4.0, 2.0, 3.0, 8.0, 9.0, 2.0, 3.0, 2.0, 4.0, 2.0, 3.0, 2.0, 2.0, 7.0, 13.0, 5.0, 16.0, 2.0, 5.0, 2.0, 4.0, 34.0, 23.0, 3.0, 2.0, 3.0, 2.0, 10.0, 3.0, 4.0, 6.0, 3.0, 3.0, 3.0, 5.0, 3.0, 4.0, 6.0, 10.0, 20.0, 4.0, 3.0, 11.0, 11.0, 5.0, 11.0, 2.0, 2.0, 3.0, 12.0, 16.0, 1.0, 1.0, 28.0, 23.0, 15.0, 14.0]
    stat = 1.6655e+05
    A = (stat/N - (N+1)/2)/M
    # if (A<0):
    #     A = (stat/M - (M+1)/2)/N
    print('p={}, effect_size={}'.format(p, A))
    # print("-----end------")

    print(tool_flimit)
    print(tool_flimit2)
    print("median: {}".format(np.median(tool_flimit)))
    print("median: {}".format(np.median(tool_flimit2)))
    print("mean: {}".format(np.mean(tool_flimit)))
    print("mean: {}".format(np.mean(tool_flimit2)))

def statistical_analysis_manual():
    print("-----start------")
    # Kruskal-Wallis H-test
    from numpy.random import seed
    from numpy.random import randn
    from scipy.stats import kruskal, wilcoxon, ranksums
    a1 = [3.0, 2.0, 14.0, 2.0, 4.0, 2.0, 1.0, 2.0, 2.0, 2.0, 35.0, 14.0, 6.0, 8.0, 2.0, 2.0, 2.0, 8.0, 2.0, 8.0, 4.0, 4.0, 9.0, 17.0, 15.0, 2.0, 4.0, 3.0, 4.0, 11.0, 6.0, 19.0, 2.0, 3.0, 5.0, 12.0, 4.0, 8.0, 17.0, 3.0, 7.0, 3.0, 6.0, 2.0, 11.0, 54.0, 12.0, 2.0, 3.0, 3.0, 5.0, 4.0, 10.0, 4.0, 3.0, 27.0, 6.0, 6.0, 4.0, 4.0, 2.0, 6.0, 20.0, 3.0, 6.0, 10.0, 4.0, 2.0, 2.0, 3.0, 4.0, 3.0, 6.0, 2.0, 4.0, 2.0, 19.0, 21.0, 2.0, 2.0, 2.0, 3.0, 4.0, 2.0, 9.0, 8.0, 4.0, 2.0, 2.0, 5.0, 5.0, 4.0, 23.0, 4.0, 4.0, 6.0, 2.0, 7.0, 3.0, 2.0, 35.0, 9.0, 19.0, 3.0, 6.0, 33.0, 4.0, 8.0, 6.0, 8.0, 2.0, 2.0, 17.0, 2.0, 8.0, 4.0, 8.0, 26.0, 12.0, 15.0, 9.0, 7.0, 19.0, 1.0, 2.0, 9.0, 2.0, 5.0, 9.0, 2.0, 9.0, 2.0, 8.0, 5.0, 1.0, 8.0, 6.0, 4.0, 2.0, 3.0, 24.0, 2.0, 6.0, 10.0, 8.0, 9.0, 2.0, 12.0, 7.0, 17.0, 18.0, 8.0, 4.0, 2.0, 4.0, 12.0, 11.0, 16.0, 17.0, 5.0, 11.0, 2.0, 3.0, 4.0, 3.0, 2.0, 3.0, 16.0, 2.0, 2.0, 4.0, 2.0, 6.0, 3.0, 2.0, 9.0, 22.0, 2.0, 6.0, 4.0, 4.0, 9.0, 4.0, 22.0, 2.0, 16.0, 7.0, 7.0, 3.0, 4.0, 9.0, 10.0, 2.0, 6.0, 3.0, 7.0, 3.0, 11.0, 24.0, 3.0, 2.0, 21.0, 5.0, 2.0, 2.0, 6.0, 43.0, 6.0, 7.0, 7.0, 13.0, 4.0, 2.0, 12.0, 3.0, 6.0, 2.0, 3.0, 22.0, 14.0, 12.0, 7.0, 18.0, 4.0, 9.0, 1.0, 2.0, 5.0, 13.0, 23.0, 2.0, 2.0, 2.0, 4.0, 2.0, 3.0, 2.0, 21.0, 4.0, 7.0, 1.0, 16.0, 4.0, 9.0, 2.0, 10.0, 3.0, 17.0, 4.0, 2.0, 2.0, 14.0, 6.0, 19.0, 2.0, 11.0, 23.0, 6.0, 4.0, 40.0, 13.0, 2.0, 21.0, 29.0, 4.0, 14.0, 23.0, 8.0, 4.0, 2.0, 11.0, 6.0, 2.0, 1.0, 1.0, 46.0, 3.0, 6.0, 3.0, 2.0, 2.0, 4.0, 5.0, 4.0, 5.0, 6.0, 2.0, 1.0, 4.0, 10.0, 22.0, 4.0, 6.0, 12.0, 17.0, 2.0, 2.0, 3.0, 6.0, 1.0, 7.0, 4.0, 13.0, 28.0, 2.0, 10.0, 8.0, 3.0, 55.0, 48.0, 11.0, 3.0, 16.0, 2.0, 2.0, 12.0, 3.0, 5.0, 8.0, 4.0, 2.0, 7.0, 5.0, 3.0, 30.0, 2.0, 2.0, 2.0, 8.0, 2.0, 5.0, 30.0, 2.0, 30.0, 5.0, 7.0, 5.0, 4.0, 6.0, 2.0, 3.0, 8.0, 9.0, 2.0, 8.0, 2.0, 7.0, 2.0, 3.0, 2.0, 2.0, 8.0, 14.0, 9.0, 19.0, 2.0, 9.0, 2.0, 5.0, 34.0, 31.0, 8.0, 2.0, 3.0, 10.0, 10.0, 3.0, 4.0, 8.0, 3.0, 4.0, 3.0, 6.0, 3.0, 7.0, 7.0, 11.0, 20.0, 4.0, 3.0, 12.0, 12.0, 5.0, 11.0, 2.0, 2.0, 4.0, 13.0, 18.0, 2.0, 1.0, 39.0, 25.0, 23.0, 14.0]
    b1 = [3.0, 2.0, 11.0, 2.0, 2.0, 2.0, 1.0, 2.0, 2.0, 2.0, 19.0, 6.0, 6.0, 5.0, 1.0, 2.0, 2.0, 7.0, 2.0, 2.0, 4.0, 4.0, 9.0, 12.0, 15.0, 2.0, 3.0, 3.0, 4.0, 10.0, 6.0, 15.0, 2.0, 2.0, 5.0, 12.0, 4.0, 1.0, 17.0, 3.0, 3.0, 3.0, 4.0, 2.0, 10.0, 1.0, 10.0, 1.0, 3.0, 2.0, 5.0, 4.0, 4.0, 4.0, 3.0, 19.0, 5.0, 2.0, 2.0, 4.0, 2.0, 3.0, 10.0, 3.0, 3.0, 7.0, 3.0, 2.0, 2.0, 3.0, 2.0, 3.0, 6.0, 2.0, 4.0, 1.0, 17.0, 21.0, 2.0, 2.0, 2.0, 1.0, 4.0, 2.0, 9.0, 7.0, 3.0, 2.0, 2.0, 3.0, 5.0, 2.0, 23.0, 3.0, 4.0, 6.0, 2.0, 2.0, 1.0, 2.0, 34.0, 9.0, 3.0, 3.0, 4.0, 24.0, 3.0, 8.0, 5.0, 8.0, 2.0, 2.0, 11.0, 2.0, 6.0, 4.0, 8.0, 21.0, 3.0, 13.0, 6.0, 5.0, 18.0, 1.0, 1.0, 9.0, 2.0, 3.0, 7.0, 2.0, 9.0, 2.0, 4.0, 5.0, 1.0, 2.0, 6.0, 4.0, 2.0, 3.0, 15.0, 2.0, 6.0, 8.0, 8.0, 9.0, 2.0, 10.0, 7.0, 6.0, 7.0, 6.0, 1.0, 1.0, 2.0, 11.0, 7.0, 16.0, 11.0, 5.0, 9.0, 2.0, 3.0, 4.0, 3.0, 2.0, 3.0, 16.0, 2.0, 2.0, 4.0, 2.0, 6.0, 3.0, 2.0, 8.0, 19.0, 2.0, 6.0, 2.0, 4.0, 8.0, 3.0, 16.0, 2.0, 16.0, 7.0, 7.0, 3.0, 4.0, 9.0, 9.0, 2.0, 6.0, 3.0, 7.0, 3.0, 9.0, 24.0, 3.0, 2.0, 15.0, 3.0, 2.0, 2.0, 3.0, 23.0, 5.0, 6.0, 5.0, 13.0, 4.0, 2.0, 12.0, 3.0, 6.0, 2.0, 2.0, 9.0, 12.0, 10.0, 7.0, 10.0, 4.0, 2.0, 1.0, 2.0, 5.0, 10.0, 22.0, 2.0, 2.0, 2.0, 2.0, 2.0, 3.0, 2.0, 21.0, 4.0, 5.0, 1.0, 14.0, 4.0, 9.0, 1.0, 8.0, 3.0, 17.0, 4.0, 2.0, 2.0, 13.0, 6.0, 15.0, 2.0, 10.0, 1.0, 3.0, 4.0, 40.0, 13.0, 2.0, 21.0, 29.0, 4.0, 2.0, 23.0, 8.0, 4.0, 2.0, 6.0, 4.0, 2.0, 1.0, 1.0, 24.0, 3.0, 6.0, 3.0, 2.0, 2.0, 4.0, 5.0, 4.0, 5.0, 4.0, 2.0, 1.0, 4.0, 7.0, 16.0, 2.0, 6.0, 9.0, 16.0, 2.0, 2.0, 3.0, 6.0, 1.0, 7.0, 2.0, 10.0, 23.0, 1.0, 10.0, 7.0, 3.0, 19.0, 44.0, 9.0, 3.0, 16.0, 2.0, 2.0, 9.0, 3.0, 4.0, 7.0, 3.0, 2.0, 7.0, 4.0, 3.0, 10.0, 2.0, 2.0, 2.0, 5.0, 2.0, 5.0, 12.0, 2.0, 30.0, 5.0, 2.0, 5.0, 3.0, 4.0, 2.0, 3.0, 8.0, 9.0, 2.0, 3.0, 2.0, 4.0, 2.0, 3.0, 2.0, 2.0, 7.0, 13.0, 5.0, 16.0, 2.0, 5.0, 2.0, 4.0, 34.0, 23.0, 3.0, 2.0, 3.0, 2.0, 10.0, 3.0, 4.0, 6.0, 3.0, 3.0, 3.0, 5.0, 3.0, 4.0, 6.0, 10.0, 20.0, 4.0, 3.0, 11.0, 11.0, 5.0, 11.0, 2.0, 2.0, 3.0, 12.0, 16.0, 1.0, 1.0, 28.0, 23.0, 15.0, 14.0]

    a = []
    b = []
    for cnt in range(len(a1)):
        if a1[cnt] != b1[cnt]:
            a.append(a1[cnt])
            b.append(b1[cnt])
    print("len: " + str(len(a)))
    print(a)
    print(b)
    

    stat, p = wilcoxon(a, b) #, alternative = "greater"
    effect_size = 0
    # stat, p = ranksums(tool_flimit, tool_flimit2)
    # effect_size = (stat - 2 + 1)/(len(tool_flimit) + len(tool_flimit2) - 2)

    print('stat={}, p={}, effect_size={}'.format(stat, p, effect_size))


# p=1.1353347226364097e-28

    # stat, p = ranksums(tool_flimit, tool_flimit2)
    N = len(a)
    M = len(b)
    # stat = 259 #simfix #239 tbar
    
    stat = 30470 #22180 # 1.4589e+05 #1.6655e+05
    A = (stat/N - (N+1)/2)/M
    # if (A<0):
    #     A = (stat/M - (M+1)/2)/N
    print('p={}, effect_size={}'.format(p, A))
    # print("-----end------")

    # print(tool_flimit)
    # print(tool_flimit2)
    print("median: {}".format(np.median(a)))
    print("median: {}".format(np.median(b)))
    print("mean: {}".format(np.mean(a)))
    print("mean: {}".format(np.mean(b)))


def get_yml_df():
    # read yaml
    yml_dir = os.path.join(cur_dir, "results")
    yml_file_list = list_all_files(yml_dir)
    yml_df = pd.DataFrame(columns=['bug_id', 'oriLines', 'purLines', 'oriChunks', 'purChunks', "timeCost"])
    # yml_df = pd.DataFrame(columns=['type', 'bug_id', 'lines', 'chunks'])
    for yml_file_path in yml_file_list:
        yml_dict = read_yaml(yml_file_path)
        yml_df.loc[yml_df.shape[0]] = [yml_dict['1 bug_id'], yml_dict['6 reduced lines']['ori_patch_lines'], \
            yml_dict['6 reduced lines']['delta_patch_lines'], yml_dict['7 reduced chunks']['original'], \
                yml_dict['7 reduced chunks']['purifiy'], yml_dict['4 time cost of purification']['project execution (Main method)']]

        # yml_df.loc[yml_df.shape[0]] = ['ori', yml_dict['1 bug_id'], yml_dict['6 reduced lines']['ori_patch_lines'], \
        #         yml_dict['7 reduced chunks']['original']]
        # yml_df.loc[yml_df.shape[0]] = ['pur', yml_dict['1 bug_id'], yml_dict['6 reduced lines']['delta_patch_lines'], \
        #     yml_dict['7 reduced chunks']['purifiy']]

    return yml_df




if __name__ == "__main__":
    parse_and_plt()
    # statistical_analysis_manual()