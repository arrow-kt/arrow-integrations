plugins {
    id "org.jetbrains.kotlin.jvm"
}

// apply from: "$ANIMALSNIFFER"

dependencies {
    compile "io.arrow-kt:arrow-core:$ARROW_VERSION"
    compile "com.fasterxml.jackson.module:jackson-module-kotlin:$JACKSON_MODULE_KOTLIN_VERSION"
    testCompile "io.arrow-kt:arrow-core-test:$ARROW_VERSION"
    testImplementation "io.kotest:kotest-property:$KOTEST_VERSION"
    testImplementation "io.kotest:kotest-runner-junit5:$KOTEST_VERSION"
}
