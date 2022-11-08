package pt.ulisboa.ewp.node.api.ewp.controller.omobilities.las.cnr;

import eu.erasmuswithoutpaper.api.architecture.v1.EmptyV1;
import eu.erasmuswithoutpaper.api.omobilities.las.cnr.v1.LasIncomingStatsResponseV1;
import eu.erasmuswithoutpaper.api.omobilities.las.cnr.v1.LasIncomingStatsResponseV1.AcademicYearLaStats;
import eu.erasmuswithoutpaper.api.omobilities.las.cnr.v1.OmobilityLaCnrResponseV1;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.ewp.host.plugin.skeleton.provider.omobilities.las.cnr.OutgoingMobilityLearningAgreementCnrV1HostProvider;
import pt.ulisboa.ewp.node.api.ewp.controller.EwpApi;
import pt.ulisboa.ewp.node.api.ewp.security.EwpApiHostAuthenticationToken;
import pt.ulisboa.ewp.node.api.ewp.utils.EwpApiConstants;
import pt.ulisboa.ewp.node.api.ewp.utils.EwpApiParamConstants;
import pt.ulisboa.ewp.node.exception.ewp.EwpBadRequestException;
import pt.ulisboa.ewp.node.plugin.manager.host.HostPluginManager;

@RestController
@EwpApi
@RequestMapping(
    EwpApiConstants.API_BASE_URI + EwpApiOutgoingMobilityLearningAgreementCnrV1Controller.BASE_PATH)
public class EwpApiOutgoingMobilityLearningAgreementCnrV1Controller {

  public static final String BASE_PATH = "omobilities/las/cnr/v1";

  private final HostPluginManager hostPluginManager;

  private final String statsPortalHeiId;

  public EwpApiOutgoingMobilityLearningAgreementCnrV1Controller(
      HostPluginManager hostPluginManager,
      @Value("${stats.portal.heiId}") String statsPortalHeiId) {
    this.hostPluginManager = hostPluginManager;
    this.statsPortalHeiId = statsPortalHeiId;
  }

