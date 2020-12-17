package io.codelair.examples.kubernetesoperator.model;

import io.fabric8.kubernetes.client.CustomResource;
import java.util.Objects;

public class Sample extends CustomResource {
  private SampleSpec spec;
  private SampleStatus status;

  public SampleSpec getSpec() {
    if (spec == null) {
      spec = new SampleSpec();
    }
    return spec;
  }

  public void setSpec(final SampleSpec spec) {
    this.spec = spec;
  }

  public SampleStatus getStatus() {
    if (status == null) {
      status = new SampleStatus();
    }
    return status;
  }

  public void setStatus(final SampleStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Sample sample = (Sample) o;
    return Objects.equals(spec, sample.spec) && Objects.equals(status, sample.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public String toString() {
    return "Sample{" +
        "spec=" + spec +
        ", status=" + status +
        "} " + super.toString();
  }
}
