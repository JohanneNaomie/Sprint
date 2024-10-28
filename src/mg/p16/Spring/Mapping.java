package mg.p16.Spring;
public class Mapping {
    String className;
    VerbAction verbAction;

    public Mapping(String className, VerbAction vb) {
        this.className = className;
        this.verbAction= vb;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public VerbAction getVerbAction() {
        return verbAction;
    }

    public void setVerbAction(VerbAction vb) {
        this.verbAction = vb;
    }
}