  @RequestMapping(method = {RequestMethod.GET,
      RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
  @Operation(
      summary = "Outgoing Mobility Learning Agreement CNR API.",
      tags = {"ewp"})
  public ResponseEntity<OmobilityLaCnrResponseV1> outgoingMobilityLearningAgreementCnr(
      @RequestParam(value = EwpApiParamConstants.SENDING_HEI_ID) String sendingHeiId,
      @RequestParam(value = EwpApiParamConstants.OMOBILITY_ID) List<String> omobilityIds) {

    Collection<OutgoingMobilityLearningAgreementCnrV1HostProvider> providers = hostPluginManager.getAllProvidersOfType(
        OutgoingMobilityLearningAgreementCnrV1HostProvider.class);
    for (OutgoingMobilityLearningAgreementCnrV1HostProvider provider : providers) {
      provider.onChangeNotification(sendingHeiId, omobilityIds);
    }

    return ResponseEntity.ok(new OmobilityLaCnrResponseV1(new EmptyV1()));
  }

  @RequestMapping(path = "/stats", method = {
      RequestMethod.GET}, produces = MediaType.APPLICATION_XML_VALUE)
  @Operation(summary = "Outgoing Mobility Learning Agreements CNR Stats API.", tags = {"ewp"})
  public ResponseEntity<LasIncomingStatsResponseV1> getStats(
      @RequestParam(EwpApiParamConstants.HEI_ID) String heiId,
      EwpApiHostAuthenticationToken authenticationToken) {

    if (!authenticationToken.getPrincipal().getHeiIdsCoveredByClient().contains(statsPortalHeiId)) {
      throw new EwpBadRequestException(
          "Unauthorized HEI IDs: " + authenticationToken.getPrincipal().getHeiIdsCoveredByClient());
    }

    Collection<OutgoingMobilityLearningAgreementCnrV1HostProvider> providers =
        hostPluginManager.getAllProvidersOfType(heiId,
            OutgoingMobilityLearningAgreementCnrV1HostProvider.class);

    Map<String, AcademicYearLaStats> receivingAcademicYearToLaStatsMap = new HashMap<>();
    for (OutgoingMobilityLearningAgreementCnrV1HostProvider provider : providers) {
      LasIncomingStatsResponseV1 stats = provider.getStats(heiId);
      for (AcademicYearLaStats currentProviderLaStats : stats.getAcademicYearLaStats()) {
        String receivingAcademicYearId = currentProviderLaStats.getReceivingAcademicYearId();
        receivingAcademicYearToLaStatsMap.computeIfAbsent(
            receivingAcademicYearId,
            EwpApiOutgoingMobilityLearningAgreementCnrV1Controller::createEmptyAcademicYearLaStats);

        receivingAcademicYearToLaStatsMap.put(receivingAcademicYearId,
            mergeAcademicYearLaStats(receivingAcademicYearToLaStatsMap.get(receivingAcademicYearId),
                currentProviderLaStats));
      }
    }

    LasIncomingStatsResponseV1 response = new LasIncomingStatsResponseV1();
    for (AcademicYearLaStats academicYearLaStats : receivingAcademicYearToLaStatsMap.values()) {
      response.getAcademicYearLaStats().add(academicYearLaStats);
    }

    return ResponseEntity.ok(response);
  }

  private static AcademicYearLaStats createEmptyAcademicYearLaStats(String receivingAcademicYear) {
    AcademicYearLaStats newLaStats = new AcademicYearLaStats();
    newLaStats.setReceivingAcademicYearId(receivingAcademicYear);
    newLaStats.setLaIncomingTotal(BigInteger.ZERO);
    newLaStats.setLaIncomingSomeVersionApproved(BigInteger.ZERO);
    newLaStats.setLaIncomingLatestVersionApproved(BigInteger.ZERO);
    newLaStats.setLaIncomingLatestVersionRejected(BigInteger.ZERO);
    newLaStats.setLaIncomingLatestVersionAwaiting(BigInteger.ZERO);
    return newLaStats;
  }

  private static AcademicYearLaStats mergeAcademicYearLaStats(AcademicYearLaStats firstLaStats,
      AcademicYearLaStats secondLaStats) {

    AcademicYearLaStats result = new AcademicYearLaStats();
    result.setReceivingAcademicYearId(firstLaStats.getReceivingAcademicYearId());
    if (!firstLaStats.getReceivingAcademicYearId()
        .equals(secondLaStats.getReceivingAcademicYearId())) {
      throw new IllegalArgumentException(
          "Statistics to merge must be of same receiving academic year");
    }

    result.setLaIncomingTotal(BigInteger.ZERO);
    if (firstLaStats.getLaIncomingTotal() != null) {
      result.setLaIncomingTotal(
          result.getLaIncomingTotal()
              .add(firstLaStats.getLaIncomingTotal()));
    }
    if (secondLaStats.getLaIncomingTotal() != null) {
      result.setLaIncomingTotal(
          result.getLaIncomingTotal()
              .add(secondLaStats.getLaIncomingTotal()));
    }

    result.setLaIncomingSomeVersionApproved(BigInteger.ZERO);
    if (firstLaStats.getLaIncomingSomeVersionApproved() != null) {
      result.setLaIncomingSomeVersionApproved(
          result.getLaIncomingSomeVersionApproved()
              .add(firstLaStats.getLaIncomingSomeVersionApproved()));
    }
    if (secondLaStats.getLaIncomingSomeVersionApproved() != null) {
      result.setLaIncomingSomeVersionApproved(
          result.getLaIncomingSomeVersionApproved()
              .add(secondLaStats.getLaIncomingSomeVersionApproved()));
    }

    result.setLaIncomingLatestVersionApproved(BigInteger.ZERO);
    if (firstLaStats.getLaIncomingLatestVersionApproved() != null) {
      result.setLaIncomingLatestVersionApproved(
          result.getLaIncomingLatestVersionApproved()
              .add(firstLaStats.getLaIncomingLatestVersionApproved()));
    }
    if (secondLaStats.getLaIncomingLatestVersionApproved() != null) {
      result.setLaIncomingLatestVersionApproved(
          result.getLaIncomingLatestVersionApproved()
              .add(secondLaStats.getLaIncomingLatestVersionApproved()));
    }

    result.setLaIncomingLatestVersionRejected(BigInteger.ZERO);
    if (firstLaStats.getLaIncomingLatestVersionRejected() != null) {
      result.setLaIncomingLatestVersionRejected(
          result.getLaIncomingLatestVersionRejected()
              .add(firstLaStats.getLaIncomingLatestVersionRejected()));
    }
    if (secondLaStats.getLaIncomingLatestVersionRejected() != null) {
      result.setLaIncomingLatestVersionRejected(
          result.getLaIncomingLatestVersionRejected()
              .add(secondLaStats.getLaIncomingLatestVersionRejected()));
    }

    result.setLaIncomingLatestVersionAwaiting(BigInteger.ZERO);
    if (firstLaStats.getLaIncomingLatestVersionAwaiting() != null) {
      result.setLaIncomingLatestVersionAwaiting(
          result.getLaIncomingLatestVersionAwaiting()
              .add(firstLaStats.getLaIncomingLatestVersionAwaiting()));
    }
    if (secondLaStats.getLaIncomingLatestVersionAwaiting() != null) {
      result.setLaIncomingLatestVersionAwaiting(
          result.getLaIncomingLatestVersionAwaiting()
              .add(secondLaStats.getLaIncomingLatestVersionAwaiting()));
    }

    return result;
  }
}
