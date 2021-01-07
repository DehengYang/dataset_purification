
package apr.purify.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.ITree;

import apr.purify.utils.FileUtil;
import edu.lu.uni.serval.gumtree.GumTreeComparer;
import edu.lu.uni.serval.gumtree.regroup.CUCreator;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalRegrouper;
import edu.lu.uni.serval.gumtree.regroup.NodeChecker;
import edu.lu.uni.serval.utils.ListSorter;

public class PatchParserUtil {
	final static Logger logger = LoggerFactory.getLogger(PatchParserUtil.class);
	
	public static Map<Chunk, List<HierarchicalActionSet>> chunkActionMap = new HashMap<>();
	
	public static List<HierarchicalActionSet> parseChangedSourceCodeWithGumTree(String prevFilePath, String revFilePath) {
		return parseChangedSourceCodeWithGumTree(new File(prevFilePath), new File(revFilePath));
	}
	
	public static List<HierarchicalActionSet> parseChangedSourceCodeWithGumTree(File prevFile, File revFile) {
		List<HierarchicalActionSet> actionSets = new ArrayList<>();
		
		List<Action> gumTreeResults = new GumTreeComparer().compareTwoFilesWithGumTree(prevFile, revFile);
		if (gumTreeResults == null) {
			return null;
		} else if (gumTreeResults.size() == 0){
			return actionSets;
		} else {
			
			List<HierarchicalActionSet> allActionSets = new HierarchicalRegrouper().regroupGumTreeResults(gumTreeResults);
			
			ListSorter<HierarchicalActionSet> sorter = new ListSorter<>(allActionSets);
			actionSets = sorter.sortAscending();
			

			return actionSets;
		}
		
		
	}
	
	
	public static Map<Chunk, List<HierarchicalActionSet>> getChunkActionMap(List<Chunk> chunks, List<HierarchicalActionSet> actionSets, String prevFilePath, String revFilePath) {
		return getChunkActionMap(chunks, actionSets, new File(prevFilePath), new File(revFilePath));
	}
	public static Map<Chunk, List<HierarchicalActionSet>> getChunkActionMap(List<Chunk> chunks, List<HierarchicalActionSet> actionSets, File prevFile, File revFile) {
		chunkActionMap.clear();
		
		CUCreator cuCreator = new CUCreator();
		CompilationUnit prevUnit = cuCreator.createCompilationUnit(prevFile);
		CompilationUnit revUnit = cuCreator.createCompilationUnit(revFile);
		if (prevUnit == null || revUnit == null) {
			FileUtil.raiseException("prevUnit == null || revUnit == null");
		}

		
		for (Chunk chunk: chunks) {
			chunk.resetByLines(); 
			
			int buggyStart = -1;
			int fixedStart = -1;
			int buggyRange = -1;
			int fixedRnage = -1;
			int buggyEnd = -1;
			int fixedEnd = -1; 
			if (! chunk.getDelLines().isEmpty()){
				buggyStart = chunk.getDelLines().get(0).getLeft();
				buggyRange = chunk.getDelLines().size();
				buggyEnd = buggyStart + (buggyRange == 0 ? 0 : (buggyRange - 1));
			}
			if (! chunk.getAddLines().isEmpty()){
				fixedStart = chunk.getAddLines().get(0).getLeft();
				fixedRnage = chunk.getAddLines().size();
				fixedEnd = fixedStart + (fixedRnage == 0 ? 0 : (fixedRnage - 1));
			}

			HierarchicalActionSet singlePatch = new HierarchicalActionSet();
			singlePatch.setAstNodeType("");

			
			for (HierarchicalActionSet actionSet : actionSets) {
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				int actionBugEndLine = actionSet.getBugEndLineNum();
				int actionFixStartLine = actionSet.getFixStartLineNum();
				int actionFixEndLine = actionSet.getFixEndLineNum();


				String actionStr = actionSet.getActionString();
				if (actionStr.startsWith("INS")) {
					if (fixedStart <= actionFixEndLine && actionFixStartLine <= fixedEnd) {
						singlePatch = addToPatchesMap(actionSet, singlePatch, chunk);
					}
				} else {
					if (buggyStart <= actionBugEndLine && actionBugStartLine <= buggyEnd) {
						singlePatch = addToPatchesMap(actionSet, singlePatch, chunk);
					}
				}
			}

			if (singlePatch.getSubActions().size() > 0) {
				addToPatchesMap(singlePatch, chunk);
			}




		}	
		
		return chunkActionMap;
	}

