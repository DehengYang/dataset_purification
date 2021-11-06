/**
 * 
 */
package apr.purify.diff;

import java.util.ArrayList;
import java.util.List;

import apr.purify.utils.Pair;
import junit.framework.TestCase;

/**
 * @author apr
 * @version Oct 17, 2020
 *
 */
public class DiffUtilTest extends TestCase {
	public void testUpdateChunkRange(){
		String COMMENT = "//[apr-purify-comment]";
				
		Chunk chunk = new Chunk();
		chunk.setClazz("test");
		List<Pair<Integer, String>> lines =  new ArrayList<>();
		lines.add(new Pair<Integer, String>(1, "-line1"));
		lines.add(new Pair<Integer, String>(2, "-" + COMMENT + "line2"));
		lines.add(new Pair<Integer, String>(3, "-line3"));
		lines.add(new Pair<Integer, String>(2, "+line4"));
		lines.add(new Pair<Integer, String>(3, "+" + COMMENT + "line5"));
		lines.add(new Pair<Integer, String>(4, "+" + "line6"));
		lines.add(new Pair<Integer, String>(4, "-" + COMMENT + "line7"));
		lines.add(new Pair<Integer, String>(5, "+" + "line8"));
		lines.add(new Pair<Integer, String>(6, "+" + "line9"));
		lines.add(new Pair<Integer, String>(7, "+" + "line10"));
		chunk.setLines(lines);
		chunk.replaceRange = new Pair<Integer, Integer>(1, 4);
		List<Chunk> chunks = DiffUtil.updateChunkRange(chunk);
		
		assertEquals(chunks.size(), 3);
	}
}
