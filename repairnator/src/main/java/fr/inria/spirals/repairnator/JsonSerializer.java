package fr.inria.spirals.repairnator;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.ProjectScanner;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by urli on 05/01/2017.
 */
public class JsonSerializer {

    private Gson serializer;
    private Date dateStart;
    private Date dateFinish;
    private ProjectScanner scanner;
    private List<ProjectInspector> inspectors;
    private String outputPath;
    private JsonObject root;

    public JsonSerializer(String outputPath) {
        this.dateStart = new Date();
        this.outputPath = outputPath;

        this.serializer = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return (fieldAttributes.getName().equals("lastBuild"));
            }

            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        this.root = new JsonObject();
    }

    public void setScanner(ProjectScanner scanner) {
        this.scanner = scanner;
    }

    public void setInspectors(List<ProjectInspector> inspectors) {
        this.inspectors = inspectors;
    }

    private JsonElement serialize(Object object) {
        return this.serializer.toJsonTree(object);
    }

    private void writeFile() throws IOException {
        String serialization = this.serializer.toJson(root);

        File outputFile = new File(this.outputPath);
        if (outputFile.isDirectory()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmmss");
            String formattedDate = dateFormat.format(new Date());
            String filename = "repairbot_"+formattedDate+".json";
            outputFile = new File(outputFile.getPath()+File.separator+filename);
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.write(serialization);
        writer.close();
    }

    private void outputNotClonableInspector(ProjectInspector inspector, JsonArray notClonable) {
        JsonObject result = new JsonObject();
        result.add("slug", serialize(inspector.getRepoSlug()));
        Build build = inspector.getBuild();
        result.add("buildId", serialize(build.getId()));
        if (build.isPullRequest()) {
            result.add("commit", serialize(build.getPRInformation()));
        } else {
            result.add("commit", serialize(build.getCommit()));
        }
        notClonable.add(result);
    }

    private void outputNotBuildableInspector(ProjectInspector inspector, JsonArray notBuildable) {
        JsonObject result = new JsonObject();
        result.add("slug", serialize(inspector.getRepoSlug()));
        Build build = inspector.getBuild();
        result.add("buildId", serialize(build.getId()));
        result.add("stepsDuration", serialize(inspector.getStepsDurations()));
        result.add("localRepo", serialize(inspector.getRepoLocalPath()));
        notBuildable.add(result);
    }

    private void outputNotTestableInspector(ProjectInspector inspector, JsonArray notTestable) {
        this.outputNotBuildableInspector(inspector, notTestable);
    }

    private void outputFailWhenGatheringInfoInspector(ProjectInspector inspector, JsonArray failWhenGatheringInfo) {
        this.outputNotBuildableInspector(inspector, failWhenGatheringInfo);
    }

    private void outputHasTestFailureInspector(ProjectInspector inspector, JsonArray hasTestFailure) {
        JsonObject result = new JsonObject();
        result.add("slug", serialize(inspector.getRepoSlug()));
        Build build = inspector.getBuild();
        result.add("buildId", serialize(build.getId()));
        result.add("stepsDuration", serialize(inspector.getStepsDurations()));
        result.add("localRepo", serialize(inspector.getRepoLocalPath()));

        GatherTestInformation testInformation = inspector.getTestInformations();
        result.add("nbTests", serialize(testInformation.getNbTotalTests()));
        result.add("nbSkippingTests", serialize(testInformation.getNbSkippingTests()));
        result.add("nbFailingtests",serialize(testInformation.getNbFailingTests()));
        result.add("typeOfFailures",serialize(testInformation.getTypeOfFailures()));
        hasTestFailure.add(result);
    }

    private void outputNotFailingInspector(ProjectInspector inspector, JsonArray notFailing) {
        JsonObject result = new JsonObject();
        result.add("slug", serialize(inspector.getRepoSlug()));
        Build build = inspector.getBuild();
        result.add("buildId", serialize(build.getId()));
        result.add("stepsDuration", serialize(inspector.getStepsDurations()));
        result.add("localRepo", serialize(inspector.getRepoLocalPath()));

        GatherTestInformation testInformation = inspector.getTestInformations();
        result.add("nbTests", serialize(testInformation.getNbTotalTests()));
        result.add("nbSkippingTests", serialize(testInformation.getNbSkippingTests()));
        notFailing.add(result);
    }

    public void createOutput() throws IOException {
        this.dateFinish = new Date();

        root.add("dateStart", serialize(this.dateStart));
        root.add("dateFinish", serialize(this.dateFinish));
        root.add("scanStatistics", serialize(this.scanner));

        JsonArray notClonableArray = new JsonArray();
        JsonArray notBuildableArray = new JsonArray();
        JsonArray notTestableArray = new JsonArray();
        JsonArray failWhenGatheringInfoArray = new JsonArray();
        JsonArray hasTestFailureArray = new JsonArray();
        JsonArray notFailingArray = new JsonArray();


        for (ProjectInspector inspector : this.inspectors) {
            switch (inspector.getState()) {
                default:
                    Launcher.LOGGER.warn("Project inspector with not defined state: "+inspector);
                    break;

                case INIT:
                    outputNotClonableInspector(inspector, notClonableArray);
                    break;

                case CLONABLE:
                    outputNotBuildableInspector(inspector, notBuildableArray);
                    break;

                case BUILDABLE:
                    outputNotTestableInspector(inspector, notTestableArray);
                    break;

                case TESTABLE:
                    outputFailWhenGatheringInfoInspector(inspector, failWhenGatheringInfoArray);
                    break;

                case HASTESTFAILURE:
                    outputHasTestFailureInspector(inspector, hasTestFailureArray);
                    break;

                case NOTFAILING:
                    outputNotFailingInspector(inspector, notFailingArray);
                    break;
            }
        }

        JsonObject hasTestFailure = new JsonObject();
        hasTestFailure.add("number", serialize(hasTestFailureArray.size()));
        hasTestFailure.add("builds", hasTestFailureArray);
        root.add("hasTestFailure",hasTestFailure);

        JsonObject notFailing = new JsonObject();
        notFailing.add("number", serialize(notFailingArray.size()));
        notFailing.add("builds", notFailingArray);
        root.add("notFailing", notFailing);

        JsonObject failWhenGatheringInfo = new JsonObject();
        notFailing.add("number", serialize(failWhenGatheringInfoArray.size()));
        notFailing.add("builds", failWhenGatheringInfoArray);
        root.add("failWhenGatheringInfo", failWhenGatheringInfo);

        JsonObject notTestable = new JsonObject();
        notFailing.add("number", serialize(notTestableArray.size()));
        notFailing.add("builds", notTestableArray);
        root.add("notTestable", notTestable);

        JsonObject notBuildable = new JsonObject();
        notFailing.add("number", serialize(notBuildableArray.size()));
        notFailing.add("builds", notBuildableArray);
        root.add("notBuildable", notBuildable);

        JsonObject notClonable = new JsonObject();
        notFailing.add("number", serialize(notClonableArray.size()));
        notFailing.add("builds", notClonableArray);
        root.add("notClonable", notClonable);

        writeFile();
    }
}
