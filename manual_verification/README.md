# 1. Manual verification on the purified human-written patches

The results of our manual verification are now available for researchers or potential users to check the validity of our verification results. 

- [1. Manual verification on the purified human-written patches](#1-manual-verification-on-the-purified-human-written-patches)
  - [1.1. Some examples](#11-some-examples)
  - [1.2. Other findings during verification](#12-other-findings-during-verification)

## 1.1. Some examples

It is noteworthy that during the manual verification, we find that DEPTest has the ability to effectively identify the complicated code refactorings tangled in human-written patches, while it might be difficult and time-consuming for us human developers to identify such cases. Here we would like to share some of such cases with you, to show the potential of DEPTest in assisting developers in the accurate dataset construction. **The human-machine interaction, where our DEPTest can provide fruitful information to dataset constructors, might be a good choice for dataset construction.**

+ Time 2: The second chunk in the human-written patch is identified as irrelevant by DEPTest, and is further identified as irrelevant (i.e., code refactoring) too by the authors. However, to precisely figure out this, we spent several hours to debug and understand the buggy program, as also discussed in our paper.
+ Closure 87: The replacement of `return NodeUtil.isExpressionNode(n.getFirstChild());` (line 522 in buggy program) with `return true;` (line 543 in fixed version) is a refactoring, and this is identified as irrelevant by DEPTest. In our manual verification, we spent more time than the DEPTest by carefully checking the `NodeUtil.isExpressionNode` method and the buggy context, and also identified it as irrelevant.

## 1.2. Other findings during verification

We also find the possibly incorrect purification performed by constructors, which may pose a threat to the reliability of the dataset.

**This fact observed by us also calls for the combination of the manual purification (by dataset constructors) and the automated purification (e.g., by our automated tool DEPTest) approaches, which might complement each other.**

+ Closure 22: we compared the [original bug fixing commit](https://github.com/google/closure-compiler/commit/43a55234ef122a1ed98681ce0350506207b878d5) with [Defects4J human-written patch](https://github.com/program-repair/defects4j-dissection/commit/1633d83). We can understand constructors kindly did some refactorings to the bug fix to make it simpler, but we don't understand why the `boolean isResultUsed = NodeUtil.isExpressionResultUsed(n);` and the addition of `if (!isResultUsed &&` are manually purified by constructors.

+ Closure 35: we compared the [original bug fixing commit](https://github.com/google/closure-compiler/commit/22784dc96c391b01692ce686eb93b9aa0ef74ede) with [Defects4J human-written patch](https://github.com/program-repair/defects4j-dissection/commit/a071a23). 4 code change chunks in the original human-written patch is purified by Defects4J constructors. That is, they assume that the method `public void matchConstraint(ObjectType contraint) {}` is already defined in the buggy version. However, the fact is that it is introduced by the bug fix in the original bug fixing commit.

+ Closure 76: we compared the [original bug fixing commit](https://github.com/google/closure-compiler/commit/78b01c3a435cd175ce3ee70f97b2f8faac342cdc) with [Defects4J human-written patch](https://github.com/program-repair/defects4j-dissection/commit/9da6ec0). Constructors made the patch simpler by adding part of the bug fix (e.g., `the case Token.OR:`, `case Token.AND:` into the buggy version. Actually, the buggy project did not have such code before the bug fix. We tend to think that this might provide "misleading prior knowledge", which actually does not exist in the buggy version, to APR techniques.
