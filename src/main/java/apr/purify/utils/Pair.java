package apr.purify.utils;

public class Pair <L, R> {
	private L left;
	private R right;

	public Pair(L left, R right) {

		this.left = left;
		this.right = right;
	}

	public L getLeft() { return left; }
	public R getRight() { return right; }
	public void setLeft(L left) { this.left = left; }
	public void setRight(R right) { this.right = right; }

	@Override
	public int hashCode() { return left.hashCode() ^ right.hashCode(); }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) return false;
		Pair<?, ?> pairo = (Pair<?, ?>) o;
		return this.left.equals(pairo.getLeft()) && this.right.equals(pairo.getRight());
	}
	
	@Override
	public String toString(){
		String leftStr = ""; 
		String rightStr = "";
		if (left != null){
			leftStr = left.toString();
		}else{
			leftStr = "null";
		}
		
		if (right != null){
			rightStr = right.toString();
		}else{
			rightStr = "null";
		}
		
		return String.format("Pair<%s,%s>", leftStr, rightStr);
	}
}
