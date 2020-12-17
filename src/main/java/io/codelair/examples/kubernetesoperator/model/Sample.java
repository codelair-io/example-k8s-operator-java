package io.codelair.examples.kubernetesoperator.model;

import io.codelair.examples.kubernetesoperator.Runner;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group(Runner.GROUP)
@Version("v1")
public class Sample extends CustomResource<SampleSpec, SampleStatus> implements Namespaced {
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
