/*
 * ProxyQueues, a Velocity queueing solution
 *
 * Copyright (c) 2021 James Lyne
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

plugins {
    id("proxy-queues.java-conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":ProxyQueuesAPI"))
    implementation(libs.configMe)
    implementation(libs.cloudVelocity)
    implementation(libs.cloudMinecraftExtras)
    implementation(libs.cloudAnnotations)
    implementation(libs.vanishBridgeHelper)

    compileOnly(libs.platformDetection)
    compileOnly(libs.proxyDiscordApi)
    compileOnly(libs.platformDetection)
    compileOnly(libs.proxyDiscordApi)
    compileOnly(libs.prometheusCore)
    compileOnly(libs.prometheusExporter)

    annotationProcessor(libs.velocityApi)
}

description = "Velocity queueing solution"

tasks {
    shadowJar {
        archiveClassifier = ""
        relocate("cloud.commandframework", "uk.co.notnull.proxyqueues.shaded.cloud")
        relocate("io.leangen.geantyref", "uk.co.notnull.proxyqueues.shaded.typetoken")
        relocate("uk.co.notnull.VanishBridge", "uk.co.notnull.proxyqueues.shaded.vanishbridge")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        expand("version" to project.version)
    }
}
