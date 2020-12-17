package io.codelair.examples.kubernetesoperator;

import io.codelair.examples.kubernetesoperator.model.Sample;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
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
    try (final var client = new KubernetesClientBuilder().build()) {

      final var v1CrdOp = client.apiextensions().v1().customResourceDefinitions();
      final var loadedCrd = v1CrdOp.load(getClass().getResource("/crd.yaml")).get();
      client.resource(loadedCrd).createOrReplace(); // This is ok

      final var informers = client.informers();
      final var sharedIndexInformer = informers.sharedIndexInformerFor(Sample.class, 60 * 1000L);

      final var controller = new Controller(client, sharedIndexInformer);
      controller.prepare();
      informers.startAllRegisteredInformers();
      controller.run();

    } catch (final Exception e) {
      log.error("An error occurred during controller processing, leaving ...", e);
      System.exit(-1);
    }
  }
}
