package pt.ulisboa.ewp.node.domain.dto.filter.field;

import pt.ulisboa.ewp.node.domain.dto.filter.FilterDto;

public abstract class FieldFilterDto extends FilterDto {

  private final String field;

  public FieldFilterDto(String field) {
    this.field = field;
  }

  public String getField() {
    return field;
  }
}