package pt.ulisboa.ewp.node.service.ewp.mapping.sync;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.erasmuswithoutpaper.api.iias.v7.endpoints.IiasGetResponseV7;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pt.ulisboa.ewp.host.plugin.skeleton.provider.iias.InterInstitutionalAgreementsV7HostProvider;
import pt.ulisboa.ewp.host.plugin.skeleton.provider.iias.MockInterInstitutionalAgreementsV7HostProvider;
import pt.ulisboa.ewp.node.config.sync.SyncMappingsProperties;
import pt.ulisboa.ewp.node.config.sync.SyncProperties;
import pt.ulisboa.ewp.node.domain.entity.mapping.EwpInterInstitutionalAgreementMapping;
import pt.ulisboa.ewp.node.plugin.manager.host.HostPluginManager;
import pt.ulisboa.ewp.node.service.ewp.mapping.EwpInterInstitutionalAgreementMappingService;

class EwpInterInstitutionalAgreementsMappingSyncServiceUnitTest {

  @Test
  void testRun_TwoHostPluginsV7WithOneNewIiaIdEach_TwoMappingsArePersisted() {
    HostPluginManager hostPluginManager = Mockito.mock(HostPluginManager.class);
    EwpInterInstitutionalAgreementMappingService mappingService = Mockito.mock(
        EwpInterInstitutionalAgreementMappingService.class);
    SyncProperties syncProperties = SyncProperties.create(SyncMappingsProperties.create(1000));
    EwpInterInstitutionalAgreementMappingSyncService syncService = new EwpInterInstitutionalAgreementMappingSyncService(
        syncProperties, hostPluginManager, mappingService);

    List<String> heiIds = Arrays.asList("h1", "h2");
    List<String> ounitIds = Arrays.asList("o1", "o2");
    List<String> iiaIds = Arrays.asList("id1", "id2");
    List<String> iiaCodes = Arrays.asList("ic1", "ic2");

    List<IiasGetResponseV7.Iia> iias = new ArrayList<>();
    for (int index = 0; index < heiIds.size(); index++) {
      IiasGetResponseV7.Iia iia = new IiasGetResponseV7.Iia();
      IiasGetResponseV7.Iia.Partner partner = new IiasGetResponseV7.Iia.Partner();
      partner.setHeiId(heiIds.get(index));
      partner.setOunitId(ounitIds.get(index));
      partner.setIiaId(iiaIds.get(index));
      partner.setIiaCode(iiaCodes.get(index));
      iia.getPartner().add(partner);
      iias.add(iia);
    }

    Map<String, Collection<MockInterInstitutionalAgreementsV7HostProvider>> providersPerHeiId = new HashMap<>();
    MockInterInstitutionalAgreementsV7HostProvider provider1 = Mockito.spy(
        new MockInterInstitutionalAgreementsV7HostProvider(
            1, 1).registerIia(heiIds.get(0),
            iiaIds.get(0), iiaCodes.get(0), iias.get(0)));
    providersPerHeiId.put(heiIds.get(0), List.of(provider1));

    MockInterInstitutionalAgreementsV7HostProvider provider2 = Mockito.spy(
        new MockInterInstitutionalAgreementsV7HostProvider(
            1, 1).registerIia(heiIds.get(1),
            iiaIds.get(1), iiaCodes.get(1), iias.get(1)));
    providersPerHeiId.put(heiIds.get(1), List.of(provider2));
    doReturn(providersPerHeiId).when(hostPluginManager)
        .getAllProvidersOfTypePerHeiId(InterInstitutionalAgreementsV7HostProvider.class);

    syncService.run();

    verify(provider1, times(1)).findByHeiIdAndIiaIds(heiIds.get(0),
        heiIds.get(0),
        Collections.singletonList(iiaIds.get(0)));
    verify(mappingService, times(1)).registerMapping(heiIds.get(0), ounitIds.get(0), iiaIds.get(0));

    verify(provider2, times(1)).findByHeiIdAndIiaIds(heiIds.get(1),
        heiIds.get(1),
        Collections.singletonList(iiaIds.get(1)));
    verify(mappingService, times(1)).registerMapping(heiIds.get(1), ounitIds.get(1), iiaIds.get(1));
  }

  @Test
  void testRun_TwoHostPluginsV7WithOneNewAndOneKnownIiaIds_OneNewMappingIsPersisted() {
    HostPluginManager hostPluginManager = Mockito.mock(HostPluginManager.class);
    EwpInterInstitutionalAgreementMappingService mappingService = Mockito.mock(
        EwpInterInstitutionalAgreementMappingService.class);
    SyncProperties syncProperties = SyncProperties.create(SyncMappingsProperties.create(1000));
    EwpInterInstitutionalAgreementMappingSyncService syncService = new EwpInterInstitutionalAgreementMappingSyncService(
        syncProperties, hostPluginManager, mappingService);

    List<String> heiIds = Arrays.asList("h1", "h2");
    List<String> ounitIds = Arrays.asList("o1", "o2");
    List<String> iiaIds = Arrays.asList("id1", "id2");
    List<String> iiaCodes = Arrays.asList("ic1", "ic2");

    List<IiasGetResponseV7.Iia> iias = new ArrayList<>();
    for (int index = 0; index < heiIds.size(); index++) {
      IiasGetResponseV7.Iia iia = new IiasGetResponseV7.Iia();
      IiasGetResponseV7.Iia.Partner partner = new IiasGetResponseV7.Iia.Partner();
      partner.setHeiId(heiIds.get(index));
      partner.setOunitId(ounitIds.get(index));
      partner.setIiaId(iiaIds.get(index));
      partner.setIiaCode(iiaCodes.get(index));
      iia.getPartner().add(partner);
      iias.add(iia);
    }

    Map<String, Collection<InterInstitutionalAgreementsV7HostProvider>> providersPerHeiId = new HashMap<>();
    MockInterInstitutionalAgreementsV7HostProvider provider1 = Mockito.spy(
        new MockInterInstitutionalAgreementsV7HostProvider(
            1, 1).registerIia(heiIds.get(0),
            iiaIds.get(0), iiaCodes.get(0), iias.get(0)));
    providersPerHeiId.put(heiIds.get(0), List.of(provider1));

    MockInterInstitutionalAgreementsV7HostProvider provider2 = Mockito.spy(
        new MockInterInstitutionalAgreementsV7HostProvider(
            1, 1).registerIia(heiIds.get(1),
            iiaIds.get(1), iiaCodes.get(1), iias.get(1)));
    providersPerHeiId.put(heiIds.get(1), List.of(provider2));
    doReturn(providersPerHeiId).when(hostPluginManager)
        .getAllProvidersOfTypePerHeiId(InterInstitutionalAgreementsV7HostProvider.class);

    doReturn(
            Optional.of(
                EwpInterInstitutionalAgreementMapping.create(
                    heiIds.get(1), ounitIds.get(1), iiaIds.get(1))))
        .when(mappingService)
        .getMapping(heiIds.get(1), iiaIds.get(1));

    syncService.run();

    verify(provider1, times(1)).findByHeiIdAndIiaIds(heiIds.get(0),
        heiIds.get(0),
        Collections.singletonList(iiaIds.get(0)));
    verify(mappingService, times(1)).registerMapping(heiIds.get(0), ounitIds.get(0), iiaIds.get(0));

    verify(provider2, times(0)).findByHeiIdAndIiaIds(heiIds.get(1),
        heiIds.get(0),
        Collections.singletonList(iiaIds.get(0)));
    verify(mappingService, times(0)).registerMapping(heiIds.get(1), ounitIds.get(1), iiaIds.get(1));
  }

}