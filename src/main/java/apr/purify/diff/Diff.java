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

public class Diff {
	final static Logger logger = LoggerFactory.getLogger(Diff.class);
	
	private String patchDiffPath;
	private String srcFolder;
	private List<Chunk> chunks = new ArrayList<>();



	
	private List<Pair<String, String>> patchPathMap = new ArrayList<>(); 
	
	private Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap = new HashMap<>();
	
	public Diff(String patchDiffPath, String srcFolder){
		this.patchDiffPath = patchDiffPath;
		this.srcFolder = srcFolder;
		
		
		getAllChunks();

		
		




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
