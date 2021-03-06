package name.kazennikov.annotations.patterns;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;


public class Rule {
	String name;
	int priority = -1;
	int number = -1;
	List<PatternElement> lhs = new ArrayList<>();
	List<RHS> rhs = new ArrayList<>();
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("name", name)
				.add("number", number)
				.add("priority", priority)
				.add("lhs", lhs)
				.add("rhs", rhs)
				.toString();
	}
	
	public List<PatternElement> lhs() {
		return lhs;
	}
	
	public List<RHS> rhs() {
		return rhs;
	}

	public int getPriority() {
		return priority;
	}
	
}
