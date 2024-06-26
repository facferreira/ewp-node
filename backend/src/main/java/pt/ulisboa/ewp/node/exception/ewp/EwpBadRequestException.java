package pt.ulisboa.ewp.node.exception.ewp;

public class EwpBadRequestException extends RuntimeException {

  private final String userMessage;
  private final String developerMessage;

  public EwpBadRequestException(String developerMessage) {
    this(null, developerMessage);
  }

  public EwpBadRequestException(String userMessage, String developerMessage) {
    this.userMessage = userMessage;
    this.developerMessage = developerMessage;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public String getDeveloperMessage() {
    return developerMessage;
  }

  @Override
  public String getMessage() {
    return getDeveloperMessage();
  }

  @Override
  public String toString() {
    return "EwpBadRequestException{"
        + "userMessage='"
        + userMessage
        + '\''
        + ", developerMessage='"
        + developerMessage
        + '\''
        + '}';
  }
}
