package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the buildType with id = 'IdeaVimCompatibility'
accordingly, and delete the patch script.
*/
changeBuildType(RelativeId("IdeaVimCompatibility")) {
    expectSteps {
        script {
            name = "Check"
            scriptContent = """
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}org.jetbrains.IdeaVim-EasyMotion' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}io.github.mishkun.ideavimsneak' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}eu.theblob42.idea.whichkey' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}IdeaVimExtension' [latest-IU] -team-city
                # Outdated java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}github.zgqq.intellij-enhance' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}com.github.copilot' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}com.github.dankinsoid.multicursor' [latest-IU] -team-city
                java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}com.joshestein.ideavim-quickscope' [latest-IU] -team-city
            """.trimIndent()
        }
    }
    steps {
        update<ScriptBuildStep>(0) {
            name = "Check (1)"
            clearConditions()
            scriptContent = """
                # We use a custom build of verifier that downloads IdeaVim from dev channel
                
                java --version
                java -jar verifier1/verifier-cli-dev-all-1.jar check-plugin '${'$'}org.jetbrains.IdeaVim-EasyMotion' [latest-IU] -team-city
                java -jar verifier1/verifier-cli-dev-all-1.jar check-plugin '${'$'}io.github.mishkun.ideavimsneak' [latest-IU] -team-city
                java -jar verifier1/verifier-cli-dev-all-1.jar check-plugin '${'$'}eu.theblob42.idea.whichkey' [latest-IU] -team-city
                java -jar verifier1/verifier-cli-dev-all-1.jar check-plugin '${'$'}IdeaVimExtension' [latest-IU] -team-city
                # Outdated java -jar verifier/verifier-cli-dev-all.jar check-plugin '${'$'}github.zgqq.intellij-enhance' [latest-IU] -team-city
                java -jar verifier1/verifier-cli-dev-all-1.jar check-plugin '${'$'}com.github.copilot' [latest-IU] -team-city
                java -jar verifier1/verifier-cli-dev-all-1.jar check-plugin '${'$'}com.github.dankinsoid.multicursor' [latest-IU] -team-city
                java -jar verifier1/verifier-cli-dev-all-1.jar check-plugin '${'$'}com.joshestein.ideavim-quickscope' [latest-IU] -team-city
            """.trimIndent()
        }
        insert(1) {
            step {
                name = "Download Verifier"
                type = "MRPP_DownloadFile"
                executionMode = BuildStep.ExecutionMode.DEFAULT
                param("system.url", "https://packages.jetbrains.team/files/p/ideavim/plugin-verifier/verifier-cli-dev-all-1.jar")
                param("system.username", "")
                param("system.dest.dir", "verifier1")
                param("system.clean.dest.dir", "false")
                param("system.password", "credentialsJSON:c42fdcd2-8e8d-4313-99d0-db62f5dfc514")
            }
        }
    }
}
