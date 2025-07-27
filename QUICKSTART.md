# 🚀 Quick Start Guide

## Prerequisites

- Kotlin 1.9.20+
- JDK 11+
- CSV file with product data

## Quick Setup

```bash
# 1. Make script executable
chmod +x kt.sh

# 2. Build the project
./kt.sh build

# 3. Run analysis
./kt.sh run product_database.csv
```

## Examples

### Basic Analysis

```bash
./kt.sh run product_database.csv
```

### Generate Reports

```bash
# Markdown report
./kt.sh run product_database.csv --format markdown --output report.md

# CSV for Excel
./kt.sh run product_database.csv --format csv --output issues.csv
```

### Filter Issues

```bash
# Critical issues only
./kt.sh run product_database.csv --severity critical

# Cost-related issues
./kt.sh run product_database.csv --type cost
```

## Expected Output

- ✅ 99.5% quality score for the sample dataset
- 🔍 425 dead stock issues identified
- 📊 Comprehensive reports in multiple formats

## Troubleshooting

- **Build fails**: Check Java/Kotlin versions
- **CSV errors**: Verify file format and headers
- **Memory issues**: Use `export JAVA_OPTS="-Xmx4g"`
