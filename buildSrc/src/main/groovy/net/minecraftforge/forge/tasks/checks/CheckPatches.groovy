package net.minecraftforge.forge.tasks.checks

import groovy.transform.CompileStatic
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

@CompileStatic
abstract class CheckPatches extends CheckTask {
    @InputDirectory abstract DirectoryProperty getPatchDir()
    @Input @Optional abstract ListProperty<String> getPatchesWithS2SArtifact()

    @Override
    void check(Reporter reporter, boolean fix) {
        final patchDir = getPatchDir().get().asFile.toPath()
        Files.walk(patchDir).withCloseable {
            it.filter(Files.&isRegularFile).forEach { path ->
                final String relativeName = patchDir.relativize(path).toString()
                verifyPatch(path, reporter, fix, relativeName, patchesWithS2SArtifact.get().contains(relativeName.replace('\\', '/')))
            }
        }
    }

    void verifyPatch(Path patch, Reporter reporter, boolean fix, String patchPath, boolean hasS2SArtifact) {
        final hunk_start_pattern = Pattern.compile('^@@ -[0-9,]* \\+[0-9,_]* @@$')
        final white_space_pattern = Pattern.compile('^[+\\-]\\s*$')
        final import_pattern = Pattern.compile('^[+\\-]\\s*import.*')
        final field_pattern = Pattern.compile('^[+\\-][\\s]*((public|protected|private)[\\s]*)?(static[\\s]*)?(final)?([^=;]*)(=.*)?;\\s*$')
        final method_pattern = Pattern.compile('^[+\\-][\\s]*((public|protected|private)[\\s]*)?(static[\\s]*)?(final)?([^(]*)[(]([^)]*)?[)]\\s*[{]\\s*$')
        final class_pattern = Pattern.compile('^[+\\-][\\s]*((public|protected|private)[\\s]*)?(static[\\s]*)?(final[\\s]*)?(class|interface)([^{]*)[{]\\s*$')

        final oldFixedErrors = reporter.fixed.size()

        final accessMap = [private: 0, null: 1, protected:2, public:3]

        final lines = Files.readAllLines(patch)

        int hunksStart = 0
        boolean onlyWhiteSpace = false

        final List<String> newLines = []

        // First two lines are file name ++/-- and we do not care
        newLines.add(lines[0] + '\n')
        newLines.add(lines[1] + '\n')

        int i
        for (i = 2; i < lines.size(); ++i) {
            def line = lines[i]
            newLines.add(line + '\n')

            if (hunk_start_pattern.matcher(line).find()) {
                if (onlyWhiteSpace) {
                    if (!hasS2SArtifact)
                        reporter.report("Patch contains only white space hunk starting at line ${hunksStart + 1}, file: $patchPath")
                    int toRemove = i - hunksStart
                    while (toRemove-- > 0)
                        newLines.remove(newLines.size() - 1)
                }
                hunksStart = i
                onlyWhiteSpace = true
                continue
            }

            if (line.startsWithAny('+','-')) {
                def prefixChange = false
                def prevLine = lines[i - 1]

                if (line.charAt(0) == (char)'+' && prevLine.charAt(0) == (char)'-') {
                    def prevTrim = prevLine.substring(1).replaceAll("\\s", "")
                    def currTrim = line.substring(1).replaceAll("\\s", "")

                    if (prevTrim == currTrim) {
                        prefixChange = true
                    }

                    def pMatcher = field_pattern.matcher(prevLine)
                    def cMatcher = field_pattern.matcher(line)

                    if (pMatcher.find() && cMatcher.find() &&
                            pMatcher.group(6) == cMatcher.group(6) && // = ...
                            pMatcher.group(5) == cMatcher.group(5) && // field name
                            pMatcher.group(3) == cMatcher.group(3) && // static
                            (accessMap[pMatcher.group(2)] < accessMap[cMatcher.group(2)] || pMatcher.group(4) != cMatcher.group(4))) {
                        reporter.report("Patch contains access changes or final removal at line ${i + 1}, file: $patchPath", true)
                    }

                    pMatcher = method_pattern.matcher(prevLine)
                    cMatcher = method_pattern.matcher(line)

                    if (pMatcher.find() && cMatcher.find() &&
                            pMatcher.group(6) == cMatcher.group(6) && // params
                            pMatcher.group(5) == cMatcher.group(5) && // <T> void name
                            pMatcher.group(3) == cMatcher.group(3) && // static
                            (accessMap[pMatcher.group(2)] < accessMap[cMatcher.group(2)] || pMatcher.group(4) != cMatcher.group(4))) {
                        reporter.report("Patch contains access changes or final removal at line ${i + 1}, file: $patchPath", true)
                    }

                    pMatcher = class_pattern.matcher(prevLine)
                    cMatcher = class_pattern.matcher(line)

                    if (pMatcher.find() && cMatcher.find() &&
                            pMatcher.group(6) == cMatcher.group(6) && // ClassName<> extends ...
                            pMatcher.group(5) == cMatcher.group(5) && // class | interface
                            pMatcher.group(3) == cMatcher.group(3) && // static
                            (accessMap[pMatcher.group(2)] < accessMap[cMatcher.group(2)] || pMatcher.group(4) != cMatcher.group(4))) {
                        reporter.report("Patch contains access changes or final removal at line ${i + 1}, file: $patchPath", true)
                    }
                }

                if (line.charAt(0) == (char)'-' && i + 1 < lines.size()) {
                    final nextLine = lines[i + 1]
                    if (nextLine.charAt(0) == (char)'+') {
                        final nextTrim = nextLine.substring(1).replaceAll("\\s", "")
                        final currTrim = line.substring(1).replaceAll("\\s", "")

                        if (nextTrim == currTrim) {
                            prefixChange = true
                        }
                    }
                }

                final isWhiteSpaceChange = white_space_pattern.matcher(line).find()

                if (!prefixChange && !isWhiteSpaceChange) {
                    onlyWhiteSpace = hasS2SArtifact && import_pattern.matcher(line).find()
                } else if (isWhiteSpaceChange) {
                    final prevLineChange = prevLine.startsWithAny('+','-')
                    final nextLineChange = i + 1 < lines.size() && lines[i + 1].startsWithAny('+','-')

                    if (!prevLineChange && !nextLineChange) {
                        reporter.report("Patch contains white space change in valid hunk at line ${i + 1} (cannot auto fix), file: $patchPath")
                    }
                }

                if (line.contains('\t')) {
                    reporter.report("Patch contains tabs on line ${i + 1}, file: $patchPath")
                    line = line.replaceAll('\t', '    ')
                    newLines.remove(newLines.size() - 1)
                    newLines.add(line + '\n')
                }

                if (import_pattern.matcher(line).find() && !hasS2SArtifact) {
                    reporter.report("Patch contains import change on line ${i + 1}, file: $patchPath", false)
                }
            }
        }

        if (onlyWhiteSpace) {
            if (!hasS2SArtifact)
                reporter.report("Patch contains only white space hunk starting at line ${hunksStart + 1}, file: $patchPath")
            def toRemove = i - hunksStart;
            while (toRemove-- > 0)
                newLines.remove(newLines.size() - 1)
        }

        if ((reporter.fixed.size() > oldFixedErrors && fix) || hasS2SArtifact) {
            if (newLines.size() <= 2) {
                logger.lifecycle("Patch is now empty removing, file: {}", patchPath)
                Files.delete(patch)
            }
            else {
                if (!hasS2SArtifact)
                    logger.lifecycle("*** Updating patch file. Please run setup then genPatches again. ***")
                Files.newBufferedWriter(patch).withCloseable {
                    newLines.each { l -> it.write(l) }
                }
            }
        }
    }
}
