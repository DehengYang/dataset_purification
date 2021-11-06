/**
 * 
 */
package apr.purify.diff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gumtreediff.actions.model.Action;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import apr.purify.utils.FileUtil;
import apr.purify.utils.Pair;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;

/**
 * @author apr
 * @version Oct 7, 2020
 *
 */
public class Diff {
	final static Logger logger = LoggerFactory.getLogger(Diff.class);
	
	private String patchDiffPath;
	private String srcFolder;
	private List<Chunk> chunks = new ArrayList<>();
//	private Map<String, String> addLines = new HashMap<>();// <line_no, real_code>
//	private Map<String, String> delLines = new HashMap<>();// <line_no, real_code>
//	private Map<String, String> buggyLines = new HashMap<>();  // from getDiff2.py, it does not provide real_code, so the <line_no, real_code> is <line_no, "">
	
	private List<Pair<String, String>> patchPathMap = new ArrayList<>(); //buggy file path -> patch file path 
	
	private Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap = new HashMap<>();
	
	/**
	 * init Diff instance with patch-diff file path and its src folder
	 * and run a series of functions to obtain change actions with gumtree&patchparser.
	 * @param patchDiffPath
	 * @param srcFolder
	 */
	public Diff(String patchDiffPath, String srcFolder){
		this.patchDiffPath = patchDiffPath;
		this.srcFolder = srcFolder;
		
		//
		getAllChunks();
//		applyAllChunksForPatch();
		
		// old methods
//		parseDiff();
//		produceJavaFilesFromDiff();
//		getGumTreeActions();
//		getModifications();
	}
	
	private void applyAllChunksForPatch() {
		String targetFolder = FileUtil.toolDir + "/mutants";
		DiffUtil.applyAllChunksForPatch(chunks, srcFolder, targetFolder);
	}

	public void getAllChunks(){
		DiffUtil.getAllChunks(chunks, patchDiffPath, srcFolder);
	}
	
	public void getModifications(){
		DiffUtil.getModifications(actionsPerFileMap);
	}
	
	public void getGumTreeActions(){
		actionsPerFileMap = DiffUtil.getGumTreeActions(patchPathMap);
	}
	
	public void produceJavaFilesFromDiff() {
		patchPathMap = DiffUtil.produceJavaFilesFromDiff(patchDiffPath, srcFolder);
	}

	public void parseDiff(){
		DiffUtil.parseDiff(patchDiffPath, chunks);
	}

	public String getPatchDiffPath() {
		return patchDiffPath;
	}

	public void setPatchDiffPath(String patchDiffPath) {
		this.patchDiffPath = patchDiffPath;
	}

	public String getSrcFolder(){
		return srcFolder;
	}
	public void setSrcFolder(String srcFolder){
		this.srcFolder = srcFolder;
	}

	public Map<Pair<String, String>, List<HierarchicalActionSet>> getActionsPerFileMap() {
		return actionsPerFileMap;
	}

	public void setActionsPerFileMap(Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap) {
		this.actionsPerFileMap = actionsPerFileMap;
	}

	public List<Pair<String, String>> getPatchPathMap() {
		return patchPathMap;
	}

	public List<Chunk> getChunks() {
		return chunks;
	}
}
