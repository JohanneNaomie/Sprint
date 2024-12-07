package mg.p16.Spring;
import java.util.List;

public class MyValidationException extends Exception {
    private List<MyExceptions> validationErrors;

    public MyValidationException(String message, List<MyExceptions> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<MyExceptions> getValidationErrors() {
        return validationErrors;
    }
}
