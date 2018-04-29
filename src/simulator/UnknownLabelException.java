package simulator;

public class UnknownLabelException extends AssemblyException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1239647656703191214L;
	private final String label;
	
	public UnknownLabelException(int line, String label) {
		super(line);
		this.label = label;
	}

	public String toString() {
		return "UNKNOWN LABEL: " + label + " @ line " + line + " is unknown.";
	}
}