package mg.p16.Spring;

public class MyExceptions extends Exception {
    private String field;
    private String error;
    private String solution;

    public MyExceptions(String field, String error, String solution) {
        super(error);
        this.field = field;
        this.error = error;
        this.solution = solution;
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

    @Override
    public String toString() {
        return "Field: " + field + ", Error: " + error + ", Solution: " + solution;
    }
}
