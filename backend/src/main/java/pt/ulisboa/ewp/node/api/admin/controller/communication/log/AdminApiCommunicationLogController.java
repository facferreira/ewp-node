package pt.ulisboa.ewp.node.api.admin.controller.communication.log;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.Operation;
import java.io.Serializable;
import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.ewp.node.api.admin.controller.AdminApi;
import pt.ulisboa.ewp.node.api.admin.dto.response.AdminApiResponseWithDataDto;
import pt.ulisboa.ewp.node.api.admin.security.AdminApiSecurityCommonConstants;
import pt.ulisboa.ewp.node.api.admin.utils.AdminApiConstants;
import pt.ulisboa.ewp.node.api.admin.utils.AdminApiResponseUtils;
import pt.ulisboa.ewp.node.domain.deserializer.filter.FilterDtoDeserializer;
import pt.ulisboa.ewp.node.domain.dto.communication.log.CommunicationLogDetailDto;
import pt.ulisboa.ewp.node.domain.dto.communication.log.CommunicationLogSummaryDto;
import pt.ulisboa.ewp.node.domain.dto.filter.FilterDto;
import pt.ulisboa.ewp.node.service.communication.log.CommunicationLogService;

@AdminApi
@RestController
@RequestMapping(AdminApiConstants.API_BASE_URI + "communications/logs")
@Secured({AdminApiSecurityCommonConstants.ROLE_ADMIN_WITH_PREFIX})
@Validated
public class AdminApiCommunicationLogController {

  private final CommunicationLogService communicationLogService;

  public AdminApiCommunicationLogController(CommunicationLogService communicationLogService) {
    this.communicationLogService = communicationLogService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Retrieves communication logs in summary format.",
      tags = {"Admin"})
  public ResponseEntity<AdminApiResponseWithDataDto<GetCommunicationLogsSummaryResponseDto>>
      getCommunicationLogs(@Valid @RequestBody GetCommunicationLogsRequestDto requestDto) {
    Collection<CommunicationLogSummaryDto> communicationLogDtos =
        this.communicationLogService.findByFilter(
            requestDto.getFilter(), requestDto.getOffset(), requestDto.getLimit());
    long totalResults = this.communicationLogService.countByFilter(requestDto.getFilter());
    GetCommunicationLogsSummaryResponseDto responseDto = new GetCommunicationLogsSummaryResponseDto(
        communicationLogDtos, totalResults);
    return AdminApiResponseUtils.toOkResponseEntity(responseDto);
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Retrieves a communication log in detail.",
      tags = {"Admin"})
  public ResponseEntity<AdminApiResponseWithDataDto<CommunicationLogDetailDto>>
  getCommunicationLogs(@Min(1) @PathVariable(name = "id") long id) {
    CommunicationLogDetailDto communicationLogDetailDto = this.communicationLogService.findById(id);
    return AdminApiResponseUtils.toOkResponseEntity(communicationLogDetailDto);
  }

  private static class GetCommunicationLogsRequestDto implements Serializable {

    @JsonDeserialize(using = FilterDtoDeserializer.class)
    private FilterDto filter;

    @Min(0)
    private int offset;

    @Min(1)
    @Max(50)
    private int limit;

    public FilterDto getFilter() {
      return filter;
    }

    public void setFilter(FilterDto filter) {
      this.filter = filter;
    }

    public int getOffset() {
      return offset;
    }

    public void setOffset(int offset) {
      this.offset = offset;
    }

    public int getLimit() {
      return limit;
    }

    public void setLimit(int limit) {
      this.limit = limit;
    }
  }

  private static class GetCommunicationLogsSummaryResponseDto implements Serializable {

    private final Collection<CommunicationLogSummaryDto> communicationLogs;
    private final long totalResults;

    private GetCommunicationLogsSummaryResponseDto(
        Collection<CommunicationLogSummaryDto> communicationLogs, long totalResults) {
      this.communicationLogs = communicationLogs;
      this.totalResults = totalResults;
    }

    public Collection<CommunicationLogSummaryDto> getCommunicationLogs() {
      return communicationLogs;
    }

    public long getTotalResults() {
      return totalResults;
    }
  }
}