final class NodeUtils {  
    static public Node findOrCreateNode(Node parent, String... path) {
		Node current = parent;
		for (String nodeName : path) {
			Node child = findChild(current, nodeName);
			if (child == null) {
				child = new Node(current, nodeName);
			}
			current = child;
		}
		return current;
	}

	static public Node findChild(Node parent, String name) {
		for (Object child : parent.children()) {
			if (child instanceof Node node) {
				if ((node.name() instanceof QName qname) && name.equals(qname.getLocalPart())) {
					return node;
				}
				if (name.equals(node.name())) {
					return node;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static public List<Node> findChildren(Node parent, String name) {
		return (List<Node>) parent.children().stream().filter((child) -> isNodeWithName(child, name)).collect(Collectors.toList());

	}

	static public boolean isNodeWithName(Object candidate, String name) {
		if (candidate instanceof Node node) {
			if ((node.name() instanceof QName qname) && name.equals(qname.getLocalPart())) {
				return true;
			}
			if (name.equals(node.name())) {
				return true;
			}
		}
		return false;
	}
}