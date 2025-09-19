import sp.kx.gradlex.asFile
import sp.kx.gradlex.buildDir
import sp.kx.gradlex.buildSrc
import sp.kx.gradlex.check

buildscript {
    repositories.mavenCentral()

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.kotlin}")
    }
}

task<Delete>("clean") {
    delete = setOf(buildDir(), buildSrc.buildDir())
}

task("checkLicense") {
    doLast {
        val author = "Stanley Wintergreen" // todo
        file("LICENSE").check(
            expected = emptySet(),
            regexes = setOf("^Copyright 2\\d{3} $author${'$'}".toRegex()),
            report = buildDir()
                .dir("reports/analysis/license")
                .asFile("index.html"),
        )
    }
}

repositories.mavenCentral()

val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest:ktlint:${Version.ktlint}") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

task<JavaExec>("checkCodeStyle") {
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    val reporter = "html"
    val output = buildDir()
        .dir("reports/analysis/code/style/html")
        .asFile("index.html")
    args(
        "build.gradle.kts",
        "settings.gradle.kts",
        "buildSrc/src/main/kotlin/**/*.kt",
        "buildSrc/build.gradle.kts",
        "lib/src/main/kotlin/**/*.kt",
        "lib/src/test/kotlin/**/*.kt",
        "lib/build.gradle.kts",
        "--reporter=$reporter,output=${output.absolutePath}",
    )
}