	private static HierarchicalActionSet addToPatchesMap(HierarchicalActionSet actionSet, HierarchicalActionSet singlePatch, Chunk chunk) {
		if (!isContained(actionSet)) {
			if (actionSet.getAstNodeType().equals("FieldDeclaration")) {
				if (singlePatch.getSubActions().size() > 0) {
					addToPatchesMap(singlePatch, chunk);
					singlePatch = new HierarchicalActionSet();
				}
				singlePatch.setAstNodeType("FieldDeclaration");
			} else {
				if (singlePatch.getAstNodeType().equals("FieldDeclaration")) {
					addToPatchesMap(singlePatch, chunk);
					singlePatch = new HierarchicalActionSet();
					singlePatch.setAstNodeType("");
				}
			}
			singlePatch.getSubActions().add(actionSet);
		} 

		
		return singlePatch;
	}
	private static boolean isContained(HierarchicalActionSet actionSet) {
		for (Map.Entry<Chunk, List<HierarchicalActionSet>> entry : chunkActionMap.entrySet()) {
			List<HierarchicalActionSet> patches = entry.getValue();
			for (HierarchicalActionSet patch : patches) {
				if (patch.getSubActions().contains(actionSet)) return true;
			}
		}
		return false;
	}
	
	
	private static void addToPatchesMap(HierarchicalActionSet singlePatch, Chunk chunk) {
		List<HierarchicalActionSet> patches = chunkActionMap.get(chunk);
		if (patches == null) {
			patches = new ArrayList<>();
			patches.add(singlePatch);
			chunkActionMap.put(chunk, patches);
		} else {
			patches.add(singlePatch);
		}
	}
	
	private static int setLineNumbers(HierarchicalActionSet actionSet, CompilationUnit prevUnit, CompilationUnit revUnit) {
		int actionBugStartLine;
		int actionBugEndLine;
		int actionFixStartLine;
		int actionFixEndLine;
		
		
		int bugStartPosition = 0;
		int bugEndPosition = 0;
		
		int fixStartPosition = 0;
		int fixEndPosition = 0;
		
		String actionStr = actionSet.getActionString();
		if (actionStr.startsWith("INS")) {
			ITree newTree = actionSet.getNode();
			fixStartPosition = newTree.getPos();
			fixEndPosition = fixStartPosition + newTree.getLength();

			List<Move> firstAndLastMov = getFirstAndLastMoveAction(actionSet);
			if (firstAndLastMov != null) {
				bugStartPosition = firstAndLastMov.get(0).getNode().getPos();
				ITree lastTree = firstAndLastMov.get(1).getNode();
				bugEndPosition = lastTree.getPos() + lastTree.getLength();
			}
		} else {
			ITree oldTree = actionSet.getNode();
			bugStartPosition = oldTree.getPos(); 
			bugEndPosition = bugStartPosition + oldTree.getLength();
			String astNodeType = actionSet.getAstNodeType();
			if ("TypeDeclaration".equals(astNodeType)) {
				bugEndPosition = getClassBodyStartPosition(oldTree);
			} else if ("MethodDeclaration".equals(astNodeType) || NodeChecker.withBlockStatement(oldTree.getType())) { 
				List<ITree> children = oldTree.getChildren();
				bugEndPosition = getEndPosition(children);
			}
			if (bugEndPosition == 0) {
				bugEndPosition = bugStartPosition + oldTree.getLength();
			}
			
			if (actionStr.startsWith("UPD")) {
				Update update = (Update) actionSet.getAction();
				ITree newNode = update.getNewNode();
				fixStartPosition = newNode.getPos();
				fixEndPosition = fixStartPosition + newNode.getLength();
				
				if ("TypeDeclaration".equals(astNodeType)) {
					fixEndPosition = getClassBodyStartPosition(newNode);
				} else if ("MethodDeclaration".equals(astNodeType)) {
					List<ITree> newChildren = newNode.getChildren();
					fixEndPosition = getEndPosition(newChildren);
				}
				if (fixEndPosition == 0) {
					fixEndPosition = fixStartPosition + newNode.getLength();
				}
			}
		}
		actionBugStartLine = bugStartPosition == 0 ? 0 : prevUnit.getLineNumber(bugStartPosition);
		actionBugEndLine = bugEndPosition == 0 ? 0 : prevUnit.getLineNumber(bugEndPosition);
		actionFixStartLine = fixStartPosition == 0 ? 0 : revUnit.getLineNumber(fixStartPosition);
		actionFixEndLine = fixEndPosition == 0 ? 0 : revUnit.getLineNumber(fixEndPosition);
		actionSet.setBugStartLineNum(actionBugStartLine);
		actionSet.setBugEndLineNum(actionBugEndLine);
		actionSet.setFixStartLineNum(actionFixStartLine);
		actionSet.setFixEndLineNum(actionFixEndLine);
		actionSet.setBugEndPosition(bugEndPosition);
		actionSet.setFixEndPosition(fixEndPosition);
		
		return actionBugStartLine;
	}
	
