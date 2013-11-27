package nl.knaw.huygens.security.server;

public class MissingParameterException extends BadRequestException {
    public MissingParameterException(String parameterName) {
        super("Missing parameter '" + parameterName + "'");
    }
}
