
package apr.purify.diff;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.match.Modification;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.Pair;

import apr.purify.utils.FileUtil;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;

public class FileModification extends Modification {
	final static Logger logger = LoggerFactory.getLogger(FileModification.class);
	
	private String buggyFilePath;
	private String patchFilePath;
	
	
	private int patchStartLineNo = -1; 
	private int patchStartCol = -1;
	private int patchEndLineNo = -1;
	private int patchEndCol = -1;
	
	private String patchStr;
	private HierarchicalActionSet actionSet;






	
	public FileModification(String buggyFilePath, String patchFilePath, HierarchicalActionSet actionSet){
		this.setBuggyFilePath(buggyFilePath);
		this.setPatchFilePath(patchFilePath);
		
		CompilationUnit cu = null;
		try {
			cu = StaticJavaParser.parse(new File(patchFilePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int startPosition = actionSet.getStartPosition();
		int length = actionSet.getLength();
		this.actionSet = actionSet;
		
		logger.debug("action str: {}", actionSet.getAction().getName());
		
		patchStr = getStartEndRowCol(startPosition, length, patchFilePath);
		
		getModification(cu);
		
		logger.debug(toString());
	}
	
	
	@Override
	public String toString() {











		
		return String.format("buggyFilePath: %s\n"
				+ "patchFilePath: %s\n"
				+ "patchStr: %s\n"
				+ "start row-col: (%s-%s)\n"
				+ "end row-col: (%s-%s)\n"
				+ "actionSet: %s\n" 
				+ "Modification: %s\n", 
				buggyFilePath,
				patchFilePath,
				patchStr,
				patchStartLineNo,
				patchStartCol,
				patchEndLineNo,
				patchEndCol,
				actionSet.toString(),
				super.toString());
	}


	public void getModification(CompilationUnit cu) {







		




		
		List<Statement> stmtList = new ArrayList<>();
		Consumer<Node> cuConsumer = n -> {
			if (n instanceof Statement){
				int startStmtRow = n.getBegin().get().line;
				int startStmtCol = n.getBegin().get().column;
				int endStmtRow = n.getEnd().get().line;
				int endStmtCol = n.getEnd().get().column;
				




				if (startStmtRow <= patchStartLineNo + 1 && startStmtCol <= patchStartCol + 1){
					if(endStmtRow >= patchEndLineNo + 1 && endStmtCol >= patchEndCol + 1){
						stmtList.add((Statement) n);
					}
				}
			}
		};
		cu.walk(TreeTraversal.PREORDER, cuConsumer);
		
		logger.debug("patchStr: {}", patchStr);
		logger.debug("stmtList.size: {}", stmtList.size());
		for (int i = 0; i < stmtList.size(); i++){
			logger.debug("stmt:{}", stmtList.get(i).toString());
		}
		



		
		if (stmtList.isEmpty()){
			FileUtil.raiseException(String.format("fail to locate the patchStr stmt: %s from the cu!", patchStr));
		}
		Statement donor = stmtList.get(stmtList.size() - 1);
		
		String actionName = actionSet.getAction().getName();
		switch(actionName){
			case "INS":

				identifyOpAndTargetNode(donor, OP.ADD);
				break;
			case "DEL":
				identifyOpAndTargetNode(donor, OP.DEL);

				break;
			case "UPD":
				identifyOpAndTargetNode(donor, OP.REP);
				break;

			case "MOV":
				identifyOpAndTargetNode(donor, OP.MOV);
				break;

			default:
				FileUtil.raiseException(String.format("unkown action: %s", actionName));
		}
		
	}


	private void identifyOpAndTargetNode(Statement donor, OP op) {
		if (op == OP.ADD){
			
			if (!donor.getParentNode().isPresent()){
				FileUtil.raiseException(String.format("stmt: {} has no parent!", donor.toString()));
			}
			Node parent = donor.getParentNode().get();
			
			List<Node> chidren = parent.getChildNodes();
			int index = chidren.indexOf(donor);
			if (index >= 0){
				if (index == 0 && chidren.size() == 1){ 
					Node targetNode = parent;
					this.setTargetNode(targetNode);
					this.setPair(new Pair<Node, Node>(targetNode, donor));
					this.setOp(OP.ADD);
				}
				
				if (index != chidren.size() - 1){ 
					Node targetNode = chidren.get(index - 1);
					this.setTargetNode(targetNode);
					this.setPair(new Pair<Node, Node>(targetNode, donor));
					this.setOp(OP.ADDAFT);
				}else{ 
					Node targetNode = chidren.get(index - 1);
					this.setTargetNode(targetNode);
					this.setPair(new Pair<Node, Node>(targetNode, donor));
					this.setOp(OP.ADDBEF);
				}
			}else{
				FileUtil.raiseException(String.format("fail to find stmt: %s from its parent!", donor.toString()));
			}
		}else{
			
			FileUtil.raiseException("not done yet!");
			this.setOp(op);
		}
		
	}

	public String getStartEndRowCol(int startPosition, int length, String patchFilePath){
		List<String> lines = FileUtil.readFile(patchFilePath);
		String linesStr = FileUtil.readFileToStr(patchFilePath);
		logger.debug("lines.size: {}", lines.size());
		


		
		int posCount = 0;
		String codeSnippet = "";
		boolean breakFlag = false;
		for (int lineNo = 0; lineNo < lines.size(); lineNo ++){
			String line = lines.get(lineNo) + "\n";
			for (int i = 0; i < line.length(); i++){
			    char c = line.charAt(i);




			    
			    if (posCount >= startPosition && posCount <= startPosition + length - 1){
			    	logger.debug("posCount: {}, posChar from linesStr: {}, posChar from lines: {}", posCount, linesStr.charAt(posCount), c);
					codeSnippet += c; 
				}
				
				if (posCount == startPosition){
					patchStartLineNo = lineNo;
					patchStartCol = i;
				}
				if (posCount == startPosition + length - 1){
					patchEndLineNo = lineNo;
					patchEndCol = i;
				}
				
				posCount += 1;
				
				
				if (posCount > startPosition + length - 1){
					breakFlag = true;
					break;
				}
			}
			
			if (breakFlag){
				break;
			}
		}
		
		logger.debug("codeSnippet: {}", codeSnippet);
		logger.debug("substring  : {}", linesStr.substring(startPosition, startPosition + length));

		logger.debug("start row-col: {}-{}, end row-col: {}-{}", patchStartLineNo, patchStartCol, patchEndLineNo, patchEndCol);
		
		return codeSnippet;
	}
	
	

	public String getBuggyFilePath() {
		return buggyFilePath;
	}

	public void setBuggyFilePath(String buggyFilePath) {
		this.buggyFilePath = buggyFilePath;
	}

	public String getPatchFilePath() {
		return patchFilePath;
	}

	public void setPatchFilePath(String patchFilePath) {
		this.patchFilePath = patchFilePath;
	}

	public int getPatchStartLineNo() {
		return patchStartLineNo;
	}

	public void setPatchStartLineNo(int patchStartLineNo) {
		this.patchStartLineNo = patchStartLineNo;
	}

	public int getPatchStartCol() {
		return patchStartCol;
	}

	public void setPatchStartCol(int patchStartCol) {
		this.patchStartCol = patchStartCol;
	}

	public int getPatchEndLineNo() {
		return patchEndLineNo;
	}

	public void setPatchEndLineNo(int patchEndLineNo) {
		this.patchEndLineNo = patchEndLineNo;
	}

	public int getPatchEndCol() {
		return patchEndCol;
	}

	public void setPatchEndCol(int patchEndCol) {
		this.patchEndCol = patchEndCol;
	}

	public String getPatchStr() {
		return patchStr;
	}

	public void setPatchStr(String patchStr) {
		this.patchStr = patchStr;
	}

	public HierarchicalActionSet getActionSet() {
		return actionSet;
	}

	public void setActionSet(HierarchicalActionSet actionSet) {
		this.actionSet = actionSet;
	}
	
	
}
