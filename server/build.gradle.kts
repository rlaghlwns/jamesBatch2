import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.5.4"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.5.21"
	kotlin("plugin.spring") version "1.5.21"
}

group = "com.mangonw"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.0")
	runtimeOnly("org.postgresql:postgresql:42.2.23.jre7")
	compileOnly("org.projectlombok:lombok:1.18.20")
	annotationProcessor("org.projectlombok:lombok:1.18.20")

	implementation("org.springframework.boot:spring-boot-starter-web:2.5.4")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	testImplementation("org.springframework.boot:spring-boot-starter-test:2.5.4")

	implementation("com.squareup.retrofit2:retrofit:2.9.0")
	implementation("com.squareup.retrofit2:converter-gson:2.9.0")
	implementation("com.squareup.okhttp3:okhttp:4.9.1")

	implementation("commons-codec:commons-codec:1.15")
	implementation("org.apache.james:apache-mime4j:0.8.7")
	implementation("javax.activation:activation:1.1.1")
	implementation("com.sun.mail:javax.mail:1.6.2")
	implementation("org.apache.commons:commons-email:1.5")

	implementation("org.apache.james:james-server-mailet-dkim:3.6.0")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.5.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:1.5.0")

	// JAX-B dependencies for JDK 9+
	implementation ("jakarta.xml.bind:jakarta.xml.bind-api:2.3.2")
	implementation ("org.glassfish.jaxb:jaxb-runtime:2.3.2")

	implementation("org.simplejavamail:simple-java-mail:7.2.0")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
