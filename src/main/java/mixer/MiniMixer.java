package mixer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.fluo.api.client.FluoAdmin.InitializationOptions;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.api.mini.MiniFluo;
import org.apache.fluo.recipes.accumulo.export.function.AccumuloExporter;
import org.apache.fluo.recipes.core.export.ExportQueue;
import org.apache.fluo.recipes.test.AccumuloExportITBase;

public class MiniMixer {
  public static void main(String[] args) throws Exception {
    File tmpDir = Files.createTempDirectory(Paths.get("target"), "mini").toFile();
    // System.out.println("tmp dir : "+tmpDir);

    MiniAccumuloCluster mac = new MiniAccumuloCluster(new MiniAccumuloConfig(tmpDir, "secret"));
    mac.start();

    FluoConfiguration fluoConfig = new FluoConfiguration();

    AccumuloExportITBase.configureFromMAC(fluoConfig, mac);

    fluoConfig.setApplicationName("mixer");
    fluoConfig.setAccumuloTable("mixer");

    fluoConfig.setObserverProvider(MixerObserverProvider.class);

    ExportQueue.configure("gu").keyType(String.class).valueType(NodeChanges.class).buckets(109)
        .save(fluoConfig);
    AccumuloExporter.configure("gu").instance(mac.getInstanceName(), mac.getZooKeepers())
        .credentials("root", "secret").table("sgraph").save(fluoConfig);

    mac.getConnector("root", "secret").tableOperations().create("sgraph");

    FluoFactory.newAdmin(fluoConfig).initialize(new InitializationOptions());

    System.out.print("Starting MiniFluo in (" + tmpDir + ")... ");

    try (MiniFluo mini = FluoFactory.newMiniFluo(fluoConfig)) {
      System.out.println("started");
      File propsFile = new File("fluo.properties");
      mini.getClientConfiguration().save(propsFile);
      System.out.println("Wrote " + propsFile);
      while (propsFile.exists()) {
        Thread.sleep(250);
      }
    }
    System.out.println("Done");
  }
}
