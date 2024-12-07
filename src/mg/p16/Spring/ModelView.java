package mg.p16.Spring;

import java.util.HashMap;

public class ModelView {
    private String url;
    private HashMap<String, Object> data;
    private String error;

    public ModelView() {
        this.data = new HashMap<>();
    } 

    public ModelView(String url) {
        this.url = url;
        this.data = new HashMap<>();
    }
    public ModelView(String url,String error) {
        this.url = url;
        this.error = error;
        this.data = new HashMap<>();
    }

    public void addObject(String name, Object value) {
        data.put(name, value);
    }

    public String getUrl() {
        return url;
    }
    public String getError() {
        return error;
    }
    
    public void setUrl(String lien) {
        this.url = lien;
    }
    public void setError(String lien) {
        this.error = lien;
    }

    public HashMap<String, Object> getData() {
        return data;
    }
}
