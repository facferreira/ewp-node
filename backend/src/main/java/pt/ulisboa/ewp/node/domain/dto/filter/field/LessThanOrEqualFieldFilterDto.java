package pt.ulisboa.ewp.node.domain.dto.filter.field;

import java.util.Objects;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class LessThanOrEqualFieldFilterDto extends FieldFilterDto {

  private final Number value;

  public LessThanOrEqualFieldFilterDto(String field, Number value) {
    super(field);
    Objects.requireNonNull(value);
    this.value = value;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public Predicate createPredicate(CriteriaBuilder criteriaBuilder, Root<?> selection) {
    return criteriaBuilder.le(selection.get(getField()), value);
  }
}