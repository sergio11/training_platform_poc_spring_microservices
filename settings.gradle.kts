rootProject.name = "poc-architecture"

include("test-support")
include("services:config-server")
include("services:training-catalog-service")
include("services:enrollment-service")
include("services:certification-service")
include("services:notification-service")
include("services:employee-service")
include("shared")
include("e2e-tests")
