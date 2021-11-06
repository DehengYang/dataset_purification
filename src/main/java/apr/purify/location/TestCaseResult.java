/**
 * 
 */
package apr.purify.location;

/**
 * @author apr
 * @version Oct 4, 2020
 *
 */
public class TestCaseResult {
	private String name;
	private boolean result;
	
	/**
	 * name:
	 * e.g., org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests#test2947660
	 * @param name
	 * @param result
	 */
	public TestCaseResult(String name, boolean result){
		this.name = name;
		this.result = result;
	}
	
	public boolean isSuccessful(){
		return result;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean getResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
		
        TestCaseResult other = (TestCaseResult) o;
        boolean equal = name.equals(other.getName());// && result == other.getResult(); // there is no need to compare the result. As sometimes I have to get the coverage of failing tests on fixed version, where the result is true.    
        return equal;
	}
	@Override
	public int hashCode(){
		int hash = 1;
		if (name != null){
			hash += name.hashCode();
		}
		return hash;
	}
	@Override
	public String toString(){
		return name + " " + result; 
	}
}
