# CI/CD Setup for PDF Comparator

This repository now includes automated CI/CD workflows using GitHub Actions.

## Workflows

### 1. CI Workflow (`.github/workflows/ci.yml`)
- Runs on every push and pull request to main/master branches
- Tests: Runs `mvn clean test`
- Build: Compiles and packages the application with `mvn clean compile package`
- Artifacts: Uploads built JAR files for download

### 2. Renovate Auto-merge (`.github/workflows/renovate-auto-merge.yml`)
- Automatically runs when Renovate bot creates pull requests
- Runs full CI validation (tests + build)
- Auto-merges dependency updates if all checks pass
- Comments on failures to alert maintainers
- Only auto-merges minor and patch updates; major updates require manual review

## Renovate Configuration

The `renovate.json` file is configured to:
- Auto-merge minor and patch dependency updates
- Require manual review for major updates
- Label PRs appropriately
- Assign PRs to the repository owner
- Limit concurrent PRs to 3
- Verify status checks before merging

## Requirements

- Java 8 (as configured in the workflows)
- Maven for dependency management and builds
- GitHub repository with Actions enabled

## Local Testing

To test the build locally:
```bash
mvn clean test        # Run tests
mvn clean package     # Build the application
```