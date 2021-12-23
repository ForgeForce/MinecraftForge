package net.minecraftforge.forge.tasks

import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.bundling.Zip

abstract class InstallerJar extends Zip {
    InstallerJar() {
        archiveClassifier.set('installer')
        archiveExtension.set('jar') // Needs to be Zip task to not override Manifest, so set extension
        destinationDirectory.set(project.extensions.findByType(BasePluginExtension).libsDirectory)

        from(project.rootProject.file('/src/main/resources/url.png'))
    }
}
