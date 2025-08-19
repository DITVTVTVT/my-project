import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerRegistryConnections
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.nuGetInstaller
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2025.07"

project {
    description = "My pet progects"

    buildType(JavaApp)

    features {
        dockerRegistry {
            id = "PROJECT_EXT_2"
            name = "Docker Registry"
            userName = "ditvtvtvt"
            password = "credentialsJSON:6d10dbdf-15d8-4f24-9120-c4dfb26af97d"
        }
    }

    subProject(ServiceFabricHttpApiClient)
}

object JavaApp : BuildType({
    name = "Java App"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            name = "build application"
            id = "build_application"
            tasks = "clean build"
        }
        dockerCommand {
            name = "build image"
            id = "build_image"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = """
                    ditvtvtvt/demo-app:myapp-9.0-%build.number%
                    ditvtvtvt/demo-app:latest
                """.trimIndent()
                commandArgs = "--pull"
            }
        }
        dockerCommand {
            name = "push image"
            id = "push_image"
            commandType = push {
                namesAndTags = """
                    ditvtvtvt/demo-app:myapp-9.0-%build.number%
                    ditvtvtvt/demo-app:latest
                """.trimIndent()
            }
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_2"
            }
        }
    }
})


object ServiceFabricHttpApiClient : Project({
    name = "Service Fabric HTTP API Client"

    vcsRoot(ServiceFabricHttpApiClient_GitHub)

    buildType(ServiceFabricHttpApiClient_Build)
})

object ServiceFabricHttpApiClient_Build : BuildType({
    name = "Build"

    params {
        param("Repo", "ServiceFabricHttpApiClient")
    }

    vcs {
        root(ServiceFabricHttpApiClient_GitHub)
    }

    steps {
        nuGetInstaller {
            name = "NuGet Restore"
            id = "jb_nuget_installer"
            toolPath = "%teamcity.tool.NuGet.CommandLine.DEFAULT%"
            projects = "ServiceFabricClient.sln"
            updatePackages = updateParams {
            }
        }
    }
})

object ServiceFabricHttpApiClient_GitHub : GitVcsRoot({
    name = "GitHub"
    url = "https://github.com/DITVTVTVT/%Repo%"
    branch = "refs/heads/master"
    branchSpec = """
        +:refs/heads/(develop)
        +:refs/heads/(feature/*)
        +:refs/heads/(release/*)
        +:refs/heads/(fix/*)
        +:refs/heads/(master)
    """.trimIndent()
})
