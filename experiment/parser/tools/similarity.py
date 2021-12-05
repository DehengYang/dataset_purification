import math
import re
from collections import Counter

WORD = re.compile(r"\w+")

"""
refer to: https://stackoverflow.com/questions/15173225/calculate-cosine-similarity-given-2-sentence-strings
"""


def get_cosine(vec1, vec2):
    intersection = set(vec1.keys()) & set(vec2.keys())
    numerator = sum([vec1[x] * vec2[x] for x in intersection])

    print([vec1[x] ** 2 for x in list(vec1.keys())])

    sum1 = sum([vec1[x] ** 2 for x in list(vec1.keys())])
    sum2 = sum([vec2[x] ** 2 for x in list(vec2.keys())])
    denominator = math.sqrt(sum1) * math.sqrt(sum2)

    if not denominator:
        return 0.0
    else:
        return float(numerator) / denominator


def text_to_vector(text):
    words = WORD.findall(text)
    return Counter(words)

def text_list_to_vector(lines):
    all_words = []
    for line in lines:
        action_str = re.sub("-+", "", line[0 : line.find("@@")])
        action = action_str.split(" ")
        words = WORD.findall(line[line.find("@@") : ])

        # token_words = []
        # for word in words:
        #     token_words.append("token")

        all_words.extend(action)
        all_words.extend(words)
    return Counter(all_words)

def get_similarity(str1, str2):
    # text1 = "This is a foo bar sentence ."
    # text2 = "This sentence is similar to a foo bar sentence ."

    # approach 1
    vector1 = text_to_vector(str1)
    vector2 = text_to_vector(str2)
    cosine = get_cosine(vector1, vector2)

    # my approach
    # vector1 = Counter(list(str1))
    # vector2 = Counter(list(str2))
    # cosine = get_cosine(vector1, vector2)


    print("Cosine:", cosine)
    return cosine

def get_similarity_for_lists(str1, str2):
    vector1 = text_list_to_vector(str1)
    vector2 = text_list_to_vector(str2)
    cosine = get_cosine(vector1, vector2)

    print("Cosine:", cosine)
    return cosine


# refer to: Find the similarity metric between two strings https://stackoverflow.com/questions/17388213/find-the-similarity-metric-between-two-strings
def get_similarity_for_str_2(str1, str2):
    from difflib import SequenceMatcher
    return SequenceMatcher(None, str1, str2).ratio()