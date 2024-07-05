plugins {
    base
    `jacoco-report-aggregation`
}

dependencies {
    jacocoAggregation(project(":lib:oas-stub-api"))
    jacocoAggregation(project(":lib:oas-stub-model"))
    jacocoAggregation(project(":lib:oas-stub-storage-api"))
    jacocoAggregation(project(":lib:storages:oas-stub-inmemory-storage"))
    jacocoAggregation(project(":lib:storages:oas-stub-hazelcast-storage"))
    jacocoAggregation(project(":lib:storages:oas-stub-mongodb-storage"))
    jacocoAggregation(project(":lib:oas-stub-engine"))
    jacocoAggregation(project(":lib:oas-stub-server"))
    jacocoAggregation(project(":lib:spring:oas-stub-storage-autoconfigure-api"))
    jacocoAggregation(project(":lib:spring:oas-stub-inmemory-storage-autoconfigure"))
    jacocoAggregation(project(":lib:spring:oas-stub-mongodb-storage-autoconfigure"))
    jacocoAggregation(project(":lib:spring:oas-stub-hazelcast-storage-autoconfigure"))
    jacocoAggregation(project(":lib:spring:oas-stub-spring-boot-starter-test"))
    jacocoAggregation(project(":test:oas-stub-spring-tests"))
}

reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport")) // <.>
}
