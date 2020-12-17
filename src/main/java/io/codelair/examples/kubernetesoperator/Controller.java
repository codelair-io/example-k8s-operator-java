package io.codelair.examples.kubernetesoperator;

import io.codelair.examples.kubernetesoperator.model.Sample;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {
  private static final Logger log = LoggerFactory.getLogger(Controller.class);

  private final KubernetesClient client;
  private final SharedIndexInformer<Sample> sharedIndexInformer;
  private final Lister<Sample> lister;
  private final ArrayBlockingQueue<String> queue;

  public Controller(final KubernetesClient client,
                    final SharedIndexInformer<Sample> sharedIndexInformer) {

    this.client = client;
    this.sharedIndexInformer = sharedIndexInformer;
    this.lister = new Lister<>(sharedIndexInformer.getIndexer());
    this.queue = new ArrayBlockingQueue<>(2048, true);
  }

  public void prepare() {
    sharedIndexInformer.addEventHandler(new ResourceEventHandler<>() {
      @Override
      public void onAdd(final Sample obj) {
        enqueue(obj);
      }

      @Override
      public void onUpdate(final Sample oldObj, final Sample newObj) {
        if (oldObj.getMetadata().getResourceVersion().equals(newObj.getMetadata().getResourceVersion())) {
          return;
        }

        enqueue(newObj);
      }

      @Override
      public void onDelete(final Sample obj, final boolean deletedFinalStateUnknown) {
        // do nothing, this should be handled by the finalizer
      }
    });
  }

  private void enqueue(final Sample obj) {
    queue.add(Cache.metaNamespaceKeyFunc(obj));
  }

  public void run() {
    log.info("Starting controller ...");
    while (!sharedIndexInformer.hasSynced()) {
      log.debug("Still waiting for sync...");
    }

    while (true) {
      try {
        final var key = queue.take();
        final var obj = lister.get(key);
        if (obj == null) {
          log.info("Custom resource {} no longer exists.", key);
        } else {
          reconcile(obj);
        }
      } catch (final InterruptedException e) {
        log.info("Controller interrupted, bailing out.");
        break;
      }
    }
  }

  private void reconcile(final Sample obj) {
    log.info("Reconcile for custom resource {} ...", obj);

    final var namespace = obj.getMetadata().getNamespace();
    final var name = obj.getMetadata().getName();

    if (obj.getMetadata().getDeletionTimestamp() != null) { // flagged for delete

      obj.getMetadata().getFinalizers().remove("finalizer." + Runner.NAME);
      // FIXME After bug in Kubernetes Client is resolved - https://github.com/fabric8io/kubernetes-client/pull/4446
      client.resources(Sample.class).inNamespace(namespace).withName(name).patch(obj);

      return;
    }

    if (!obj.getMetadata().getFinalizers().contains("finalizer." + Runner.NAME)) { // has no finalizer, yet
      obj.getMetadata().getFinalizers().add("finalizer." + Runner.NAME);
      // FIXME After bug in Kubernetes Client is resolved - https://github.com/fabric8io/kubernetes-client/pull/4446
      client.resources(Sample.class).inNamespace(namespace).withName(name).patch(obj);

      return;
    }

    if (!obj.getMetadata().getGeneration().equals(obj.getStatus().getObservedGeneration())) { // didn't act upon latest spec yet
      log.info("User wrote {}", obj.getSpec().getMessage());

      obj.getStatus().setObservedGeneration(obj.getMetadata().getGeneration());
      client.resource(obj).patchStatus();
    }
  }
}
