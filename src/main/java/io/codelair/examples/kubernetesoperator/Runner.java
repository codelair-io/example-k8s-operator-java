package io.codelair.examples.kubernetesoperator;

import io.codelair.examples.kubernetesoperator.model.Sample;
import io.codelair.examples.kubernetesoperator.model.SampleList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
  private static final String KIND = "Sample";
  private static final String SINGULAR = KIND.toLowerCase();
  private static final String PLURAL = SINGULAR + 's';

  private static final Logger log = LoggerFactory.getLogger(Runner.class);

  public static final String GROUP = "io.codelair.examples.kubernetesoperator";
  public static final String NAME = PLURAL + '.' + GROUP;

  public static void main(final String[] args) {
    log.info("Preparing controller for CRD {}.", NAME);

    final var runner = new Runner();
    runner.start();
  }

  private void start() {
    try (final var client = new DefaultKubernetesClient()) {

      final var v1CrdOp = client.customResourceDefinitions();
      final var loadedCrd = v1CrdOp.load(getClass().getResource("/crd.yaml")).get();
      final var persistedCrd = v1CrdOp.createOrReplace(loadedCrd);
      final var crdContext = CustomResourceDefinitionContext.fromCrd(persistedCrd);

      final var informers = client.informers();
      final var sharedIndexInformer = informers.sharedIndexInformerForCustomResource(crdContext, Sample.class, SampleList.class, 60 * 1000L);
      final var crOp = client.customResources(crdContext, Sample.class, SampleList.class);

      final var controller = new Controller(client, sharedIndexInformer, crOp);
      controller.prepare();
      informers.startAllRegisteredInformers();
      controller.run();

    } catch (final Exception e) {
      log.error("An error occurred during controller processing, leaving ...", e);
      System.exit(-1);
    }
  }
}
