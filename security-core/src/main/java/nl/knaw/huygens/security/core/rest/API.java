package nl.knaw.huygens.security.core.rest;

public interface API {
    public static final String ID_PARAM = "id";
    public static final String SESSION_AUTHENTICATION_URI = "/sessions";
    public static final String SESSION_AUTHENTICATION_PATH = "/{" + ID_PARAM + "}";

    public static final String SESSION_ID_HTTP_PARAM = "hsid";
    public static final String REDIRECT_URL_HTTP_PARAM = "hsurl";
}
