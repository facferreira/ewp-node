package pt.ulisboa.ewp.node.api.ewp.security;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import pt.ulisboa.ewp.node.api.common.filter.security.logging.MDCAuthenticationFilter;
import pt.ulisboa.ewp.node.api.ewp.security.filter.EwpApiAuthenticationFilter;
import pt.ulisboa.ewp.node.api.ewp.security.filter.EwpApiPreAuthenticationFilter;
import pt.ulisboa.ewp.node.api.ewp.security.filter.EwpApiResponseCommunicationIdFillerFilter;
import pt.ulisboa.ewp.node.api.ewp.security.filter.EwpApiResponseSignerFilter;
import pt.ulisboa.ewp.node.api.ewp.utils.EwpApiConstants;
import pt.ulisboa.ewp.node.client.ewp.registry.RegistryClient;
import pt.ulisboa.ewp.node.config.security.SecurityProperties;
import pt.ulisboa.ewp.node.service.communication.log.http.ewp.EwpHttpCommunicationLogService;
import pt.ulisboa.ewp.node.service.ewp.security.signer.response.ResponseAuthenticationSigner;
import pt.ulisboa.ewp.node.service.ewp.security.verifier.request.AbstractRequestAuthenticationMethodVerifier;
import pt.ulisboa.ewp.node.utils.http.converter.xml.Jaxb2HttpMessageConverter;

@Configuration
public class EwpApiSecurityConfig {

  @Autowired
  private Collection<AbstractRequestAuthenticationMethodVerifier>
      requestAuthenticationMethodVerifiers;

  @Autowired private ResponseAuthenticationSigner responseSigner;

  @Autowired private SecurityProperties securityProperties;

  @Autowired private RegistryClient registryClient;

  @Autowired private Jaxb2HttpMessageConverter jaxb2HttpMessageConverter;

  @Autowired private EwpHttpCommunicationLogService ewpHttpCommunicationLogService;

  @Bean
  @Order(3)
  public SecurityFilterChain ewpFilterChain(HttpSecurity http) throws Exception {
    http.antMatcher(EwpApiConstants.API_BASE_URI + "**")
        .cors()
        .and()
        .csrf()
        .disable()
        .authorizeRequests()
        .antMatchers(EwpApiConstants.API_BASE_URI + "manifest")
        .permitAll()
        .antMatchers(EwpApiConstants.API_BASE_URI + "**")
        .authenticated()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(new UnauthorizedAuthenticationEntryPoint());

    http.addFilterBefore(new EwpApiResponseSignerFilter(responseSigner), HeaderWriterFilter.class);
    http.addFilterBefore(
        new EwpApiResponseCommunicationIdFillerFilter(), EwpApiResponseSignerFilter.class);

    http.addFilterBefore(
        new EwpApiPreAuthenticationFilter(jaxb2HttpMessageConverter),
        BasicAuthenticationFilter.class);
    http.addFilterAfter(
        new EwpApiAuthenticationFilter(
            requestAuthenticationMethodVerifiers, ewpHttpCommunicationLogService),
        EwpApiPreAuthenticationFilter.class);

    http.addFilterAfter(new MDCAuthenticationFilter(), SessionManagementFilter.class);

    return http.build();
  }

  private static class UnauthorizedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private UnauthorizedAuthenticationEntryPoint() {}

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException) {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
    }
  }
}
