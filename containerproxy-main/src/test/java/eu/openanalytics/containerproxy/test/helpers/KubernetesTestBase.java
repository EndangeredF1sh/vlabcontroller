package eu.openanalytics.containerproxy.test.helpers;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

import java.util.Arrays;
import java.util.List;

public abstract class KubernetesTestBase {

    public static interface TestBody {
        public void run(NamespacedKubernetesClient client, String namespace, String overriddenNamespace) throws InterruptedException;
    }

    public static final String namespace = "itest";
    public static final String overriddenNamespace = "itest-overridden";
    private final List<String> managedNamespaces = Arrays.asList(namespace, overriddenNamespace);

    static protected final DefaultKubernetesClient client = new DefaultKubernetesClient();

    protected void setup(TestBody test) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::deleteNamespaces));

        deleteNamespaces();
        createNamespaces();

        try {
            Thread.sleep(1000); // wait for namespaces and tokens to become ready

            NamespacedKubernetesClient namespacedKubernetesClient = client.inNamespace(namespace);

            test.run(namespacedKubernetesClient, namespace, overriddenNamespace);
        } catch (InterruptedException e) {
        } finally {
            deleteNamespaces();
        }
    }

    private void deleteNamespaces() {
        try {
            for (String namespace : managedNamespaces) {
                Namespace ns = client.namespaces().withName(namespace).get();
                if (ns == null) {
                    continue;
                }

                client.namespaces().delete(ns);

                while (client.namespaces().withName(namespace).get() != null) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
        }
    }

    private void createNamespaces() {
        for (String namespace : managedNamespaces) {
            client.namespaces().create(new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .endMetadata()
                    .build());
        }
    }

}
