plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jooq.jooq-codegen-gradle") version "3.20.5"
}

group = "codes.abbott"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	// jooq
	implementation("org.jooq:jooq:3.20.5")
	implementation("org.jooq:jooq-codegen-gradle:3.20.5")
	implementation("org.jooq.jooq-codegen-gradle:org.jooq.jooq-codegen-gradle.gradle.plugin:3.20.5")
	implementation("org.jooq:jooq-meta:3.20.5")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	// postgres
	runtimeOnly("org.postgresql:postgresql")
	jooqCodegen("org.postgresql:postgresql")
	testImplementation("org.testcontainers:postgresql")
	// liquibase
	//implementation("org.liquibase:liquibase-core:4.32.0")
	// spring boot general
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-starter-actuator")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("/generated/src"))
        }
    }
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

jooq {
	configuration {
		jdbc {
			//driver = "org.postgresql.Driver"
			url = "jdbc:postgresql://aws-0-ca-central-1.pooler.supabase.com/postgres"
			user = "postgres.otvzmnexcifgmdtyuhvb"
		}
		generator {
			name = "org.jooq.codegen.KotlinGenerator"
			target {
				packageName = "codes.abbott.treeDemo.db"
				directory = "build/generated/src"
				//clean = true
			}
			database {
				name = "org.jooq.meta.postgres.PostgresDatabase"
				includes = "public.edge"
				excludes = ""
			}
			generate {
				isDeprecated = false
				isPojos = true
				isPojosAsKotlinDataClasses = true
				isFluentSetters = true
			}
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
