package com.pinterest.deployservice.bean.rodimus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsgSummaryBean {
  private Integer minSize;
  private Integer maxSize;
}
