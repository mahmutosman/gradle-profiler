package org.gradle.profiler.jfr;

import org.gradle.profiler.GradleScenarioDefinition;
import org.gradle.profiler.InstrumentingProfiler;
import org.gradle.profiler.JvmArgsCalculator;
import org.gradle.profiler.ScenarioSettings;

import java.io.File;
import java.util.function.Consumer;

public class JfrProfiler extends InstrumentingProfiler {
    private static final String PROFILE_JFR_SUFFIX = ".jfr";
    private static final String PROFILE_JFR_DIRECTORY_SUFFIX = "-jfr";

    private final JFRArgs jfrArgs;

    JfrProfiler(JFRArgs jfrArgs) {
        this.jfrArgs = jfrArgs;
    }

    @Override
    public String toString() {
        return "JFR";
    }

    @Override
    public void summarizeResultFile(File resultFile, Consumer<String> consumer) {
        if (resultFile.getName().endsWith(".jfr")) {
            consumer.accept("JFR recording: " + resultFile.getAbsolutePath());
        } else if (resultFile.getName().endsWith(".jfr-flamegraphs")) {
            consumer.accept("JFR Flame Graphs: " + resultFile.getAbsolutePath());
        }
    }

    @Override
    protected SnapshotCapturingProfilerController doNewController(ScenarioSettings settings) {
        File jfrFile = getJfrFile(settings);
        return new JFRControl(jfrArgs, jfrFile);
    }

    @Override
    protected JvmArgsCalculator jvmArgsWithInstrumentation(ScenarioSettings settings, boolean startRecordingOnProcessStart, boolean captureSnapshotOnProcessExit) {
        File jfrFile = getJfrFile(settings);
        return new JFRJvmArgsCalculator(jfrArgs, startRecordingOnProcessStart, captureSnapshotOnProcessExit, jfrFile);
    }

    private File getJfrFile(ScenarioSettings settings) {
        GradleScenarioDefinition scenario = settings.getScenario();
        if (scenario.createsMultipleProcesses()) {
            File jfrFilesDirectory = new File(scenario.getOutputDir(), scenario.getProfileName() + PROFILE_JFR_DIRECTORY_SUFFIX);
            jfrFilesDirectory.mkdirs();
            return jfrFilesDirectory;
        } else {
             return new File(scenario.getOutputDir(), scenario.getProfileName() + PROFILE_JFR_SUFFIX);
        }
    }

    @Override
    public void validate(ScenarioSettings settings, Consumer<String> reporter) {
        validateMultipleIterationsWithCleanupAction(settings, reporter);
    }

    @Override
    protected boolean canRestartRecording(ScenarioSettings settings) {
        return !settings.getScenario().getInvoker().isReuseDaemon();
    }
}
