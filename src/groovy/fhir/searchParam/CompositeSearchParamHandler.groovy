package fhir.searchParam

import java.util.Map;

import org.hl7.fhir.instance.model.Resource
import org.hl7.fhir.instance.model.Conformance.SearchParamType
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import com.mongodb.BasicDBObject;

// Generates "composite" matches based on FHIR spec.  But unclear whether/how
// composites interact with modifiers.  Are the terms in a composite individually
// modifiable?  It not, how can one express "before this date" in a status-date pair?

public class CompositeSearchParamHandler extends SearchParamHandler {

	private String parent;
	private List<String> children = []

	@Override
	protected void init(){
		def paths = xpath.split('\\$');
		parent = paths[0];
		children = paths[1..-1].collect {
			"./$it/@value";
		}
	}

	@Override
	protected String paramXpath() {
		"//$parent"
	}

	@Override
	public void processMatchingXpaths(List<Node> compositeRoots, List<SearchParamValue> index){

		for (Node n : compositeRoots) {
			List<String> combined = [];

			for (String child : children) {
				List<Node> childMatches = query(child, n);
				if (childMatches.size() > 1) {
					throw new Exception("Expected <= 1 composite child for " +
					parent + "/" + child);
				} else if (childMatches.size() == 1){
					combined.add(childMatches.get(0).nodeValue);
				}
			}

			if (combined.size() == children.size()) {
				index.add(value(combined.join('$')))
			}

		}
	}

	@Override
	public BasicDBObject searchClause(Map searchedFor) {
		return [(fieldName): searchedFor.value]
	}
}