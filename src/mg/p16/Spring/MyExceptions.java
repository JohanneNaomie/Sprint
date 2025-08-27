package mg.p16.Spring;

public class MyExceptions extends Exception {
    private String field;
    private String error;
    private String solution;
    private String value; // New attribute to hold the submitted value

    public MyExceptions(String field, String error, String solution, String value) {
        super(error);
        this.field = field;
        this.error = error;
        this.solution = solution;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getError() {
        return error;
    }

    public String getSolution() {
        return solution;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Field: " + field + ", Error: " + error + ", Solution: " + solution;
    }
}
