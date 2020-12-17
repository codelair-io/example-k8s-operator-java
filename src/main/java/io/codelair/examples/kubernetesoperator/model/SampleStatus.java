package io.codelair.examples.kubernetesoperator.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import java.util.Objects;

@JsonDeserialize
public class SampleStatus implements KubernetesResource {
  private Long observedGeneration;

  public Long getObservedGeneration() {
    return observedGeneration;
  }

  public void setObservedGeneration(final Long observedGeneration) {
    this.observedGeneration = observedGeneration;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SampleStatus that = (SampleStatus) o;
    return Objects.equals(observedGeneration, that.observedGeneration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(observedGeneration);
  }

  @Override
  public String toString() {
    return "SampleStatus{" +
        "observedGeneration=" + observedGeneration +
        '}';
  }
}
