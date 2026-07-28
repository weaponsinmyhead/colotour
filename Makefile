.PHONY: api-test api-run mobile-test mobile-build

api-test:
	cd apps/api && go test ./...

api-run:
	cd apps/api && go run ./cmd/api

mobile-test:
	cd apps/mobile && bash ./gradlew testDebugUnitTest

mobile-build:
	cd apps/mobile && bash ./gradlew assembleDebug
