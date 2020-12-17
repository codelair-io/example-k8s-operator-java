package io.codelair.examples.kubernetesoperator.model;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableSample extends CustomResourceDoneable<Sample> {
  public DoneableSample(final Sample resource, final Function<Sample, Sample> function) {
    super(resource, function);
  }
}
