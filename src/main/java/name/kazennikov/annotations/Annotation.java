package name.kazennikov.annotations;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class Annotation {

	String name;
	int start;
	int end;
	
	Map<String, Object> features = Maps.newHashMap();
	
	public Annotation(String name, int start, int end) {
		this.name = name;
		this.start = start;
		this.end = end;
	}
	
	public String getName() {
		return name;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public Set<String> getFeatureNames() {
		return features.keySet();
	}
	public Object getFeature(String feat) {
		return features.get(feat);
	}
	
	public <E> E getFeature(String feat, Class<E> cls) {
		return cls.cast(getFeature(feat));
	}
	
	public void setFeature(String feat, Object value) {
		features.put(feat, value);
	}
	
	public Map<String, Object> getFeatureMap() {
		return features;
	}
	
	public String toString() {
		return String.format("%s@[%d,%d]%s", name, start, end, features);
	}
	
	
	public String getText(Document d) {
		return d.getText().substring(start, end);
	}
	
	public boolean isEmpty() {
		return start == end;
	}

}
