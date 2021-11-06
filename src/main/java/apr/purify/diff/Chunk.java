/**
 * 
 */
package apr.purify.diff;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import apr.purify.Configuration;
import apr.purify.utils.GeneralUtil;
import apr.purify.utils.Pair;

/**
 * this is to save chunk info from getDiff2.py output
 * @author apr
 * @version Oct 13, 2020
 *
 */
public class Chunk implements Cloneable {
	final static Logger logger = LoggerFactory.getLogger(Chunk.class);
	
	// because its hard to rank addLines/delLines... and its hard to setAddLines (putAll will change the elements order)
	// so I changed Map into List<Pair>.
	private List<Pair<Integer, String>> lines = new ArrayList<>(); 
	
	private List<Pair<Integer, String>> addLines = new ArrayList<>();// <line_no, real_code>
	private List<Pair<Integer, String>> delLines = new ArrayList<>();// <line_no, real_code>
	private List<Pair<Integer, String>> buggyLines = new ArrayList<>();  // from getDiff2.py, it does not provide real_code, so the <line_no, real_code> is <line_no, "">
	private List<String> methods = new ArrayList<>();
	private String clazz;
//	private String srcFolder; 
	public Pair<Integer, Integer> replaceRange = new Pair<Integer, Integer>(null, null); // if insert, only have left element (i.e., <insertAfterLineNo, null>). Otherwise, <replaceStartLineNo, replaceEndLineNo>
	
	public Chunk(){
		
	}
	
	public Chunk(List<Pair<Integer, String>> lines, List<String> methods, String clazz, Pair<Integer, Integer> replaceRange ){
		setLines(lines);
		setMethods(methods);
		setClazz(clazz);
		resetByLines();
		setReplaceRange(replaceRange);
	}
	
	public void setReplaceRange(Pair<Integer, Integer> replaceRange2){
		replaceRange.setLeft(replaceRange2.getLeft());
		replaceRange.setRight(replaceRange2.getRight());		
	}
	
	public boolean hasLine(){
		int size = addLines.size() + delLines.size() + buggyLines.size();
		if (size > 0){
			return true;
		}else{
			return false;
		}
	}
	
//	public Pair<Integer, Integer> addLinesRange(){
//		return new Pair<Integer, Integer>(addLines.get(0).getLeft(),addLines.get(addLines.size() - 1).getLeft()); 
//	}
//	public Pair<Integer, Integer> delLinesRange(){
//		return new Pair<Integer, Integer>(delLines.get(0).getLeft(),delLines.get(delLines.size() - 1).getLeft()); 
//	}
	public List<Integer> getAddLineNos(){
		List<Integer> lineNos = new ArrayList<>();
		for (Pair<Integer, String> pair : addLines){
			lineNos.add(pair.getLeft());
		}
		return lineNos;
	}
	public List<Integer> getDelLineNos(){
		List<Integer> lineNos = new ArrayList<>();
		for (Pair<Integer, String> pair : delLines){
			lineNos.add(pair.getLeft());
		}
		return lineNos;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("chunk class: %s\n", clazz));
//		sb.append(String.format("srcFolder: %s\n", srcFolder));
		for (String method : methods){
			sb.append(String.format("method: %s\n", method));
		}
		sb.append(String.format("replaceRange: %s\n", replaceRange.toString()));
		for (Pair<Integer, String> pair : lines){
			sb.append(String.format("lineNo:%s, lineCode:%s\n", pair.getLeft(), pair.getRight()));
		}
//		for (Pair<Integer, String> pair : addLines){
//			sb.append(String.format("addLine:%s==line==%s\n", pair.getLeft(), pair.getRight()));
//		}
//		for (Pair<Integer, String> pair : delLines){
//			sb.append(String.format("delLine:%s==line==%s\n", pair.getLeft(), pair.getRight()));
//		}
//		for (Pair<Integer, String> pair : buggyLines){
//			sb.append(String.format("buggyLine:%s==line==%s\n", pair.getLeft(), pair.getRight()));
//		}
		
		return sb.toString();
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public List<Pair<Integer, String>> getAddLines() {
		return addLines;
	}

	public void setAddLines(List<Pair<Integer, String>> addLines) {
		this.addLines.addAll(addLines);
	}

	public List<Pair<Integer, String>> getDelLines() {
		return delLines;
	}

	public void setDelLines(List<Pair<Integer, String>> delLines) {
		this.delLines.addAll(delLines);
	}

	public List<Pair<Integer, String>> getBuggyLines() {
		return buggyLines;
	}

	public void setBuggyLines(List<Pair<Integer, String>> buggyLines) {
		this.buggyLines.addAll(buggyLines);
	}

//	public String getSrcFolder() {
//		return srcFolder;
//	}
//
//	public void setSrcFolder(String srcFolder) {
//		this.srcFolder = srcFolder;
//	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @return
	 */
	public String getAllChanges() {
		String all = "";
		for(Pair<Integer, String> line : lines){
			String lineCode = line.getRight();
			if (isAdd(lineCode)){
				all += lineCode.substring(1) + "\n";
			}
		}
		return all;
	}
	
