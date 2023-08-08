package pt.ulisboa.ewp.node.domain.entity.communication.log;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import pt.ulisboa.ewp.node.domain.utils.DomainConstants;
import pt.ulisboa.ewp.node.utils.StringUtils;

@Entity
@Table(name = "COMMUNICATION_LOG")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "communication_type", discriminatorType = DiscriminatorType.STRING)
public class CommunicationLog {

  private long id;
  private ZonedDateTime startProcessingDateTime;
  private ZonedDateTime endProcessingDateTime;
  private String exceptionStacktrace;
  private String observations;
  private CommunicationLog parentCommunication;
  private Set<CommunicationLog> childrenCommunications = new HashSet<>();

  protected CommunicationLog() {}

  protected CommunicationLog(
      ZonedDateTime startProcessingDateTime,
      ZonedDateTime endProcessingDateTime,
      String observations,
      CommunicationLog parentCommunication) {
    this.startProcessingDateTime = startProcessingDateTime;
    this.endProcessingDateTime = endProcessingDateTime;
    setObservations(observations);
    setParentCommunication(parentCommunication);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", unique = true, nullable = false)
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "start_processing_date_time", nullable = false)
  public ZonedDateTime getStartProcessingDateTime() {
    return startProcessingDateTime;
  }

  public void setStartProcessingDateTime(ZonedDateTime startProcessingDateTime) {
    this.startProcessingDateTime = startProcessingDateTime;
  }

  @Column(name = "end_processing_date_time")
  public ZonedDateTime getEndProcessingDateTime() {
    return endProcessingDateTime;
  }

  public void setEndProcessingDateTime(ZonedDateTime endProcessingDateTime) {
    this.endProcessingDateTime = endProcessingDateTime;
  }

  @Column(name = "exception_stacktrace", columnDefinition = "TEXT")
  public String getExceptionStacktrace() {
    return exceptionStacktrace;
  }

  public void setExceptionStacktrace(String exceptionStacktrace) {
    this.exceptionStacktrace = exceptionStacktrace;
  }

  @Column(name = "observations", nullable = true, columnDefinition = "TEXT")
  public String getObservations() {
    return this.observations;
  }

  public void setObservations(String observations) {
    this.observations =
        StringUtils.truncateWithSuffix(
            observations, DomainConstants.MAX_TEXT_COLUMN_TEXT_LENGTH, "====TRUNCATED====");
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_communication_id")
  public CommunicationLog getParentCommunication() {
    return parentCommunication;
  }

  public void setParentCommunication(CommunicationLog parentCommunication) {
    this.parentCommunication = parentCommunication;
    if (this.parentCommunication != null) {
      this.parentCommunication.getChildrenCommunications().add(this);
    }
  }

  @Transient
  public Collection<CommunicationLog> getSortedChildrenCommunications() {
    return getChildrenCommunications().stream()
        .sorted(Comparator.comparing(CommunicationLog::getStartProcessingDateTime))
        .collect(Collectors.toList());
  }

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "parentCommunication", cascade = CascadeType.ALL)
  public Set<CommunicationLog> getChildrenCommunications() {
    return childrenCommunications;
  }

  public void setChildrenCommunications(Set<CommunicationLog> childrenCommunications) {
    this.childrenCommunications = childrenCommunications;
  }
}