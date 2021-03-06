package com.hsl.journeyplanner.request;

import com.hsl.journeyplanner.Credential;
import com.hsl.journeyplanner.Settings;
import com.hsl.journeyplanner.annotation.Title;
import com.hsl.journeyplanner.connection.Connection;
import com.hsl.journeyplanner.resource.common.Coordinate;
import com.hsl.journeyplanner.util.Utils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.List;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;

/**
 *
 * @author  Koskela Jani
 */
public abstract class Request {
    
    private static final String BASE_URL = "http://api.reittiopas.fi/hsl/prod/?request=";
    private static final String ENCODING_UTF8 = "UTF-8";
    protected boolean useClientValidation = true;

    private void validateRequest() throws IllegalArgumentException {
        Validator validator = new Validator();
        List<ConstraintViolation> violations = validator.validate(this);
        if (violations.size() > 0)
            throw new IllegalArgumentException(violations.toString());
    }
    
    protected String getRequestUrl() {
        String requestUrl = "";
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String key = field.getAnnotation(Title.class).value();
                String value;
                if (field.get(this) instanceof List) {
                    value = Utils.convertListToString((List) field.get(this));
                } else if (field.get(this) instanceof Coordinate) {
                    value = ((Coordinate) field.get(this)).toString();
                } else {
                    value = field.get(this).toString();
                }
                if (value != null) {
                    try {
                        requestUrl += key + "=" + URLEncoder.encode(value, ENCODING_UTF8) + "&";
                    } catch (UnsupportedEncodingException es) {}
                }
            }catch (IllegalArgumentException e) {
                continue;
            }catch (IllegalAccessException ex) {
                continue;
            }catch (NullPointerException ee) {
                continue;
            }
        }
        if (requestUrl.endsWith(("&")))
            requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
        return requestUrl;
    }
        
    private String getUrl() throws IllegalArgumentException {
        return (BASE_URL + this.getActionName() + "&user=" + Credential.getApiUsernmae() + "&pass=" + Credential.getApiPassword() + "&epsg_in=" + Settings.getInCoordinateSystem() + "&epsg_out=" + Settings.getOutCoordinateSystem() + "&format=json&" + this.getRequestUrl()).trim();
    }
    
    public <T>List<T> execute() throws IllegalArgumentException, IOException {
        if (this.useClientValidation)
            this.validateRequest();
        String url = this.getUrl();
        String resp = Connection.sendRequest(url);
        return this.parseResponse(resp);
    }
    
    protected abstract String getActionName();
    protected abstract <T> List<T> parseResponse(String response);

}
