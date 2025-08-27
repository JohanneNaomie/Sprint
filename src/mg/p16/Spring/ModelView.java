package mg.p16.Spring;

import java.util.HashMap;


public class ModelView {
    private String url;
    private HashMap<String, Object> data;
    private String error;

    public ModelView() {
        this.data = new HashMap<>();
    } 

    public void getSessionAdd(MySession session, String name, Object object)throws Exception {
        // Get the class of the object
        Class<?> realObjectClass = object.getClass();

        // Retrieve the object from the session
        Object util = session.get(name);

        // Check if the object exists in the session
        if (util == null) {
            return; // If it doesn't exist, return without doing anything
        }

        // Cast the session object to its real class and add it to the ModelView
        if (realObjectClass.isInstance(util)) {
            this.addObject(name, util);
        } else {
            throw new IllegalArgumentException("The object in the session is not of the expected type: " + realObjectClass.getName());
        }
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
