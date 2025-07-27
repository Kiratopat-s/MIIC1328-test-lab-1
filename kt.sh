#!/bin/bash

# E-commerce Product Quality Analyzer Runner Script
# This script provides an easy way to compile and run the analyzer

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check if Kotlin is installed
check_kotlin() {
    if ! command_exists kotlinc; then
        print_error "Kotlin compiler (kotlinc) not found!"
        print_info "Please install Kotlin from https://kotlinlang.org/docs/command-line.html"
        print_info "Or use SDKMAN: sdk install kotlin"
        exit 1
    fi
    
    if ! command_exists kotlin; then
        print_error "Kotlin runtime (kotlin) not found!"
        print_info "Please install Kotlin from https://kotlinlang.org/docs/command-line.html"
        exit 1
    fi
    
    print_success "Kotlin installation verified"
}

# Function to check if Gradle is available
check_gradle() {
    if command_exists gradle; then
        print_success "Gradle found - using Gradle build"
        return 0
    elif [ -f "./gradlew" ]; then
        print_success "Gradle wrapper found - using ./gradlew"
        return 0
    else
        print_warning "Gradle not found - will use manual compilation"
        return 1
    fi
}

# Function to build with Gradle
build_with_gradle() {
    print_info "Building with Gradle..."
    
    if [ -f "./gradlew" ]; then
        ./gradlew build
        ./gradlew jar
    else
        gradle build
        gradle jar
    fi
    
    if [ -f "build/libs/test-lab-1-1.0.0.jar" ]; then
        print_success "JAR file created: build/libs/test-lab-1-1.0.0.jar"
        return 0
    else
        print_error "JAR file not found after build"
        return 1
    fi
}

# Function to compile manually (fallback)
manual_compile() {
    print_info "Compiling manually..."
    
    # Create output directory
    mkdir -p build/classes
    
    # Download dependencies (simplified - for production use proper dependency management)
    print_info "Note: Manual compilation requires kotlin-csv and kotlinx-datetime JARs"
    print_info "For production use, please use Gradle build"
    
    # Compile source files
    find src/main/kotlin -name "*.kt" -print0 | xargs -0 kotlinc -d build/classes
    
    # Create JAR
    cd build/classes
    jar cfm ../analyzer.jar ../../META-INF/MANIFEST.MF .
    cd ../..
    
    print_success "Manual compilation complete: build/analyzer.jar"
}

# Function to run the analyzer
run_analyzer() {
    local jar_file=""
    
    # Find the JAR file
    if [ -f "build/libs/test-lab-1-1.0.0.jar" ]; then
        jar_file="build/libs/test-lab-1-1.0.0.jar"
    elif [ -f "build/analyzer.jar" ]; then
        jar_file="build/analyzer.jar"
    else
        print_error "No JAR file found. Please build first with: ./kt.sh build"
        exit 1
    fi
    
    print_info "Running analyzer with JAR: $jar_file"
    kotlin -cp "$jar_file" com.ecommerce.analyzer.MainKt "$@"
}

# Function to clean build artifacts
clean_build() {
    print_info "Cleaning build artifacts..."
    rm -rf build/
    rm -rf .gradle/
    print_success "Clean complete"
}

# Function to run tests
run_tests() {
    if check_gradle; then
        print_info "Running tests with Gradle..."
        if [ -f "./gradlew" ]; then
            ./gradlew test
        else
            gradle test
        fi
    else
        print_warning "Tests require Gradle build system"
        print_info "Please install Gradle to run tests"
    fi
}

# Function to show help
show_help() {
    echo "🛠️  E-commerce Product Quality Analyzer Build Script"
    echo ""
    echo "USAGE:"
    echo "  ./kt.sh <command> [options]"
    echo ""
    echo "COMMANDS:"
    echo "  build                    Build the project (compile and create JAR)"
    echo "  run <csv-file> [options] Run the analyzer with specified CSV file"
    echo "  test                     Run unit tests"
    echo "  clean                    Clean build artifacts"
    echo "  help                     Show this help message"
    echo ""
    echo "RUN OPTIONS (passed to analyzer):"
    echo "  --format <format>        Output format: console, markdown, csv"
    echo "  --severity <level>       Filter by severity: critical, warning, info, all"
    echo "  --type <type>            Filter by issue type"
    echo "  --output <file>          Save report to file"
    echo ""
    echo "EXAMPLES:"
    echo "  ./kt.sh build"
    echo "  ./kt.sh run product_database.csv"
    echo "  ./kt.sh run product_database.csv --format markdown --output report.md"
    echo "  ./kt.sh run product_database.csv --severity critical"
    echo "  ./kt.sh test"
    echo "  ./kt.sh clean"
    echo ""
}

# Main script logic
main() {
    case "${1:-help}" in
        "build")
            check_kotlin
            if check_gradle; then
                build_with_gradle
            else
                manual_compile
            fi
            ;;
        "run")
            if [ $# -lt 2 ]; then
                print_error "CSV file argument required for run command"
                print_info "Usage: ./kt.sh run <csv-file> [options]"
                exit 1
            fi
            check_kotlin
            shift # Remove 'run' from arguments
            run_analyzer "$@"
            ;;
        "test")
            check_kotlin
            run_tests
            ;;
        "clean")
            clean_build
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
    

