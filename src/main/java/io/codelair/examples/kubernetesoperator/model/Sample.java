package io.codelair.examples.kubernetesoperator.model;

import io.fabric8.kubernetes.client.CustomResource;

public class Sample extends CustomResource<SampleSpec, SampleStatus> {
  @Override
  public SampleSpec getSpec() {
    if (super.getSpec() == null) {
      setSpec(new SampleSpec());
    }
    return super.getSpec();
  }

  @Override
  public SampleStatus getStatus() {
    if (super.getStatus() == null) {
      setStatus(new SampleStatus());
    }
    return super.getStatus();
  }
}
