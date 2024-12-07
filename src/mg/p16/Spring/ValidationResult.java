package mg.p16.Spring;
import java.util.List;
import java.util.Map;

public class ValidationResult {
    private List<MyExceptions> errors;
    private Map<String, String> fieldValues;

    public ValidationResult(List<MyExceptions> errors, Map<String, String> fieldValues) {
        this.errors = errors;
        this.fieldValues = fieldValues;
    }

    public List<MyExceptions> getErrors() {
        return errors;
    }

    public Map<String, String> getFieldValues() {
        return fieldValues;
    }
}
