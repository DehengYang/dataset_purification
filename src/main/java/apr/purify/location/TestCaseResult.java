
package apr.purify.location;


public class TestCaseResult {
	private String name;
	private boolean result;
	

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
        boolean equal = name.equals(other.getName());
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
