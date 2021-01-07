
package apr.purify.execution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Executor {
	final static Logger logger = LoggerFactory.getLogger(Executor.class);
	
	public abstract boolean compile();
	public abstract boolean testFailCases();
	public abstract boolean testAll();
	
	public abstract List<String> getFailingCasesInTestAll();
}
