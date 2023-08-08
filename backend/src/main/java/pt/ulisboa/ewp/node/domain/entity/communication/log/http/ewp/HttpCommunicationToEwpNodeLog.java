package pt.ulisboa.ewp.node.domain.entity.communication.log.http.ewp;

import java.io.IOException;
import java.time.ZonedDateTime;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import pt.ulisboa.ewp.node.domain.entity.api.ewp.auth.EwpAuthenticationMethod;
import pt.ulisboa.ewp.node.domain.entity.communication.log.http.HttpCommunicationLog;
import pt.ulisboa.ewp.node.domain.entity.communication.log.http.HttpRequestLog;
import pt.ulisboa.ewp.node.domain.entity.communication.log.http.HttpResponseLog;

@Entity
@DiscriminatorValue("EWP_OUT")
public class HttpCommunicationToEwpNodeLog extends EwpHttpCommunicationLog {

  public HttpCommunicationToEwpNodeLog() {}

  public HttpCommunicationToEwpNodeLog(
      EwpAuthenticationMethod authenticationMethod,
      HttpRequestLog request,
      HttpResponseLog response,
      ZonedDateTime startProcessingDateTime,
      ZonedDateTime endProcessingDateTime,
      String observations,
      HttpCommunicationLog parentCommunication) throws IOException {
    super(
        authenticationMethod,
        request,
        response,
        startProcessingDateTime,
        endProcessingDateTime,
        observations,
        parentCommunication);
  }
}