package pt.ulisboa.ewp.node.client.ewp;

import eu.erasmuswithoutpaper.api.architecture.ErrorResponse;
import java.io.Serializable;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import pt.ulisboa.ewp.node.client.ewp.exception.AbstractEwpClientErrorException;
import pt.ulisboa.ewp.node.client.ewp.exception.EwpClientErrorResponseException;
import pt.ulisboa.ewp.node.client.ewp.exception.EwpClientProcessorException;
import pt.ulisboa.ewp.node.client.ewp.operation.request.EwpRequest;
import pt.ulisboa.ewp.node.client.ewp.operation.response.EwpResponse;
import pt.ulisboa.ewp.node.client.ewp.operation.result.AbstractEwpOperationResult;
import pt.ulisboa.ewp.node.client.ewp.operation.result.error.EwpErrorResponseOperationResult;
import pt.ulisboa.ewp.node.client.ewp.operation.result.error.EwpInternalErrorOperationResult;
import pt.ulisboa.ewp.node.client.ewp.operation.result.error.EwpInvalidResponseOperationResult;
import pt.ulisboa.ewp.node.client.ewp.operation.result.success.EwpSuccessOperationResult;
import pt.ulisboa.ewp.node.domain.entity.api.ewp.auth.EwpAuthenticationMethod;
import pt.ulisboa.ewp.node.exception.XmlCannotUnmarshallToTypeException;
import pt.ulisboa.ewp.node.exception.ewp.EwpClientAuthenticationFailedException;
import pt.ulisboa.ewp.node.exception.ewp.EwpServerAuthenticationFailedException;
import pt.ulisboa.ewp.node.exception.ewp.EwpServerException;
import pt.ulisboa.ewp.node.service.http.log.ewp.EwpHttpCommunicationLogService;
import pt.ulisboa.ewp.node.service.keystore.KeyStoreService;
import pt.ulisboa.ewp.node.service.security.ewp.HttpSignatureService;
import pt.ulisboa.ewp.node.service.security.ewp.verifier.EwpAuthenticationResult;
import pt.ulisboa.ewp.node.service.security.ewp.verifier.response.ResponseAuthenticationVerifier;
import pt.ulisboa.ewp.node.utils.XmlUtils;
import pt.ulisboa.ewp.node.utils.http.HttpUtils;
import pt.ulisboa.ewp.node.utils.keystore.DecodedKeystore;
import pt.ulisboa.ewp.node.utils.keystore.KeyStoreUtil;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class EwpClient {

  @Autowired private Logger log;

  @Autowired private KeyStoreService keystoreService;

  @Autowired private HttpSignatureService httpSignatureService;

  @Autowired private ResponseAuthenticationVerifier responseAuthenticationVerifier;

  @Autowired private EwpHttpCommunicationLogService ewpHttpCommunicationLogService;

  /**
   * Sends a request to the target API, resolving its response, returning it only upon success. If a
   * request fails or the response obtained indicates an error then a corresponding exception is
   * thrown.
   *
   * @param request Request to send
   * @param responseBodyType Expected response's body class type upon success.
   * @return
   * @throws EwpClientProcessorException Request/Response processing failed for some reason.
   * @throws EwpClientErrorResponseException Target API returned an error response (see {@see
   *     eu.erasmuswithoutpaper.api.architecture.ErrorResponse}).
   * @throws pt.ulisboa.ewp.node.client.ewp.exception.EwpClientInvalidResponseException Target API
   *     returned an invalid response.
   */
  @SuppressWarnings("unchecked")
  public <T extends Serializable> EwpSuccessOperationResult<T> executeWithLoggingExpectingSuccess(
      EwpRequest request, Class<T> responseBodyType) throws AbstractEwpClientErrorException {
    AbstractEwpOperationResult operationResult = executeWithLogging(request, responseBodyType);
    return getSuccessOperationResult(operationResult);
  }

  private <T extends Serializable> EwpSuccessOperationResult<T> getSuccessOperationResult(
      AbstractEwpOperationResult operationResult) throws AbstractEwpClientErrorException {
    if (operationResult.isSuccess()) {
      return operationResult.asSuccess();
    } else if (operationResult.isError()) {
      throw operationResult.asError().toClientException();
    } else {
      throw new IllegalArgumentException("Unknown result type: " + operationResult.getResultType());
    }
  }

  private <T extends Serializable> AbstractEwpOperationResult executeWithLogging(
      EwpRequest request, Class<T> responseBodyType) {
    ZonedDateTime startProcessingDateTime = ZonedDateTime.now();
    AbstractEwpOperationResult operationResult = execute(request, responseBodyType);
    ZonedDateTime endProcessingDateTime = ZonedDateTime.now();
    ewpHttpCommunicationLogService.logCommunicationToEwpNode(
        operationResult,
        startProcessingDateTime,
        endProcessingDateTime,
        operationResult.getSummary());

    return operationResult;
  }

  protected <T extends Serializable> AbstractEwpOperationResult execute(
      EwpRequest request, Class<T> expectedResponseBodyType) {
    EwpResponse ewpResponse = null;
    EwpAuthenticationResult responseAuthenticationResult = null;
    try {
      Client client = getClient();

      WebTarget target = client.target(request.getUrl());
      target.property("http.autoredirect", true);

      signRequest(request, target);
      Invocation invocation = buildRequest(request, target);

      log.info("Sending EWP request to: {}", request.getUrl());

      Response response = invocation.invoke();

      sanitizeResponse(response);

      EwpResponse.Builder responseBuilder =
          new EwpResponse.Builder(HttpStatus.resolve(response.getStatus()));
      responseBuilder.mediaType(response.getMediaType().toString());

      response
          .getHeaders()
          .forEach(
              (headerName, headerValues) ->
                  responseBuilder.header(
                      headerName,
                      headerValues.stream().map(String::valueOf).collect(Collectors.toList())));

      if (response.hasEntity()) {
        response.bufferEntity();

        responseBuilder.rawBody(response.readEntity(String.class));
      }

      ewpResponse = responseBuilder.build();

      responseAuthenticationResult =
          responseAuthenticationVerifier.verifyAgainstMethod(request, ewpResponse);
      if (!responseAuthenticationResult.isValid()) {
        throw new EwpServerAuthenticationFailedException(
            request, ewpResponse, responseAuthenticationResult);
      }

      return resolveResponseToOperationStatus(
          request, expectedResponseBodyType, ewpResponse, responseAuthenticationResult);

    } catch (NoSuchAlgorithmException
        | KeyManagementException
        | UnrecoverableKeyException
        | KeyStoreException
        | NoSuchProviderException e) {
      log.error("Failed to initialize EWP client", e);
      return new EwpInternalErrorOperationResult.Builder().request(request).exception(e).build();

    } catch (EwpServerAuthenticationFailedException | XmlCannotUnmarshallToTypeException e) {
      log.error("Invalid server's response", e);
      return new EwpInvalidResponseOperationResult.Builder(e)
          .request(request)
          .response(ewpResponse)
          .responseAuthenticationResult(responseAuthenticationResult)
          .build();

    } catch (Exception e) {
      log.error("Failed to execute request", e);
      return new EwpInternalErrorOperationResult.Builder()
          .request(request)
          .response(ewpResponse)
          .responseAuthenticationResult(responseAuthenticationResult)
          .exception(e)
          .build();
    }
  }

  private <T extends Serializable> AbstractEwpOperationResult resolveResponseToOperationStatus(
      EwpRequest request,
      Class<T> expectedResponseBodyType,
      EwpResponse ewpResponse,
      EwpAuthenticationResult responseAuthenticationResult)
      throws XmlCannotUnmarshallToTypeException {
    if (ewpResponse.isClientError()) {
      return resolveClientErrorToOperationResult(
          request, ewpResponse, responseAuthenticationResult);

    } else if (ewpResponse.isServerError()) {
      return new EwpInvalidResponseOperationResult.Builder(
              new EwpServerException(request, ewpResponse))
          .request(request)
          .response(ewpResponse)
          .responseAuthenticationResult(responseAuthenticationResult)
          .build();

    } else if (ewpResponse.isSuccess()) {
      T responseBody = XmlUtils.unmarshall(ewpResponse.getRawBody(), expectedResponseBodyType);
      return new EwpSuccessOperationResult.Builder<T>()
          .request(request)
          .response(ewpResponse)
          .responseAuthenticationResult(responseAuthenticationResult)
          .responseBody(responseBody)
          .build();

    } else {
      throw new IllegalStateException(
          "Cannot handle response's status code: " + ewpResponse.getStatus());
    }
  }

  private AbstractEwpOperationResult resolveClientErrorToOperationResult(
      EwpRequest request,
      EwpResponse ewpResponse,
      EwpAuthenticationResult responseAuthenticationResult)
      throws XmlCannotUnmarshallToTypeException {
    ErrorResponse errorResponse =
        XmlUtils.unmarshall(ewpResponse.getRawBody(), ErrorResponse.class);
    if (HttpStatus.BAD_REQUEST.equals(ewpResponse.getStatus())) {
      return new EwpErrorResponseOperationResult.Builder()
          .request(request)
          .response(ewpResponse)
          .responseAuthenticationResult(responseAuthenticationResult)
          .errorResponse(errorResponse)
          .build();
    } else {
      return new EwpInternalErrorOperationResult.Builder()
          .request(request)
          .response(ewpResponse)
          .responseAuthenticationResult(responseAuthenticationResult)
          .exception(
              new EwpClientAuthenticationFailedException(
                  request, ewpResponse, errorResponse.getDeveloperMessage().getValue()))
          .build();
    }
  }

  private void sanitizeResponse(Response response) {
    // NOTE: sanitize possibly wrong XML content type header
    // namely, some servers respond with a Content-Type like "xml;charset=ISO-8859-1" which is not
    // considered corrected for Jersey since it contains only the subtype and not the type
    String contentType = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
    if (contentType.matches("[ \t]*xml[ \t]*;[ \t]*charset=.*")) {
      String correctContentType = contentType.replace("xml", "application/xml");
      response.getMetadata().putSingle(HttpHeaders.CONTENT_TYPE, correctContentType);
    }
  }

  private Client getClient()
      throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException,
          UnrecoverableKeyException, KeyManagementException {
    DecodedKeystore decodedKeystore = keystoreService.getDecodedKeyStoreFromStorage();
    SSLContext sslContext =
        createSecurityContext(
            decodedKeystore.getKeyStore(), null, decodedKeystore.getKeyStorePassword());
    return ClientBuilder.newBuilder()
        .sslContext(sslContext)
        .hostnameVerifier((hostname, session) -> hostname.equalsIgnoreCase(session.getPeerHost()))
        .build();
  }

  private Invocation buildRequest(EwpRequest request, WebTarget target) {
    Invocation.Builder requestBuilder = target.request();
    setRequestHeaders(requestBuilder, request);

    switch (request.getMethod()) {
      case GET:
        return buildGetRequest(requestBuilder);

      case POST:
        return buildPostRequest(requestBuilder, request);

      default:
        throw new IllegalArgumentException("Unsupported method: " + request.getMethod().name());
    }
  }

  private void setRequestHeaders(Invocation.Builder requestBuilder, EwpRequest request) {
    HttpUtils.toHeadersMap(request.getHeaders()).forEach(requestBuilder::header);
  }

  private Invocation buildGetRequest(Invocation.Builder requestBuilder) {
    return requestBuilder.buildGet();
  }

  private Invocation buildPostRequest(Invocation.Builder requestBuilder, EwpRequest request) {
    String formData = HttpUtils.serializeFormData(request.getBodyParams());
    Entity<String> entity = Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    return requestBuilder.buildPost(entity);
  }

  private void signRequest(EwpRequest request, WebTarget target) {
    if (request.getAuthenticationMethod().equals(EwpAuthenticationMethod.HTTP_SIGNATURE)) {
      httpSignatureService.signRequest(
          request.getMethod().name(),
          target.getUri(),
          HttpUtils.serializeFormData(request.getBodyParams()),
          request.getId(),
          request::header);
    }
  }

  private static SSLContext createSecurityContext(
      KeyStore keyStore, KeyStore trustStore, String password)
      throws NoSuchProviderException, NoSuchAlgorithmException, UnrecoverableKeyException,
          KeyStoreException, KeyManagementException {
    KeyManager[] keyManagers = null;
    if (!KeyStoreUtil.isSelfIssued(
        keyStore, (X509Certificate) keyStore.getCertificate(keyStore.aliases().nextElement()))) {
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
      keyManagerFactory.init(keyStore, password.toCharArray());
      keyManagers = keyManagerFactory.getKeyManagers();
    }

    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);

    SSLContext context = SSLContext.getInstance("TLS", "SunJSSE");
    context.init(
        keyManagers, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());
    return context;
  }
}