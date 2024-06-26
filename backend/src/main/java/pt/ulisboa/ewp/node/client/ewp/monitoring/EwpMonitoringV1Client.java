package pt.ulisboa.ewp.node.client.ewp.monitoring;

import eu.erasmuswithoutpaper.api.monitoring.v1.MonitoringResponseV1;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.ulisboa.ewp.node.api.ewp.utils.EwpApiParamConstants;
import pt.ulisboa.ewp.node.client.ewp.exception.EwpClientErrorException;
import pt.ulisboa.ewp.node.client.ewp.http.EwpHttpClient;
import pt.ulisboa.ewp.node.client.ewp.http.EwpHttpClient.ResponseBodySpecification;
import pt.ulisboa.ewp.node.client.ewp.operation.request.EwpRequest;
import pt.ulisboa.ewp.node.client.ewp.operation.request.body.EwpRequestFormDataUrlEncodedBody;
import pt.ulisboa.ewp.node.client.ewp.operation.result.EwpSuccessOperationResult;
import pt.ulisboa.ewp.node.client.ewp.registry.RegistryClient;
import pt.ulisboa.ewp.node.domain.entity.api.ewp.EwpMonitoringApiConfiguration;
import pt.ulisboa.ewp.node.utils.EwpApiSpecification.Monitoring;
import pt.ulisboa.ewp.node.utils.http.HttpParams;

@Service
public class EwpMonitoringV1Client {

  private static final Set<String> BLACKLIST_API_NAMES = Set.of("discovery", "echo", "monitoring", "registry");

  private final RegistryClient registryClient;
  private final EwpHttpClient ewpHttpClient;
  private final String monitoringHeiId;

  public EwpMonitoringV1Client(RegistryClient registryClient,
      EwpHttpClient ewpHttpClient, @Value("${stats.portal.heiId}") String monitoringHeiId) {
    this.registryClient = registryClient;
    this.ewpHttpClient = ewpHttpClient;
    this.monitoringHeiId = monitoringHeiId;
  }

  public EwpSuccessOperationResult<MonitoringResponseV1> reportIssue(String serverHeiId, String apiName,
      String endpointName, Integer httpCode, String serverMessage, String clientMessage)
      throws EwpClientErrorException {
    Objects.requireNonNull(serverHeiId);
    Objects.requireNonNull(apiName);

    if (BLACKLIST_API_NAMES.contains(apiName)) {
      throw new IllegalArgumentException("API name is blacklisted to report to monitoring");
    }

    if (!isErrorStatusCode(httpCode) && StringUtils.isEmpty(clientMessage)) {
      throw new IllegalArgumentException("Client message must be set for HTTP non-error responses");
    }

    EwpMonitoringApiConfiguration api = getApiConfigurationForHeiId(monitoringHeiId);

    HttpParams bodyParams = new HttpParams();
    bodyParams.param(EwpApiParamConstants.SERVER_HEI_ID, serverHeiId);
    bodyParams.param(EwpApiParamConstants.API_NAME, apiName);
    bodyParams.param(EwpApiParamConstants.ENDPOINT_NAME, endpointName);
    bodyParams.param(EwpApiParamConstants.HTTP_CODE, httpCode);
    bodyParams.param(EwpApiParamConstants.SERVER_MESSAGE, serverMessage);
    bodyParams.param(EwpApiParamConstants.CLIENT_MESSAGE, clientMessage);

    EwpRequest request =
        EwpRequest.createPost(api, "", api.getUrl(), new EwpRequestFormDataUrlEncodedBody(bodyParams));
    return ewpHttpClient.execute(
        request, ResponseBodySpecification.createStrict(MonitoringResponseV1.class));
  }

  private boolean isErrorStatusCode(int httpCode) {
    return 400 <= httpCode && httpCode < 600;
  }

  protected EwpMonitoringApiConfiguration getApiConfigurationForHeiId(
      String heiId) {
    return Monitoring.V1.getConfigurationForHeiId(registryClient, heiId);
  }
}