	private static int getEndPosition(List<ITree> children) {
		int endPosition = 0;
		for (int i = 0, size = children.size(); i < size; i ++) {
			ITree child = children.get(i);
			int type = child.getType();
			if (NodeChecker.isStatement2(type)) {
				if ( i > 0) {
					child = children.get(i - 1);
					endPosition = child.getPos() + child.getLength();
				} else {
					endPosition = child.getPos() - 1;
				}
				break;
			}
		}
		return endPosition;
	}
	
	private static int getClassBodyStartPosition(ITree tree) {
		List<ITree> children = tree.getChildren();
		for (int i = 0, size = children.size(); i < size; i ++) {
			ITree child = children.get(i);
			int type = child.getType();
			
			if (type != 83 && type != 77 && type != 78 && type != 79
				&& type != 5 && type != 39 && type != 43 && type != 74 && type != 75
				&& type != 76 && type != 84 && type != 87 && type != 88 && type != 42) {
				
				
				if (i > 0) {
					child = children.get(i - 1);
					return child.getPos() + child.getLength() + 1;
				} else {
					return child.getPos() - 1;
				}
			}
		}
		return 0;
	}
	
	private static List<Move> getFirstAndLastMoveAction(HierarchicalActionSet gumTreeResult) {
		List<Move> firstAndLastMoveActions = new ArrayList<>();
		List<HierarchicalActionSet> actions = new ArrayList<>();
		actions.addAll(gumTreeResult.getSubActions());
		if (actions.size() == 0) {
			return null;
		}
		Move firstMoveAction = null;
		Move lastMoveAction = null;
		while (actions.size() > 0) {
			List<HierarchicalActionSet> subActions = new ArrayList<>();
			for (HierarchicalActionSet action : actions) {
				subActions.addAll(action.getSubActions());
				if (action.toString().startsWith("MOV")) {
					if (firstMoveAction == null) {
						firstMoveAction = (Move) action.getAction();
						lastMoveAction = (Move) action.getAction();
					} else {
						int startPosition = action.getStartPosition();
						int length = action.getLength();
						int startPositionFirst = firstMoveAction.getPosition();
						int startPositionLast = lastMoveAction.getPosition();
						int lengthLast = lastMoveAction.getNode().getLength();
						if (startPosition < startPositionFirst || (startPosition == startPositionFirst && length > firstMoveAction.getLength())) {
							firstMoveAction = (Move) action.getAction();
						}
						if ((startPosition + length) > (startPositionLast + lengthLast)) {
							lastMoveAction = (Move) action.getAction();
						} 
					}
				}
			}
			
			actions.clear();
			actions.addAll(subActions);
		}
		if (firstMoveAction == null) {
			return null;
		}
		firstAndLastMoveActions.add(firstMoveAction);
		firstAndLastMoveActions.add(lastMoveAction);
		return firstAndLastMoveActions;
	}
	
	public static int getLineNumber(String patchFilePath, int position){
		List<String> lines = FileUtil.readFile(patchFilePath);

		logger.debug("lines.size: {}", lines.size());

		int posCount = 0;
		for (int lineNo = 0; lineNo < lines.size(); lineNo ++){
			String line = lines.get(lineNo) + "\n";
			for (int i = 0; i < line.length(); i++){			    
			    if (posCount == position){
			    	logger.info("position: {}, line: {}, lineNo: {}", position, line, lineNo);
			    	return lineNo; 
				}
			    
			    posCount ++;
			}
		}
		
		return -1;
	}
	
	public static int getLineNumberFromCU(String patchFilePath, int position){
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map<String, String> pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
		parser.setCompilerOptions(pOptions);

		parser.setSource(FileUtil.readFileToStr(patchFilePath).toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		
		int lineNo = cu.getLineNumber(position);
		return lineNo;
	}
}
