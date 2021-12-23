package net.minecraftforge.forge.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.CopySpec
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.bundling.AbstractArchiveTask

/**
 * Configures an {@link AbstractArchiveTask}, usually {@link InstallerJar}, with the packed dependencies as a
 * {@link CopySpec}.
 * <p>
 * Because an archive task cannot add new copy specs when it is executing, we must workaround it by making a task which
 * executes before the archive task (through {@link Task#dependsOn}) and configures it. This task also defines a task
 * dependency on the configuration's task dependencies.
 */
abstract class ConfigureInstallerJar extends DefaultTask {
    @Internal abstract Property<AbstractArchiveTask> getTargetTask()
    @Input @Optional abstract Property<Configuration> getPackedConfiguration()
    @Input @Optional abstract Property<Boolean> getAddPackedDependencies()

    ConfigureInstallerJar() {
        addPackedDependencies.convention(false)
        dependsOn packedConfiguration.map { it.buildDependencies }
        doLast {
            var t = targetTask.get()

            if (addPackedDependencies.get() && packedConfiguration.isPresent()) {
                for (ResolvedArtifact child : packedConfiguration.get().resolvedConfiguration.resolvedArtifacts) {
                    var id = child.moduleVersion.id
                    t.from(child.file) {
                        into "/maven/${id.group.replace('.', '/')}/${id.name}/${id.version}/"
                    }
                }
            }
        }
    }
}
