package fr.pantheonsorbonne.cri;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

@Mojo(name = "enforce", defaultPhase = LifecyclePhase.COMPILE)
class DextormEnforcerPluginMojo extends AbstractMojo {
    final private static String JACOCO_GROUP_ID = "org.jacoco";
    final private static String JACOCO_ARTIFACT_ID = "jacoco-maven-plugin";
    final private static String JACOCO_VERSION = "0.8.7";
    final private static String JACOCO_EXECUTION_ID = "prepare-agent";
    final private static String JACOCO_EXECUTION_GOAL = "prepare-agent";
    final private static String JACOCO_EXECUTION_PHASE = "process-test-resources";
    final private static String JACOCO_EXECUTION_CONFIGURATION = "<configuration><destFile>${project.build.directory}/coverage-reports/jacoco-ut.exec</destFile><propertyName>surefireArgLine</propertyName></configuration>";
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Plugin surefirePlugin = getSurefirePlugin();
            surefirePlugin.setVersion("3.0.0-M7");
            Xpp3Dom configuration = Xpp3DomBuilder.build(new StringReader("<configuration><argLine>@{surefireArgLine}</argLine></configuration>"));

            surefirePlugin.setConfiguration(configuration);

            getLog().info("surefirePlugin:" + surefirePlugin);
            this.project.getBuild().getPlugins().remove(surefirePlugin);

        } catch (NoSuchPluginException nspe) {

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Plugin surefirePlugin = new Plugin();
        customizePlugin(surefirePlugin, "org.apache.maven.plugins", "maven-surefire-plugin", "3.0.0-M7", "test", "test", "test", "<configuration><argLine>@{surefireArgLine}</argLine></configuration>");
        this.project.getBuild().getPlugins().add(surefirePlugin);
        try {
            Plugin jacocoplugin = this.project.getBuild().getPlugins().stream().filter(p -> p.getArtifactId().equals("jacoco-maven-plugin")).findFirst().orElseThrow(() -> new NoSuchPluginException());
            getLog().info("jacoco:" + jacocoplugin.toString());

        } catch (NoSuchPluginException nspe) {


            Plugin jacocoPlugin = new Plugin();
            customizePlugin(jacocoPlugin, JACOCO_GROUP_ID, JACOCO_ARTIFACT_ID, JACOCO_VERSION, JACOCO_EXECUTION_ID, JACOCO_EXECUTION_GOAL, JACOCO_EXECUTION_PHASE, JACOCO_EXECUTION_CONFIGURATION);
            customizePlugin(jacocoPlugin, JACOCO_GROUP_ID, JACOCO_ARTIFACT_ID, JACOCO_VERSION, "report", "report", "test", "<configuration><dataFile>${project.build.directory}/coverage-reports/jacoco-ut.exec</dataFile><outputDirectory>${project.reporting.outputDirectory}/jacoco-ut</outputDirectory></configuration>");
            this.project.getBuild().getPlugins().add(jacocoPlugin);
        }
        MavenXpp3Writer writer = new MavenXpp3Writer();

        try {
            FileWriter sw = new FileWriter("dextorm-effective-pom.xml");
            writer.write(sw, this.project.getModel());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private Plugin getSurefirePlugin() throws NoSuchPluginException {
        return this.project.getBuild().getPlugins().stream().filter(p -> p.getArtifactId().equals("maven-surefire-plugin")).findFirst().orElseThrow(() -> new NoSuchPluginException());
    }

    private void customizePlugin(Plugin plugin, String groupId, String artifactId, String version, String id, String e1, String phase, String configuration) {

        if (groupId != null)
            plugin.setGroupId(groupId);
        if (artifactId != null)
            plugin.setArtifactId(artifactId);
        if (version != null)
            plugin.setVersion(version);
        PluginExecution execution = new PluginExecution();
        execution.setId(id);
        execution.getGoals().add(e1);
        execution.setPhase(phase);
        try {
            addXMLExecution(execution, configuration);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        plugin.getExecutions().add(execution);

    }

    private void addXMLExecution(PluginExecution execution, String s) throws XmlPullParserException, IOException {
        Xpp3Dom configuration = Xpp3DomBuilder.build(new StringReader(s));
        execution.setConfiguration(configuration);
    }
}