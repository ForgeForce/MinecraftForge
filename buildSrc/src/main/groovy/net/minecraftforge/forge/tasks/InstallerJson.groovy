package net.minecraftforge.forge.tasks

import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

import java.nio.file.Files

abstract class InstallerJson extends DefaultTask {
    @OutputFile abstract RegularFileProperty getOutput()
    @InputFiles abstract ConfigurableFileCollection getInput()
    @Input @Optional abstract Property<Configuration> getPackedConfiguration()
    @Input @Optional final Map<String, Object> libraries = new LinkedHashMap<>()
    @Input Map<String, Object> json = new LinkedHashMap<>()
    @InputFile abstract RegularFileProperty getIcon()
    @Input abstract Property<String> getLauncherJsonName()
    @Input abstract Property<String> getLogo()
    @Input abstract Property<String> getMirrors()
    @Input abstract Property<String> getWelcome()

    InstallerJson() {
        getLauncherJsonName().convention('/version.json')
        getLogo().convention('/big_logo.png')
        getMirrors().convention('https://files.minecraftforge.net/mirrors-2.0.json')
        getWelcome().convention("Welcome to the simple ${project.name.capitalize()} installer.")

        getOutput().convention(project.layout.buildDirectory.file('install_profile.json'))

        input.from(packedConfiguration)
    }

    @TaskAction
    protected void exec() {
        def libs = libraries
        if (packedConfiguration.isPresent()) {
            for (ResolvedArtifact child : packedConfiguration.get().resolvedConfiguration.resolvedArtifacts) {
                addLibrary(libs, Util.getMavenDep(child), Util.getMavenPath(child), child.file)
            }
        }
        json.libraries = libs.values().sort{a,b -> a.name.compareTo(b.name)}
        json.icon = "data:image/png;base64," + new String(Base64.getEncoder().encode(Files.readAllBytes(icon.get().asFile.toPath())))
        json.json = launcherJsonName.get()
        json.logo = logo.get()
        if (!mirrors.get().isEmpty())
            json.mirrorList = mirrors.get()
        json.welcome = welcome.get()

        Files.writeString(output.get().getAsFile().toPath(), new JsonBuilder(json).toPrettyString())
    }

    static void addLibrary(Map<String, Object> libraries, String mavenDep, String mavenPath, File file) {
        libraries[mavenDep] = [
            name     : mavenDep,
            downloads: [
                artifact: [
                    path: mavenPath,
                    url : "https://maven.minecraftforge.net/${mavenPath}",
                    sha1: file.sha1(),
                    size: file.length()
                ]
            ]
        ]
    }
}