	public boolean isAdd(String lineCode){
		return lineCode.startsWith("+");
	}
	public boolean isDel(String lineCode){
		return lineCode.startsWith("-");
	}
	public boolean isAdd(Pair<Integer, String> line){
		return line.getRight().startsWith("+");
	}
	public boolean isDel(Pair<Integer, String> line){
		return line.getRight().startsWith("-");
	}
	
	public boolean isCommentDel(Pair<Integer, String> line){
		return line.getRight().startsWith("-" + Configuration.COMMENT);
	}
	
	public boolean isCommentLine(Pair<Integer, String> line){
		return line.getRight().startsWith("-" + Configuration.COMMENT) || line.getRight().startsWith("+" + Configuration.COMMENT);
	}

	public List<Pair<Integer, String>> getLines() {
		return lines;
	}

	public void setLines(List<Pair<Integer, String>> lines) {
		this.lines.clear();
		this.lines.addAll(lines);
	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 */
	public void resetByLines() {
		this.addLines.clear();
		this.delLines.clear();
		
		for(Pair<Integer, String> pair : lines){
			if (isAdd(pair)){
				addLines.add(pair);
			}else{
				delLines.add(pair);
			}
		}
	}
	
	public void resetByLinesRemoveComment() {
		this.addLines.clear();
		this.delLines.clear();
		
		for(Pair<Integer, String> pair : lines){
			if (isCommentLine(pair)){
				continue;
			}
			if (isAdd(pair)){
				addLines.add(pair);
			}else{
				delLines.add(pair);
			}
		}
	}

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @return
	 */
//	public boolean hasRealDelLines() {
//		// TODO Auto-generated method stub
//		return false;
//	}

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @return
	 */
	public boolean hasCommentedDelLine() {
		for(Pair<Integer, String> line : lines){
			if (isCommentDel(line)){
				return true;
			}
		}
		
		return false;
	}

	public List<String> getMethods() {
		return methods;
	}

	public void setMethods(List<String> methods) {
		this.methods.clear();
		this.methods.addAll(methods);
	}

	/** @Description 
	 * @author apr
	 * @version Oct 19, 2020
	 *
	 * @param srcJavaDir
	 * @param string
	 */
	public void updateMethods(String srcJavaDir, String mutJavaDir) {
		String delFilePath = srcJavaDir + "/" + clazz + ".java";
		String addFilePath = mutJavaDir + "/" + clazz + ".java";
		
		resetByLinesRemoveComment();
		List<String> mdsDel = getSpanedMethods(delFilePath, delLines);
		if (mdsDel.isEmpty()){ //exposed by time 11
			mdsDel.add("OutOfMethod");
		}
		List<String> mdsAdd = getSpanedMethods(addFilePath, addLines);
		if (mdsAdd.isEmpty()){ //exposed by time 11
			mdsAdd.add("OutOfMethod");
		}
		
		methods.clear();
		for(String md : mdsDel){
			if (! methods.contains(md)){
				methods.add(md);
			}
		}
		for(String md : mdsAdd){
			if (! methods.contains(md)){
				methods.add(md);
			}
		}
		
	}
	
	public List<String> getSpanedMethods(String filePath, List<Pair<Integer, String>> lines){
		List<String> mds = new ArrayList<>();
		
		try {
			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
			
			List<Integer> lineNos = new ArrayList<>();
			for (Pair<Integer, String> pair : lines){
				lineNos.add(pair.getLeft());
			}
			
			Consumer<Node> cuConsumer = n -> {
				if (n instanceof MethodDeclaration){
					MethodDeclaration md = (MethodDeclaration) n;
					int startRow = md.getBegin().get().line;
					int endRow = md.getEnd().get().line;
					
					for (int lineNo : lineNos){
						if (lineNo >= startRow && lineNo <= endRow){
							// public LegendItemCollection getLegendItems() {
							String mdStr = String.format("modifiers: %s, type: %s, name: %s, parameters: %s", 
									GeneralUtil.listToStringWithSep(md.getModifiers(), " "), md.getType().asString(),
									md.getName(), GeneralUtil.listToStringWithSep(md.getParameters(), " ")
									);
							if (!mds.contains(mdStr)){
								mds.add(mdStr);
							}
						}
					}
				}
			};
			cu.walk(TreeTraversal.PREORDER, cuConsumer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return mds;
	}
	
	// Overriding the inbuilt clone class 
    @Override
	public Chunk clone() 
    { 
        return new Chunk(lines, methods, clazz, replaceRange); 
    }

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *
	 * @param left
	 * @param delOrAdd
	 * @return
	 */
	public Pair<Integer, String> getPairByLineNo(Integer lineNo, String delOrAdd) {
		if (delOrAdd.equals("+")){
			for(Pair<Integer, String> pair : addLines){
				if (pair.getLeft().equals(lineNo)){
					return pair;
				}
			}
		}else{
			for(Pair<Integer, String> pair : delLines){
				if (pair.getLeft().equals(lineNo)){
					return pair;
				}
			}
		}
		
		return null;
	} 
}
