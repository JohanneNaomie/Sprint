package mg.p16.Spring;

public class VerbAction {
    private String verb;
    private String action; //methodName

    public VerbAction(String action,String verb) {
        this.action = action; //method
        this.verb = verb; //methodname
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

}
