package io.codelair.examples.kubernetesoperator;

import io.codelair.examples.kubernetesoperator.model.DoneableSample;
import io.codelair.examples.kubernetesoperator.model.Sample;
import io.codelair.examples.kubernetesoperator.model.SampleList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
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
  private final MixedOperation<Sample, SampleList, DoneableSample, Resource<Sample, DoneableSample>> crOp;

  public Controller(final KubernetesClient client,
                    final SharedIndexInformer<Sample> sharedIndexInformer,
                    final MixedOperation<Sample, SampleList, DoneableSample, Resource<Sample, DoneableSample>> crOp) {

    this.client = client;
    this.sharedIndexInformer = sharedIndexInformer;
    this.crOp = crOp;
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

    if (obj.getMetadata().getDeletionTimestamp() != null) { // flagged for delete

      obj.getMetadata().getFinalizers().remove("finalizer." + Runner.NAME);
      crOp.inNamespace(obj.getMetadata().getNamespace()).withName(obj.getMetadata().getName()).patch(obj);

      return;
    }

    if (!obj.getMetadata().getFinalizers().contains("finalizer." + Runner.NAME)) { // has no finalizer, yet
      obj.getMetadata().getFinalizers().add("finalizer." + Runner.NAME);
      crOp.inNamespace(obj.getMetadata().getNamespace()).withName(obj.getMetadata().getName()).patch(obj);

      return;
    }

    if (!obj.getMetadata().getGeneration().equals(obj.getStatus().getObservedGeneration())) { // didn't act upon latest spec yet
      log.info("User wrote {}", obj.getSpec().getMessage());

      obj.getStatus().setObservedGeneration(obj.getMetadata().getGeneration());
      crOp.inNamespace(obj.getMetadata().getNamespace()).withName(obj.getMetadata().getName()).updateStatus(obj);
    }
  }
}
